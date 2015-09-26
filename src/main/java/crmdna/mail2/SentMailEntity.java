package crmdna.mail2;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.condition.IfNotNull;
import com.googlecode.objectify.condition.IfTrue;

import java.util.*;

@Entity
@Cache
public class SentMailEntity {
    public String rejectReason;
    @Id
    long sentMailId; // nano second timestamp
    @Index(IfNotNull.class)
    Long memberId;
    @Index
    String email;
    @Index
    long mailContentId;
    @Index(IfNotNull.class)
    Long mailScheduldId;
    String from;
    Long sendMS;
    @Index(IfTrue.class)
    boolean open;
    @Index(IfNotNull.class)
    Long openMS;
    @Index(IfNotNull.class)
    String countryCity; // country and city where a link from this email was clicked e.g: US/Texas
    @Index(IfTrue.class)
    boolean mobile; // true if a link in the email was clicked from a mobile device
    @Index(IfTrue.class)
    boolean complaint; // true if user reported it as spam
    @Index(IfTrue.class)
    boolean reject; // true if rejected by mandrill
    @Index(IfTrue.class)
    boolean softBounce;
    @Index(IfTrue.class)
    boolean hardBounce;
    @Index(IfTrue.class)
    boolean defer;
    @Index(IfTrue.class)
    boolean click;

    // store details for every click
    List<Long> clickMS = new ArrayList<>(); // milli seconds since 1 Jan 1970
    @Index
    List<Long> urlIds = new ArrayList<>(); // to be removed

    @Index
    List<String> urls = new ArrayList<>(); // url is max 100 char

    @Index(IfNotNull.class)
    Long tagSetId;

    public SentMailProp toProp() {

        SentMailProp prop = new SentMailProp();
        final int MILLION = 1000000;

        prop.sendAttempted = new Date(sentMailId / MILLION);
        prop.memberId = memberId;
        prop.email = email;
        prop.mailContentId = mailContentId;
        prop.from = from;
        prop.sendMS = sendMS;

        return prop;
    }
}
