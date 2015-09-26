package crmdna.mail2;

import java.util.Date;

/**
 * Created by Sathya on 26/9/2015.
 */
public class MailScheduleInput {
    public long mailContentId;
    public Date scheduledTime;
    public long listId;
    public Long programId;
    public String defaultFirstName;
    public String defaultLastName;
    public String senderEmail;
}
