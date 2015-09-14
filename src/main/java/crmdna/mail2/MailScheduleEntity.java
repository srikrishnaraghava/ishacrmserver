package crmdna.mail2;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Date;

@Entity
public class MailScheduleEntity {
    @Id
    long mailScheduleId;

    @Index
    long mailContentId;

    @Index
    long listId;

    String defaultFirstName;
    String defaultLastName;

    @Index
    String owner;

    @Index
    long groupId;

    @Index
    long scheduledTimeMs;

    @Index
    boolean sendAttempted;

    @Index
    boolean cancelled;

    Long sendAttemptedTimeMs;

    @Index
    Boolean sendSuccess;

    String failureReason;

    public MailScheduleProp toProp() {
        MailScheduleProp mailScheduleProp = new MailScheduleProp();

        mailScheduleProp.mailScheduleId = mailScheduleId;
        mailScheduleProp.mailContentId = mailContentId;
        mailScheduleProp.owner = owner;
        mailScheduleProp.groupId = groupId;
        mailScheduleProp.scheduledTime = new Date(scheduledTimeMs);
        mailScheduleProp.sendAttempted = sendAttempted;
        mailScheduleProp.sendAttemptedTime = new Date(sendAttemptedTimeMs);
        mailScheduleProp.sendSuccess = sendSuccess;
        mailScheduleProp.failureReason = failureReason;
        mailScheduleProp.cancelled = cancelled;

        return mailScheduleProp;
    }
}
