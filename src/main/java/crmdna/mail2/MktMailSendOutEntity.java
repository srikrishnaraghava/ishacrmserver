package crmdna.mail2;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class MktMailSendOutEntity {
    @Id
    long mktMailSendOutId;

    @Index
    String name;
    String displayName;

    @Index
    long mailMessageId;

    @Index
    long ts; // milliseconds since 1 Jan 1970
    @Index
    String fromEmail;
    @Index
    String login;
    @Index
    long groupId;
    @Index
    long mktMailId;

    @Index
    long programId;

    String subject;
}
