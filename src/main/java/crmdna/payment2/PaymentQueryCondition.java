package crmdna.payment2;

import crmdna.common.Utils.Currency;
import crmdna.payment2.Payment.PaymentType;

import java.util.Date;
import java.util.Set;

public class PaymentQueryCondition {
    public Currency currency;
    public String transactionId;
    public PaymentType paymentType;
    public String chequeNo;
    public String collectedBy; // email
    public Date startDate;
    public Date endDate;
    public Set<String> tags;
}
