package org.mifos.connector.mtn.dto;

import static org.mifos.connector.mtn.camel.config.CamelProperties.SECONDARY_IDENTIFIER_NAME;

import org.json.JSONObject;

/**
 * DTO to be used by the channel connector to confirm a payment.
 */
public record ChannelConfirmationRequest(JSONObject payer, JSONObject payee, JSONObject amount) {

    /**
     * Create a {@link ChannelConfirmationRequest} from a {@link PaybillPaymentRequest}.
     *
     * @param request
     *            the AirtelConfirmationRequest to create the ChannelConfirmationRequest from
     * @param paybillProps
     *            {@link PaybillProps}
     * @return {@link ChannelConfirmationRequest}
     */
    public static ChannelConfirmationRequest fromPaybillConfirmation(PaybillPaymentRequest request,
            PaybillProps paybillProps) {
        JSONObject payer = new JSONObject();
        JSONObject partyIdInfoPayer = new JSONObject();
        partyIdInfoPayer.put("partyIdType", SECONDARY_IDENTIFIER_NAME);
        partyIdInfoPayer.put("partyIdentifier", request.getAccountHolderId());
        payer.put("partyIdInfo", partyIdInfoPayer);

        JSONObject payee = new JSONObject();
        JSONObject partyIdInfoPayee = new JSONObject();
        partyIdInfoPayee.put("partyIdType", paybillProps.getAmsIdentifier());
        partyIdInfoPayee.put("partyIdentifier", request.getReceivingFri());
        payee.put("partyIdInfo", partyIdInfoPayee);

        JSONObject amount = new JSONObject();
        amount.put("amount", request.getAmount().getAmount().toString());
        amount.put("currency", request.getAmount().getCurrency());
        return new ChannelConfirmationRequest(payer, payee, amount);
    }

    public String toString() {
        return "{" + "payer:" + payer + ", payee:" + payee + ", amount:" + amount + "}";
    }
}
