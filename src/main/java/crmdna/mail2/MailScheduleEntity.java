package crmdna.mail2;

import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import com.googlecode.objectify.annotation.Serialize;
import com.googlecode.objectify.condition.IfNotNull;
import crmdna.common.StackTraceElementProp;

import java.util.Date;
import java.util.List;

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

    int numRecipients;

    String failureReason;

    @Serialize(zip = true)
    List<StackTraceElementProp> stackTraceElementProps;


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
        mailScheduleProp.numRecipients = numRecipients;
        mailScheduleProp.failureReason = failureReason;
        mailScheduleProp.stackTraceElementProps = stackTraceElementProps;

        return mailScheduleProp;
    }
}
