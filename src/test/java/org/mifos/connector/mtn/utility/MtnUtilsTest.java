package org.mifos.connector.mtn.utility;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.common.channel.dto.TransactionChannelC2BRequestDTO;
import org.mifos.connector.common.gsma.dto.GsmaParty;
import org.mifos.connector.common.mojaloop.dto.MoneyData;
import org.mifos.connector.mtn.MtnConnectorApplicationTests;
import org.mifos.connector.mtn.dto.PaymentRequestDto;

class MtnUtilsTest extends MtnConnectorApplicationTests {

    @DisplayName("Convert valid transaction request with phone number without '+' prefix")
    @Test
    void test_channel_request_converter_with_plain_phone_number() {
        MtnUtils mtnUtils = new MtnUtils();

        TransactionChannelC2BRequestDTO requestDTO = new TransactionChannelC2BRequestDTO();
        GsmaParty gsmaParty1 = new GsmaParty();
        gsmaParty1.setKey("MSISDN");
        gsmaParty1.setValue("123456789");
        GsmaParty gsmaParty2 = new GsmaParty();
        gsmaParty2.setKey("MESSAGE");
        gsmaParty2.setValue("Test Message");

        requestDTO.setPayer(new GsmaParty[] { gsmaParty1, gsmaParty2 });

        MoneyData amount = new MoneyData();
        amount.setAmount("100");
        amount.setCurrency("EUR");
        requestDTO.setAmount(amount);

        PaymentRequestDto result = mtnUtils.channelRequestConvertor(requestDTO, "test-transaction-id");

        assertEquals("123456789", result.getPayer().getPartyId());
        assertEquals("100", result.getAmount());
        assertEquals("EUR", result.getCurrency());
        assertEquals("test-transaction-id", result.getExternalId());
        assertEquals("Test Message", result.getPayerMessage());
        assertEquals("Test Message", result.getPayeeNote());
    }

    @DisplayName("Handle empty phone number in transaction request")
    @Test
    void test_channel_request_converter_with_empty_phone_number() {
        MtnUtils mtnUtils = new MtnUtils();

        TransactionChannelC2BRequestDTO requestDTO = new TransactionChannelC2BRequestDTO();
        GsmaParty gsmaParty1 = new GsmaParty();
        gsmaParty1.setKey("MSISDN");
        gsmaParty1.setValue("");
        GsmaParty gsmaParty2 = new GsmaParty();
        gsmaParty2.setKey("MESSAGE");
        gsmaParty2.setValue("Test Message");
        requestDTO.setPayer(new GsmaParty[] { gsmaParty1, gsmaParty2 });

        MoneyData amount = new MoneyData();
        amount.setAmount("100");
        amount.setCurrency("EUR");
        requestDTO.setAmount(amount);

        PaymentRequestDto result = mtnUtils.channelRequestConvertor(requestDTO, "test-transaction-id");

        assertEquals("", result.getPayer().getPartyId());
        assertEquals("100", result.getAmount());
        assertEquals("EUR", result.getCurrency());
        assertEquals("test-transaction-id", result.getExternalId());
        assertEquals("Test Message", result.getPayerMessage());
        assertEquals("Test Message", result.getPayeeNote());
    }

    @DisplayName("Convert valid transaction request with phone number with '+' prefix")
    @Test
    void test_channel_request_converter_with_plain_phone_number_with_plus() {
        MtnUtils mtnUtils = new MtnUtils();

        TransactionChannelC2BRequestDTO requestDTO = new TransactionChannelC2BRequestDTO();
        GsmaParty gsmaParty1 = new GsmaParty();
        gsmaParty1.setKey("MSISDN");
        gsmaParty1.setValue("+123456789");
        GsmaParty gsmaParty2 = new GsmaParty();
        gsmaParty2.setKey("MESSAGE");
        gsmaParty2.setValue("Test Message");

        requestDTO.setPayer(new GsmaParty[] { gsmaParty1, gsmaParty2 });

        MoneyData amount = new MoneyData();
        amount.setAmount("100");
        amount.setCurrency("EUR");
        requestDTO.setAmount(amount);

        PaymentRequestDto result = mtnUtils.channelRequestConvertor(requestDTO, "test-transaction-id");

        assertEquals("123456789", result.getPayer().getPartyId());
        assertEquals("100", result.getAmount());
        assertEquals("EUR", result.getCurrency());
        assertEquals("test-transaction-id", result.getExternalId());
        assertEquals("Test Message", result.getPayerMessage());
        assertEquals("Test Message", result.getPayeeNote());
    }

}
