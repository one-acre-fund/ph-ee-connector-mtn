package org.mifos.connector.mtn.dto;

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
@XmlRootElement(name = "getfinancialresourceinformationrequest", namespace = "http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend/client")
@XmlAccessorType(XmlAccessType.FIELD)
public class FinancialResourceInformationRequest {

    @NotBlank
    private String resource;

    @NotBlank
    @XmlElement(name = "accountholderid")
    private String accountHolderId;
}
