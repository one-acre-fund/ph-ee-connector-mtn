package org.mifos.connector.mtn.flowcomponents.state;

import static org.mifos.connector.common.zeebe.ZeebeVariables.CHANNEL_REQUEST;
import static org.mifos.connector.common.zeebe.ZeebeVariables.ORIGIN_DATE;
import static org.mifos.connector.mtn.camel.config.CamelProperties.CONFIRMATION_REQUEST_BODY;
import static org.mifos.connector.mtn.camel.config.CamelProperties.CORRELATION_ID;
import static org.mifos.connector.mtn.camel.config.CamelProperties.MTN_PAYBILL_WORKFLOW_SUBTYPE;
import static org.mifos.connector.mtn.camel.config.CamelProperties.MTN_PAYBILL_WORKFLOW_TYPE;
import static org.mifos.connector.mtn.utility.MtnUtils.getWorkflowId;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.CLIENT_CORRELATION_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.CONFIRMATION_RECEIVED;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.CONFIRMATION_TIMER;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.EXTERNAL_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.PARTY_LOOKUP_FAILED;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.PENDING_CONFIRMATION_MESSAGE_NAME;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TRANSACTION_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TRANSFER_CREATE_FAILED;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.mifos.connector.mtn.dto.ChannelConfirmationRequest;
import org.mifos.connector.mtn.dto.PaybillPaymentRequest;
import org.mifos.connector.mtn.dto.PaybillProps;
import org.mifos.connector.mtn.utility.MtnUtils;
import org.springframework.stereotype.Component;

/**
 * Processor for handling paybill confirmation.
 */
@Component
@Slf4j
public class PaybillConfirmationProcessor implements Processor {

    private final ZeebeClient zeebeClient;
    private final PaybillProps paybillProps;
    private final WorkflowInstanceStore workflowInstanceStore;

    public PaybillConfirmationProcessor(ZeebeClient zeebeClient, PaybillProps paybillProps,
            WorkflowInstanceStore workflowInstanceStore) {
        this.zeebeClient = zeebeClient;
        this.paybillProps = paybillProps;
        this.workflowInstanceStore = workflowInstanceStore;
    }

    @Override
    public void process(Exchange exchange) throws Exception {
        PaybillPaymentRequest paymentRequest = exchange.getProperty(CONFIRMATION_REQUEST_BODY,
                PaybillPaymentRequest.class);
        String workflowTransactionId = workflowInstanceStore.get(paymentRequest.getOafReference());
        ChannelConfirmationRequest channelConfirmationRequest = ChannelConfirmationRequest
                .fromPaybillConfirmation(paymentRequest, paybillProps);
        Map<String, Object> variables = new HashMap<>();
        variables.put(CONFIRMATION_RECEIVED, true);
        variables.put(CHANNEL_REQUEST, channelConfirmationRequest.toString());
        variables.put(paybillProps.getAmsIdentifier(), paymentRequest.getReceivingFri());
        variables.put(TRANSACTION_ID, paymentRequest.getOafReference());
        variables.put(CORRELATION_ID, workflowTransactionId);
        variables.put(EXTERNAL_ID, paymentRequest.getTransactionId());
        variables.put(TRANSFER_CREATE_FAILED, false);
        variables.put("phoneNumber", paymentRequest.getAccountHolderId());
        variables.put("amount", paymentRequest.getAmount().getAmount());
        if (workflowTransactionId != null) {
            zeebeClient.newPublishMessageCommand().messageName(PENDING_CONFIRMATION_MESSAGE_NAME)
                    .correlationKey(workflowTransactionId).timeToLive(Duration.ofMillis(300)).variables(variables)
                    .send();
            workflowInstanceStore.remove(paymentRequest.getOafReference());
        } else {
            workflowTransactionId = MtnUtils.generateWorkflowId();
            variables.put(CLIENT_CORRELATION_ID, workflowTransactionId);
            variables.put(CORRELATION_ID, workflowTransactionId);
            variables.put(ORIGIN_DATE, Instant.now().toEpochMilli());
            variables.put(CONFIRMATION_TIMER, paybillProps.getTimer());
            variables.put(PARTY_LOOKUP_FAILED, paymentRequest.getOafReference() == null);
            String workflowId = getWorkflowId(MTN_PAYBILL_WORKFLOW_TYPE, MTN_PAYBILL_WORKFLOW_SUBTYPE,
                    paybillProps.getAmsName(), paybillProps.getAccountHoldingInstitutionId());
            ProcessInstanceEvent instance = zeebeClient.newCreateInstanceCommand().bpmnProcessId(workflowId)
                    .latestVersion().variables(variables).send().join();
            log.info(
                    "New workflow instance from process {} started for transaction {}, with correlationId {}, "
                            + "instance key: {}",
                    workflowId, paymentRequest.getOafReference(), workflowTransactionId,
                    instance.getProcessInstanceKey());
            zeebeClient.newPublishMessageCommand().messageName(PENDING_CONFIRMATION_MESSAGE_NAME)
                    .correlationKey(workflowTransactionId).timeToLive(Duration.ofMillis(300)).variables(variables)
                    .send();
        }
    }
}
