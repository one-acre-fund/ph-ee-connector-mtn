package org.mifos.connector.mtn.camel.config;

/**
 * Camel properties.
 */
public class CamelProperties {

    public CamelProperties() {}

    public static final String BUY_GOODS_REQUEST_BODY = "buyGoodsRequestBody";
    public static final String CORRELATION_ID = "correlationId";
    public static final String DEPLOYED_PROCESS = "deployedProcess";

    public static final String ACCESS_TOKEN = "accessToken";
    public static final String MTN_API_RESPONSE = "mpesaApiResponse";
    public static final String IS_RETRY_EXCEEDED = "isRetryExceeded";
    public static final String IS_TRANSACTION_PENDING = "isTransactionPending";
    public static final String LAST_RESPONSE_BODY = "lastResponseBody";
    public static final String ERROR_CODE = "errorCode";
    public static final String ERROR_DESCRIPTION = "errorDescription";
    public static final String ERROR_INFORMATION = "errorInformation";
    public static final String AMS_URL = "amsUrl";
    public static final String AMS_NAME = "amsName";
    public static final String ACCOUNT_HOLDING_INSTITUTION_ID = "accountHoldingInstitutionId";
    public static final String PRIMARY_IDENTIFIER = "primaryIdentifier";
    public static final String PRIMARY_IDENTIFIER_VALUE = "primaryIdentifierValue";
    public static final String SECONDARY_IDENTIFIER_NAME = "MSISDN";
    public static final String GET_ACCOUNT_DETAILS_FLAG = "getAccountDetails";
    public static final String CURRENCY = "currency";
    public static final String CORRELATION_ID_HEADER = "X-CorrelationID";
    public static final String CHANNEL_VALIDATION_RESPONSE = "channelValidationResponse";
    public static final String BRIDGE_ENDPOINT_QUERY_PARAM = "?bridgeEndpoint=true&throwExceptionOnFailure=false";
    public static final String PLATFORM_TENANT_ID = "Platform-TenantId";
    public static final String CONFIRMATION_REQUEST_BODY = "confirmationRequestBody";
    public static final String MTN_PAYBILL_WORKFLOW_TYPE = "mtn";
    public static final String MTN_PAYBILL_WORKFLOW_SUBTYPE = "inbound";
    public static final String PAYBILL_ACCOUNT_NUMBER_EXTRACTION_REGEX = "^FRI:|@.*$";
    public static final String PAYBILL_MSISDN_EXTRACTION_REGEX = "^ID:|/MSISDN$";

}
