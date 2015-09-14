package crmdna.group;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.Work;
import crmdna.client.Client;
import crmdna.common.EmailConfig;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.list.ListProp;
import crmdna.mail2.Mandrill;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserCore;
import crmdna.user.UserProp;

import java.util.*;

import static crmdna.common.AssertUtils.*;
import static crmdna.common.OfyService.ofy;

public class Group {
    public static GroupProp create(String client, String displayName, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_GROUP);

        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName.toLowerCase());

        List<Key<GroupEntity>> keys =
                ofy(client).load().type(GroupEntity.class).filter("name", name).keys().list();

        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a group with name [" + displayName + "]");

        String key = getUniqueKey(client, name);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a group with name [" + displayName + "]");

        GroupEntity entity = new GroupEntity();
        entity.groupId = Sequence.getNext(client, SequenceType.GROUP);
        entity.name = name;
        entity.displayName = displayName;
        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    private static String getUniqueKey(String namespace, String name) {
        return namespace + "_" + SequenceType.GROUP + "_" + name;
    }

    public static GroupEntity safeGet(String client, long groupId) {

        Client.ensureValid(client);

        GroupEntity entity = ofy(client).load().type(GroupEntity.class).id(groupId).now();
        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Group [" + groupId + "] does not exist");
        return entity;
    }

    public static GroupEntity safeGetByIdOrName(String client, String idOrName) {

        Client.ensureValid(client);
        ensureNotNull(idOrName);

        if (Utils.canParseAsLong(idOrName)) {
            long groupId = Utils.safeParseAsLong(idOrName);
            return safeGet(client, groupId);
        }

        // idOrName should be name
        idOrName = Utils.removeSpaceUnderscoreBracketAndHyphen(idOrName.toLowerCase());
        List<GroupEntity> entities =
                ofy(client).load().type(GroupEntity.class).filter("name", idOrName).list();

        if (entities.size() == 0)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Group [" + idOrName + "] does not exist");

        if (entities.size() > 1)
            // should never come here
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT)
                    .message(
                            "Found [" + entities.size() + "] matches for group [" + idOrName
                                    + "]. Please specify Id");
        return entities.get(0);
    }

    public static Map<Long, GroupProp> get(String client, Set<Long> groupIds) {

        Client.ensureValid(client);
        ensureNotNull(groupIds);
        ensureNoNullElement(groupIds);
        groupIds.remove((long) 0);

        Map<Long, GroupEntity> map = ofy(client).load().type(GroupEntity.class).ids(groupIds);

        Map<Long, GroupProp> props = new HashMap<>();

        for (Long groupId : map.keySet()) {
            props.put(groupId, map.get(groupId).toProp());
        }

        return props;
    }

    static Map<Long, GroupEntity> getEntities(String client, Set<Long> groupIds) {

        Client.ensureValid(client);
        return ofy(client).load().type(GroupEntity.class).ids(groupIds);
    }

    public static GroupProp rename(String client, long groupId, String newDisplayName, String login) {
        Client.ensureValid(client);

        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_GROUP);

        GroupEntity groupEntity = safeGet(client, groupId);

        String newName = Utils.removeSpaceUnderscoreBracketAndHyphen(newDisplayName.toLowerCase());

        if (groupEntity.name.equals(newName)) {
            // ideally should be inside a transaction
            groupEntity.displayName = newDisplayName;
            ofy(client).save().entity(groupEntity).now();
            return groupEntity.toProp();
        }

        List<Key<GroupEntity>> keys =
                ofy(client).load().type(GroupEntity.class).filter("name", newName).keys().list();
        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a center with name [" + newDisplayName + "]");

        String key = getUniqueKey(client, newDisplayName);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a center with name [" + newDisplayName + "]");

        // ideally should be inside a transaction
        groupEntity.name = newName;
        groupEntity.displayName = newDisplayName;
        ofy(client).save().entity(groupEntity).now();

        return groupEntity.toProp();
    }

    public static List<GroupProp> getAll(String client, boolean populateLists) {
        Client.ensureValid(client);

        List<GroupEntity> entities = ofy(client).load().type(GroupEntity.class).order("name").list();

        List<GroupProp> props = new ArrayList<>();
        for (GroupEntity entity : entities) {
            GroupProp groupProp = entity.toProp();
            if (populateLists) {
                groupProp.listProps = crmdna.list.List.querySortedProps(client, groupProp.groupId);
            }
            props.add(groupProp);
        }

        return props;
    }

    public static Set<Long> getAllGroupIds(String client) {

        List<GroupProp> props = getAll(client, false);

        Set<Long> groupIds = new HashSet<>();
        for (GroupProp prop : props)
            groupIds.add(prop.groupId);

        return groupIds;
    }

    public static void ensureValidGroupIds(String client, List<Long> groupIds) {
        Set<Long> allGroupIds = Group.getAllGroupIds(client);
        for (int i = 0; i < groupIds.size(); i++) {
            long groupId = groupIds.get(i);
            if (!allGroupIds.contains(groupId))
                throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                        "Error in record no [" + (i + 1) + "]: Group id [" + groupIds.get(i)
                                + "] does not exist");
        }
    }

    public static void delete(String client, long groupId, String login) {

        throw new APIException().status(Status.ERROR_NOT_IMPLEMENTED).message(
                "This functionality is not implemented yet");

        // GroupEntity groupEntity = safeGet(client, groupId);

        // ofy(client).delete().entity(groupEntity).now();
    }

    public static PaypalApiCredentialsProp setPaypalApiCredentials(final String client,
                                                                   final long groupId, final String apiLogin, final String apiPwd, final String apiSecret,
                                                                   final Boolean sandbox, final Boolean disable, String login) {

        Client.ensureValid(client);

        User.ensureGroupLevelPrivilege(client, groupId, login,
                GroupLevelPrivilege.UPDATE_PAYMENT_CONFIG);

        PaypalApiCredentialsProp prop = ofy(client).transact(new Work<PaypalApiCredentialsProp>() {

            @Override
            public PaypalApiCredentialsProp run() {
                GroupEntity groupEntity = Group.safeGet(client, groupId);

                boolean changed = false;

                if (apiLogin != null) {
                    groupEntity.paypalApiLogin = apiLogin;
                    changed = true;
                }

                if (apiPwd != null) {
                    groupEntity.paypalApiPwd = apiPwd;
                    changed = true;
                }

                if (apiSecret != null) {
                    groupEntity.paypalApiSecret = apiSecret;
                    changed = true;
                }

                if (sandbox != null) {
                    groupEntity.paypalApiSandbox = sandbox;
                    changed = true;
                }

                if (disable != null) {
                    groupEntity.paypalApiDisable = disable;
                    changed = true;
                }

                Utils.ensureNotNullOrEmpty(groupEntity.paypalApiLogin, "Paypal login is null or empty");
                Utils.ensureNotNullOrEmpty(groupEntity.paypalApiPwd, "Paypal login is null or empty");
                Utils.ensureNotNullOrEmpty(groupEntity.paypalApiSecret, "Paypal secret is null or empty");

                if (changed)
                    ofy(client).save().entity(groupEntity).now();

                PaypalApiCredentialsProp prop = new PaypalApiCredentialsProp();
                prop.login = groupEntity.paypalApiLogin;
                prop.pwd = groupEntity.paypalApiPwd;
                prop.secret = groupEntity.paypalApiSecret;
                prop.sandbox = groupEntity.paypalApiSandbox;
                prop.disable = groupEntity.paypalApiDisable;

                return prop;
            }
        });

        return prop;
    }

    public static EmailConfig setMandrillApiKey(final String client, final long groupId,
                                                final String apiKey, String login) {

        Client.ensureValid(client);

        User.ensureGroupLevelPrivilege(client, groupId, login, GroupLevelPrivilege.UPDATE_EMAIL_CONFIG);

        Mandrill.ensureValidApiKey(apiKey);

        EmailConfig emailConfigProp = ofy(client).transact(new Work<EmailConfig>() {

            @Override
            public EmailConfig run() {
                GroupEntity groupEntity = Group.safeGet(client, groupId);


                groupEntity.mandrillApiKey = apiKey;
                ofy(client).save().entity(groupEntity).now();

                EmailConfig emailConfigProp = new EmailConfig();
                emailConfigProp.mandrillApiKey = groupEntity.mandrillApiKey;
                emailConfigProp.allowedFromEmailVsName = groupEntity.allowedFromEmailVsName;

                return emailConfigProp;
            }
        });

        return emailConfigProp;
    }

    public static void setContactInfo(final String client, final long groupId,
        final String contactEmail, final String contactName, String login) {

        Client.ensureValid(client);

        User.ensureGroupLevelPrivilege(client, groupId, login, GroupLevelPrivilege.UPDATE_EMAIL_CONFIG);

        Utils.ensureValidEmail(contactEmail);

        ofy(client).transact(new Work<GroupProp>() {

            @Override
            public GroupProp run() {
            GroupEntity groupEntity = Group.safeGet(client, groupId);

                groupEntity.contactEmail = contactEmail;
                groupEntity.contactName = contactName;
                groupEntity.allowedFromEmailVsName.put(contactEmail, contactName);

                ofy(client).save().entity(groupEntity).now();

                return groupEntity.toProp();
            }
        });


    }

    public static EmailConfig addOrDeleteAllowedEmailSender(final String client, final long groupId,
                                                            final String fromEmail, final String fromName, final boolean add, String login) {

        Client.ensureValid(client);

        User.ensureGroupLevelPrivilege(client, groupId, login, GroupLevelPrivilege.UPDATE_EMAIL_CONFIG);

        Utils.ensureValidEmail(fromEmail);

        EmailConfig emailConfigProp = ofy(client).transact(new Work<EmailConfig>() {

            @Override
            public EmailConfig run() {
                GroupEntity groupEntity = Group.safeGet(client, groupId);

                if (add) {
                    ensureNotNull(fromName, "fromName is null");
                    ensure(!fromName.isEmpty(), "fromName is null");

                    groupEntity.allowedFromEmailVsName.put(fromEmail.toLowerCase(), fromName);
                } else
                    groupEntity.allowedFromEmailVsName.remove(fromEmail.toLowerCase());

                ofy(client).save().entity(groupEntity).now();

                EmailConfig emailConfigProp = new EmailConfig();
                emailConfigProp.mandrillApiKey = groupEntity.mandrillApiKey;
                emailConfigProp.allowedFromEmailVsName = groupEntity.allowedFromEmailVsName;

                return emailConfigProp;
            }
        });

        if (UserCore.isSuperUser(login))
            return emailConfigProp;

        UserProp userProp = User.safeGet(client, login).toProp(client);

        // mask the key if user does not have access
        if (!userProp.clientLevelPrivileges.contains(ClientLevelPrivilege.VIEW_API_KEY.toString()))
            emailConfigProp.mandrillApiKey = EmailConfig.TEXT_API_KEY_MASKED;

        return emailConfigProp;
    }

    public static EmailConfig getEmailConfig(String client, long groupId, String login) {

        Client.ensureValid(client);

        GroupEntity groupEntity = Group.safeGet(client, groupId);

        User.ensureValidUser(client, login);

        EmailConfig emailConfig = new EmailConfig();
        EmailConfig clientEmailConfig = Client.getEmailConfig(client, login);

        emailConfig.mandrillApiKey = (groupEntity.mandrillApiKey != null) ?
            groupEntity.mandrillApiKey : clientEmailConfig.mandrillApiKey;
        emailConfig.allowedFromEmailVsName = (groupEntity.allowedFromEmailVsName != null) ?
            groupEntity.allowedFromEmailVsName : clientEmailConfig.allowedFromEmailVsName;
        emailConfig.contactName = (groupEntity.contactName != null) ?
            groupEntity.contactName : clientEmailConfig.contactName;
        emailConfig.contactEmail = (groupEntity.contactEmail != null) ?
            groupEntity.contactEmail : clientEmailConfig.contactEmail;

        if (UserCore.isSuperUser(login))
            return emailConfig;

        UserProp userProp = User.safeGet(client, login).toProp(client);

        // mask the key if user does not have access
        if (!userProp.clientLevelPrivileges.contains(ClientLevelPrivilege.VIEW_API_KEY.toString()))
            emailConfig.mandrillApiKey = EmailConfig.TEXT_API_KEY_MASKED;

        return emailConfig;
    }

    public static void setEmailHtmlTemplate(final String client, final long groupId,
                                            final EmailType emailType, final String emailTemplate, String login) {

        Client.ensureValid(client);

        User.ensureGroupLevelPrivilege(client, groupId, login, GroupLevelPrivilege.UPDATE_EMAIL_CONFIG);

        Utils.ensureNotNullOrEmpty(emailTemplate, "Email template cannot be null or empty");

        ofy(client).transact(new VoidWork() {

            @Override
            public void vrun() {
                GroupEntity groupEntity = Group.safeGet(client, groupId);

                if (emailType == EmailType.REGISTRATION_CONFIRMATION) {
                    groupEntity.registrationConfirmationEmailTemplate = emailTemplate;
                } else if (emailType == EmailType.REGISTRATION_REMINDER) {
                    groupEntity.registrationReminderEmailTemplate = emailTemplate;
                } else {
                    throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                            "Invalid emailType [" + emailType + "]");
                }

                ofy(client).save().entity(groupEntity).now();
            }
        });
    }

    static String getEmailTemplate(String client, long groupId, EmailType emailType) {

        Client.ensureValid(client);

        GroupEntity groupEntity = Group.safeGet(client, groupId);
        if (emailType == EmailType.REGISTRATION_CONFIRMATION) {
            return groupEntity.registrationConfirmationEmailTemplate;
        } else if (emailType == EmailType.REGISTRATION_REMINDER) {
            return groupEntity.registrationReminderEmailTemplate;
        } else {
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Invalid emailType [" + emailType + "]");
        }
    }

    public static PaypalApiCredentialsProp getPaypalApiCredentials(String client, long groupId,
                                                                   String login) {

        Client.ensureValid(client);

        GroupEntity groupEntity = Group.safeGet(client, groupId);

        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.VIEW_API_KEY);

        PaypalApiCredentialsProp prop = new PaypalApiCredentialsProp();
        prop.login = groupEntity.paypalApiLogin;
        prop.pwd = groupEntity.paypalApiPwd;
        prop.secret = groupEntity.paypalApiSecret;
        prop.sandbox = groupEntity.paypalApiSandbox;
        prop.disable = groupEntity.paypalApiDisable;

        return prop;
    }

    public static String safeGetSenderNameFromEmail(String client, long groupId, String senderEmail) {
        Client.ensureValid(client);

        Group.safeGet(client, groupId);
        ensureNotNull(senderEmail, "sender email is null");

        EmailConfig emailConfig = Group.getEmailConfig(client, groupId, User.SUPER_USER);

        String senderName = emailConfig.allowedFromEmailVsName.get(senderEmail.toLowerCase());

        if (senderName == null) {
            String message =
                    "[" + senderEmail + "] is not an allowed email sender for client [" + client + "], group ["
                            + groupId + "]";
            throw new APIException(message).status(Status.ERROR_RESOURCE_INCORRECT);
        }

        return senderName;
    }

    public enum EmailType {
        REGISTRATION_CONFIRMATION, REGISTRATION_REMINDER
    }

    public static class GroupProp {
        public long groupId;
        public String name;
        public String displayName;

        public List<ListProp> listProps;
    }
}
