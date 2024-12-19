package org.mifos.connector.mtn.utility;

import org.junit.jupiter.api.Assertions;
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

        GsmaParty gsmaParty1 = new GsmaParty();
        gsmaParty1.setKey("MSISDN");
        gsmaParty1.setValue("123456789");
        GsmaParty gsmaParty2 = new GsmaParty();
        gsmaParty2.setKey("MESSAGE");
        gsmaParty2.setValue("Test Message");

        TransactionChannelC2BRequestDTO requestDto = new TransactionChannelC2BRequestDTO();
        requestDto.setPayer(new GsmaParty[] { gsmaParty1, gsmaParty2 });

        MoneyData amount = new MoneyData();
        amount.setAmount("100");
        amount.setCurrency("EUR");
        requestDto.setAmount(amount);
        MtnUtils mtnUtils = new MtnUtils();
        PaymentRequestDto result = mtnUtils.channelRequestConvertor(requestDto, "test-transaction-id");

        Assertions.assertEquals("123456789", result.getPayer().getPartyId());
        Assertions.assertEquals("100", result.getAmount());
        Assertions.assertEquals("EUR", result.getCurrency());
        Assertions.assertEquals("test-transaction-id", result.getExternalId());
        Assertions.assertEquals("Test Message", result.getPayerMessage());
        Assertions.assertEquals("Test Message", result.getPayeeNote());
    }

    @DisplayName("Handle empty phone number in transaction request")
    @Test
    void test_channel_request_converter_with_empty_phone_number() {

        GsmaParty gsmaParty1 = new GsmaParty();
        gsmaParty1.setKey("MSISDN");
        gsmaParty1.setValue("");
        GsmaParty gsmaParty2 = new GsmaParty();
        gsmaParty2.setKey("MESSAGE");
        gsmaParty2.setValue("Test Message");

        TransactionChannelC2BRequestDTO requestDto = new TransactionChannelC2BRequestDTO();
        requestDto.setPayer(new GsmaParty[] { gsmaParty1, gsmaParty2 });

        MoneyData amount = new MoneyData();
        amount.setAmount("100");
        amount.setCurrency("EUR");
        requestDto.setAmount(amount);

        MtnUtils mtnUtils = new MtnUtils();
        PaymentRequestDto result = mtnUtils.channelRequestConvertor(requestDto, "test-transaction-id");

        Assertions.assertEquals("", result.getPayer().getPartyId());
        Assertions.assertEquals("100", result.getAmount());
        Assertions.assertEquals("EUR", result.getCurrency());
        Assertions.assertEquals("test-transaction-id", result.getExternalId());
        Assertions.assertEquals("Test Message", result.getPayerMessage());
        Assertions.assertEquals("Test Message", result.getPayeeNote());
    }

    @DisplayName("Convert valid transaction request with phone number with '+' prefix")
    @Test
    void test_channel_request_converter_with_plain_phone_number_with_plus() {

        GsmaParty gsmaParty1 = new GsmaParty();
        gsmaParty1.setKey("MSISDN");
        gsmaParty1.setValue("+123456789");
        GsmaParty gsmaParty2 = new GsmaParty();
        gsmaParty2.setKey("MESSAGE");
        gsmaParty2.setValue("Test Message");

        TransactionChannelC2BRequestDTO requestDto = new TransactionChannelC2BRequestDTO();
        requestDto.setPayer(new GsmaParty[] { gsmaParty1, gsmaParty2 });

        MoneyData amount = new MoneyData();
        amount.setAmount("100");
        amount.setCurrency("EUR");
        requestDto.setAmount(amount);

        MtnUtils mtnUtils = new MtnUtils();
        PaymentRequestDto result = mtnUtils.channelRequestConvertor(requestDto, "test-transaction-id");

        Assertions.assertEquals("123456789", result.getPayer().getPartyId());
        Assertions.assertEquals("100", result.getAmount());
        Assertions.assertEquals("EUR", result.getCurrency());
        Assertions.assertEquals("test-transaction-id", result.getExternalId());
        Assertions.assertEquals("Test Message", result.getPayerMessage());
        Assertions.assertEquals("Test Message", result.getPayeeNote());
    }

}
