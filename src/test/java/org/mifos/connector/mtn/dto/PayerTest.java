package org.mifos.connector.mtn.dto;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.mtn.MtnConnectorApplicationTests;

class PayerTest extends MtnConnectorApplicationTests {

    @DisplayName("Create Payer instance with valid partyIdType and partyId using parameterized constructor")
    @Test
    void test_create_payer_with_valid_values() {
        String partyIdType = "MSISDN";
        String partyId = "123456789";

        Payer payer = new Payer(partyIdType, partyId);

        Assertions.assertEquals(partyIdType, payer.getPartyIdType());
        Assertions.assertEquals(partyId, payer.getPartyId());
    }

    @DisplayName("Create Payer with null values for both fields")
    @Test
    void test_create_payer_with_null_values() {
        Payer payer = new Payer(null, null);

        Assertions.assertNull(payer.getPartyIdType());
        Assertions.assertNull(payer.getPartyId());
    }

    @DisplayName("Returns formatted string with both partyIdType and partyId values")
    @Test
    void test_toString_returns_formatted_string() {
        Payer payer = new Payer("MSISDN", "123456789");

        String result = payer.toString();

        Assertions.assertEquals("Payer{partyIdType='MSISDN', partyId='123456789'}", result);
    }

    @DisplayName("Handles null values for partyIdType")
    @Test
    void test_toString_handles_null_partyIdType() {
        Payer payer = new Payer(null, "123456789");

        String result = payer.toString();

        Assertions.assertEquals("Payer{partyIdType='null', partyId='123456789'}", result);
    }

}
