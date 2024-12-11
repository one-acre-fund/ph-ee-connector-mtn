package org.mifos.connector.mtn.camel.routes;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mifos.connector.mtn.camel.config.CamelProperties.ACCESS_TOKEN;
import static org.mifos.connector.mtn.camel.config.CamelProperties.BUY_GOODS_REQUEST_BODY;
import static org.mifos.connector.mtn.camel.config.CamelProperties.CORRELATION_ID;
import static org.mifos.connector.mtn.camel.config.CamelProperties.ERROR_DESCRIPTION;
import static org.mifos.connector.mtn.camel.config.CamelProperties.IS_RETRY_EXCEEDED;
import static org.mifos.connector.mtn.camel.config.CamelProperties.IS_TRANSACTION_PENDING;
import static org.mifos.connector.mtn.camel.config.CamelProperties.LAST_RESPONSE_BODY;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.CALLBACK_RECEIVED;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.FINANCIAL_TRANSACTION_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.SERVER_TRANSACTION_STATUS_RETRY_COUNT;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TRANSACTION_FAILED;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.camel.CamelContext;
import org.apache.camel.EndpointInject;
import org.apache.camel.Exchange;
import org.apache.camel.FluentProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.component.mock.MockEndpoint;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.mtn.MtnConnectorApplicationTests;
import org.mifos.connector.mtn.auth.AccessTokenStore;
import org.mifos.connector.mtn.dto.MtnCallback;
import org.mifos.connector.mtn.dto.Payer;
import org.mifos.connector.mtn.dto.PaymentRequestDto;
import org.mifos.connector.mtn.utility.ConnectionUtils;
import org.mifos.connector.mtn.utility.MtnProps;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.annotation.DirtiesContext;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class MtnRouteBuilderTest extends MtnConnectorApplicationTests {

    @Autowired
    private CamelContext camelContext;

    @Autowired
    private FluentProducerTemplate fluentProducerTemplate;

    @EndpointInject("mock:mockGet-access-token")
    protected MockEndpoint mockGetAccessToken;

    @EndpointInject("mock:mockMtn-transaction-response-handler")
    protected MockEndpoint mockMtnTransactionResponseHandler;

    @EndpointInject("mock:mockRequestToPay")
    protected MockEndpoint mockRequestToPay;

    @EndpointInject("mock:mockAmsConfirmationEndpoint")
    protected MockEndpoint mockApiEndpoint;

    @EndpointInject("mock:transactionResponseProcessor")
    protected MockEndpoint mockProcessor;

    @EndpointInject("mock:callBackResponseProcessor")
    protected MockEndpoint mockCallBackProcessor;

    @EndpointInject("mock:transactionStatusResponseHandler")
    protected MockEndpoint mockTransactionStatusResponseHandler;

    @EndpointInject("mock:mockTransactionStatus")
    protected MockEndpoint mockTransactionStatus;

    @EndpointInject("mock:mtn-transaction-status-failure-processor")
    protected MockEndpoint mockTransactionStatusFailureProcessor;

    @MockBean
    AccessTokenStore accessTokenStore;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Autowired
    MtnProps mtnProps;

    @Value("${mtn.api.timeout}")
    private Integer mtnTimeout;

    @DisplayName("Test initiate payment flow and retrieve access token, and sends it to the transaction "
            + "response handler")
    @Test
    void test_initiate_payment_flow_and_retrieve_access_token_sends_token_to_the_handler() throws Exception {

        AdviceWith.adviceWith(camelContext, "request-to-pay-base", routeBuilder -> {
            routeBuilder.weaveByToUri("direct:get-access-token").replace().to(mockGetAccessToken);
            routeBuilder.weaveByToUri("direct:mtn-transaction-response-handler").replace()
                    .to(mockMtnTransactionResponseHandler);
        });
        when(accessTokenStore.getAccessToken()).thenReturn("valid-token");

        mockMtnTransactionResponseHandler.expectedMessageCount(1);
        mockMtnTransactionResponseHandler.expectedMessagesMatches(exchange -> {
            String accessTokenProperty = exchange.getProperty(ACCESS_TOKEN, String.class);
            return "valid-token".equals(accessTokenProperty);
        });

        camelContext.start();
        fluentProducerTemplate.to("direct:request-to-pay-base").request(Exchange.class);

        mockMtnTransactionResponseHandler.assertIsSatisfied();

    }

    @DisplayName("Test request to pay route sets headers and body")
    @Test
    void test_request_to_pay_route_sets_headers_and_body() throws Exception {
        AdviceWith.adviceWith(camelContext, "request-to-pay", routeBuilder -> {
            routeBuilder.weaveByToUri(mtnProps.getApiHost() + "/collection/v1_0/requesttopay"
                    + "?bridgeEndpoint=true&throwExceptionOnFailure=false&"
                    + ConnectionUtils.getConnectionTimeoutDsl(mtnTimeout)).replace().to(mockApiEndpoint);
        });

        // Create a mock PaymentRequestDto
        PaymentRequestDto mockRequestBody = new PaymentRequestDto();
        mockRequestBody.setExternalId("12345");
        mockRequestBody.setAmount("1000");
        mockRequestBody.setCurrency("RWF");
        mockRequestBody.setPayerMessage("test-payer-message");
        mockRequestBody.setPayeeNote("test-payee-note");
        mockRequestBody.setPayer(new Payer("testPartyIdType", "testPartyId"));

        Exchange exchange = camelContext.getEndpoint("direct:request-to-pay").createExchange();
        exchange.setProperty(CORRELATION_ID, "test-correlation-id");
        exchange.setProperty(ACCESS_TOKEN, "test-access-token");
        exchange.setProperty(BUY_GOODS_REQUEST_BODY, mockRequestBody);

        mockApiEndpoint.expectedMessageCount(1);
        mockApiEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "POST");
        mockApiEndpoint.expectedHeaderReceived("Content-Type", "application/json");
        mockApiEndpoint.expectedHeaderReceived("Ocp-Apim-Subscription-Key", mtnProps.getSubscriptionKey());
        mockApiEndpoint.expectedHeaderReceived("X-Callback-Url", mtnProps.getCallBack());
        mockApiEndpoint.expectedHeaderReceived("X-Reference-Id", "test-correlation-id");
        mockApiEndpoint.expectedHeaderReceived("X-Target-Environment", mtnProps.getEnvironment());
        mockApiEndpoint.expectedHeaderReceived("Authorization", "Bearer test-access-token");

        Exchange result = fluentProducerTemplate.to("direct:request-to-pay").withExchange(exchange).send();
        Exchange exchangeResponse = mockApiEndpoint.getExchanges().get(0);
        String receivedBody = exchangeResponse.getIn().getBody(String.class);

        mockApiEndpoint.assertIsSatisfied();
        assertNotNull(result);
        assertTrue(receivedBody.contains("\"currency\":\"RWF\""));
        assertTrue(receivedBody.contains("\"payerMessage\":\"test-payer-message\""));
        assertTrue(receivedBody.contains("\"amount\":\"1000\""));
        assertTrue(receivedBody.contains("\"externalId\":\"12345\""));
        assertTrue(receivedBody.contains("\"payeeNote\":\"test-payee-note\""));
        assertTrue(receivedBody.contains("\"partyId\":\"testPartyId\""));
        assertTrue(receivedBody.contains("\"partyIdType\":\"testPartyIdType\""));
    }

    @DisplayName("Test transaction response handler success")
    @Test
    void test_transaction_response_handler_success() throws Exception {
        // Advice the route to replace the success processor with a mock
        AdviceWith.adviceWith(camelContext, "mtn-transaction-response-handler", routeBuilder -> {
            routeBuilder.weaveById("success-processor").replace().to(mockProcessor);
        });

        // Set up the mock expectations
        mockProcessor.expectedMessageCount(1);
        mockProcessor.expectedPropertyReceived(TRANSACTION_FAILED, null);

        // Send a mock exchange to the route
        fluentProducerTemplate.to("direct:mtn-transaction-response-handler")
                .withHeader(Exchange.HTTP_RESPONSE_CODE, 202).withBody("Success Response Body").send();

        // Assertions
        mockProcessor.assertIsSatisfied();
    }

    @DisplayName("Test transaction response handler failure")
    @Test
    void test_transaction_response_handler_failure() throws Exception {
        // Advice the route to replace the failure processor with a mock
        AdviceWith.adviceWith(camelContext, "mtn-transaction-response-handler", routeBuilder -> {
            routeBuilder.weaveById("failure-processor").replace().to(mockProcessor);
        });

        // Set up the mock expectations
        mockProcessor.expectedMessageCount(1);
        mockProcessor.expectedPropertyReceived(TRANSACTION_FAILED, true);

        // Send a mock exchange to the route
        fluentProducerTemplate.to("direct:mtn-transaction-response-handler")
                .withHeader(Exchange.HTTP_RESPONSE_CODE, 400).withHeader(Exchange.HTTP_RESPONSE_TEXT, "Bad Request")
                .withBody("Failure Response Body").send();

        // Assertions
        mockProcessor.assertIsSatisfied();
    }

    @DisplayName("Test callback handler success")
    @Test
    void test_successful_callback_handling() throws Exception {
        // Advice the route to replace the final processor with a mock using weaveById
        AdviceWith.adviceWith(camelContext, "mtn-callback-handler", routeBuilder -> {
            routeBuilder.weaveById("callback-processor").replace().to(mockCallBackProcessor);
        });

        // Set up the mock expectations
        mockCallBackProcessor.whenAnyExchangeReceived(exchange -> {
            // Assert properties directly
            Assertions.assertEquals(false, exchange.getProperty(TRANSACTION_FAILED));
            Assertions.assertEquals(true, exchange.getProperty(CALLBACK_RECEIVED));
            Assertions.assertEquals("test-external-id", exchange.getProperty(CORRELATION_ID));
        });

        // Prepare a sample callback body
        MtnCallback callback = new MtnCallback();
        callback.setExternalId("test-external-id");
        callback.setStatus("SUCCESSFUL");
        callback.setFinancialTransactionId("test-transaction-id");

        String callbackBody = objectMapper.writeValueAsString(callback);

        // Send the exchange to the route
        fluentProducerTemplate.to("direct:mtn-callback-handler").withBody(callbackBody).send();

        // Assertions
        mockCallBackProcessor.assertIsSatisfied();
    }

    @DisplayName("Test callback handler failure")
    @Test
    void test_failure_callback_handling() throws Exception {
        // Advice the route to replace the final processor with a mock using weaveById
        AdviceWith.adviceWith(camelContext, "mtn-callback-handler", routeBuilder -> {
            routeBuilder.weaveById("callback-processor").replace().to(mockCallBackProcessor);
        });

        // Set up the mock expectations
        mockCallBackProcessor.whenAnyExchangeReceived(exchange -> {
            // Assert properties directly
            Assertions.assertEquals(true, exchange.getProperty(TRANSACTION_FAILED));
            Assertions.assertNull(exchange.getProperty(CALLBACK_RECEIVED));
            Assertions.assertEquals("test-external-id", exchange.getProperty(CORRELATION_ID));
        });

        // Prepare a sample callback body
        MtnCallback callback = new MtnCallback();
        callback.setExternalId("test-external-id");
        callback.setStatus("ERROR");
        callback.setFinancialTransactionId("test-transaction-id");
        String callbackBody = objectMapper.writeValueAsString(callback);

        // Send the exchange to the route
        fluentProducerTemplate.to("direct:mtn-callback-handler").withBody(callbackBody).send();

        // Assertions
        mockCallBackProcessor.assertIsSatisfied();
    }

    @DisplayName("Test transaction status retry within limit")
    @Test
    void test_transaction_status_retry_within_limit() throws Exception {
        // Advice the route to replace endpoints with mocks
        AdviceWith.adviceWith(camelContext, "mtn-buy-goods-get-transaction-status-base", routeBuilder -> {
            routeBuilder.weaveByToUri("direct:get-access-token").replace().to(mockGetAccessToken);
            routeBuilder.weaveByToUri("direct:mtn-transaction-status").replace().to(mockTransactionStatus);
            routeBuilder.weaveByToUri("direct:mtn-transaction-status-response-handler").replace()
                    .to(mockTransactionStatusResponseHandler);
        });

        // Prepare mock expectations
        mockGetAccessToken.expectedMessageCount(1);
        mockTransactionStatus.expectedMessageCount(1);
        mockTransactionStatusResponseHandler.expectedMessageCount(1);

        // Set properties
        Exchange exchange = camelContext.getEndpoint("direct:mtn-get-transaction-status-base").createExchange();
        exchange.setProperty(SERVER_TRANSACTION_STATUS_RETRY_COUNT, 2);
        fluentProducerTemplate.to("direct:mtn-get-transaction-status-base").withExchange(exchange).send();

        // Assertions
        mockGetAccessToken.assertIsSatisfied();
        mockTransactionStatus.assertIsSatisfied();
        mockTransactionStatusResponseHandler.assertIsSatisfied();
    }

    @DisplayName("Test transaction status retry exceeded")
    @Test
    void test_transaction_status_retry_exceeded() throws Exception {
        // Advice the route to replace the failure processor with a mock
        AdviceWith.adviceWith(camelContext, "mtn-buy-goods-get-transaction-status-base", routeBuilder -> {
            routeBuilder.weaveById("mtn-transaction-status-failure-processor").replace()
                    .to(mockTransactionStatusFailureProcessor);
        });

        // Prepare mock expectations
        mockTransactionStatusFailureProcessor.expectedMessageCount(1);
        mockTransactionStatusFailureProcessor.expectedPropertyReceived(IS_RETRY_EXCEEDED, true);
        mockTransactionStatusFailureProcessor.expectedPropertyReceived(TRANSACTION_FAILED, true);

        // Set properties
        Exchange exchange = camelContext.getEndpoint("direct:mtn-get-transaction-status-base").createExchange();
        exchange.setProperty(SERVER_TRANSACTION_STATUS_RETRY_COUNT, 5);
        fluentProducerTemplate.to("direct:mtn-get-transaction-status-base").withExchange(exchange).send();

        // Assertions
        mockTransactionStatusFailureProcessor.assertIsSatisfied();
    }

    @DisplayName("Test MTN transaction status route")
    @Test
    void test_mtn_transaction_status_route() throws Exception {
        // Advice the route to replace the HTTP call with a mock
        AdviceWith.adviceWith(camelContext, "mtn-transaction-status", routeBuilder -> {
            routeBuilder.weaveByToUri(mtnProps.getApiHost() + "/collection/v1_0/requesttopay/*").replace()
                    .to(mockApiEndpoint);
        });

        // Prepare mock expectations
        mockApiEndpoint.expectedMessageCount(1);
        mockApiEndpoint.expectedHeaderReceived(Exchange.HTTP_METHOD, "GET");
        mockApiEndpoint.expectedHeaderReceived("Content-Type", "application/json");
        mockApiEndpoint.expectedHeaderReceived("Ocp-Apim-Subscription-Key", mtnProps.getSubscriptionKey());
        mockApiEndpoint.expectedHeaderReceived("X-Reference-Id", "test-correlation-id");
        mockApiEndpoint.expectedHeaderReceived("X-Target-Environment", mtnProps.getEnvironment());
        mockApiEndpoint.expectedHeaderReceived("Authorization", "Bearer test-access-token");

        // Set properties and send exchange
        Exchange exchange = camelContext.getEndpoint("direct:mtn-transaction-status").createExchange();
        exchange.setProperty(CORRELATION_ID, "test-correlation-id");
        exchange.setProperty(ACCESS_TOKEN, "test-access-token");
        fluentProducerTemplate.to("direct:mtn-transaction-status").withExchange(exchange).send();

        // Assertions
        mockApiEndpoint.assertIsSatisfied();
    }

    @DisplayName("Test MTN transaction status response handler")
    @Test
    void test_mtn_transaction_status_response_handler_successful() throws Exception {
        // Advice the route to replace the final processor with a mock
        AdviceWith.adviceWith(camelContext, "mtn-transaction-status-response-handler", routeBuilder -> {
            routeBuilder.weaveById("successful-processing").replace().to(mockProcessor);
        });

        // Set up the mock expectations
        mockProcessor.expectedMessageCount(1);
        mockProcessor.expectedPropertyReceived(TRANSACTION_FAILED, false);
        mockProcessor.expectedPropertyReceived(LAST_RESPONSE_BODY,
                "{\"status\": \"SUCCESSFUL\", \"financialTransactionId\": \"12345\"}");
        mockProcessor.expectedPropertyReceived(FINANCIAL_TRANSACTION_ID, "12345");

        // Prepare the response body as a successful transaction
        String responseBody = "{\"status\": \"SUCCESSFUL\", \"financialTransactionId\": \"12345\"}";

        // Send the exchange to the route
        fluentProducerTemplate.to("direct:mtn-transaction-status-response-handler")
                .withHeader(Exchange.HTTP_RESPONSE_CODE, "200").withBody(responseBody).send();

        // Assertions
        mockProcessor.assertIsSatisfied();
    }

    @DisplayName("Test MTN transaction status response handler pending")
    @Test
    void test_mtn_transaction_status_response_handler_pending() throws Exception {
        // Advice the route to replace the final processor with a mock
        AdviceWith.adviceWith(camelContext, "mtn-transaction-status-response-handler", routeBuilder -> {
            routeBuilder.weaveById("successful-processing").replace().to(mockProcessor);
        });

        // Set up the mock expectations
        mockProcessor.expectedMessageCount(1);
        mockProcessor.expectedPropertyReceived(IS_TRANSACTION_PENDING, true);
        mockProcessor.expectedPropertyReceived(LAST_RESPONSE_BODY, "{\"status\": \"PENDING\"}");

        // Prepare the response body as a pending transaction
        String responseBody = "{\"status\": \"PENDING\"}";

        // Send the exchange to the route
        fluentProducerTemplate.to("direct:mtn-transaction-status-response-handler")
                .withHeader(Exchange.HTTP_RESPONSE_CODE, "200").withBody(responseBody).send();

        // Assertions
        mockProcessor.assertIsSatisfied();
    }

    @DisplayName("Test MTN transaction status response handler failure")
    @Test
    void test_mtn_transaction_status_response_handler_failure() throws Exception {
        // Advice the route to replace the final processor with a mock
        AdviceWith.adviceWith(camelContext, "mtn-transaction-status-response-handler", routeBuilder -> {
            routeBuilder.weaveById("successful-processing").replace().to(mockProcessor);
        });

        // Set up the mock expectations
        mockProcessor.expectedMessageCount(1);
        mockProcessor.expectedPropertyReceived(TRANSACTION_FAILED, true);
        mockProcessor.expectedPropertyReceived(LAST_RESPONSE_BODY,
                "{\"status\": \"FAILED\", \"reason\": \"Insufficient funds\"}");
        mockProcessor.expectedPropertyReceived(ERROR_DESCRIPTION, "Insufficient funds");

        // Prepare the response body as a failed transaction
        String responseBody = "{\"status\": \"FAILED\", \"reason\": \"Insufficient funds\"}";

        // Send the exchange to the route
        fluentProducerTemplate.to("direct:mtn-transaction-status-response-handler")
                .withHeader(Exchange.HTTP_RESPONSE_CODE, "200").withBody(responseBody).send();

        // Assertions
        mockProcessor.assertIsSatisfied();
    }

    @DisplayName("Test MTN transaction status response handler error")
    @Test
    void test_mtn_transaction_status_response_handler_error() throws Exception {
        // Advice the route to replace the final processor with a mock
        AdviceWith.adviceWith(camelContext, "mtn-transaction-status-response-handler", routeBuilder -> {
            routeBuilder.weaveById("unsuccessful-processing").replace().to(mockProcessor);
        });

        // Set up the mock expectations
        mockProcessor.expectedMessageCount(1);
        mockProcessor.expectedPropertyReceived(TRANSACTION_FAILED, true);

        // Prepare the response body as an error (non-200 HTTP response)
        String responseBody = "Error response from API";

        // Send the exchange to the route
        fluentProducerTemplate.to("direct:mtn-transaction-status-response-handler")
                .withHeader(Exchange.HTTP_RESPONSE_CODE, "500").withBody(responseBody).send();

        // Assertions
        mockProcessor.assertIsSatisfied();
    }

    @DisplayName("Test MTN transaction status response handler successful no status")
    @Test
    void test_mtn_transaction_status_response_handler_successful_no_status() throws Exception {
        // Advice the route to replace the final processor with a mock
        AdviceWith.adviceWith(camelContext, "mtn-transaction-status-response-handler", routeBuilder -> {
            routeBuilder.weaveById("successful-processing").replace().to(mockProcessor);
        });

        // Set up the mock expectations
        mockProcessor.expectedMessageCount(1);
        mockProcessor.expectedPropertyReceived(TRANSACTION_FAILED, null);
        // Prepare the response body as a successful transaction
        String responseBody = "{\"financialTransactionId\": \"12345\"}";

        // Send the exchange to the route
        fluentProducerTemplate.to("direct:mtn-transaction-status-response-handler")
                .withHeader(Exchange.HTTP_RESPONSE_CODE, "200").withBody(responseBody).send();

        // Assertions
        mockProcessor.assertIsSatisfied();
    }

}
