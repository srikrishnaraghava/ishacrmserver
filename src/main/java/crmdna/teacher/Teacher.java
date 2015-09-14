package crmdna.teacher;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class Teacher {
    public static TeacherProp create(String client, String firstName, String lastName, String email,
        long groupId, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_TEACHER);

        Utils.ensureValidEmail(email);
        email = email.toLowerCase();

        List<Key<TeacherEntity>> keys =
            ofy(client).load().type(TeacherEntity.class).filter("email", email).keys().list();

        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a teacher with name [" + email + "]");

        String key = getUniqueKey(client, email);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a teacher with email [" + email + "]");

        TeacherEntity entity = new TeacherEntity();
        entity.firstName = firstName;
        entity.lastName = lastName;
        entity.teacherId = Sequence.getNext(client, SequenceType.TEACHER);
        entity.email = email;
        entity.groupId = groupId;
        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    private static String getUniqueKey(String namespace, String name) {
        return namespace + "_" + SequenceType.TEACHER + "_" + name;
    }

    public static TeacherEntity safeGet(String client, long teacherId) {

        Client.ensureValid(client);

        TeacherEntity entity = ofy(client).load().type(TeacherEntity.class).id(teacherId).now();
        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Teacher [" + teacherId + "] does not exist");

        return entity;
    }

    public static TeacherEntity safeGetByIdOrEmail(String client, String idOrEmail) {

        Client.ensureValid(client);
        ensureNotNull(idOrEmail, "idOrEmail is null");

        if (Utils.canParseAsLong(idOrEmail)) {
            long teacherId = Utils.safeParseAsLong(idOrEmail);
            return safeGet(client, teacherId);
        }

        ensure(Utils.isValidEmailAddress(idOrEmail), "email [" + idOrEmail + "] is not valid");

        idOrEmail = idOrEmail.toLowerCase();
        List<TeacherEntity> entities =
            ofy(client).load().type(TeacherEntity.class).filter("email", idOrEmail).list();

        if (entities.size() == 0)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Teacher [" + idOrEmail + "] does not exist");

        if (entities.size() > 1)
            // should never come here
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                "Found [" + entities.size() + "] matches for teacher [" + idOrEmail
                    + "]. Please specify Id");
        return entities.get(0);
    }

    public static TeacherProp update(String client, long teacherId, String newEmail,
        Long newGroupId, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_TEACHER);

        if (newGroupId != null)
            Group.safeGet(client, newGroupId);

        TeacherEntity entity = safeGet(client, teacherId);

        if (newEmail != null) {
            Utils.ensureValidEmail(newEmail);
            newEmail = newEmail.toLowerCase();

            List<Key<TeacherEntity>> keys =
                ofy(client).load().type(TeacherEntity.class).filter("email", newEmail).keys()
                    .list();
            if (keys.size() != 0)
                throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                        "There is already a teacher with email [" + newEmail + "]");

            String key = getUniqueKey(client, newEmail);
            long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

            if (val != 1)
                throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                        "There is already a teacher with name [" + newEmail + "]");
        }

        // ideally should be inside a transaction
        if (newEmail != null)
            entity.email = newEmail;
        if (newGroupId != null)
            entity.groupId = newGroupId;

        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static List<TeacherProp> getAll(String client) {

        Client.ensureValid(client);

        List<TeacherEntity> entities =
            ofy(client).load().type(TeacherEntity.class).order("email").list();

        List<TeacherProp> props = new ArrayList<>();
        for (TeacherEntity entity : entities)
            props.add(entity.toProp());

        return props;
    }

    public static void delete(String namespace, long teacherId, String login) {

        // TODO
        // need to check for all references before supporting delete
        throw new APIException().status(Status.ERROR_NOT_IMPLEMENTED).message(
                "Deleting teacher entity is not yet implemented");
    }

    public static class TeacherProp {
        public long teacherId;
        public String firstName;
        public String lastName;
        public String email;
        public long groupId;
    }
}
