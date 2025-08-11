package org.mifos.connector.mtn.dto;

import static org.mifos.connector.mtn.utility.MtnConstants.PAYBILL_PAYMENT_COMPLETED_ENDPOINT_NAMESPACE;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO representing MTN paybill payment completed request.
 */
@Getter
@Setter
@XmlRootElement(name = "paymentcompletedrequest", namespace = PAYBILL_PAYMENT_COMPLETED_ENDPOINT_NAMESPACE)
@XmlAccessorType(XmlAccessType.FIELD)
public class PaymentCompletedRequest {

    @XmlElement(name = "transactionid")
    private String transactionId;
    @XmlElement(name = "providertransactionid")
    private String providerTransactionId;
    private String status;
}
