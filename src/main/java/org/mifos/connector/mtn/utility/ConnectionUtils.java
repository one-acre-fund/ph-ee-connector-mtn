package org.mifos.connector.mtn.utility;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

/**
 * Connection utilities.
 */
public class ConnectionUtils {

    /**
     * returns camel dsl for applying connection timeout.
     *
     * @param timeout
     *            timeout value in ms
     * @return a string of timeout with the format needed
     */
    public static String getConnectionTimeoutDsl(final int timeout) {
        String base = "httpClient.connectTimeout={}&httpClient.connectionRequestTimeout={}&httpClient.socketTimeout={}";
        return base.replace("{}", "" + timeout);
    }

    public static String createAuthHeader(String key, String secret) {
        key = key.replace("\n", "");
        secret = secret.replace("\n", "");
        byte[] credential = (key + ":" + secret).getBytes(StandardCharsets.UTF_8);
        return Base64.getEncoder().encodeToString(credential);
    }
}
