package org.mifos.connector.mtn.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO representing MTN paybill payment completed response.
 */
@Getter
@Setter
@XmlRootElement(name = "paymentcompletedresponse", namespace = "http://www.ericsson.com/em/emm/serviceprovider/v1_0/backend")
@XmlAccessorType(XmlAccessType.FIELD)
public class PaymentCompletedResponse {

    @XmlElement(name = "paymentstatus")
    private String paymentStatus;
}
