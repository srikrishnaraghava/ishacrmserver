package crmdna.sessionpass;

import java.util.HashSet;
import java.util.Set;

public class SessionPassProp {

    public long sessionPassId;
    public long memberId;
    public long purchaseMS;
    public long expiryMS;
    public boolean used;
    public long programId;
    public String transactionId;
    public double amount;
    public String currency;

    public Set<String> tags = new HashSet<>();
}
