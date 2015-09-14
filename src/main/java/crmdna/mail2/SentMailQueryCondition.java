package crmdna.mail2;

import java.util.Set;

public class SentMailQueryCondition {
    public Long memberId;
    public String email;
    public Long mailContentId;
    public Set<String> tags;
    public Boolean open;
    public Boolean click;
    public Boolean mobileClick;
    public Boolean reject;
    public Boolean softBounce;
    public Boolean hardBounce;
    public Boolean defer;
    public Set<String> clickUrls;

    // in memory filter conditions
    public Long startMS;
    public Long endMS;
    public Integer numResults;
}
