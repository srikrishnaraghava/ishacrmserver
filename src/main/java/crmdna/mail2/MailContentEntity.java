package crmdna.mail2;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Date;
import java.util.Set;

@Entity
@Cache
public class MailContentEntity {
    @Id
    long mailContentId;

    String subject;
    String body;

    @Index
    String owner;

    @Index
    long updatedMS;

    String displayName;

    @Index
    String name;

    @Index
    long groupId;

    @Index
    Long programTypeId;

    @Index
    Set<String> tags;

    public MailContentProp toProp() {
        MailContentProp prop = new MailContentProp();
        prop.mailContentId = mailContentId;
        prop.subject = subject;
        prop.displayName = displayName;
        prop.name = name;
        prop.body = body;

        prop.updatedBy = owner;
        prop.updatedTS = new Date(updatedMS);
        prop.groupId = groupId;

        prop.tags = tags;

        return prop;
    }
}
