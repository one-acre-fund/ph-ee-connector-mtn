package org.mifos.connector.mtn.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO representing MTN paybill validation response.
 */
@Getter
@Setter
@XmlRootElement(name = "getfinancialresourceinformationresponse", namespace = "http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend/client")
@XmlAccessorType(XmlAccessType.FIELD)
public class FinancialResourceInformationResponse {

    private String message;
    private Extension extension;

    @Getter
    @Setter
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Extension {

        @XmlElement(name = "accountname")
        private String accountName;
        private String oafReference;
    }
}
