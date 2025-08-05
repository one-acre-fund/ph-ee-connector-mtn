package org.mifos.connector.mtn.dto;

import static org.mifos.connector.mtn.camel.config.CamelProperties.CURRENCY;
import static org.mifos.connector.mtn.camel.config.CamelProperties.GET_ACCOUNT_DETAILS_FLAG;
import static org.mifos.connector.mtn.camel.config.CamelProperties.SECONDARY_IDENTIFIER_NAME;
import static org.mifos.connector.mtn.utility.MtnUtils.extractPaybillAccountNumber;
import static org.mifos.connector.mtn.utility.MtnUtils.extractPaybillMsisdn;
import static org.mifos.connector.mtn.zeebe.ZeebeVariables.TRANSACTION_ID;

import java.util.List;
import org.mifos.connector.common.gsma.dto.CustomData;

/**
 * DTO to be used in sending validation request to channel connector.
 */
public record ChannelValidationRequest(CustomData primaryIdentifier, CustomData secondaryIdentifier,
        List<CustomData> customData) {

    /**
     * Create a {@link ChannelValidationRequest} from a {@link FinancialResourceInformationRequest}.
     *
     * @param request
     *            {@link FinancialResourceInformationRequest}
     * @param paybillProps
     *            {@link PaybillProps}
     * @param transactionId
     *            transaction ID
     * @return {@link ChannelValidationRequest}
     */
    public static ChannelValidationRequest fromPaybillValidation(FinancialResourceInformationRequest request,
            PaybillProps paybillProps, String transactionId) {
        CustomData primaryIdentifier = new CustomData(paybillProps.getAmsIdentifier(),
                extractPaybillAccountNumber(request.getResource()));
        CustomData secondaryIdentifier = new CustomData(SECONDARY_IDENTIFIER_NAME,
                extractPaybillMsisdn(request.getAccountHolderId()));
        CustomData txnId = new CustomData(TRANSACTION_ID, transactionId);
        CustomData currency = new CustomData(CURRENCY, paybillProps.getCurrency());
        CustomData getAccountDetails = new CustomData(GET_ACCOUNT_DETAILS_FLAG, Boolean.TRUE);
        return new ChannelValidationRequest(primaryIdentifier, secondaryIdentifier,
                List.of(txnId, currency, getAccountDetails));
    }
}
