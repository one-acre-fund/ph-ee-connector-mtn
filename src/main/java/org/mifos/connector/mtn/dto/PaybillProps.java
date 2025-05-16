package org.mifos.connector.mtn.dto;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Paybill related configuration properties.
 */
@Component
@ConfigurationProperties(prefix = "paybill")
@Getter
@Setter
public class PaybillProps {

    private String accountHoldingInstitutionId;
    private String timer;
    private String amsName;
    private String amsUrl;
    private String amsIdentifier;
    private String currency;
    private String paymentCompletedUrl;
    private String username;
    private String password;
}
