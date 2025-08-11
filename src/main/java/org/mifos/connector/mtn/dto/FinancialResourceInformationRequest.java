package org.mifos.connector.mtn.dto;

import static org.mifos.connector.mtn.utility.MtnConstants.MTN_PAYBILL_GET_FINANCIAL_RESOURCE_INFORMATION_ENDPOINT_NAMESPACE;

import javax.validation.constraints.NotBlank;
import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO representing MTN paybill validation request.
 */
@Getter
@Setter
@XmlRootElement(name = "getfinancialresourceinformationrequest", namespace = MTN_PAYBILL_GET_FINANCIAL_RESOURCE_INFORMATION_ENDPOINT_NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class FinancialResourceInformationRequest {

    @NotBlank
    private String resource;

    @NotBlank
    @XmlElement(name = "accountholderid")
    private String accountHolderId;
}
