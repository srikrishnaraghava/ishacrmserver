package crmdna.mail2;

import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.User.GroupLevelPrivilege;

import java.util.Date;
import java.util.List;
import java.util.Set;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class MailContent {

    public static MailContentProp create(String client, String displayName, long groupId,
                                         String subject, String body, String login) {

        Client.ensureValid(client);

        User.ensureValidUser(client, login);

        ensureNotNull(displayName, "displayName is null");
        ensure(!displayName.isEmpty(), "displayName is empty");
        ensureNotNull(subject, "subject is null");
        ensureNotNull(body, "body is null");
        ensure(!subject.isEmpty(), "subject is empty");
        ensure(!body.isEmpty(), "body is empty");

        if (groupId != 0) {
            Group.safeGet(client, groupId);
        } else {
            // client specific email
            User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_MAIL_CONTENT);
        }

        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName.toLowerCase());

        int count =
                ofy(client).load().type(MailContentEntity.class).filter("name", name)
                        .filter("groupId", groupId).count();

        if (count != 0) {
            throw new APIException("There is another MailContent with the same name for group ["
                    + groupId + "]").status(Status.ERROR_RESOURCE_ALREADY_EXISTS);
        }

        MailContentEntity entity = new MailContentEntity();
        entity.mailContentId = Sequence.getNext(client, SequenceType.MAIL_CONTENT);
        entity.body = body;
        entity.subject = subject;
        entity.displayName = displayName;
        entity.name = name;
        entity.owner = login.toLowerCase();
        entity.updatedMS = new Date().getTime();
        entity.groupId = groupId;

        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static MailContentEntity safeGet(String client, long mailContentId) {
        Client.ensureValid(client);

        MailContentEntity entity =
                ofy(client).load().type(MailContentEntity.class).id(mailContentId).now();

        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Mail content id [" + mailContentId + "] not found for client [" + client + "]");

        return entity;
    }

    public static MailContentEntity getByName(String client, String name, long groupId) {
        Client.ensureValid(client);
        ensureNotNull(name, "name is null");

        name = Utils.removeSpaceUnderscoreBracketAndHyphen(name.toLowerCase());
        List<MailContentEntity> entities =
                ofy(client).load().type(MailContentEntity.class).filter("name", name)
                        .filter("groupId", groupId).list();

        if (entities.isEmpty())
            return null;

        if (entities.size() > 1) {
            RuntimeException ex =
                    new RuntimeException("Alert: There are [" + entities.size()
                            + "] MailContent entities with name [" + name + "] for group [" + groupId + "]");
            Utils.sendAlertEmailToDevTeam(client, ex, null, "unknown");
        }

        return entities.get(0);
    }

    public static List<MailContentEntity> query(String client, String owner, Long startMS,
                                                Long endMS, Set<String> tags, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        Query<MailContentEntity> q = ofy(client).load().type(MailContentEntity.class);
        if (owner != null)
            q = q.filter("owner", owner);

        if (startMS != null)
            q = q.filter("updatedMS >=", startMS);

        if (endMS != null)
            q = q.filter("updatedMS <=", endMS);

        q = q.order("-updatedMS");

        if (tags != null) {
            q = q.filter("tags in", tags);
        }

        return q.list();
    }

    public static MailContentProp update(String client, long mailContentId, String newDisplayName,
                                         String newSubject, String newBody, boolean allowIfMailSent, String login) {

        Client.ensureValid(client);

        User.ensureValidUser(client, login);

        int numEmailsAlreadySent =
                ofy(client).load().type(SentMailEntity.class).filter("mailContentId", mailContentId)
                        .count();

        MailContentEntity entity = safeGet(client, mailContentId);

        if (!entity.owner.equalsIgnoreCase(login)) {
            if (!entity.owner.equalsIgnoreCase(login)) {
                if (entity.groupId != 0)
                    User.ensureGroupLevelPrivilege(client, entity.groupId, login,
                            GroupLevelPrivilege.UPDATE_MAIL_CONTENT);
                else {
                    User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_MAIL_CONTENT);
                }
            }
        }

        if (newDisplayName != null) {
            ensure(!newDisplayName.isEmpty(), "newDisplayName is empty");

            String newName = Utils.removeSpaceUnderscoreBracketAndHyphen(newDisplayName.toLowerCase());

            if (!newName.equals(entity.name)) {
                int count =
                        ofy(client).load().type(MailContentEntity.class).filter("name", newName)
                                .filter("groupId", entity.groupId).count();

                if (count != 0)
                    throw new APIException("There is another MailContent with the same name")
                            .status(Status.ERROR_RESOURCE_ALREADY_EXISTS);
            }

            entity.displayName = newDisplayName;
            entity.name = newName;
        }

        if (newSubject != null) {
            if (!allowIfMailSent && (numEmailsAlreadySent != 0))
                throw new APIException().status(Status.ERROR_OPERATION_NOT_ALLOWED).message(
                        "Subject cannot be updated as [" + numEmailsAlreadySent
                                + "] email(s) have already gone out with this content. "
                                + " Please create new Mail Content (rather than updating)");

            ensure(!newSubject.isEmpty(), "newSubject is empty");
            entity.subject = newSubject;
        }

        if (newBody != null) {
            if (!allowIfMailSent && (numEmailsAlreadySent != 0))
                throw new APIException().status(Status.ERROR_OPERATION_NOT_ALLOWED).message(
                        "Mail content cannot be updated as [" + numEmailsAlreadySent
                                + "] email(s) have already gone out with this content. "
                                + " Please create new Mail Content (rather than updating)");

            ensure(!newBody.isEmpty(), "newBody is empty");
            entity.body = newBody;
        }

        ofy(client).save().entity(entity);

        return entity.toProp();
    }

    public static void delete(String client, long mailContentId, String login) {

        Client.ensureValid(client);

        User.ensureValidUser(client, login);

        int numEmailsAlreadySent =
                ofy(client).load().type(SentMailEntity.class).filter("mailContentId", mailContentId)
                        .count();

        if (numEmailsAlreadySent != 0)
            throw new APIException().status(Status.ERROR_OPERATION_NOT_ALLOWED).message(
                    "Content cannot be deleted as [" + numEmailsAlreadySent
                            + "] email(s) have already gone out with this content.");

        MailContentEntity entity = safeGet(client, mailContentId);
        if (!entity.owner.equalsIgnoreCase(login)) {
            if (entity.groupId != 0)
                User.ensureGroupLevelPrivilege(client, entity.groupId, login,
                        GroupLevelPrivilege.UPDATE_MAIL_CONTENT);
            else {
                User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_MAIL_CONTENT);
            }
        }

        ofy(client).delete().entity(entity).now();
    }

    public static void ensureReservedMailContentExists(String client, long groupId, ReservedMailContentName name) {
        MailContentEntity entity = MailContent.getByName(client, name.toString(), groupId);
        ensureNotNull(entity, "MailContent for " + name + " not found");
    }

    public enum ReservedMailContentName {
        RESERVED_EMAIL_VERIFICATION, RESERVED_PASSWORD_CHANGE, RESERVED_PASSWORD_RESET, RESERVED_REGISTRATION_CONFIRMATION, RESERVED_SUBSCRIPTION_PURCHASE, RESERVED_RECEIPT
    }

    public enum MailContentTag {
        NURTURE,
        REGIGISTRATION
    }
}
