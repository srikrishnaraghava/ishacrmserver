package crmdna.mail2;

import java.util.Date;


public class MailScheduleQueryCondition {

    public long mailContentId;

    public String owner;

    public long groupId;

    public Date scheduledTimeStart;
    public Date scheduledTimeEnd;

    public boolean sendAttempted;
}
