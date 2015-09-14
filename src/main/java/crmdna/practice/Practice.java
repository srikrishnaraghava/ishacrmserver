package crmdna.practice;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;

import java.util.*;

import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.AssertUtils.ensureNotNullNotEmpty;
import static crmdna.common.OfyService.ofy;

public class Practice {
    public static PracticeProp create(String client, String displayName, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_PRACTICE);

        ensureNotNullNotEmpty(displayName, "displayName is null or empty");
        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName.toLowerCase());

        List<Key<PracticeEntity>> keys =
                ofy(client).load().type(PracticeEntity.class).filter("name", name).keys().list();

        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a practice with name [" + displayName + "]");

        String key = getUniqueKey(client, name);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a group with name [" + displayName + "]");

        PracticeEntity entity = new PracticeEntity();
        entity.practiceId = Sequence.getNext(client, SequenceType.PRACTICE);
        entity.name = name;
        entity.displayName = displayName;
        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    private static String getUniqueKey(String namespace, String name) {
        return namespace + "_" + SequenceType.PRACTICE + "_" + name;
    }

    public static PracticeEntity safeGet(String client, long practiceId) {

        Client.ensureValid(client);

        PracticeEntity entity = ofy(client).load().type(PracticeEntity.class).id(practiceId).now();
        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Practice [" + practiceId + "] does not exist");

        return entity;
    }

    public static Map<Long, PracticeProp> get(String client, Set<Long> practiceIds) {

        Client.ensureValid(client);

        Map<Long, PracticeEntity> map = ofy(client).load().type(PracticeEntity.class).ids(practiceIds);

        Map<Long, PracticeProp> props = new HashMap<>();

        for (Long practiceId : map.keySet()) {
            props.put(practiceId, map.get(practiceId).toProp());
        }

        return props;
    }

    public static Map<Long, PracticeEntity> getEntities(String client, Set<Long> practiceIds) {

        Client.ensureValid(client);

        Map<Long, PracticeEntity> map = ofy(client).load().type(PracticeEntity.class).ids(practiceIds);

        return map;
    }

    public static PracticeProp rename(String client, long practiceId, String newDisplayName,
                                      String login) {
        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_PRACTICE);

        PracticeEntity entity = safeGet(client, practiceId);

        ensureNotNullNotEmpty(newDisplayName, "newDisplayName is null or empty");
        String newName = Utils.removeSpaceUnderscoreBracketAndHyphen(newDisplayName.toLowerCase());

        if (entity.name.equals(newName)) {
            // ideally should be inside a transaction
            entity.displayName = newDisplayName;
            ofy(client).save().entity(entity).now();
            return entity.toProp();
        }

        List<Key<PracticeEntity>> keys =
                ofy(client).load().type(PracticeEntity.class).filter("name", newName).keys().list();
        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a practice with name [" + newDisplayName + "]");

        String key = getUniqueKey(client, newName);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a practice with name [" + newDisplayName + "]");

        // ideally should be inside a transaction
        entity.name = newName;
        entity.displayName = newDisplayName;
        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static List<PracticeProp> getAll(String client) {
        Client.ensureValid(client);

        List<PracticeEntity> entities =
                ofy(client).load().type(PracticeEntity.class).order("name").list();

        List<PracticeProp> props = new ArrayList<>();
        for (PracticeEntity entity : entities)
            props.add(entity.toProp());

        return props;
    }

    public static void delete(String client, long practiceId, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_PRACTICE);

        // there should not be any program type referencing this
        throw new APIException().status(Status.ERROR_NOT_IMPLEMENTED).message(
                "This functionality is not implemented yet");
    }

    public static PracticeEntity safeGetByIdOrName(String client, String idOrName) {

        Client.ensureValid(client);
        ensureNotNull(idOrName);

        if (Utils.canParseAsLong(idOrName)) {
            long practiceId = Utils.safeParseAsLong(idOrName);
            return safeGet(client, practiceId);
        }

        idOrName = Utils.removeSpaceUnderscoreBracketAndHyphen(idOrName.toLowerCase());
        List<PracticeEntity> entities =
                ofy(client).load().type(PracticeEntity.class).filter("name", idOrName).list();

        if (entities.size() == 0)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Practice [" + idOrName + "] does not exist");

        if (entities.size() > 1)
            // should never come here
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Found [" + entities.size() + "] matches for practice [" + idOrName
                            + "]. Please specify Id. ");
        return entities.get(0);
    }

    public static class PracticeProp implements Comparable<PracticeProp> {
        public long practiceId;
        public String name;
        public String displayName;

        @Override
        public int compareTo(PracticeProp arg0) {
            return name.compareTo(arg0.name);
        }
    }
}
