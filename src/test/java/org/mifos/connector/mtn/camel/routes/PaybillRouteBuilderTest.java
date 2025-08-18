package org.mifos.connector.mtn.camel.routes;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.MTN_PAYMENT_COMPLETED;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.MTN_PAYMENT_COMPLETION_HTTP_CODE_RESPONSE;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.MTN_PAYMENT_COMPLETION_RESPONSE;

import java.math.BigDecimal;
import java.util.stream.Stream;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.ProducerTemplate;
import org.apache.camel.builder.AdviceWith;
import org.apache.camel.support.DefaultExchange;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mifos.connector.mtn.MtnConnectorApplicationTests;
import org.mifos.connector.mtn.dto.ErrorResponse;
import org.mifos.connector.mtn.dto.FinancialResourceInformationResponse;
import org.mifos.connector.mtn.dto.PaybillPaymentRequest;
import org.mifos.connector.mtn.dto.PaybillPaymentResponse;
import org.mifos.connector.mtn.dto.PaymentCompletedResponse;
import org.mifos.connector.mtn.flowcomponents.state.WorkflowInstanceStore;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Test class for {@link PaybillRouteBuilder}.
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class PaybillRouteBuilderTest extends MtnConnectorApplicationTests {

    @Autowired
    private ProducerTemplate template;
    @Autowired
    private CamelContext camelContext;
    @Autowired
    WorkflowInstanceStore workflowInstanceStore;

    @Test
    void testSuccessfulPaybillValidation() throws Exception {
        String accountStatusResponse = """
                {
                  "reconciled": true,
                  "amsName": "fineract",
                  "accountHoldingInstitutionId": "oaf",
                  "transactionId": "TXN7890",
                  "amount": "150",
                  "currency": "RWF",
                  "msisdn": "250782345678",
                  "clientName": "John Doe"
                }
                """;

        String workflowResponse = """
                {
                  "transactionId": "TXN7890CDFRRTEYYR"
                }
                """;

        AdviceWith.adviceWith(camelContext, "account-status", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setBody(accountStatusResponse);
                ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            });
        });
        AdviceWith.adviceWith(camelContext, "start-paybill-workflow", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setBody(workflowResponse);
                ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            });
        });
        Exchange exchange = new DefaultExchange(camelContext);
        String requestBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns0:getfinancialresourceinformationrequest xmlns:ns0="http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend/client">
                   <resource>FRI:15834384@tubura.sp/SP</resource>
                   <accountholderid>ID:250790690134/MSISDN</accountholderid>
                </ns0:getfinancialresourceinformationrequest>
                    """;
        exchange.getIn().setBody(requestBody);
        template.send("direct:paybill-validation", exchange);

        assertEquals(200, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("application/xml", exchange.getIn().getHeader(Exchange.CONTENT_TYPE));
        FinancialResourceInformationResponse response = exchange.getIn()
                .getBody(FinancialResourceInformationResponse.class);
        assertEquals("completed", response.getMessage());
        assertEquals("John Doe", response.getExtension().getAccountName());
        assertEquals("TXN7890", response.getExtension().getOafReference());
    }

    @Test
    void testFailedPaybillValidation_whenWorkflowNotStarted() throws Exception {
        String accountStatusResponse = """
                {
                  "reconciled": true,
                  "amsName": "fineract",
                  "accountHoldingInstitutionId": "oaf",
                  "transactionId": "TXN7890",
                  "amount": "150",
                  "currency": "RWF",
                  "msisdn": "250782345678",
                  "clientName": "John Doe"
                }
                """;

        AdviceWith.adviceWith(camelContext, "account-status", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setBody(accountStatusResponse);
                ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            });
        });
        AdviceWith.adviceWith(camelContext, "start-paybill-workflow", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setBody("");
                ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 404);
            });
        });
        Exchange exchange = new DefaultExchange(camelContext);
        String requestBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns0:getfinancialresourceinformationrequest xmlns:ns0="http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend/client">
                   <resource>15834384</resource>
                   <accountholderid>250790690134</accountholderid>
                </ns0:getfinancialresourceinformationrequest>
                    """;
        exchange.getIn().setBody(requestBody);
        template.send("direct:paybill-validation", exchange);

        assertEquals(500, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("application/xml", exchange.getIn().getHeader(Exchange.CONTENT_TYPE));
        ErrorResponse response = exchange.getIn().getBody(ErrorResponse.class);
        assertEquals("An error occurred while processing the request", response.getMessage());
    }

    @Test
    void testFailedPaybillValidation() throws Exception {
        String accountStatusResponse = """
                {
                    "reconciled": false
                }
                """;
        AdviceWith.adviceWith(camelContext, "account-status", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setBody(accountStatusResponse);
                ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            });
        });
        Exchange exchange = new DefaultExchange(camelContext);
        String requestBody = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns0:getfinancialresourceinformationrequest xmlns:ns0="http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend/client">
                   <resource>15834384</resource>
                   <accountholderid>250790690134</accountholderid>
                </ns0:getfinancialresourceinformationrequest>
                    """;
        exchange.getIn().setBody(requestBody);

        template.send("direct:paybill-validation", exchange);

        assertEquals(404, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("application/xml", exchange.getIn().getHeader(Exchange.CONTENT_TYPE));
        FinancialResourceInformationResponse response = exchange.getIn()
                .getBody(FinancialResourceInformationResponse.class);
        assertEquals("completed", response.getMessage());
        assertNull(response.getExtension());
    }

    @Test
    void testSuccessfulPaybillConfirmation_whenTransactionAlreadyCommitted() throws Exception {
        String txnStatusResponse = """
                {
                    "transferState": "COMMITTED"
                }
                """;
        AdviceWith.adviceWith(camelContext, "paybill-transaction-status-check-base", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setBody(txnStatusResponse);
                ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            });
        });
        PaybillPaymentRequest paymentRequest = new PaybillPaymentRequest();
        paymentRequest.setTransactionId("1025552");
        paymentRequest.setAccountHolderId("250790690134");
        paymentRequest.setReceivingFri("15834384");
        paymentRequest.setExtension(new PaybillPaymentRequest.Extension());
        paymentRequest.getExtension().setOafReference("5ef1933d-7daf-4214-857a-ff4ced7e0918");
        PaybillPaymentRequest.Amount amount = new PaybillPaymentRequest.Amount();
        amount.setAmount(BigDecimal.TEN);
        amount.setCurrency("RWF");
        paymentRequest.setAmount(amount);
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/xml");
        exchange.getIn().setBody(paymentRequest);

        template.send("direct:paybill-confirmation-base", exchange);

        assertEquals(200, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("application/xml", exchange.getIn().getHeader(Exchange.CONTENT_TYPE));
        PaybillPaymentResponse response = exchange.getIn().getBody(PaybillPaymentResponse.class);
        assertEquals("COMPLETED", response.getStatus());
        assertEquals("5ef1933d-7daf-4214-857a-ff4ced7e0918", response.getProviderTransactionId());
    }

    @Test
    void testSuccessfulPaybillConfirmation_whenTransactionIsNew() throws Exception {
        String txnStatusResponse = """
                {
                    "transferState": "RECEIVED"
                }
                """;
        AdviceWith.adviceWith(camelContext, "paybill-transaction-status-check-base", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setBody(txnStatusResponse);
                ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            });
        });
        PaybillPaymentRequest paymentRequest = new PaybillPaymentRequest();
        paymentRequest.setTransactionId("1025552");
        paymentRequest.setAccountHolderId("250790690134");
        paymentRequest.setReceivingFri("15834384");
        paymentRequest.setExtension(new PaybillPaymentRequest.Extension());
        paymentRequest.getExtension().setOafReference("5ef1933d-7daf-4214-857a-ff4ced7e0918");
        PaybillPaymentRequest.Amount amount = new PaybillPaymentRequest.Amount();
        amount.setAmount(BigDecimal.TEN);
        amount.setCurrency("RWF");
        paymentRequest.setAmount(amount);
        workflowInstanceStore.put("5ef1933d-7daf-4214-857a-ff4ced7e0918", "54t45ety5t3er");
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/xml");
        exchange.getIn().setBody(paymentRequest);

        template.send("direct:paybill-confirmation-base", exchange);

        assertEquals(202, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("application/xml", exchange.getIn().getHeader(Exchange.CONTENT_TYPE));
        PaybillPaymentResponse response = exchange.getIn().getBody(PaybillPaymentResponse.class);
        assertEquals("PENDING", response.getStatus());
        assertEquals("5ef1933d-7daf-4214-857a-ff4ced7e0918", response.getProviderTransactionId());
    }

    @Test
    void testSuccessfulPaybillConfirmation_whenTransactionIsNewAndNotFoundInWorkflowStore() throws Exception {
        String txnStatusResponse = """
                {
                    "transferState": "RECEIVED"
                }
                """;
        AdviceWith.adviceWith(camelContext, "paybill-transaction-status-check-base", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setBody(txnStatusResponse);
                ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            });
        });
        AdviceWith.adviceWith(camelContext, "paybill-confirmation-base", routeBuilder -> {
            routeBuilder.mockEndpointsAndSkip("direct:paybill-confirmation");
        });
        PaybillPaymentRequest paymentRequest = new PaybillPaymentRequest();
        paymentRequest.setTransactionId("1025552");
        paymentRequest.setAccountHolderId("250790690134");
        paymentRequest.setReceivingFri("15834384");
        paymentRequest.setExtension(new PaybillPaymentRequest.Extension());
        paymentRequest.getExtension().setOafReference("5ef1933d-7daf-4214-857a-ff4ced7e0918");
        PaybillPaymentRequest.Amount amount = new PaybillPaymentRequest.Amount();
        amount.setAmount(BigDecimal.TEN);
        amount.setCurrency("RWF");
        paymentRequest.setAmount(amount);
        Exchange exchange = new DefaultExchange(camelContext);
        exchange.getIn().setHeader(Exchange.CONTENT_TYPE, "application/xml");
        exchange.getIn().setBody(paymentRequest);

        template.send("direct:paybill-confirmation-base", exchange);

        assertEquals(202, exchange.getIn().getHeader(Exchange.HTTP_RESPONSE_CODE));
        assertEquals("application/xml", exchange.getIn().getHeader(Exchange.CONTENT_TYPE));
        PaybillPaymentResponse response = exchange.getIn().getBody(PaybillPaymentResponse.class);
        assertEquals("PENDING", response.getStatus());
        assertEquals("5ef1933d-7daf-4214-857a-ff4ced7e0918", response.getProviderTransactionId());
    }

    @Test
    void testSuccessfulPaymentCompletion() throws Exception {
        String payload = """
                <?xml version="1.0" encoding="UTF-8"?><ns0:paymentcompletedresponse xmlns:ns0="http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend"/>
                """;
        AdviceWith.adviceWith(camelContext, "mtn-payment-completion", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setBody(payload);
                ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 200);
            });
        });

        Exchange exchange = template.send("direct:mtn-payment-completion", new DefaultExchange(camelContext));

        assertNotNull(exchange.getProperty(MTN_PAYMENT_COMPLETION_RESPONSE));
        assertTrue(exchange.getProperty(MTN_PAYMENT_COMPLETED, Boolean.class));
        PaymentCompletedResponse response = exchange.getIn().getBody(PaymentCompletedResponse.class);
        assertNotNull(response);
        assertNull(response.getPaymentStatus());
    }

    @ParameterizedTest
    @MethodSource("provideFailedPaymentCompletedPayloads")
    void testFailedPaymentCompletion(String payload) throws Exception {
        AdviceWith.adviceWith(camelContext, "mtn-payment-completion", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setBody(payload);
            });
        });

        Exchange exchange = template.send("direct:mtn-payment-completion", new DefaultExchange(camelContext));

        assertFalse(exchange.getProperty(MTN_PAYMENT_COMPLETED, Boolean.class));
    }

    @ParameterizedTest
    @MethodSource("provideFailedPaymentCompletedHttpCodes")
    @DisplayName("Test failed payment completion with various HTTP codes")
    void testFailedPaymentCompletion_withHttpCode(Integer httpStatusCode) throws Exception {
        String payload = """
                <?xml version="1.0" encoding="UTF-8"?><ns0:paymentcompletedresponse xmlns:ns0="http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend"/>
                """;
        AdviceWith.adviceWith(camelContext, "mtn-payment-completion", routeBuilder -> {
            routeBuilder.weaveByToUri("http://*").replace().process(ex -> {
                ex.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, httpStatusCode);
                ex.getIn().setBody(payload);
            });
        });

        Exchange exchange = template.send("direct:mtn-payment-completion", new DefaultExchange(camelContext));

        assertThat(exchange.getProperty(MTN_PAYMENT_COMPLETED, Boolean.class)).isFalse();
        assertThat(exchange.getProperty(MTN_PAYMENT_COMPLETION_RESPONSE)).isEqualTo(payload);
        assertThat(exchange.getProperty(MTN_PAYMENT_COMPLETION_HTTP_CODE_RESPONSE)).isEqualTo(httpStatusCode);
    }

    static Stream<String> provideFailedPaymentCompletedPayloads() {
        String failedPayload = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns0:paymentcompletedresponse xmlns:ns0="http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend">
                    <paymentstatus>FAILED</paymentstatus>
                </ns0:paymentcompletedresponse>
                """;
        String pendingPayload = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns0:paymentcompletedresponse xmlns:ns0="http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend">
                    <paymentstatus>PENDING</paymentstatus>
                </ns0:paymentcompletedresponse>
                """;
        String acknowledgedPayload = """
                <?xml version="1.0" encoding="UTF-8"?>
                <ns0:paymentcompletedresponse xmlns:ns0="http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend">
                    <paymentstatus>ACKNOWLEDGED</paymentstatus>
                </ns0:paymentcompletedresponse>
                """;
        return Stream.of(failedPayload, pendingPayload, acknowledgedPayload, "", null);
    }

    static Stream<Integer> provideFailedPaymentCompletedHttpCodes() {

        return Stream.of(401, 500, 502, 504, null);
    }
}
