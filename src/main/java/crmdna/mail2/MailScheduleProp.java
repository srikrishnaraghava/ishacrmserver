package crmdna.mail2;

import crmdna.common.StackTraceElementProp;

import java.util.Date;
import java.util.List;

public class MailScheduleProp {

    long mailScheduleId;

    long mailContentId;

    long listId;

    Long programId;

    String defaultFirstName;
    String defaultLastName;

    String userEmail;

    long groupId;

    String senderEmail;

    Date scheduledTime;

    boolean sendAttempted;

    Date sendAttemptedTime;

    Boolean sendSuccess;

    String failureReason;
    public int numRecipients;
    public List<StackTraceElementProp> stackTraceElementProps;
}
