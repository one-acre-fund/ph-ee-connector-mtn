package org.mifos.connector.mtn.utility;

import java.util.Map;
import lombok.Data;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

/**
 * Models the mtn values.
 */
@Component
@Configuration
@ConfigurationProperties(prefix = "mtn")
@Data
public class MtnProps {

    private String authHost;
    private String apiHost;
    private String environment;
    private String subscriptionKey;
    private String callBack;
    private Map<String, MtnCountryCreds> countryConfig;

    /**
     * Holds the mtn user authentication credentials for a particular country
     */
    @Getter
    @Setter
    public static class MtnCountryCreds {

        private String clientKey;
        private String clientSecret;
    }
}
