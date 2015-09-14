package crmdna.sessionpass;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.HashSet;
import java.util.Set;

@Entity
@Cache
public class SessionPassEntity {
    @Id
    long sessionPassId;

    @Index
    long memberId;

    @Index
    long purchaseMS;

    @Index
    String purchaseUpdatedBy;

    double amount;
    String currency;

    @Index
    long expiryMS;

    @Index
    boolean used;

    @Index
    long programId;

    @Index
    String transactionId;

    @Index
    Set<String> tags = new HashSet<>();

    public SessionPassProp toProp() {
        SessionPassProp prop = new SessionPassProp();

        prop.sessionPassId = sessionPassId;
        prop.memberId = memberId;
        prop.purchaseMS = purchaseMS;
        prop.expiryMS = expiryMS;
        prop.used = used;
        prop.programId = programId;
        prop.transactionId = transactionId;
        prop.tags = tags;

        return prop;
    }
}
