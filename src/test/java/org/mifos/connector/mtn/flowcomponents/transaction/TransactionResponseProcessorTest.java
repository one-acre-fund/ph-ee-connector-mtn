package org.mifos.connector.mtn.flowcomponents.transaction;

import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TRANSACTION_FAILED;

import org.apache.camel.CamelContext;
import org.apache.camel.Exchange;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.mtn.MtnConnectorApplicationTests;
import org.springframework.beans.factory.annotation.Autowired;

class TransactionResponseProcessorTest extends MtnConnectorApplicationTests {

    @Autowired
    private CamelContext camelContext;

    @DisplayName("Process exchange with hasTransferFailed=false and set TRANSACTION_FAILED property to false")
    @Test
    void test_process_exchange_with_transfer_not_failed() {
        TransactionResponseProcessor processor = new TransactionResponseProcessor();

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();
        ;
        exchange.setProperty(TRANSACTION_FAILED, Boolean.FALSE);

        processor.process(exchange);

        Assertions.assertFalse((Boolean) exchange.getProperty(TRANSACTION_FAILED));
    }

    @DisplayName("Process exchange with TRANSACTION_FAILED property null")
    @Test
    void test_process_exchange_with_transfer_null() {
        TransactionResponseProcessor processor = new TransactionResponseProcessor();

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();

        exchange.setProperty(TRANSACTION_FAILED, null);

        processor.process(exchange);

        Assertions.assertFalse((Boolean) exchange.getProperty(TRANSACTION_FAILED));
    }

    @DisplayName("Process exchange with TRANSACTION_FAILED property not set")
    @Test
    void test_process_exchange_with_transfer_not_set() {
        TransactionResponseProcessor processor = new TransactionResponseProcessor();

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();

        processor.process(exchange);

        Assertions.assertFalse((Boolean) exchange.getProperty(TRANSACTION_FAILED));
    }

    @DisplayName("Process exchange with TRANSACTION_FAILED property set to true")
    @Test
    void test_process_exchange_with_transfer_true() {
        TransactionResponseProcessor processor = new TransactionResponseProcessor();

        Exchange exchange = camelContext.getEndpoint("mock:test").createExchange();

        exchange.setProperty(TRANSACTION_FAILED, true);

        processor.process(exchange);

        Assertions.assertTrue((Boolean) exchange.getProperty(TRANSACTION_FAILED));
    }

}
