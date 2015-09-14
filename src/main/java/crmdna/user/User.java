package crmdna.user;

import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;

import java.util.List;

public class User {

    public static final String SUPER_USER = "sathyanarayanant@gmail.com";

    public static UserProp create(String client, String email, long groupId, String login) {
        Client.ensureValid(client);

        ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_USER);
        Group.safeGet(client, groupId);

        return UserCore.create(client, email, groupId);
    }

    public static UserProp updateEmail(String client, String existingEmail, String newEmail,
                                       String login) {

        Client.ensureValid(client);
        ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_USER);

        UserProp userProp = UserCore.updateEmail(client, existingEmail, newEmail);
        return userProp;
    }

    public static UserProp updateGroup(String client, String email, long newGroupId, String login) {

        Client.ensureValid(client);
        ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_USER);

        Group.safeGet(client, newGroupId);

        return UserCore.updateGroup(client, email, newGroupId);
    }

    public static UserEntity get(String client, String email) {

        Client.ensureValid(client);
        return UserCore.get(client, email);
    }

    public static UserEntity safeGet(String client, String email) {

        Client.ensureValid(client);
        return UserCore.safeGet(client, email);
    }

    public static List<UserProp> getAll(String client, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        return UserCore.getAll(client);
    }

    public static List<UserProp> getAllForGroup(String client, long groupId) {
        Client.ensureValid(client);
        return UserCore.getAllForGroup(client, groupId);
    }

    public static UserProp addClientLevelPrivilege(String client, String email,
                                                   ClientLevelPrivilege privilege, String login) {

        return addOrDeleteClientLevelPrivilege(client, email, privilege, true, login);
    }

    public static UserProp deleteClientLevelPrivilege(String client, String email,
                                                      ClientLevelPrivilege privilege, String login) {

        return addOrDeleteClientLevelPrivilege(client, email, privilege, false, login);
    }

    private static UserProp addOrDeleteClientLevelPrivilege(String client, String email,
                                                            ClientLevelPrivilege privilege, boolean add, String login) {

        Client.ensureValid(client);
        ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_USER);

        UserProp userProp =
                UserCore.addOrDeletePrivilege(client, email, "CLIENT", client, privilege.toString(), add);
        return userProp;
    }

    public static UserProp addGroupLevelPrivilege(String client, long groupId, String email,
                                                  GroupLevelPrivilege privilege, String login) {

        return addOrDeleteGroupLevelPrivilege(client, groupId, email, privilege, true, login);
    }

    public static UserProp deleteGroupLevelPrivilege(String client, long groupId, String email,
                                                     GroupLevelPrivilege privilege, String login) {

        return addOrDeleteGroupLevelPrivilege(client, groupId, email, privilege, false, login);
    }

    private static UserProp addOrDeleteGroupLevelPrivilege(String client, long groupId, String email,
                                                           GroupLevelPrivilege privilege, boolean add, String login) {

        Client.ensureValid(client);
        ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_USER);

        UserProp userProp =
                UserCore.addOrDeletePrivilege(client, email, "GROUP", groupId + "", privilege.toString(),
                        add);
        return userProp;
    }

    public static UserProp clonePrivileges(String client, String sourceEmail, String targetEmail,
                                           String login) {

        Client.ensureValid(client);
        ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_USER);

        UserProp userProp = UserCore.clonePrivileges(client, sourceEmail, targetEmail);
        return userProp;
    }

    public static void ensureValidUser(String client, String login) {

        Client.ensureValid(client);
        UserCore.ensureValidUser(client, login);
    }

    public static void ensureClientLevelPrivilege(String client, String login,
                                                  ClientLevelPrivilege privilege) {

        Client.ensureValid(client);

        if (UserCore.hasPrivilege(client, login, "CLIENT", client, privilege.toString()))
            return;

        // throw exception
        String message =
                "User [" + login + "] does not have client level privilege [" + privilege
                        + "] for client [" + client + "]";
        throw new APIException(message).status(Status.ERROR_INSUFFICIENT_PERMISSION);
    }

    public static void ensureGroupLevelPrivilege(String client, long groupId, String login,
                                                 GroupLevelPrivilege privilege) {

        Client.ensureValid(client);

        if (UserCore.hasPrivilege(client, login, "GROUP", groupId + "", privilege.toString()))
            return;

        // throw exception
        String message =
                "User [" + login + "] does not have group level privilege [" + privilege + "] for client ["
                        + client + "], group [" + groupId + "]";
        throw new APIException(message).status(Status.ERROR_INSUFFICIENT_PERMISSION);
    }

    public static UserProp addApp(String client, String email, App app, String login) {
        Client.ensureValid(client);

        ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_USER);

        return UserCore
                .addOrDeletePrivilege(client, email, "APP", app.toString(), app.toString(), true);
    }

    public static UserProp removeApp(String client, String email, App app, String login) {
        Client.ensureValid(client);

        ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_USER);

        return UserCore.addOrDeletePrivilege(client, email, "APP", app.toString(), app.toString(),
                false);
    }

    public enum ResourceType {
        MEMBER, GROUP, USER, INTERACTION, PRACTICE, PROGRAM_TYPE, VENUE, TEACHER, PROGRAM, CUSTOM_CONFIG, PAYPAL_CREDENTIALS, FROM_EMAIL, EMAIL_KEY, CONFIG, INVENTORY_ITEM_TYPE, INVENTORY_ITEM, DEPARTMENT, LIST, MAIL_CONTENT, MEAL
    }

    public enum GroupLevelPrivilege {
        SEND_EMAIL, VIEW_LIST, UPDATE_LIST, UPDATE_PROGRAM, CHECK_IN, UPDATE_EMAIL_CONFIG, UPDATE_PAYMENT_CONFIG, UPDATE_INVENTORY_QUANTITY, UPDATE_INVENTORY_ITEM, UPDATE_MAIL_CONTENT, UPDATE_MAIL_SCHEDULE, UPDATE_PARTICIPANT,
        UPDATE_CAMPAIGN
    }

    public enum ClientLevelPrivilege {
        UPDATE_GROUP, ENABLE_DISABLE_ACCOUNT, UPDATE_USER, UPDATE_CLIENT_CONTACT_EMAIL, PURGE_MEMBER_DATA, VIEW_API_KEY, UPDATE_CUSTOM_CONFIG, UPDATE_DEPARTMENT, UPDATE_INTERACTION, UPDATE_INVENTORY_ITEM_TYPE, UPDATE_MEAL_COUNT, UPDATE_MAIL_CONTENT, VERIFY_EMAIL, UPDATE_PAYMENT, UPDATE_PRACTICE, UPDATE_PROGRAM_TYPE, UPDATE_SESSION_PASS, UPDATE_TEACHER, UPDATE_VENUE,
        SUBSCRIBE_GROUP //required to undo unsubscribe. should be given only to select people
    }

    public enum App {
        SEARCH, CHECK_IN, INVENTORY
    }
}
