package org.mifos.connector.mtn.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.mtn.MtnConnectorApplicationTests;

class AccessTokenStoreTest extends MtnConnectorApplicationTests {

    @DisplayName("Store and retrieve access token string value")
    @Test
    void store_and_retrieve_access_token() {
        AccessTokenStore tokenStore = new AccessTokenStore();

        String testToken = "test-access-token-123";
        tokenStore.setAccessToken(testToken);

        String retrievedToken = tokenStore.getAccessToken();

        assertEquals(testToken, retrievedToken);
    }

    @DisplayName("Check token validity with null datetime parameter")
    @Test
    void check_token_validity_with_null_datetime() {
        AccessTokenStore tokenStore = new AccessTokenStore();

        assertThrows(NullPointerException.class, () -> {
            tokenStore.isValid(null);
        });
    }

    @DisplayName("Return true when input datetime is before expiration time")
    @Test
    void test_valid_token_before_expiry() {
        AccessTokenStore tokenStore = new AccessTokenStore();
        tokenStore.setExpiresOn(3600);
        LocalDateTime testTime = LocalDateTime.now();

        boolean isValid = tokenStore.isValid(testTime);

        assertTrue(isValid);
        assertEquals(tokenStore.getExpiresOn(), tokenStore.expiresOn);
    }

    @DisplayName("Return false when input datetime is after expiration time")
    @Test
    void test_expired_token_after_expiry() {
        AccessTokenStore tokenStore = new AccessTokenStore();
        tokenStore.setExpiresOn(-3600);
        LocalDateTime testTime = LocalDateTime.now();

        boolean isValid = tokenStore.isValid(testTime);

        assertFalse(isValid);
    }
}
