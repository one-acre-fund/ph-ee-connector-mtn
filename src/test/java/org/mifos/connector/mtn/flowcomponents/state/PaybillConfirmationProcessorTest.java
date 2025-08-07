package org.mifos.connector.mtn.flowcomponents.state;

import static org.mifos.connector.mtn.camel.config.CamelProperties.CONFIRMATION_REQUEST_BODY;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.PENDING_CONFIRMATION_MESSAGE_NAME;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.CreateProcessInstanceCommandStep1;
import io.camunda.zeebe.client.api.command.PublishMessageCommandStep1;
import io.camunda.zeebe.client.api.response.ProcessInstanceEvent;
import java.math.BigDecimal;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mifos.connector.mtn.dto.PaybillPaymentRequest;
import org.mifos.connector.mtn.dto.PaybillProps;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

/**
 * Test class for {@link PaybillConfirmationProcessor}.
 */
@ExtendWith(MockitoExtension.class)
class PaybillConfirmationProcessorTest {

    @Mock
    ZeebeClient zeebeClient;
    @Mock
    PaybillProps paybillProps;
    @Mock
    WorkflowInstanceStore workflowInstanceStore;
    @Mock
    Exchange exchange;

    @Mock
    PublishMessageCommandStep1 publishMessageCommandStep1;
    @Mock
    PublishMessageCommandStep1.PublishMessageCommandStep2 publishMessageCommandStep2;
    @Mock
    PublishMessageCommandStep1.PublishMessageCommandStep3 publishMessageCommandStep3;
    @Mock
    CreateProcessInstanceCommandStep1 createProcessInstanceCommandStep1;
    @Mock
    CreateProcessInstanceCommandStep1.CreateProcessInstanceCommandStep2 createProcessInstanceCommandStep2;
    @Mock
    CreateProcessInstanceCommandStep1.CreateProcessInstanceCommandStep3 createProcessInstanceCommandStep3;
    @Mock
    ZeebeFuture<ProcessInstanceEvent> zeebeFuture;
    @Mock
    ProcessInstanceEvent processInstanceEvent;

    PaybillConfirmationProcessor processor;

    @Test
    void process_whenWorkflowIdNotExists_startsNewProcessInstance() throws Exception {
        processor = new PaybillConfirmationProcessor(zeebeClient, paybillProps, workflowInstanceStore);
        PaybillPaymentRequest paymentRequest = createTestPaybillPaymentRequest();
        when(exchange.getProperty(eq(CONFIRMATION_REQUEST_BODY), eq(PaybillPaymentRequest.class)))
                .thenReturn(paymentRequest);
        when(zeebeClient.newCreateInstanceCommand()).thenReturn(createProcessInstanceCommandStep1);
        when(createProcessInstanceCommandStep1.bpmnProcessId(anyString()))
                .thenReturn(createProcessInstanceCommandStep2);
        when(createProcessInstanceCommandStep2.latestVersion()).thenReturn(createProcessInstanceCommandStep3);
        when(createProcessInstanceCommandStep3.variables(anyMap())).thenReturn(createProcessInstanceCommandStep3);
        when(createProcessInstanceCommandStep3.send()).thenReturn(zeebeFuture);
        when(zeebeFuture.join()).thenReturn(processInstanceEvent);
        when(processInstanceEvent.getProcessInstanceKey()).thenReturn(123456789L);

        when(zeebeClient.newPublishMessageCommand()).thenReturn(publishMessageCommandStep1);
        when(publishMessageCommandStep1.messageName(eq(PENDING_CONFIRMATION_MESSAGE_NAME)))
                .thenReturn(publishMessageCommandStep2);
        when(publishMessageCommandStep2.correlationKey(anyString())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.timeToLive(any())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.variables(anyMap())).thenReturn(publishMessageCommandStep3);

        processor.process(exchange);

        verify(zeebeClient, times(1)).newCreateInstanceCommand();
        verify(zeebeClient, times(1)).newPublishMessageCommand();
    }

    @Test
    void process_whenWorkflowIdExists_publishesZeebeMessage() throws Exception {
        processor = new PaybillConfirmationProcessor(zeebeClient, paybillProps, workflowInstanceStore);
        PaybillPaymentRequest paymentRequest = createTestPaybillPaymentRequest();
        when(workflowInstanceStore.get(anyString())).thenReturn("grtgthr454532");
        when(exchange.getProperty(eq(CONFIRMATION_REQUEST_BODY), eq(PaybillPaymentRequest.class)))
                .thenReturn(paymentRequest);
        when(zeebeClient.newPublishMessageCommand()).thenReturn(publishMessageCommandStep1);
        when(publishMessageCommandStep1.messageName(eq(PENDING_CONFIRMATION_MESSAGE_NAME)))
                .thenReturn(publishMessageCommandStep2);
        when(publishMessageCommandStep2.correlationKey(anyString())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.timeToLive(any())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.variables(anyMap())).thenReturn(publishMessageCommandStep3);

        processor.process(exchange);

        verify(zeebeClient, times(1)).newPublishMessageCommand();
        verify(workflowInstanceStore).remove(paymentRequest.getExtension().getOafReference());
    }

    private PaybillPaymentRequest createTestPaybillPaymentRequest() {
        PaybillPaymentRequest paymentRequest = new PaybillPaymentRequest();
        paymentRequest.setTransactionId("12345");
        paymentRequest.setExtension(new PaybillPaymentRequest.Extension());
        paymentRequest.getExtension().setOafReference("try564rttt");
        paymentRequest.setReceivingFri("FRI123");
        PaybillPaymentRequest.Amount amount = new PaybillPaymentRequest.Amount();
        amount.setAmount(BigDecimal.TEN);
        amount.setCurrency("RWF");
        paymentRequest.setAmount(amount);
        return paymentRequest;
    }
}
