package crmdna.mail2;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;


public class MailScheduleProp {

    public long mailScheduleId;

    public long mailContentId;

    public String subject;

    public Set<Long> listIds = new HashSet<>();

    public String owner;

    public long groupId;

    public Date scheduledTime;

    public boolean sendAttempted;

    public Date sendAttemptedTime;

    public Boolean sendSuccess;

    public String failureReason;

    public boolean cancelled;
}
