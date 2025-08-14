package org.mifos.connector.mtn.utility;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.stream.Stream;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mifos.connector.mtn.exception.MissingConfigurationException;

/**
 * Connection utilities test class.
 */
class ConnectionUtilsTest {

    private static Stream<Arguments> credentialsProvider() {
        return Stream.of(
                // Valid key and secret
                Arguments.of("username", "password", "username:password"),
                Arguments.of("user", "pass123", "user:pass123"),
                Arguments.of("testUser", "testPass", "testUser:testPass"),
                // Empty key and secret
                Arguments.of("", "", ":"),
                // Key and secret with newlines
                Arguments.of("user\nname", "pass\nword", "username:password"));
    }

    @DisplayName("Create Base64-encoded auth header with valid key and secret")
    @ParameterizedTest
    @MethodSource("credentialsProvider")
    void createAuthHeader_shouldReturnEncodedHeader(String key, String secret, String expectedDecoded) {
        String result = ConnectionUtils.createBasicAuthHeaderValue(key, secret);
        assertThat(result).contains("Basic ")
                .contains(Base64.getEncoder().encodeToString(expectedDecoded.getBytes(StandardCharsets.UTF_8)));
    }

    @DisplayName("Create Base64-encoded auth header with null key or secret")
    @Test
    void createAuthHeader_withNullKeyOrSecret_shouldThrowNullPointerException() {
        Assertions.assertThrows(MissingConfigurationException.class,
                () -> ConnectionUtils.createBasicAuthHeaderValue(null, "password"));
        Assertions.assertThrows(MissingConfigurationException.class,
                () -> ConnectionUtils.createBasicAuthHeaderValue("username", null));
    }
}
