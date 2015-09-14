package crmdna.inventory;

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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class InventoryItemType {

    public static InventoryItemTypeProp create(String client, String displayName, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_INVENTORY_ITEM_TYPE);

        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName.toLowerCase());

        List<Key<InventoryItemTypeEntity>> keys =
                ofy(client).load().type(InventoryItemTypeEntity.class).filter("name", name).keys().list();

        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a inventory item type with name [" + displayName + "]");

        String key = getUniqueKey(client, name);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a inventory item type with name [" + displayName + "]");

        InventoryItemTypeEntity entity = new InventoryItemTypeEntity();
        entity.inventoryItemTypeId = Sequence.getNext(client, SequenceType.INVENTORY_ITEM_TYPE);
        entity.name = name;
        entity.displayName = displayName;
        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    private static String getUniqueKey(String namespace, String name) {
        return namespace + "_" + SequenceType.INVENTORY_ITEM_TYPE + "_" + name;
    }

    public static InventoryItemTypeEntity safeGet(String client, long inventoryItemTypeId) {

        Client.ensureValid(client);

        InventoryItemTypeEntity entity =
                ofy(client).load().type(InventoryItemTypeEntity.class).id(inventoryItemTypeId).now();

        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Inventory item type id  [" + inventoryItemTypeId + "] does not exist");

        return entity;
    }

    public static InventoryItemTypeEntity safeGetByName(String client, String name) {

        Client.ensureValid(client);
        ensureNotNull(name);

        name = Utils.removeSpaceUnderscoreBracketAndHyphen(name.toLowerCase());
        List<InventoryItemTypeEntity> entities =
                ofy(client).load().type(InventoryItemTypeEntity.class).filter("name", name).list();

        if (entities.size() == 0)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Inventory item type [" + name + "] does not exist");

        if (entities.size() > 1)
            // should never come here
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Found [" + entities.size() + "] matches for inventory item type [" + name
                            + "]. Please specify Id");
        return entities.get(0);
    }

    public static InventoryItemTypeProp rename(String client, long inventoryItemTypeId,
                                               String newDisplayName, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_INVENTORY_ITEM_TYPE);

        InventoryItemTypeEntity entity = safeGet(client, inventoryItemTypeId);

        String newName = newDisplayName.toLowerCase();

        if (entity.name.equals(newName)) {
            // ideally should be inside a transaction
            entity.displayName = newDisplayName;
            ofy(client).save().entity(entity).now();
            return entity.toProp();
        }

        List<Key<InventoryItemTypeEntity>> keys =
                ofy(client).load().type(InventoryItemTypeEntity.class).filter("name", newName).keys()
                        .list();
        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a inventory item type with name [" + newDisplayName + "]");

        String key = getUniqueKey(client, newDisplayName);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a inventory item type with name [" + newDisplayName + "]");

        // ideally should be inside a transaction
        entity.name = newName;
        entity.displayName = newDisplayName;
        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static List<InventoryItemTypeProp> getAll(String client) {
        Client.ensureValid(client);

        List<InventoryItemTypeEntity> entities =
                ofy(client).load().type(InventoryItemTypeEntity.class).order("name").list();

        List<InventoryItemTypeProp> props = new ArrayList<>();
        for (InventoryItemTypeEntity entity : entities)
            props.add(entity.toProp());

        return props;
    }

    public static Map<Long, InventoryItemTypeEntity> get(String client, Iterable<Long> ids) {

        Map<Long, InventoryItemTypeEntity> map =
                ofy(client).load().type(InventoryItemTypeEntity.class).ids(ids);

        return map;
    }

    public static void delete(String client, long groupId, String login) {

        throw new APIException().status(Status.ERROR_NOT_IMPLEMENTED).message(
                "This functionality is not implemented yet");

        // GroupEntity groupEntity = safeGet(client, groupId);

        // ofy(client).delete().entity(groupEntity).now();
    }
}
