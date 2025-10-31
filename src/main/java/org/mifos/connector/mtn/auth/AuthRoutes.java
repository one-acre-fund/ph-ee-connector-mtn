package org.mifos.connector.mtn.auth;

import static org.mifos.connector.mtn.utility.ConnectionUtils.createBasicAuthHeaderValue;
import static org.mifos.connector.mtn.utility.MtnUtils.getCountryFromExchange;

import java.time.LocalDateTime;
import org.apache.camel.Exchange;
import org.apache.camel.LoggingLevel;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.mifos.connector.common.gsma.dto.AccessTokenDTO;
import org.mifos.connector.mtn.utility.ConnectionUtils;
import org.mifos.connector.mtn.utility.MtnProps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Class for authentications routes.
 */
@Component
public class AuthRoutes extends RouteBuilder {

    @Autowired
    private AccessTokenStore accessTokenStore;
    @Autowired
    private MtnProps mtnProps;
    @Value("${mtn.api.timeout}")
    private Integer mtnTimeout;

    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void configure() {
        /*
         * Error handling route
         */
        from("direct:access-token-error").id("access-token-error").process(exchange -> {
            String body = exchange.getIn().getBody(String.class);
            String header = exchange.getIn().getHeaders().toString();
            logger.error(body);
            logger.error("headers:" + header);
        });

        /*
         * Save Access Token to AccessTokenStore
         */
        from("direct:access-token-save").id("access-token-save").unmarshal()
                .json(JsonLibrary.Jackson, AccessTokenDTO.class).process(exchange -> {
                    accessTokenStore.setAccessToken(getCountryFromExchange(exchange),
                            exchange.getIn().getBody(AccessTokenDTO.class).getAccess_token(),
                            exchange.getIn().getBody(AccessTokenDTO.class).getExpires_in());
                    logger.info("Saved Access Token");
                });

        /*
         * Fetch Access Token from mpesa API
         */
        from("direct:access-token-fetch").id("access-token-fetch").log(LoggingLevel.INFO, "Fetching access token")
                .removeHeader(Exchange.HTTP_PATH).process(exchange -> {
                    logger.info("AMS Name in Route " + mtnProps.getAuthHost());
                    String country = getCountryFromExchange(exchange);
                    logger.info("Tenant in Route " + country);
                    MtnProps.MtnCountryCreds countryCredentials = mtnProps.getCountryConfig().get(country);
                    String authHeader = createBasicAuthHeaderValue(countryCredentials.getClientKey(),
                            countryCredentials.getClientSecret());
                    exchange.getIn().setHeader("Authorization", authHeader);
                }).setHeader(Exchange.HTTP_METHOD, constant("POST"))
                .setHeader("X-Target-Environment", constant(mtnProps.getEnvironment()))
                .setHeader("Ocp-Apim-Subscription-Key", constant(mtnProps.getSubscriptionKey()))
                .toD(mtnProps.getAuthHost() + "/collection/token/" + "?bridgeEndpoint=true" + "&"
                        + "throwExceptionOnFailure=false&" + ConnectionUtils.getConnectionTimeoutDsl(mtnTimeout));

        /*
         * Access Token check validity and return value
         */
        from("direct:get-access-token").id("get-access-token").choice()
                .when(exchange -> accessTokenStore.isValid(getCountryFromExchange(exchange), LocalDateTime.now()))
                .log("Access token valid. Continuing.").otherwise().log("Access token expired or not present")
                .to("direct:access-token-fetch").choice().when(header("CamelHttpResponseCode").isEqualTo("200"))
                .log("Access Token Fetch Successful").to("direct:access-token-save").otherwise()
                .log("Access Token Fetch Unsuccessful").to("direct:access-token-error");
    }

}
