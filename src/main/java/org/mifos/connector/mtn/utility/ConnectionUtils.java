package org.mifos.connector.mtn.utility;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Base64;
import org.mifos.connector.mtn.exception.MissingConfigurationException;

/**
 * Connection utilities.
 */
public class ConnectionUtils {

    /**
     * Private constructor to prevent instantiation.
     */
    private ConnectionUtils() {
        // Utility class, no instances allowed
    }

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

    /**
     * Creates a Basic Authentication header value from the provided key and secret.
     *
     * @param key
     *            the authentication key (e.g., username or client ID)
     * @param secret
     *            the authentication secret (e.g., password or client secret)
     * @return a Basic Authentication header value in the format "Basic <Base64-encoded key:secret>"
     * @throws MissingConfigurationException
     *             if key or secret is null
     */
    public static String createBasicAuthHeaderValue(String key, String secret) {
        return "Basic " + createAuthHeader(key, secret);
    }

    /**
     * Creates a Base64-encoded authentication header from the provided key and secret.
     *
     * @param key
     *            the authentication key (e.g., username or client ID)
     * @param secret
     *            the authentication secret (e.g., password or client secret)
     * @return a Base64-encoded string in the format "key:secret"
     */
    private static String createAuthHeader(String key, String secret) {
        if (key == null || secret == null) {
            throw new MissingConfigurationException("Key and secret must not be null");
        }
        // Defensive: strip control chars to prevent header injections.
        String skipCharacters = "\n";
        key = key.replace(skipCharacters, "");
        secret = secret.replace(skipCharacters, "");
        byte[] credential = (key + ":" + secret).getBytes(StandardCharsets.UTF_8);
        try {
            return Base64.getEncoder().encodeToString(credential);
        } finally {
            // Clear sensitive data from memory
            Arrays.fill(credential, (byte) 0);
        }
    }
}
