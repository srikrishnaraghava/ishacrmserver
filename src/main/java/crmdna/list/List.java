package crmdna.list;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.member.MemberLoader;
import crmdna.member.MemberQueryCondition;
import crmdna.practice.Practice;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;

import java.util.*;
import java.util.Map.Entry;

import static crmdna.common.AssertUtils.*;
import static crmdna.common.OfyService.ofy;

public class List {

    public static ListProp createRestricted(String client, long groupId, String displayName,
                                            Set<Long> practiceIds, String login) {

        return create(client, groupId, displayName, true, practiceIds, true, login);
    }

    public static ListProp createPublic(String client, long groupId, String displayName, String login) {

        return create(client, groupId, displayName, false, null, true, login);
    }

    private static ListProp create(String client, long groupId, String displayName,
                                   boolean restricted, Set<Long> practiceIds, boolean enabled, String login) {

        Client.ensureValid(client);
        Group.safeGet(client, groupId);

        User.ensureGroupLevelPrivilege(client, groupId, login, GroupLevelPrivilege.UPDATE_LIST);

        ensureNotNull(displayName, "displayName is null");
        ensure(displayName.length() != 0, "displayName is empty");

        if (practiceIds == null)
            practiceIds = new HashSet<>();
        for (Long practiceId : practiceIds) {
            Practice.safeGet(client, practiceId);
        }

        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName.toLowerCase());

        java.util.List<Key<ListEntity>> keys =
                ofy(client).load().type(ListEntity.class).filter("name", name).filter("groupId", groupId)
                        .keys().list();

        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a list with name [" + displayName + "] for group [" + groupId + "]");

        ListEntity entity = new ListEntity();
        entity.listId = Sequence.getNext(client, SequenceType.LIST);
        entity.name = name;
        entity.displayName = displayName;
        entity.groupId = groupId;
        entity.restricted = restricted;
        entity.enabled = enabled;
        entity.practiceIds = practiceIds;

        ofy(client).save().entity(entity).now();
        return entity.toProp();
    }

    public static ListProp enable(String client, long listId, String login) {
        return update(client, listId, null, null, null, true, login);
    }

    public static ListProp disable(String client, long listId, String login) {
        return update(client, listId, null, null, null, false, login);
    }

    public static ListProp rename(String client, long listId, String newDisplayName, String login) {
        return update(client, listId, newDisplayName, null, null, null, login);
    }

    public static ListProp update(String client, long listId, String newDisplayName, Long newGroupId,
                                  Boolean newRestricted, Boolean newEnabled, String login) {

        Client.ensureValid(client);

        if (newGroupId != null) {
            Group.safeGet(client, newGroupId);

            ListEntity entity = safeGet(client, listId);
            User.ensureGroupLevelPrivilege(client, newGroupId, login, GroupLevelPrivilege.UPDATE_LIST);
            User.ensureGroupLevelPrivilege(client, entity.groupId, login, GroupLevelPrivilege.UPDATE_LIST);
        }

        ListEntity entity = safeGet(client, listId);
        User.ensureGroupLevelPrivilege(client, entity.groupId, login, GroupLevelPrivilege.UPDATE_LIST);

        // return if no changes
        if (newDisplayName == null)
            if (newGroupId == null)
                if (newRestricted == null)
                    if (newEnabled == null)
                        return entity.toProp();

        String newName = entity.name;
        if (newDisplayName != null) {
            ensure(newDisplayName.length() != 0, "displayName is empty");
            newName = Utils.removeSpaceUnderscoreBracketAndHyphen(newDisplayName);
        }

        if (newGroupId == null) {
            newGroupId = entity.groupId;
        }

        boolean checkForUnique = true;
        if (newName.equals(entity.name) && (newGroupId == entity.groupId)) {
            // changing case of the display name
            checkForUnique = false;
        }

        if (newRestricted == null)
            newRestricted = entity.restricted;

        if (newEnabled == null)
            newEnabled = entity.enabled;

        if (checkForUnique) {
            java.util.List<Key<ListEntity>> keys =
                    ofy(client).load().type(ListEntity.class).filter("name", newName)
                            .filter("groupId", entity.groupId).keys().list();

            if (keys.size() != 0)
                throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                        "There is already another list with name [" + newDisplayName + "] for group ["
                                + entity.groupId + "]");
        }

        entity.displayName = newDisplayName;
        entity.name = newName;
        entity.groupId = newGroupId;
        entity.enabled = newEnabled;
        entity.restricted = newRestricted;

        ofy(client).save().entity(entity).now();
        return entity.toProp();
    }

    public static ListEntity safeGet(String client, long listId) {

        Client.ensureValid(client);

        ensure(listId != 0, "listId is 0");
        ListEntity entity = ofy(client).load().type(ListEntity.class).id(listId).now();
        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "List id  [" + listId + "] does not exist");

        return entity;
    }

    public static ListEntity safeGetByGroupIdAndName(String client, long groupId, String name) {

        Client.ensureValid(client);

        Group.safeGet(client, groupId);

        ensureNotNullNotEmpty(name, "List name is either null or empty");
        name = Utils.removeSpaceUnderscoreBracketAndHyphen(name.toLowerCase());

        java.util.List<ListEntity> listEntities =
                ofy(client).load().type(ListEntity.class).filter("groupId", groupId).filter("name", name)
                        .list();

        ensure(!listEntities.isEmpty(), "There is no list [" + name + "] for group [" + groupId + "]");

        ensure(listEntities.size() == 1, "Found multiple [" + listEntities.size()
                + "] lists with name [" + name + "] for group [" + groupId + "]");

        return listEntities.get(0);
    }

    public static Map<Long, ListEntity> get(String client, Set<Long> listIds) {
        Client.ensureValid(client);

        ensureNotNull(listIds, "listIds is null");
        listIds.remove((long) 0);

        Map<Long, ListEntity> map = ofy(client).load().type(ListEntity.class).ids(listIds);
        return map;
    }

    public static java.util.List<ListProp> querySortedProps(String client, Long groupId) {
        Client.ensureValid(client);

        Query<ListEntity> q = ofy(client).load().type(ListEntity.class);
        if (groupId != null)
            q = q.filter("groupId", groupId);

        java.util.List<ListEntity> entities = q.list();

        java.util.List<ListProp> props = new ArrayList<>();
        for (ListEntity entity : entities) {
            props.add(entity.toProp());
        }

        Collections.sort(props);

        return props;
    }

    public static void delete(String client, long listId, String login) {

        Client.ensureValid(client);
        ListEntity entity = safeGet(client, listId);

        User.ensureGroupLevelPrivilege(client, entity.groupId, login, GroupLevelPrivilege.UPDATE_LIST);

        if (entity.enabled)
            throw new APIException("List should be disabled before deleting")
                    .status(Status.ERROR_OPERATION_NOT_ALLOWED);

        MemberQueryCondition mqc = new MemberQueryCondition(client, 20000);
        mqc.listIds = Utils.getSet(listId);
        int numSubscriptions = MemberLoader.getCount(mqc, login);
        if (numSubscriptions != 0)
            throw new APIException("List [" + listId + "] as it has [" + numSubscriptions
                    + "] subscribed members").status(Status.ERROR_PRECONDITION_FAILED);

        ofy(client).delete().entity(entity).now();
    }

    public static Set<Long> getPracticeIds(String client, Set<Long> listIds) {
        Client.ensureValid(client);

        Map<Long, ListEntity> map = ofy(client).load().type(ListEntity.class).ids(listIds);

        Set<Long> practiceIds = new HashSet<>();
        for (Entry<Long, ListEntity> entry : map.entrySet()) {
            ListEntity listEntity = entry.getValue();
            practiceIds.addAll(listEntity.practiceIds);
        }

        return practiceIds;
    }
}
