package org.mifos.connector.mtn.dto;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO representing MTN paybill payment response.
 */
@Getter
@Setter
@XmlRootElement(name = "paymentresponse", namespace = "http://www.ericsson.com/em/emm/sp/backend")
@XmlAccessorType(XmlAccessType.FIELD)
public class PaybillPaymentResponse {

    @XmlElement(name = "providertransactionid")
    private String providerTransactionId;
    private String status;
}
