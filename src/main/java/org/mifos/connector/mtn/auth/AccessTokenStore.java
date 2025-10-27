package org.mifos.connector.mtn.auth;

import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Class that holds the access tokens by country.
 */
@Component
public class AccessTokenStore {

    private final ConcurrentHashMap<String, String> accessTokens = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, LocalDateTime> expiresOn = new ConcurrentHashMap<>();

    public void setAccessToken(String country, String accessToken, int expiresIn) {
        accessTokens.put(country, accessToken);
        expiresOn.put(country, LocalDateTime.now().plusSeconds(expiresIn));
    }

    public String getAccessToken(String country) {
        return accessTokens.get(country);
    }

    public LocalDateTime getExpiresOn(String country) {
        return expiresOn.get(country);
    }

    /**
     * Checks if the token is still valid.
     *
     * @param dateTime
     *            the date to check time against
     * @return boolean
     */
    public boolean isValid(String country, LocalDateTime dateTime) {
        LocalDateTime expiry = expiresOn.get(country);
        return expiry != null && dateTime.isBefore(expiry);
    }
}
