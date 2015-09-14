package crmdna.payment2;

import crmdna.common.Utils.Currency;
import crmdna.payment2.Payment.PaymentType;

import java.util.Date;
import java.util.Set;

public class PaymentProp {
    public long paymentId;
    public double amount;
    public Currency currency;
    public String transactionId;
    public PaymentType paymentType;
    public String chequeNo;
    public String bank;
    public String collectedBy; // email
    public Date date;
    public Set<String> tags;
}
