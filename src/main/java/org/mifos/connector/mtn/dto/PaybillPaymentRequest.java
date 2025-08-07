package org.mifos.connector.mtn.dto;

import java.math.BigDecimal;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO representing MTN paybill payment request.
 */
@Getter
@Setter
@XmlRootElement(name = "paymentrequest", namespace = "http://www.ericsson.com/em/emm/serviceprovider/v1_1/backend/client")
@XmlAccessorType(XmlAccessType.FIELD)
public class PaybillPaymentRequest {

    @NotBlank
    @XmlElement(name = "transactionid")
    private String transactionId;

    @NotBlank
    @XmlElement(name = "accountholderid")
    private String accountHolderId;

    @NotBlank
    @XmlElement(name = "receivingfri")
    private String receivingFri;

    @NotNull
    @Valid
    private Amount amount;
    @XmlElement(name = "transmissioncounter")
    private int transmissionCounter;

    @NotNull
    @Valid
    private Extension extension;

    /**
     * Amount class representing the amount and currency information.
     */
    @Getter
    @Setter
    public static class Amount {

        @NotNull
        @Positive
        private BigDecimal amount;

        @NotBlank
        private String currency;
    }

    /**
     * Extension class for additional properties.
     */
    @Getter
    @Setter
    public static class Extension {

        @NotBlank
        private String oafReference;
    }
}
