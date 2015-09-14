package crmdna.user;

import com.googlecode.objectify.ObjectifyFilter;
import com.googlecode.objectify.Work;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class UserCore {

    static final String DELIMITER = "||";

    static UserProp create(String namespace, String email, long groupId) {

        Utils.ensureValidEmail(email);
        email = email.toLowerCase();

        UserEntity entity = ofy(namespace).load().type(UserEntity.class).id(email).now();

        if (null != entity)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already an user with email [" + email + "]");

        UserEntity userEntity = new UserEntity();
        userEntity.email = email;
        userEntity.userId = Sequence.getNext(namespace, SequenceType.USER);
        userEntity.groupId = groupId;

        ofy(namespace).save().entity(userEntity).now();

        return userEntity.toProp(namespace);
    }

    static UserProp updateEmail(String namespace, String existingEmail, String newEmail) {

        Utils.ensureValidEmail(newEmail);

        existingEmail = existingEmail.toLowerCase();
        newEmail = newEmail.toLowerCase();

        UserEntity existingEntity = UserCore.safeGet(namespace, existingEmail);

        if (existingEmail.equals(newEmail))
            return existingEntity.toProp(namespace);

        // newEmail should not already exist
        if (null != get(namespace, newEmail))
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already an user with email [" + newEmail + "]");

        UserEntity newEntity = new UserEntity();
        newEntity.email = newEmail;
        newEntity.userId = existingEntity.userId;
        newEntity.groupId = existingEntity.groupId;
        newEntity.privileges = existingEntity.privileges;

        ofy(namespace).save().entity(newEntity);
        ofy(namespace).delete().entity(existingEntity);
        ObjectifyFilter.complete();

        return newEntity.toProp(namespace);
    }

    static UserProp updateGroup(final String namespace, final String email, final long newGroupId) {

        // email should be present
        safeGet(namespace, email.toLowerCase());

        UserProp userProp = ofy(namespace).transact(new Work<UserProp>() {
            @Override
            public UserProp run() {
                UserEntity userEntity =
                        ofy(namespace).load().type(UserEntity.class).id(email.toLowerCase()).now();
                if (null == userEntity)
                    throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                            "There is no user with email [" + email + "]");

                userEntity.groupId = newGroupId;
                ofy(namespace).save().entity(userEntity).now();
                return userEntity.toProp(namespace);
            }
        });

        return userProp;
    }

    static UserEntity get(String namespace, String email) {

        email = email.toLowerCase();
        UserEntity entity = ofy(namespace).load().type(UserEntity.class).id(email).now();

        if (entity == null)
            return null;

        return entity;
    }

    public static UserEntity safeGet(String client, String email) {
        UserEntity userEntity = get(client, email);

        if (null == userEntity)
            throw new APIException("User [" + email + "] is not valid").status(Status.ERROR_INVALID_USER);

        return userEntity;
    }

    public static List<UserProp> getAll(String namespace) {
        Client.ensureValid(namespace);

        List<UserEntity> entities =
                ofy(namespace).load().type(UserEntity.class).order("__key__").list();

        List<UserProp> props = new ArrayList<>();
        for (UserEntity entity : entities) {
            props.add(entity.toProp(namespace));
        }

        return props;
    }

    public static List<UserProp> getAllForGroup(String namespace, long groupId) {

        List<UserProp> all = getAll(namespace);

        List<UserProp> userProps = new ArrayList<>();
        for (UserProp userProp : all) {
            if (userProp.groupId == groupId)
                userProps.add(userProp);
        }

        return userProps;
    }

    static UserProp addOrDeletePrivilege(final String namespace, final String email,
                                         final String resourceType, final String resourceId, final String privilege, final boolean add) {

        safeGet(namespace, email.toLowerCase()); // ensures user is valid

        UserProp userProp = ofy(namespace).transact(new Work<UserProp>() {

            @Override
            public UserProp run() {
                UserEntity userEntity =
                        ofy(namespace).load().type(UserEntity.class).id(email.toLowerCase()).now();

                if (null == userEntity)
                    throw new APIException().status(Status.ERROR_INVALID_USER).message(
                            "Invalid user [" + email + "]");

                String rawprivilege = getRawPrivilege(resourceType, resourceId, privilege);

                boolean change;
                if (add)
                    change = userEntity.privileges.add(rawprivilege);
                else
                    change = userEntity.privileges.remove(rawprivilege);

                if (change)
                    ofy(namespace).save().entity(userEntity).now();

                return userEntity.toProp(namespace);
            }
        });

        return userProp;
    }

    static UserProp clonePrivileges(final String namespace, final String sourceEmail,
                                    final String targetEmail) {

        if (null == sourceEmail)
            Utils.throwIncorrectSpecException("sourceUser cannot be null");

        if (null == targetEmail)
            Utils.throwIncorrectSpecException("targetUser cannot be null");

        safeGet(namespace, sourceEmail.toLowerCase());
        safeGet(namespace, targetEmail.toLowerCase());

        UserProp userProp = ofy(namespace).transact(new Work<UserProp>() {

            @Override
            public UserProp run() {
                UserEntity sourceEntity =
                        ofy(namespace).load().type(UserEntity.class).id(sourceEmail.toLowerCase()).now();
                if (null == sourceEntity)
                    throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                            "Source email [" + sourceEmail + "] does not exist");

                UserEntity targetEntity =
                        ofy(namespace).load().type(UserEntity.class).id(targetEmail.toLowerCase()).now();
                if (null == targetEntity)
                    throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                            "Target email [" + targetEmail + "] does not exist");

                targetEntity.privileges = sourceEntity.privileges;
                ofy(namespace).save().entity(targetEntity).now();
                return targetEntity.toProp(namespace);
            }
        });

        return userProp;
    }

    public static void ensureValidUser(String client, String login) {

        ensureNotNull(client, "client is null");
        ensure(!client.isEmpty(), "client is empty");

        if (null == login)
            throw new APIException().status(Status.ERROR_LOGIN_REQUIRED).message(
                    "Email cannot be null or empty");

        if (UserCore.isSuperUser(login))
            return;

        login = login.toLowerCase();
        UserEntity userEntity = ofy(client).load().type(UserEntity.class).id(login).now();

        if (null == userEntity)
            throw new APIException("Invalid user [" + login + "] for client [" + client + "]")
                    .status(Status.ERROR_INVALID_USER);
    }

    static boolean hasPrivilege(String client, String login, String resourceType, String resourceId,
                                String privilege) {

        ensureNotNull(client, "client is null");
        ensure(!client.isEmpty(), "client is empty");

        if (null == login)
            throw new APIException().status(Status.ERROR_LOGIN_REQUIRED).message(
                    "Email cannot be null or empty");

        if (UserCore.isSuperUser(login))
            return true;

        login = login.toLowerCase();
        UserEntity userEntity = ofy(client).load().type(UserEntity.class).id(login).now();

        if (null == userEntity)
            throw new APIException("Invalid user [" + login + "] for client [" + client + "]")
                    .status(Status.ERROR_INVALID_USER);

        String rawPrivilege = getRawPrivilege(resourceType, resourceId, privilege);
        if (userEntity.privileges.contains(rawPrivilege))
            return true;
        else
            return false;
    }

    static String getRawPrivilege(String resourceType, String resourceId, String action) {

        return resourceType.toString() + DELIMITER + resourceId + DELIMITER + action.toString();
    }

    public static boolean isSuperUser(String login) {
        if (login == null) {
            return false;
        }

        String[] superUsers = {"sathyanarayanant@gmail.com", "thulasidhar@gmail.com"};

        return Arrays.asList(superUsers).contains(login.toLowerCase());
    }
}
