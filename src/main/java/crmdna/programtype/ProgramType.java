package crmdna.programtype;

import com.googlecode.objectify.Key;
import crmdna.client.Client;
import crmdna.common.MemcacheLock;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.member.Member;
import crmdna.member.MemberLoader;
import crmdna.member.MemberQueryCondition;
import crmdna.practice.Practice;
import crmdna.practice.PracticeEntity;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.User.ResourceType;
import crmdna.user.UserCore;

import java.util.*;
import java.util.Map.Entry;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class ProgramType {
    public static ProgramTypeProp create(String client, String displayName, Set<Long> practiceIds,
                                         String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_PROGRAM_TYPE);

        ensureNotNull(displayName, "displayName is null");
        ensure(displayName.length() != 0, "displayName is empty");

        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName).toLowerCase();

        List<Key<ProgramTypeEntity>> keys =
                ofy(client).load().type(ProgramTypeEntity.class).filter("name", name).keys().list();

        if (keys.size() != 0)
            throw new APIException("There is already a program type with name [" + displayName + "]")
                    .status(Status.ERROR_RESOURCE_ALREADY_EXISTS);

        try (MemcacheLock lock = new MemcacheLock(client, ResourceType.PROGRAM_TYPE, name)) {

            ProgramTypeEntity entity = new ProgramTypeEntity();
            entity.programTypeId = Sequence.getNext(client, SequenceType.PROGRAM_TYPE);
            entity.name = name;
            entity.displayName = displayName;

            if (practiceIds != null) {
                for (long practiceId : practiceIds) {
                    Practice.safeGet(client, practiceId);
                    entity.practiceIds.add(practiceId);
                }
            }

            ofy(client).save().entity(entity).now();
            return entity.toProp(client);
        }
    }

    public static ProgramTypeProp updatePracticeIds(String client, long programTypeId,
                                                    Set<Long> newPracticeIds, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_PROGRAM_TYPE);
        ensureNotNull(newPracticeIds, "newPracticeIds is null");

        ProgramTypeEntity programTypeEntity = safeGet(client, programTypeId);

        Set<Long> existingPracticeIds = programTypeEntity.practiceIds;

        if (existingPracticeIds.containsAll(newPracticeIds)
                && newPracticeIds.containsAll(existingPracticeIds)) {
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "newPracticeIds are identical to existing practice Ids");
        }

        Map<Long, PracticeEntity> map = Practice.getEntities(client, newPracticeIds);
        if (map.size() != newPracticeIds.size())
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "One or more practiceIds not found");

        programTypeEntity.practiceIds = map.keySet();

        ofy(client).save().entity(programTypeEntity).now();

        MemberQueryCondition mqc = new MemberQueryCondition(client, MemberLoader.MAX_RESULT_SIZE);
        mqc.programTypeIds = Utils.getSet(programTypeId);

        Member.rebuild(mqc, login);

        return programTypeEntity.toProp(client);
    }

    public static ProgramTypeEntity safeGet(String client, long programTypeId) {

        Client.ensureValid(client);

        ProgramTypeEntity entity =
                ofy(client).load().type(ProgramTypeEntity.class).id(programTypeId).now();
        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Program type [" + programTypeId + "] does not exist");
        return entity;
    }

    public static ProgramTypeEntity safeGetByIdOrName(String client, String idOrName) {

        Client.ensureValid(client);
        ensureNotNull(idOrName);

        if (Utils.canParseAsLong(idOrName)) {
            long programTypeId = Utils.safeParseAsLong(idOrName);
            return safeGet(client, programTypeId);
        }

        idOrName = Utils.removeSpaceUnderscoreBracketAndHyphen(idOrName.toLowerCase());
        List<ProgramTypeEntity> entities =
                ofy(client).load().type(ProgramTypeEntity.class).filter("name", idOrName).list();

        if (entities.size() == 0)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Program Type [" + idOrName + "] does not exist");

        if (entities.size() > 1)
            // should never come here
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Found [" + entities.size() + "] matches for program type [" + idOrName
                            + "]. Please specify Id");
        return entities.get(0);
    }

    public static void ensureValid(String client, Set<Long> programTypeIds) {

        Client.ensureValid(client);

        Map<Long, ProgramTypeEntity> map =
                ofy(client).load().type(ProgramTypeEntity.class).ids(programTypeIds);

        if (map.size() == programTypeIds.size())
            return; // all ids are valid

        for (Long id : programTypeIds) {
            if (!map.containsKey(id))
                Utils.throwNotFoundException("programTypeId [" + id + "] does not exist");
        }
    }

    public static List<ProgramTypeProp> getAll(String client) {
        Client.ensureValid(client);

        List<ProgramTypeEntity> entities =
                ofy(client).load().type(ProgramTypeEntity.class).order("name").list();

        List<ProgramTypeProp> props = new ArrayList<>();
        for (ProgramTypeEntity entity : entities)
            props.add(entity.toProp(client));

        return props;
    }

    static Map<Long, ProgramTypeEntity> getEntities(String client, Set<Long> programTypeIds) {
        Client.ensureValid(client);

        ensureNotNull(programTypeIds, "programTypeIds is null");

        Map<Long, ProgramTypeEntity> map =
                ofy(client).load().type(ProgramTypeEntity.class).ids(programTypeIds);

        return map;
    }

    public static Set<Long> getPracticeIds(String client, long programTypeId) {
        Client.ensureValid(client);

        return safeGet(client, programTypeId).practiceIds;
    }

    public static Set<Long> getPracticeIds(String client, Set<Long> programTypeIds) {
        Client.ensureValid(client);

        Map<Long, ProgramTypeEntity> map =
                ofy(client).load().type(ProgramTypeEntity.class).ids(programTypeIds);

        Set<Long> practiceIds = new HashSet<>();
        for (Entry<Long, ProgramTypeEntity> entry : map.entrySet()) {
            practiceIds.addAll(entry.getValue().practiceIds);
        }

        return practiceIds;
    }

    public static ProgramTypeProp rename(final String client, final long programTypeId,
                                         final String newDisplayName, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_PROGRAM_TYPE);

        ProgramTypeEntity entity = safeGet(client, programTypeId);

        final String newName =
                Utils.removeSpaceUnderscoreBracketAndHyphen(newDisplayName).toLowerCase();

        // ensure unique if it not a simple upper case/lower case conversion
        if (!entity.name.equals(newName)) {
            List<Key<ProgramTypeEntity>> keys =
                    ofy(client).load().type(ProgramTypeEntity.class).filter("name", newName).keys().list();
            if (keys.size() != 0)
                throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                        "There is already a program typee with name [" + newDisplayName + "]");
        }

        try (MemcacheLock lock = new MemcacheLock(client, ResourceType.PROGRAM_TYPE, newName)) {
            entity.name = newName;
            entity.displayName = newDisplayName;
            ofy(client).save().entity(entity).now();
        }

        return entity.toProp(client);
    }

    public static void delete(String client, long programTypeId, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_PROGRAM_TYPE);

        throw new APIException().status(Status.ERROR_NOT_IMPLEMENTED).message(
                "Functionality to delete program type not yet implemented");
    }

    // this should be removed after bhairavi data migration is complete
    public static void resaveAll(String client, String login) {
        Client.ensureValid(client);

        ensure(UserCore.isSuperUser(login), "Allowed only for super user");

        List<ProgramTypeEntity> all = ofy(client).load().type(ProgramTypeEntity.class).list();

        ofy(client).save().entities(all).now();
    }
}
