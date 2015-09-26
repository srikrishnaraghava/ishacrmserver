package crmdna.mail2;

import java.util.Date;


public class MailScheduleQueryCondition {

    public Long mailContentId;

    public String userEmail;

    public Long groupId;

    public Date scheduledTimeStart;
    public Date scheduledTimeEnd;

    public Boolean sendAttempted;
    public Long listId;
    public Long programId;
    public String senderEmail;
}
