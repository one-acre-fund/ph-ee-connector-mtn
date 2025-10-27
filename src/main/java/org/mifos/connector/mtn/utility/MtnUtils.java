package org.mifos.connector.mtn.utility;

import static org.mifos.connector.mtn.camel.config.CamelProperties.*;
import static org.mifos.connector.mtn.utility.MtnConstants.DEFAULT_TENANT;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.AMS;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.CLIENT_CORRELATION_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.CONFIRMATION_RECEIVED;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.CONFIRMATION_TIMER;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.PARTY_LOOKUP_FAILED;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TENANT_ID;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TRANSACTION_ID;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.apache.camel.Exchange;
import org.mifos.connector.common.channel.dto.TransactionChannelC2BRequestDTO;
import org.mifos.connector.common.gsma.dto.CustomData;
import org.mifos.connector.common.gsma.dto.GsmaTransfer;
import org.mifos.connector.common.gsma.dto.Party;
import org.mifos.connector.mtn.dto.ChannelValidationResponse;
import org.mifos.connector.mtn.dto.Payer;
import org.mifos.connector.mtn.dto.PaymentRequestDto;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

/**
 * Mtn utilities class.
 */
@Component
public class MtnUtils {

    /**
     * Channel request converter.
     *
     * @param transactionChannelRequestDto
     *            transaction request dto
     *
     * @param transactionId
     *            transactionId
     * @return PaymentRequestDto
     */
    public PaymentRequestDto channelRequestConvertor(TransactionChannelC2BRequestDTO transactionChannelRequestDto,
            String transactionId) {
        String phoneNumber = transactionChannelRequestDto.getPayer()[0].getValue();
        if (phoneNumber.startsWith("+")) {
            phoneNumber = phoneNumber.substring(1);
        }
        PaymentRequestDto paymentRequestDto = new PaymentRequestDto();
        paymentRequestDto.setAmount(transactionChannelRequestDto.getAmount().getAmount().trim());
        paymentRequestDto.setCurrency(transactionChannelRequestDto.getAmount().getCurrency());
        paymentRequestDto.setExternalId(transactionId);
        paymentRequestDto.setPayer(new Payer(transactionChannelRequestDto.getPayer()[0].getKey(), phoneNumber));
        paymentRequestDto.setPayerMessage(transactionChannelRequestDto.getPayer()[1].getValue());
        paymentRequestDto.setPayeeNote(transactionChannelRequestDto.getPayer()[1].getValue());
        return paymentRequestDto;
    }

    /**
     * Generate the transaction identifier.
     *
     * @return the transaction id
     */
    public static String generateWorkflowId() {
        return UUID.randomUUID().toString();
    }

    /**
     * Creates a GSMA transfer request.
     *
     * @param channelValidationResponse
     *            {@link ChannelValidationResponse}
     * @param primaryIdentifier
     *            the primary identifier type
     * @param primaryIdentifierValue
     *            the primary identifier value
     * @param timer
     *            the confirmation timer
     * @return {@link GsmaTransfer}
     */
    public static GsmaTransfer createGsmaTransferRequest(ChannelValidationResponse channelValidationResponse,
            String primaryIdentifier, String primaryIdentifierValue, String timer) {

        Party payer = new Party();
        payer.setPartyIdIdentifier(channelValidationResponse.msisdn());
        payer.setPartyIdType(SECONDARY_IDENTIFIER_NAME);

        Party payee = new Party();
        payee.setPartyIdIdentifier(primaryIdentifierValue);
        payee.setPartyIdType(primaryIdentifier);

        GsmaTransfer gsmaTransfer = new GsmaTransfer();
        List<CustomData> customData = createCustomData(channelValidationResponse, timer);
        gsmaTransfer.setCustomData(customData);
        String currentDateTime = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").format(new Date());
        gsmaTransfer.setRequestDate(currentDateTime);
        gsmaTransfer.setPayee(List.of(payee));
        gsmaTransfer.setPayer(List.of(payer));
        gsmaTransfer.setSubType("inbound");
        gsmaTransfer.setType("mtn");
        gsmaTransfer.setDescriptionText("description");
        gsmaTransfer.setRequestingOrganisationTransactionReference(channelValidationResponse.transactionId());
        gsmaTransfer.setAmount(channelValidationResponse.amount());
        gsmaTransfer.setCurrency(channelValidationResponse.currency());
        return gsmaTransfer;
    }

    /**
     * Creates custom data for the GSMA transfer request.
     *
     * @param validationResponse
     *            {@link ChannelValidationResponse}
     * @param timer
     *            the confirmation timer
     * @return a list of {@link CustomData}
     */
    private static List<CustomData> createCustomData(ChannelValidationResponse validationResponse, String timer) {
        CustomData reconciled = new CustomData(PARTY_LOOKUP_FAILED, !validationResponse.reconciled());
        CustomData confirmationReceived = new CustomData(CONFIRMATION_RECEIVED, false);
        CustomData transactionId = new CustomData(TRANSACTION_ID, validationResponse.transactionId());
        CustomData ams = new CustomData(AMS, validationResponse.amsName());
        CustomData tenantId = new CustomData(TENANT_ID, validationResponse.accountHoldingInstitutionId());
        CustomData clientCorrelationId = new CustomData(CLIENT_CORRELATION_ID, validationResponse.transactionId());
        CustomData currency = new CustomData(CURRENCY, validationResponse.currency());
        CustomData confirmationTimer = new CustomData(CONFIRMATION_TIMER, timer);
        return List.of(reconciled, confirmationReceived, transactionId, ams, tenantId, clientCorrelationId, currency,
                confirmationTimer);
    }

    /**
     * Constructs the workflow identifier.
     *
     * @param type
     *            the workflow type
     * @param subtype
     *            the workflow subtype
     * @param amsName
     *            the AMS name
     * @param accountHoldingInstitutionId
     *            the account holding institution ID
     * @return the workflow identifier
     */
    public static String getWorkflowId(String type, String subtype, String amsName,
            String accountHoldingInstitutionId) {
        return subtype + "_" + type + "_" + amsName + "-" + accountHoldingInstitutionId;

    }

    /**
     * Extracts a value from the input string based on the provided regex.
     *
     * @param input
     *            the input string
     * @param regex
     *            the regex pattern for the extraction
     * @return the extracted value or null if the input is blank
     */
    private static String extractValue(String input, String regex) {
        if (!StringUtils.hasText(input)) {
            return null;
        }
        return input.trim().replaceAll(regex, "");
    }

    /**
     * Extracts the paybill account number from the input string.
     *
     * @param input
     *            the input string
     * @return the extracted account number or null if the input is blank
     */
    public static String extractPaybillAccountNumber(String input) {
        return extractValue(input, PAYBILL_ACCOUNT_NUMBER_EXTRACTION_REGEX);
    }

    /**
     * Extracts the paybill MSISDN from the input string.
     *
     * @param input
     *            the input string
     * @return the extracted MSISDN or null if the input is blank
     */
    public static String extractPaybillMsisdn(String input) {
        return extractValue(input, PAYBILL_MSISDN_EXTRACTION_REGEX);
    }

    /**
     * Retrieves the country from the exchange properties.
     *
     * @param exchange
     *            the Camel exchange
     * @return the country code or the default tenant if not found
     */
    public static String getCountryFromExchange(Exchange exchange) {
        return Optional.ofNullable(exchange.getProperty(PLATFORM_TENANT_ID, String.class)).orElse(DEFAULT_TENANT);
    }
}
