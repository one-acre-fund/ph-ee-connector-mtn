package org.mifos.connector.mtn.dto;

import static org.mifos.connector.mtn.utility.MtnConstants.PAYBILL_GET_FINANCIAL_RESOURCE_INFORMATION_ENDPOINT_NAMESPACE;

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
@XmlRootElement(name = "getfinancialresourceinformationresponse", namespace = PAYBILL_GET_FINANCIAL_RESOURCE_INFORMATION_ENDPOINT_NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class FinancialResourceInformationResponse {

    private String message;
    private Extension extension;

    /**
     * Extension class representing additional information in the response.
     */
    @Getter
    @Setter
    @XmlAccessorType(XmlAccessType.FIELD)
    public static class Extension {

        @XmlElement(name = "accountname")
        private String accountName;
        private String oafReference;
    }
}
