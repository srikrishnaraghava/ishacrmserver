package crmdna.mail2;

import crmdna.client.Client;
import crmdna.group.Group;
import crmdna.list.ListProp;
import crmdna.member.MemberQueryCondition;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;

import java.util.Date;
import java.util.List;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class MailSchedule {

    public static MailScheduleProp create(String client, long mailContentId, Date scheduledTime,
                                          long listId, String defaultFirstName, String defaultLastName, String login) {

        Client.ensureValid(client);
        MailContent.safeGet(client, mailContentId);

        ensureNotNull(scheduledTime, "scheduledTime is null");
        ensure(scheduledTime.getTime() < System.currentTimeMillis(), "scheduledTime [" + scheduledTime
                + "] is in the past");

        ListProp listProp = crmdna.list.List.safeGet(client, listId).toProp();

        User.ensureGroupLevelPrivilege(client, listProp.groupId, login, GroupLevelPrivilege.SEND_EMAIL);

        String groupName = Group.safeGet(client, listProp.groupId).toProp().displayName;

        // lists should have subscribed members
        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        mqc.listIds.add(listId);
        MailMap mailMap = MailMapFactory.getFromMemberQueryCondition(mqc, listProp.groupId,
                defaultFirstName, defaultLastName, login);

        ensure(!mailMap.isEmpty(), "There are no members in list [" + listProp.displayName
                + "] for group [" + groupName + "]");

        // There should be another scheduled email for the list within 24 hours
        List<MailScheduleEntity> existingScheduleEntities =
                ofy(client).load().type(MailScheduleEntity.class).filter("listId", listId)
                        .filter("cancelled", false).filter("sendAttempted", false).list();

        for (MailScheduleEntity existingScheduleEntity : existingScheduleEntities) {
            long diff = Math.abs(existingScheduleEntity.scheduledTimeMs - scheduledTime.getTime());
            final long DAY_IN_MS = 86400 * 1000;
            ensure(diff < DAY_IN_MS, "There is another scheduled email within 24 hours");
        }

        MailScheduleEntity mailScheduleEntity = new MailScheduleEntity();
        mailScheduleEntity.mailScheduleId = Sequence.getNext(client, SequenceType.MAIL_SCHEDULE);
        mailScheduleEntity.mailContentId = mailContentId;

        // wip
        return null;
    }

    public static MailScheduleProp safeGet(String client, long mailScheduleId) {
        return null;
    }

    public static MailScheduleProp cancel(String client, long mailScheduleId, String login) {
        return null;
    }

    public static MailScheduleProp updateTimeToSend(String client, Date newTimeToSend, String login) {
        return null;
    }

    public static MailScheduleProp undoCancel(String client, long mailScheduleId, String login) {
        return null;
    }

    public static List<MailScheduleProp> query(String client, MailScheduleQueryCondition qc,
                                               String login) {
        return null;
    }

    public static void processScheduledEmails(String client, Date currentTime) {

    }
}
