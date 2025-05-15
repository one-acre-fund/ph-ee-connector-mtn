package org.mifos.connector.mtn.utility;

import static org.mifos.connector.mtn.camel.config.CamelProperties.CURRENCY;
import static org.mifos.connector.mtn.camel.config.CamelProperties.SECONDARY_IDENTIFIER_NAME;
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
import java.util.UUID;
import org.mifos.connector.common.channel.dto.TransactionChannelC2BRequestDTO;
import org.mifos.connector.common.gsma.dto.CustomData;
import org.mifos.connector.common.gsma.dto.GsmaTransfer;
import org.mifos.connector.common.gsma.dto.Party;
import org.mifos.connector.mtn.dto.ChannelValidationResponse;
import org.mifos.connector.mtn.dto.Payer;
import org.mifos.connector.mtn.dto.PaymentRequestDto;
import org.springframework.stereotype.Component;

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
}
