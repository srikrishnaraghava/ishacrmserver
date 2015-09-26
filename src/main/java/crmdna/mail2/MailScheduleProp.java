package crmdna.mail2;

import java.util.Date;

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
}
