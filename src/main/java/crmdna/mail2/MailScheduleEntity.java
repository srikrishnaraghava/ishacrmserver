package crmdna.mail2;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.condition.IfNotNull;

import java.util.Date;

@Entity
public class MailScheduleEntity {
    @Id
    long mailScheduleId;

    @Index
    long mailContentId;

    @Index
    long listId;

    @Index(IfNotNull.class)
    Long programId;

    String defaultFirstName;
    String defaultLastName;

    @Index
    String userEmail;

    @Index
    long groupId;

    @Index
    String senderEmail;

    @Index
    long scheduledTimeMs;

    @Index
    boolean sendAttempted;

    Long sendAttemptedTimeMs;

    @Index
    Boolean sendSuccess;

    String failureReason;

    public MailScheduleProp toProp() {
        MailScheduleProp mailScheduleProp = new MailScheduleProp();

        mailScheduleProp.mailScheduleId = mailScheduleId;
        mailScheduleProp.mailContentId = mailContentId;
        mailScheduleProp.userEmail = userEmail;
        mailScheduleProp.groupId = groupId;
        mailScheduleProp.scheduledTime = new Date(scheduledTimeMs);
        mailScheduleProp.sendAttempted = sendAttempted;
        mailScheduleProp.sendAttemptedTime = new Date(sendAttemptedTimeMs);
        mailScheduleProp.sendSuccess = sendSuccess;
        mailScheduleProp.failureReason = failureReason;

        return mailScheduleProp;
    }
}
