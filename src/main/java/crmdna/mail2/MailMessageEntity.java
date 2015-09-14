package crmdna.mail2;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class MailMessageEntity {
    @Id
    public long mailMessageId;

    @Index
    String name;
    String displayName;

    @Index
    long mktMailId;

    @Index
    long mktMailCampaignId;

    @Index
    long groupId;

    String subject;
    String content;
}
