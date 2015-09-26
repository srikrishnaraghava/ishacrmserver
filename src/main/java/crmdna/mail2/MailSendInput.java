package crmdna.mail2;

import java.util.Set;

/**
 * Created by sathya on 6/9/15.
 */
public class MailSendInput {
    public boolean isTransactionEmail; //true if transactional email, false if marketing email
    public Long groupId; //null if email at client level
    public long mailContentId;
    public String senderEmail;
    public boolean suppressIfAlreadySent = true;
    public boolean createMember;
    public String overrideSubject;
    public Set<String> tags;
    public Long mailScheduleId;
}
