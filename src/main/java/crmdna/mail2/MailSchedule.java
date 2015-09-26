package crmdna.mail2;

import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.group.Group;
import crmdna.list.ListProp;
import crmdna.member.MemberQueryCondition;
import crmdna.program.Program;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static crmdna.common.AssertUtils.*;
import static crmdna.common.OfyService.ofy;

public class MailSchedule {

    public static MailScheduleProp create(String client, MailScheduleInput msi, String login) {

        Client.ensureValid(client);

        ensureNotNull(msi, "MailScheduleInput is null");
        ensure(msi.mailContentId != 0, "mailContentId is 0");
        MailContent.safeGet(client, msi.mailContentId);

        ensureNotNull(msi.scheduledTime, "scheduledTime not specified");
        ensure(msi.scheduledTime.getTime() < System.currentTimeMillis(), "scheduledTime [" + msi.scheduledTime
                + "] is in the past");

        ensure(msi.listId != 0, "listId is 0");
        ListProp listProp = crmdna.list.List.safeGet(client, msi.listId).toProp();

        User.ensureGroupLevelPrivilege(client, listProp.groupId, login, GroupLevelPrivilege.SEND_EMAIL);

        Group.safeGet(client, listProp.groupId);

        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        mqc.listIds.add(msi.listId);
        if (msi.programId != null) {
            Program.safeGet(client, msi.programId);
            mqc.programIds.add(msi.programId);
        }

        MailMap mailMap = MailMapFactory.getFromMemberQueryCondition(mqc, listProp.groupId,
                msi.defaultFirstName, msi.defaultLastName, login);
        ensure(!mailMap.isEmpty(), "No email recipients");

        ensureNotNullNotEmpty(msi.senderEmail, "senderEmail not specified");
        Group.safeGetSenderNameFromEmail(client, listProp.groupId, msi.senderEmail);

        //todo - check if reserved mail contents are present

        MailScheduleEntity mailScheduleEntity = new MailScheduleEntity();
        mailScheduleEntity.mailScheduleId = Sequence.getNext(client, SequenceType.MAIL_SCHEDULE);
        mailScheduleEntity.defaultFirstName = msi.defaultFirstName;
        mailScheduleEntity.defaultLastName = msi.defaultLastName;
        mailScheduleEntity.groupId = listProp.groupId;
        mailScheduleEntity.mailContentId = msi.mailContentId;
        mailScheduleEntity.userEmail = login;
        mailScheduleEntity.programId = msi.programId;
        mailScheduleEntity.scheduledTimeMs = msi.scheduledTime.getTime();
        mailScheduleEntity.senderEmail = msi.senderEmail;

        ofy(client).save().entity(mailScheduleEntity).now();

        return mailScheduleEntity.toProp();
    }

    public static MailScheduleEntity safeGet(String client, long mailScheduleId) {
        Client.ensureValid(client);

        ensure(mailScheduleId != 0, "mailScheduleId is 0 (should be positive)");

        MailScheduleEntity mse = ofy(client).load().type(MailScheduleEntity.class)
                .id(mailScheduleId).now();

        if (mse == null) {
            throw new APIException().status(APIResponse.Status.ERROR_RESOURCE_NOT_FOUND)
                    .message("Mail schedule id [" + mailScheduleId +
                            "] not found for client [" + client + "]");
        }

        return mse;
    }

    public static void delete(String client, long mailScheduleId, String login) {
        Client.ensureValid(client);

        MailScheduleEntity entity = safeGet(client, mailScheduleId);

        ensure(!entity.sendAttempted, "Mail schedule id [" + mailScheduleId + "] has already been processed");

        User.ensureGroupLevelPrivilege(client, entity.groupId, login,
                GroupLevelPrivilege.UPDATE_MAIL_SCHEDULE);

        ofy(client).delete().entity(entity).now();
    }

    public static List<MailScheduleProp> query(String client, MailScheduleQueryCondition qc, String login) {
        Client.ensureValid(client);

        User.ensureValidUser(client, login);

        Query<MailScheduleEntity> q = ofy(client).load().type(MailScheduleEntity.class);

        if (qc.mailContentId != null) {
            q = q.filter("mailContentId", qc.mailContentId);
        }
        if (qc.listId != null) {
            q = q.filter("listId", qc.listId);
        }
        if (qc.programId != null) {
            q = q.filter("programId", qc.programId);
        }
        if (qc.userEmail != null) {
            q = q.filter("userEmail", qc.userEmail);
        }
        if (qc.groupId != null) {
            q = q.filter("groupId", qc.groupId);
        }
        if (qc.senderEmail != null) {
            q = q.filter("senderEmail", qc.senderEmail);
        }
        if (qc.sendAttempted != null) {
            q = q.filter("sendAttempted", qc.sendAttempted);
        }
        if (qc.scheduledTimeStart != null) {
            q = q.filter("scheduledTimeMs >=", qc.scheduledTimeStart.getTime());
        }
        if (qc.scheduledTimeEnd != null) {
            q = q.filter("scheduledTimeMs <=", qc.scheduledTimeEnd.getTime());
        }

        int resultSize = q.count();
        final int MAX = 5000;
        if (resultSize > MAX) {
            throw new APIException().status(APIResponse.Status.ERROR_OVERFLOW)
                    .message("Query would fetch [" + resultSize + "] entities. Max allowed is [" + MAX + "]");
        }

        q = q.order("scheduledTimeMs");

        List<MailScheduleEntity> entities = q.list();
        List<MailScheduleProp> props = new ArrayList();

        for (MailScheduleEntity entity : entities) {
            props.add(entity.toProp());
        }

        return props;
    }

    public static void processFirstScheduledEmail(String client, Date currentTime) {

        Client.ensureValid(client);
        ensureNotNull(currentTime, "currentTime not specified");

        List<MailScheduleEntity> entities = ofy(client).load().type(MailScheduleEntity.class).filter("sendAttempted", false)
                .filter("scheduledTimeMs <=", currentTime.getTime()).order("scheduledTimeMs").limit(1).list();

        if (entities.isEmpty()) {
            return;
        }

        ensureEqual(1, entities.size(),
                "MailScheduleEntity query fetched [" + entities.size() + "] entities but expected 1");

        MailScheduleEntity mse = entities.get(0);

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.groupId = mse.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = mse.mailContentId;
        msi.senderEmail = mse.senderEmail;
        msi.suppressIfAlreadySent = true;
        msi.mailScheduleId = mse.mailScheduleId;

        MemberQueryCondition mqc = new MemberQueryCondition();
        mqc.listIds.add(mse.listId);
        if (mse.programId != null) {
            mqc.programIds.add(mse.programId);
        }

        MailMap mailMap = MailMapFactory.getFromMemberQueryCondition(mqc, mse.groupId, mse.defaultFirstName,
                mse.defaultLastName, mse.userEmail);

        mse.sendAttempted = true;
        mse.sendAttemptedTimeMs = System.currentTimeMillis();

        try {
            List<SentMailEntity> sentMailEntities = Mail.send(client, msi, mailMap, mse.userEmail);
            mse.numRecipients = sentMailEntities.size();

            //todo - send mail to user saying scheduled email has been sent successfully
            //need to create a reserved mail content id

        } catch (Exception ex) {
            mse.failureReason = ex.getMessage();
            mse.stackTraceElementProps = Utils.getStackTrace(ex);

            //todo - send mail to user saying scheduled email has not been sent successfully
            //need to create a reserved mail content id
            //attach stack trace
        }
    }
}
