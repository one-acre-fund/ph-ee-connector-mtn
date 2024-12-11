package org.mifos.connector.mtn.flowcomponents.transaction;

import static org.apache.camel.Exchange.EXCEPTION_CAUGHT;
import static org.apache.camel.Exchange.HTTP_RESPONSE_TEXT;
import static org.mifos.connector.mtn.camel.config.CamelProperties.CORRELATION_ID;
import static org.mifos.connector.mtn.camel.config.CamelProperties.ERROR_CODE;
import static org.mifos.connector.mtn.camel.config.CamelProperties.ERROR_INFORMATION;
import static org.mifos.connector.mtn.camel.config.CamelProperties.IS_RETRY_EXCEEDED;
import static org.mifos.connector.mtn.camel.config.CamelProperties.IS_TRANSACTION_PENDING;
import static org.mifos.connector.mtn.camel.config.CamelProperties.LAST_RESPONSE_BODY;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.EXTERNAL_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.FINANCIAL_TRANSACTION_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.GET_TRANSACTION_STATUS_RESPONSE_CODE;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.SERVER_TRANSACTION_STATUS_RETRY_COUNT;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TIMER;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TRANSACTION_FAILED;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TRANSACTION_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.ZEEBE_ELEMENT_INSTANCE_KEY;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.anyMap;
import static org.mockito.Mockito.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import io.camunda.zeebe.client.ZeebeClient;
import io.camunda.zeebe.client.api.ZeebeFuture;
import io.camunda.zeebe.client.api.command.PublishMessageCommandStep1;
import io.camunda.zeebe.client.api.command.SetVariablesCommandStep1;
import io.camunda.zeebe.client.api.response.PublishMessageResponse;
import java.util.Map;
import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.apache.camel.http.base.HttpOperationFailedException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.mtn.MtnConnectorApplicationTests;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.util.ReflectionTestUtils;

@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
class CollectionResponseProcessorTest extends MtnConnectorApplicationTests {

    @Mock
    private ZeebeClient zeebeClient;

    private CollectionResponseProcessor processor;

    @Autowired
    private CamelContext camelContext;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        processor = new CollectionResponseProcessor(zeebeClient);
        ReflectionTestUtils.setField(processor, "timeToLive", 5000);
    }

    @DisplayName("Test Process With Failed Transaction")
    @Test
    void testProcessWithFailedTransaction() throws Exception {

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();
        exchange.setProperty(IS_TRANSACTION_PENDING, false);
        exchange.setProperty(IS_RETRY_EXCEEDED, true);
        exchange.setProperty(TRANSACTION_ID, "12345");
        exchange.setProperty(CORRELATION_ID, "123456789");
        exchange.setProperty(FINANCIAL_TRANSACTION_ID, "9786182098");
        exchange.setProperty(TRANSACTION_FAILED, true);
        exchange.setProperty(ERROR_INFORMATION, "Wrong PIN entered");

        PublishMessageCommandStep1.PublishMessageCommandStep3 commandStep3 = mockPublishMessageCommandStep3();
        processor.process(exchange);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(commandStep3).variables(variablesCaptor.capture());

        // Retrieve the captured map
        Map<String, Object> capturedVariables = variablesCaptor.getValue();

        // Assert values in the map
        Assertions.assertNotNull(capturedVariables);
        Assertions.assertEquals("Wrong PIN entered", capturedVariables.get(ERROR_INFORMATION));
        Assertions.assertEquals("Wrong PIN entered", capturedVariables.get(ERROR_CODE));
        Assertions.assertEquals("12345", capturedVariables.get(TRANSACTION_ID));
        Assertions.assertEquals("9786182098", capturedVariables.get(EXTERNAL_ID));
        Assertions.assertEquals("123456789", capturedVariables.get(CORRELATION_ID));

        verify(zeebeClient).newPublishMessageCommand();
    }

    @DisplayName("Test non failed transaction With Retry And Pending while Retry Exceeded")
    @Test
    void testProcessWithRetryAndPendingTransactionWithRetryExceeded() throws Exception {

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();
        exchange.setProperty(IS_TRANSACTION_PENDING, true);
        exchange.setProperty(IS_RETRY_EXCEEDED, true);
        exchange.setProperty(TRANSACTION_FAILED, false);
        exchange.setProperty(SERVER_TRANSACTION_STATUS_RETRY_COUNT, 1);
        exchange.setProperty(CORRELATION_ID, 12345);
        exchange.setProperty(FINANCIAL_TRANSACTION_ID, "9786182098");

        PublishMessageCommandStep1.PublishMessageCommandStep3 commandStep3 = mockPublishMessageCommandStep3();
        processor.process(exchange);

        ArgumentCaptor<Map<String, Object>> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(commandStep3).variables(variablesCaptor.capture());

        // Retrieve the captured map
        Map<String, Object> capturedVariables = variablesCaptor.getValue();

        // Assert values in the map
        Assertions.assertNotNull(capturedVariables);
        Assertions.assertEquals("9786182098", capturedVariables.get(EXTERNAL_ID));
        Assertions.assertEquals("12345", capturedVariables.get(CORRELATION_ID));

        verify(zeebeClient).newPublishMessageCommand();
    }

    @DisplayName("Test Process With Retry And Pending Transaction")
    @Test
    void testProcessWithRetryAndPendingTransaction() throws Exception {

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();
        exchange.setProperty(IS_TRANSACTION_PENDING, true);
        exchange.setProperty(IS_RETRY_EXCEEDED, false);
        exchange.setProperty(TIMER, "PT15S");
        exchange.setProperty(ZEEBE_ELEMENT_INSTANCE_KEY, 1L);

        SetVariablesCommandStep1 setVariablesCommandStep1 = mock(SetVariablesCommandStep1.class);
        SetVariablesCommandStep1.SetVariablesCommandStep2 setVariablesCommandStep2 = mock(
                SetVariablesCommandStep1.SetVariablesCommandStep2.class);

        when(zeebeClient.newSetVariablesCommand(anyLong())).thenReturn(setVariablesCommandStep1);
        when(setVariablesCommandStep1.variables(anyMap())).thenReturn(setVariablesCommandStep2);
        when(setVariablesCommandStep2.send()).thenReturn(mock(ZeebeFuture.class));
        processor.process(exchange);

        ArgumentCaptor<Map> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(setVariablesCommandStep1).variables(variablesCaptor.capture());

        // Retrieve the captured map
        Map capturedVariables = variablesCaptor.getValue();

        // Assert values in the map
        Assertions.assertNotNull(capturedVariables);
        Assertions.assertEquals("PT16S", capturedVariables.get(TIMER));

        verify(zeebeClient).newSetVariablesCommand(anyLong());
    }

    @DisplayName("Test Process With Failed Transaction With Retry")
    @Test
    void testProcessWithFailedTransactionWithRetry() throws Exception {

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();
        exchange.setProperty(IS_TRANSACTION_PENDING, true);
        exchange.setProperty(IS_RETRY_EXCEEDED, false);
        exchange.setProperty(TRANSACTION_FAILED, true);
        exchange.setProperty(SERVER_TRANSACTION_STATUS_RETRY_COUNT, 1);
        exchange.setProperty(LAST_RESPONSE_BODY, null);
        exchange.setProperty(HTTP_RESPONSE_TEXT, "test");
        exchange.setProperty(TIMER, "PT15S");
        exchange.setProperty(ZEEBE_ELEMENT_INSTANCE_KEY, 1L);

        exchange.getIn().setHeader(Exchange.HTTP_RESPONSE_CODE, 400);

        SetVariablesCommandStep1 setVariablesCommandStep1 = mock(SetVariablesCommandStep1.class);
        SetVariablesCommandStep1.SetVariablesCommandStep2 setVariablesCommandStep2 = mock(
                SetVariablesCommandStep1.SetVariablesCommandStep2.class);

        when(zeebeClient.newSetVariablesCommand(anyLong())).thenReturn(setVariablesCommandStep1);
        when(setVariablesCommandStep1.variables(anyMap())).thenReturn(setVariablesCommandStep2);
        when(setVariablesCommandStep2.send()).thenReturn(mock(ZeebeFuture.class));
        processor.process(exchange);

        ArgumentCaptor<Map> variablesCaptor = ArgumentCaptor.forClass(Map.class);

        verify(setVariablesCommandStep1).variables(variablesCaptor.capture());

        // Retrieve the captured map
        Map<String, Object> capturedVariables = variablesCaptor.getValue();

        // Assert values in the map
        Assertions.assertNotNull(capturedVariables);
        Assertions.assertEquals("PT16S", capturedVariables.get(TIMER));
        Assertions.assertEquals(400, capturedVariables.get(GET_TRANSACTION_STATUS_RESPONSE_CODE));

        verify(zeebeClient).newSetVariablesCommand(anyLong());
    }

    @DisplayName("Test Process With Failed Transaction With Retry And Pending throws Exception")
    @Test
    void testProcessWithFailedTransactionWithRetryThrowsException() throws Exception {

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();
        exchange.setProperty(IS_TRANSACTION_PENDING, true);
        exchange.setProperty(IS_RETRY_EXCEEDED, false);
        exchange.setProperty(TRANSACTION_FAILED, true);
        exchange.setProperty(SERVER_TRANSACTION_STATUS_RETRY_COUNT, 1);
        exchange.setProperty(LAST_RESPONSE_BODY, null);
        exchange.setProperty(HTTP_RESPONSE_TEXT, "test");
        exchange.setProperty(TIMER, "PT15S");
        exchange.setProperty(ZEEBE_ELEMENT_INSTANCE_KEY, 1L);
        exchange.setProperty(EXCEPTION_CAUGHT, HttpOperationFailedException.class);

        SetVariablesCommandStep1 setVariablesCommandStep1 = mock(SetVariablesCommandStep1.class);
        SetVariablesCommandStep1.SetVariablesCommandStep2 setVariablesCommandStep2 = mock(
                SetVariablesCommandStep1.SetVariablesCommandStep2.class);

        when(zeebeClient.newSetVariablesCommand(anyLong())).thenReturn(setVariablesCommandStep1);
        when(setVariablesCommandStep1.variables(anyMap())).thenReturn(setVariablesCommandStep2);
        when(setVariablesCommandStep2.send()).thenReturn(mock(ZeebeFuture.class));
        processor.process(exchange);

        ArgumentCaptor<Map> variablesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(setVariablesCommandStep1).variables(variablesCaptor.capture());

        // Retrieve the captured map
        Map<String, Object> capturedVariables = variablesCaptor.getValue();

        // Assert values in the map
        Assertions.assertNotNull(capturedVariables);
        Assertions.assertEquals("PT16S", capturedVariables.get(TIMER));
        Assertions.assertNull(capturedVariables.get(GET_TRANSACTION_STATUS_RESPONSE_CODE));

        verify(zeebeClient).newSetVariablesCommand(anyLong());
    }

    /**
     * Mocks the PublishMessageCommandStep3.
     *
     * @return PublishMessageCommandStep3.
     */
    private PublishMessageCommandStep1.PublishMessageCommandStep3 mockPublishMessageCommandStep3() {
        PublishMessageCommandStep1 publishMessageCommand = mock(PublishMessageCommandStep1.class);
        PublishMessageCommandStep1.PublishMessageCommandStep2 publishMessageCommandStep2 = mock(
                PublishMessageCommandStep1.PublishMessageCommandStep2.class);

        PublishMessageCommandStep1.PublishMessageCommandStep3 publishMessageCommandStep3 = mock(
                PublishMessageCommandStep1.PublishMessageCommandStep3.class);

        ZeebeFuture<PublishMessageResponse> zeebeFutureMock = mock(ZeebeFuture.class);

        when(zeebeClient.newPublishMessageCommand()).thenReturn(publishMessageCommand);
        when(publishMessageCommand.messageName(anyString())).thenReturn(publishMessageCommandStep2);
        when(publishMessageCommandStep2.correlationKey(anyString())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.timeToLive(any())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.variables(anyMap())).thenReturn(publishMessageCommandStep3);
        when(publishMessageCommandStep3.send()).thenReturn(zeebeFutureMock);
        return publishMessageCommandStep3;

    }

}