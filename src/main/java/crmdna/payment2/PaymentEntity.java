package crmdna.payment2;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.condition.IfNotNull;
import crmdna.common.Utils.Currency;
import crmdna.payment2.Payment.PaymentType;

import java.util.Date;
import java.util.Set;

@Entity
@Cache
public class PaymentEntity {
    @Id
    long paymentId;

    double amount;

    @Index
    Currency currency;

    @Index
    long ms;

    @Index
    String transactionId;

    @Index
    PaymentType paymentType;

    @Index
    String email;

    @Index(IfNotNull.class)
    String chequeNo;

    String bank;

    @Index(IfNotNull.class)
    String collectedBy; // email

    @Index
    Set<String> tags;

    public PaymentProp toProp() {
        PaymentProp prop = new PaymentProp();

        prop.paymentId = paymentId;
        prop.amount = amount;
        prop.currency = currency;
        prop.transactionId = transactionId;
        prop.paymentType = paymentType;
        prop.chequeNo = chequeNo;
        prop.bank = bank;
        prop.collectedBy = collectedBy;
        prop.date = new Date(ms);
        prop.tags = tags;

        return prop;
    }
}
