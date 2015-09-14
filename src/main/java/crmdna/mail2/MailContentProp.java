package crmdna.mail2;

import java.util.Date;


public class MailContentProp {
    public long mailContentId;

    public String subject;
    public String body;
    public String displayName;
    public String name;
    public long groupId;

    public String updatedBy;
    public Date updatedTS;

    public String bodyUrl; // used from api explorer
}
