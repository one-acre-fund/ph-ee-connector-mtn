package org.mifos.connector.mtn.utility;

/**
 * Constants used in the MTN Connector. This class contains static final strings representing various endpoint
 * namespaces.
 */
public final class MtnConstants {

    private MtnConstants() {
        // Prevent instantiation
    }

    public static final String PAYBILL_GET_FINANCIAL_RESOURCE_INFO_ENDPOINT_NAMESPACE = "http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend/client";
    public static final String PAYBILL_PAYMENT_ENDPOINT_NAMESPACE = "http://www.ericsson.com/em/emm/serviceprovider/v1_1/backend/client";
    public static final String PAYBILL_PAYMENT_COMPLETED_ENDPOINT_NAMESPACE = "http://www.ericsson.com/em/emm/serviceprovider/v1_1/backend";
}
