package crmdna.sessionpass;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class SubscriptionEntity {
    @Id
    long subscriptionId;

    @Index
    long memberId;

    long groupId;

    long purchaseMS;

    double amount;
    String currency;
    int numSessions;
    long expiryMS;

    @Index
    String transactionId;

    public SubscriptionProp toProp() {
        SubscriptionProp prop = new SubscriptionProp();

        prop.subscriptionId = subscriptionId;
        prop.memberId = memberId;
        prop.groupId = groupId;
        prop.purchaseMS = purchaseMS;
        prop.expiryMS = expiryMS;
        prop.transactionId = transactionId;
        prop.amount = amount;
        prop.currency = currency;
        prop.numSessions = numSessions;

        return prop;
    }
}
