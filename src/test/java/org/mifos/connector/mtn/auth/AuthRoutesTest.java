package org.mifos.connector.mtn.auth;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDateTime;
import org.apache.camel.FluentProducerTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mifos.connector.mtn.MtnConnectorApplicationTests;
import org.springframework.beans.factory.annotation.Autowired;

class AuthRoutesTest extends MtnConnectorApplicationTests {

    @Autowired
    private FluentProducerTemplate fluentProducerTemplate;

    @Autowired
    private AccessTokenStore accessTokenStore;

    @DisplayName("Test Access Token Save Route")
    @Test
    void testAccessTokenSaveRoute() {

        // JSON input for the route
        String inputJson = """
                {
                  "access_token": "test-access-token",
                  "expires_in": 3600
                }
                """;

        // Send input to the route
        fluentProducerTemplate.to("direct:access-token-save").withBody(inputJson).send();

        LocalDateTime actualExpirationTime = accessTokenStore.getExpiresOn();
        LocalDateTime expectedExpirationTime = LocalDateTime.now().plusSeconds(3600);

        // Assertions
        assertEquals("test-access-token", accessTokenStore.getAccessToken());
        assertTrue(
                !actualExpirationTime.isBefore(expectedExpirationTime.minusSeconds(5))
                        && !actualExpirationTime.isAfter(expectedExpirationTime.plusSeconds(5)),
                "Expiration time is within tolerance range");
    }

}
