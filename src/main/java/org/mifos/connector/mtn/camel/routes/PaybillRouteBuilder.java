package org.mifos.connector.mtn.camel.routes;

import static org.apache.camel.Exchange.CONTENT_TYPE;
import static org.apache.camel.Exchange.HTTP_RESPONSE_CODE;
import static org.mifos.connector.mtn.camel.config.CamelProperties.ACCOUNT_HOLDING_INSTITUTION_ID;
import static org.mifos.connector.mtn.camel.config.CamelProperties.AMS_NAME;
import static org.mifos.connector.mtn.camel.config.CamelProperties.AMS_URL;
import static org.mifos.connector.mtn.camel.config.CamelProperties.BRIDGE_ENDPOINT_QUERY_PARAM;
import static org.mifos.connector.mtn.camel.config.CamelProperties.CHANNEL_VALIDATION_RESPONSE;
import static org.mifos.connector.mtn.camel.config.CamelProperties.CONFIRMATION_REQUEST_BODY;
import static org.mifos.connector.mtn.camel.config.CamelProperties.CORRELATION_ID_HEADER;
import static org.mifos.connector.mtn.camel.config.CamelProperties.PLATFORM_TENANT_ID;
import static org.mifos.connector.mtn.camel.config.CamelProperties.PRIMARY_IDENTIFIER;
import static org.mifos.connector.mtn.camel.config.CamelProperties.PRIMARY_IDENTIFIER_VALUE;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.EXTERNAL_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.MTN_PAYMENT_COMPLETED;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.MTN_PAYMENT_COMPLETION_RESPONSE;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TENANT_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TRANSACTION_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TRANSFER_SETTLEMENT_FAILED;

import java.util.List;
import javax.xml.bind.UnmarshalException;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.bean.validator.BeanValidationException;
import org.mifos.connector.common.channel.dto.TransactionStatusResponseDTO;
import org.mifos.connector.common.mojaloop.type.TransferState;
import org.mifos.connector.mtn.dto.ChannelValidationRequest;
import org.mifos.connector.mtn.dto.ChannelValidationResponse;
import org.mifos.connector.mtn.dto.ErrorResponse;
import org.mifos.connector.mtn.dto.FinancialResourceInformationRequest;
import org.mifos.connector.mtn.dto.FinancialResourceInformationResponse;
import org.mifos.connector.mtn.dto.PaybillPaymentRequest;
import org.mifos.connector.mtn.dto.PaybillPaymentResponse;
import org.mifos.connector.mtn.dto.PaybillProps;
import org.mifos.connector.mtn.dto.PaymentCompletedRequest;
import org.mifos.connector.mtn.dto.PaymentCompletedResponse;
import org.mifos.connector.mtn.dto.WorkflowResponse;
import org.mifos.connector.mtn.flowcomponents.state.PaybillConfirmationProcessor;
import org.mifos.connector.mtn.flowcomponents.state.WorkflowInstanceStore;
import org.mifos.connector.mtn.utility.ConnectionUtils;
import org.mifos.connector.mtn.utility.MtnUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Route handlers for Paybill flow.
 */
@Component
@Slf4j
public class PaybillRouteBuilder extends RouteBuilder {

    private final PaybillConfirmationProcessor paybillConfirmationProcessor;
    private final PaybillProps paybillProps;
    private final String channelUrl;
    private final Integer mtnTimeout;
    private final WorkflowInstanceStore workflowInstanceStore;

    public PaybillRouteBuilder(PaybillConfirmationProcessor paybillConfirmationProcessor, PaybillProps paybillProps,
            @Value("${channel.host}") String channelUrl, @Value("${mtn.api.timeout}") Integer mtnTimeout,
            WorkflowInstanceStore workflowInstanceStore) {
        this.paybillConfirmationProcessor = paybillConfirmationProcessor;
        this.paybillProps = paybillProps;
        this.channelUrl = channelUrl;
        this.mtnTimeout = mtnTimeout;
        this.workflowInstanceStore = workflowInstanceStore;
    }

    @Override
    public void configure() throws Exception {

        rest("/paybill").post("/getfinancialresourceinformation").consumes(MediaType.APPLICATION_XML_VALUE)
                .produces(MediaType.APPLICATION_XML_VALUE).type(FinancialResourceInformationRequest.class)
                .outType(FinancialResourceInformationResponse.class).to("direct:paybill-validation")

                .post("/payment").consumes(MediaType.APPLICATION_XML_VALUE).produces(MediaType.APPLICATION_XML_VALUE)
                .type(PaybillPaymentRequest.class).outType(PaybillPaymentResponse.class)
                .to("direct:paybill-confirmation-base");

        onException(UnmarshalException.class).handled(true).process(exchange -> {
            UnmarshalException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT, UnmarshalException.class);
            String message = exception != null ? exception.getMessage() : "Invalid payload";
            exchange.getIn().setBody(new ErrorResponse(message, null));
            exchange.getIn().setHeader(HTTP_RESPONSE_CODE, 400);
            exchange.getIn().setHeader(CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
        });

        onException(BeanValidationException.class).handled(true).process(exchange -> {
            BeanValidationException exception = exchange.getProperty(Exchange.EXCEPTION_CAUGHT,
                    BeanValidationException.class);
            List<String> errors = exception.getConstraintViolations().stream()
                    .map(v -> v.getPropertyPath() + " " + v.getMessage()).toList();
            exchange.getIn().setBody(new ErrorResponse("Errors exist in the request body", errors));
            exchange.getIn().setHeader(HTTP_RESPONSE_CODE, 400);
        });

        onException(Exception.class).handled(true).log(LoggingLevel.ERROR, "Caught exception: ${exception.stacktrace}")
                .process(exchange -> {
                    ErrorResponse response = new ErrorResponse("An error occurred while processing the request", null);
                    exchange.getIn().setBody(response);
                    exchange.getIn().setHeader(HTTP_RESPONSE_CODE, 500);
                    exchange.getIn().setHeader(CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
                });

        from("direct:paybill-validation").id("paybill-validation").to("bean-validator:validation-request-validator")
                .process(exchange -> {
                    exchange.setProperty(TRANSACTION_ID, MtnUtils.generateWorkflowId());
                })
                .log("Received mtn validation request with generated transaction id: ${exchangeProperty."
                        + TRANSACTION_ID + "}, body: ${body} ")
                .to("direct:account-status")
                .log("Response from account status check for transaction id " + "${exchangeProperty." + TRANSACTION_ID
                        + "}: ${body}")
                .unmarshal().json(ChannelValidationResponse.class).choice().when(simple("${body.reconciled} == 'true'"))
                .to("direct:start-paybill-workflow").to("direct:paybill-validation-response-success").otherwise()
                .to("direct:paybill-validation-response-failure").end();

        from("direct:account-status").id("account-status").setBody(exchange -> {
            exchange.getIn().removeHeaders("*");
            exchange.getIn().setHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
            exchange.getIn().setHeader(AMS_URL, paybillProps.getAmsUrl());
            exchange.getIn().setHeader(AMS_NAME, paybillProps.getAmsName());
            exchange.getIn().setHeader(ACCOUNT_HOLDING_INSTITUTION_ID, paybillProps.getAccountHoldingInstitutionId());
            exchange.setProperty(PRIMARY_IDENTIFIER, paybillProps.getAmsIdentifier());
            FinancialResourceInformationRequest request = exchange.getIn()
                    .getBody(FinancialResourceInformationRequest.class);
            exchange.setProperty(PRIMARY_IDENTIFIER_VALUE, request.getResource());
            return ChannelValidationRequest.fromPaybillValidation(request, paybillProps,
                    exchange.getProperty(TRANSACTION_ID, String.class));
        }).marshal().json()
                .toD(channelUrl + "/accounts/validate/${header.primaryIdentifier}/${header.primaryIdentifierValue}"
                        + BRIDGE_ENDPOINT_QUERY_PARAM);

        from("direct:start-paybill-workflow").id("start-paybill-workflow")
                .log("Starting paybill workflow for transaction id ${exchangeProperty.transactionId}")
                .setBody(exchange -> {
                    ChannelValidationResponse validationResponse = exchange.getIn()
                            .getBody(ChannelValidationResponse.class);
                    exchange.getIn().removeHeaders("*");
                    exchange.getIn().setHeader(ACCOUNT_HOLDING_INSTITUTION_ID,
                            validationResponse.accountHoldingInstitutionId());
                    exchange.getIn().setHeader(AMS_NAME, validationResponse.amsName());
                    exchange.getIn().setHeader(TENANT_ID, validationResponse.accountHoldingInstitutionId());
                    exchange.getIn().setHeader(CORRELATION_ID_HEADER, validationResponse.transactionId());
                    exchange.getIn().setHeader(CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE);
                    exchange.setProperty(CHANNEL_VALIDATION_RESPONSE, validationResponse);
                    String primaryIdentifier = exchange.getProperty(PRIMARY_IDENTIFIER, String.class);
                    String primaryIdentifierValue = exchange.getProperty(PRIMARY_IDENTIFIER_VALUE, String.class);
                    String timer = paybillProps.getTimer();
                    return MtnUtils.createGsmaTransferRequest(validationResponse, primaryIdentifier,
                            primaryIdentifierValue, timer);
                }).marshal().json().toD(channelUrl + "/channel/gsma/transaction" + BRIDGE_ENDPOINT_QUERY_PARAM)
                .log("Paybill workflow response from channel for transaction id "
                        + "${exchangeProperty.transactionId}: ${body}");

        from("direct:paybill-validation-response-success").id("paybill-validation-response-success").unmarshal()
                .json(WorkflowResponse.class).setBody(exchange -> {
                    WorkflowResponse workflowResponse = exchange.getIn().getBody(WorkflowResponse.class);
                    if (workflowResponse == null || workflowResponse.transactionId() == null
                            || workflowResponse.transactionId().isBlank()
                            || workflowResponse.transactionId().trim().equalsIgnoreCase("null")) {
                        String transactionId = exchange.getProperty(TRANSACTION_ID, String.class);
                        String errorMessage = "Failed to start paybill workflow for transaction id " + transactionId;
                        log.error(errorMessage + ". Response from channel is {}", workflowResponse);
                        throw new RuntimeException(errorMessage);
                    }
                    ChannelValidationResponse validationResponse = exchange.getProperty(CHANNEL_VALIDATION_RESPONSE,
                            ChannelValidationResponse.class);
                    workflowInstanceStore.put(validationResponse.transactionId(), workflowResponse.transactionId());
                    FinancialResourceInformationResponse.Extension extension = new FinancialResourceInformationResponse.Extension();
                    FinancialResourceInformationResponse response = new FinancialResourceInformationResponse();
                    extension.setAccountName(validationResponse.clientName());
                    extension.setOafReference(validationResponse.transactionId());
                    response.setExtension(extension);
                    response.setMessage("completed");
                    return response;
                }).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(200))
                .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_XML_VALUE));

        from("direct:paybill-validation-response-failure").id("paybill-validation-response-failure")
                .setBody(exchange -> {
                    FinancialResourceInformationResponse response = new FinancialResourceInformationResponse();
                    response.setMessage("completed");
                    return response;
                }).setHeader(Exchange.HTTP_RESPONSE_CODE, constant(404))
                .setHeader(Exchange.CONTENT_TYPE, constant(MediaType.APPLICATION_XML_VALUE));

        from("direct:paybill-confirmation-base").id("paybill-confirmation-base")
                .to("bean-validator:confirmation-request-validator")
                .log("Received mtn confirmation request with transaction id: ${body.oafReference}," + " body: ${body} ")
                .setProperty(CONFIRMATION_REQUEST_BODY, simple("${body}"))
                .to("direct:paybill-transaction-status-check-base").choice().when(exchange -> {
                    TransactionStatusResponseDTO transactionStatusResponse = exchange.getIn()
                            .getBody(TransactionStatusResponseDTO.class);
                    return transactionStatusResponse != null
                            && TransferState.COMMITTED.equals(transactionStatusResponse.getTransferState());
                }).to("direct:paybill-confirmation-response-success").otherwise().to("direct:paybill-confirmation")
                .process(exchange -> {
                    PaybillPaymentRequest paymentRequest = exchange.getProperty(CONFIRMATION_REQUEST_BODY,
                            PaybillPaymentRequest.class);
                    PaybillPaymentResponse response = new PaybillPaymentResponse();
                    response.setStatus("PENDING");
                    response.setProviderTransactionId(paymentRequest.getOafReference());
                    exchange.getIn().setHeader(HTTP_RESPONSE_CODE, 202);
                    exchange.getIn().setHeader(CONTENT_TYPE, MediaType.APPLICATION_XML_VALUE);
                    exchange.getIn().setBody(response);
                }).end();

        from("direct:paybill-transaction-status-check-base").id("paybill-transaction-status-check-base")
                .process(exchange -> {
                    PaybillPaymentRequest paymentRequest = exchange.getProperty(CONFIRMATION_REQUEST_BODY,
                            PaybillPaymentRequest.class);
                    exchange.setProperty(TRANSACTION_ID, paymentRequest.getOafReference());
                    exchange.getIn().removeHeaders("*");
                    exchange.getIn().setBody(null);
                    exchange.getIn().setHeader(PLATFORM_TENANT_ID, paybillProps.getAccountHoldingInstitutionId());
                    exchange.getIn().setHeader("requestType", "transfers");
                }).toD(channelUrl + "/channel/transfer/${header.transactionId}" + BRIDGE_ENDPOINT_QUERY_PARAM)
                .log("Received status response for transaction ${header.transactionId}: ${body}").unmarshal()
                .json(TransactionStatusResponseDTO.class);

        from("direct:paybill-confirmation-response-success").id("paybill-confirmation-response-success")
                .setHeader(HTTP_RESPONSE_CODE, constant(200))
                .setHeader(CONTENT_TYPE, constant(MediaType.APPLICATION_XML_VALUE)).setBody(exchange -> {
                    PaybillPaymentResponse response = new PaybillPaymentResponse();
                    response.setProviderTransactionId(exchange.getProperty(TRANSACTION_ID, String.class));
                    response.setStatus("COMPLETED");
                    return response;
                });

        from("direct:paybill-confirmation").id("paybill-confirmation").process(paybillConfirmationProcessor);

        from("direct:mtn-payment-completion").id("mtn-payment-completion").setBody(exchange -> {
            PaymentCompletedRequest request = new PaymentCompletedRequest();
            request.setProviderTransactionId(exchange.getProperty(TRANSACTION_ID, String.class));
            request.setTransactionId(exchange.getProperty(EXTERNAL_ID, String.class));
            Boolean settlementFailed = exchange.getProperty(TRANSFER_SETTLEMENT_FAILED, Boolean.class);
            request.setStatus(Boolean.FALSE.equals(settlementFailed) ? "COMPLETED" : "FAILED");
            return request;
        }).log("Sending mtn payment completed request with transaction id: ${exchangeProperty.transactionId}, "
                + "body: ${body} ").setHeader(CONTENT_TYPE, constant(MediaType.APPLICATION_XML_VALUE))
                .setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .toD(paybillProps.getPaymentCompletedUrl() + BRIDGE_ENDPOINT_QUERY_PARAM + "&"
                        + ConnectionUtils.getConnectionTimeoutDsl(mtnTimeout) + "&authUsername="
                        + paybillProps.getUsername() + "&authPassword=" + paybillProps.getPassword())
                .log("Received payment completed response ${header.CamelHttpResponseCode} for transaction"
                        + " ${header.transactionId}: ${body}")
                .setProperty(MTN_PAYMENT_COMPLETION_RESPONSE, simple("${body}")).process(exchange -> {
                    try {
                        PaymentCompletedResponse response = exchange.getIn().getBody(PaymentCompletedResponse.class);
                        if (response != null && response.getPaymentStatus() != null
                                && "COMPLETED".equalsIgnoreCase(response.getPaymentStatus().trim())) {
                            exchange.setProperty(MTN_PAYMENT_COMPLETED, true);
                        } else {
                            exchange.setProperty(MTN_PAYMENT_COMPLETED, false);
                        }
                    } catch (Exception e) {
                        exchange.setProperty(MTN_PAYMENT_COMPLETED, false);
                    }
                }).end();

    }
}
