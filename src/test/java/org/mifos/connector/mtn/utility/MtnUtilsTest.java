package org.mifos.connector.mtn.utility;

import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
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

    @ParameterizedTest
    @MethodSource("validPaybillAccountNumbers")
    void extractPaybillAccountNumber_whenInputIsValid_shouldExtractCorrectValue(String input, String expected) {
        Assertions.assertEquals(expected, MtnUtils.extractPaybillAccountNumber(input));
    }

    @ParameterizedTest
    @MethodSource("invalidPaybillValues")
    void extractPaybillAccountNumber_whenInputIsInvalid_shouldReturnNull(String input, String expected) {
        Assertions.assertEquals(expected, MtnUtils.extractPaybillAccountNumber(input));
    }

    @ParameterizedTest
    @MethodSource("validMsisdnValues")
    void extractPaybillMsisdn_whenInputIsValid_shouldExtractCorrectValue(String input, String expected) {
        Assertions.assertEquals(expected, MtnUtils.extractPaybillMsisdn(input));
    }

    @ParameterizedTest
    @MethodSource("invalidPaybillValues")
    void extractPaybillMsisdn_whenInputIsInvalid_shouldReturnNull(String input, String expected) {
        Assertions.assertEquals(expected, MtnUtils.extractPaybillMsisdn(input));
    }

    private static Stream<Arguments> validPaybillAccountNumbers() {
        return Stream.of(Arguments.of("FRI:33859939@tubura.sp/SP", "33859939"),
                Arguments.of("FRI:12345@some.domain/SP", "12345"), Arguments.of("FRI:987654321@x.y.z", "987654321"),
                Arguments.of("   FRI:1111@domain/SP   ", "1111"), Arguments.of("33859939", "33859939"));
    }

    private static Stream<Arguments> invalidPaybillValues() {
        return Stream.of(Arguments.of("", null), Arguments.of("   ", null), Arguments.of(null, null));
    }

    public static Stream<Arguments> validMsisdnValues() {
        return Stream.of(Arguments.of("ID:250790690134/MSISDN", "250790690134"),
                Arguments.of("ID:12345/MSISDN", "12345"), Arguments.of("   ID:987654321/MSISDN   ", "987654321"),
                Arguments.of("987654321/MSISDN   ", "987654321"), Arguments.of("987654321", "987654321"));
    }

}
