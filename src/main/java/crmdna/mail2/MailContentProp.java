package crmdna.mail2;

import java.util.Date;
import java.util.Set;


public class MailContentProp {
    public long mailContentId;

    public String subject;
    public String body;
    public String displayName;
    public String name;
    public long groupId;

    public String updatedBy;
    public Date updatedTS;

    public Set<String> tags;

    public String bodyUrl; // used from api explorer
}
