package crmdna.inventory;

import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryKeys;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.group.Group;
import crmdna.sequence.Sequence;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.User.GroupLevelPrivilege;

import java.util.*;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class PackagedInventoryItem {

    public static int create(String client, long groupId,
                             long inventoryItemId, int expiryYYYYMMDD, double costPrice, double sellingPrice,
                             Utils.Currency currency, int qty, long batchId, String login) {

        Client.ensureValid(client);
        Group.safeGet(client, groupId);
        InventoryItem.safeGet(client, inventoryItemId);

        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_INVENTORY_ITEM_TYPE);

        ensure(costPrice > 0, "CostPrice name cannot <= 0");
        ensure(sellingPrice > 0, "SellingPrice name cannot <= 0");
        ensure(qty > 0, "Quantity name cannot <= 0");

        List<PackagedInventoryItemEntity> entities = new ArrayList<>();

        for (int i = 0; i < qty; i++) {
            PackagedInventoryItemEntity entity = new PackagedInventoryItemEntity();
            entity.packagedInventoryItemId =
                    Sequence.getNext(client, Sequence.SequenceType.PACKAGED_INVENTORY_ITEM);
            entity.inventoryItemId = inventoryItemId;
            entity.locationId = 1;
            entity.expiryYYYYMMDD = expiryYYYYMMDD;
            entity.costPrice = costPrice;
            entity.sellingPrice = sellingPrice;
            entity.currency = currency;
            entity.salesId = 0;
            entity.batchId = batchId;
            entity.lastUpdatedMS = System.currentTimeMillis();

            entities.add(entity);
        }

        ofy(client).save().entities(entities).now();

        return entities.size();
    }

    public static PackagedInventoryItemProp update(String client, long packagedInventoryItemId,
                                                   Long newInventoryItemId, Integer newExpiryYYYYMMDD, Double newCostPrice,
                                                   Double newSellingPrice, String login) {

        Client.ensureValid(client);

        PackagedInventoryItemEntity entity = safeGet(client, packagedInventoryItemId);
        InventoryItemEntity inventoryItemEntity = InventoryItem.safeGet(client, entity.inventoryItemId);

        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_INVENTORY_ITEM_TYPE);

        Group.safeGet(client, inventoryItemEntity.groupId);

        boolean changed = false;
        if (newInventoryItemId != null) {
            entity.inventoryItemId = newInventoryItemId;
            changed = true;
        }

        if (newExpiryYYYYMMDD != null) {
            DateUtils.ensureFormatYYYYMMDD(newExpiryYYYYMMDD);
            entity.expiryYYYYMMDD = newExpiryYYYYMMDD;
            changed = true;
        }

        if (newCostPrice != null) {
            entity.costPrice = newCostPrice;
            changed = true;
        }

        if (newSellingPrice != null) {
            entity.sellingPrice = newSellingPrice;
            changed = true;
        }

        if (changed) {
            entity.lastUpdatedMS = System.currentTimeMillis();
            ofy(client).save().entity(entity).now();
        }

        return entity.toProp(client);
    }

    public static void transfer(String client, long groupId, Set<Long> packagedInventoryItemIds,
                                Long fromLocationId, Long toLocationId, String login) {
        Client.ensureValid(client);
        ensureNotNull(fromLocationId, "From location cannot be null");
        ensureNotNull(toLocationId, "To location cannot be null");

        User.ensureGroupLevelPrivilege(client, groupId, login,
                GroupLevelPrivilege.UPDATE_INVENTORY_ITEM);

        InventoryLocation.safeGet(client, fromLocationId);
        InventoryLocation.safeGet(client, toLocationId);

        ensure(packagedInventoryItemIds.size() <= 1000, "Max transfer size is 1000");

        Map<Long, PackagedInventoryItemEntity> map = get(client, packagedInventoryItemIds);
        Collection<PackagedInventoryItemEntity> entities = map.values();
        for (PackagedInventoryItemEntity entity : entities) {
            ensure(entity.locationId == fromLocationId, "Item [" + entity.packagedInventoryItemId
                    + "]'s location (" + entity.locationId + ") != (" + fromLocationId + ")");

            entity.locationId = toLocationId;
            entity.lastUpdatedMS = System.currentTimeMillis();
        }
        ofy(client).save().entities(entities).now();

        InventoryTransferEntity entity = new InventoryTransferEntity();
        entity.inventoryTransferId = Sequence.getNext(client, Sequence.SequenceType.INVENTORY_TRANSFER);
        entity.packagedInventoryItemIds = map.keySet();
        entity.transferMS = System.currentTimeMillis();
        entity.fromLocationId = fromLocationId;
        entity.toLocationId = toLocationId;

        ofy(client).save().entity(entity).now();
    }

    public static void updateSold(String client, long groupId, Map<Long, Double> packagedInventoryItemIdMap,
                                  String salesOrder, boolean paidOnline, String login) {
        Client.ensureValid(client);

        User.ensureGroupLevelPrivilege(client, groupId, login,
                GroupLevelPrivilege.UPDATE_INVENTORY_QUANTITY);

        ensure(packagedInventoryItemIdMap.size() <= 1000, "Max transfer size is 1000");

        PackagedInventorySalesEntity salesEntity = new PackagedInventorySalesEntity();
        salesEntity.salesId = Sequence.getNext(client, Sequence.SequenceType.PACKAGED_INVENTORY_SALES);
        salesEntity.paidOnline = paidOnline;
        salesEntity.salesOrder = salesOrder;
        salesEntity.salesMS = System.currentTimeMillis();
        salesEntity.user = login;

        Map<Long, PackagedInventoryItemEntity> map = get(client, packagedInventoryItemIdMap.keySet());
        Collection<PackagedInventoryItemEntity> entities = map.values();
        for (PackagedInventoryItemEntity entity : entities) {
            ensure(entity.salesId == 0, "Item [" + entity.packagedInventoryItemId
                    + "] is already sold (" + entity.salesId + ")");

            entity.salesId = salesEntity.salesId;
            entity.locationId = 0;
            entity.sellingPrice = packagedInventoryItemIdMap.get(entity.packagedInventoryItemId);
            entity.lastUpdatedMS = System.currentTimeMillis();
        }

        ofy(client).save().entities(entities).now();
        ofy(client).save().entity(salesEntity).now();
    }

    public static PackagedInventoryItemEntity safeGet(String client, long packagedInventoryItemId) {

        Client.ensureValid(client);

        PackagedInventoryItemEntity entity =
                ofy(client).load().type(PackagedInventoryItemEntity.class).id(packagedInventoryItemId)
                        .now();

        if (null == entity)
            throw new APIException().status(APIResponse.Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Packaged Inventory item id [" + packagedInventoryItemId + "] does not exist");

        return entity;
    }

    public static Map<Long, PackagedInventoryItemEntity> get(String client,
                                                             Set<Long> packagedInventoryItemIds) {

        Client.ensureValid(client);

        return ofy(client).load().type(PackagedInventoryItemEntity.class).ids(packagedInventoryItemIds);
    }

    public static List<PackagedInventoryItemProp> query(String client,
                                                        PackagedInventoryItemQueryCondition qc, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        List<Key<PackagedInventoryItemEntity>> keys = queryKeys(client, qc).list();

        ensureNotNull(keys);

        Collection<PackagedInventoryItemEntity> entities = ofy(client).load().keys(keys).values();

        List<PackagedInventoryItemProp> props = new ArrayList<>(keys.size());

        for (PackagedInventoryItemEntity entity : entities) {
            InventoryItemEntity itemEntity = InventoryItem.safeGet(client, entity.inventoryItemId);
            PackagedInventoryItemProp prop = entity.toProp(client);
            prop.inventoryItemDisplayName = itemEntity.displayName;
            props.add(prop);
        }

        Collections.sort(props);
        return props;
    }

    public static Map<Long, PackagedInventoryItemEntity> queryEntities(String client,
                                                                       PackagedInventoryItemQueryCondition qc, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        return PackagedInventoryItem.queryEntities(client, qc);
    }

    static QueryKeys<PackagedInventoryItemEntity> queryKeys(String namespace,
                                                            PackagedInventoryItemQueryCondition qc) {

        ensureNotNull(qc);

        Query<PackagedInventoryItemEntity> query =
                ofy(namespace).load().type(PackagedInventoryItemEntity.class);

        if ((qc.inventoryItemIds != null) && !qc.inventoryItemIds.isEmpty()) {
            query = query.filter("inventoryItemId in", qc.inventoryItemIds);
        }

        if (qc.expiryYYYYMMDD != null) {
            query = query.filter("expiryYYYYMMDD", qc.expiryYYYYMMDD);
        }

        if (qc.locationId != null) {
            query = query.filter("locationId", qc.locationId);
        }

        if (qc.salesId != null) {
            query = query.filter("salesId", qc.salesId);
        }

        if (qc.batchId != null) {
            query = query.filter("batchId", qc.batchId);
        }

        return query.keys();
    }

    static Map<Long, PackagedInventoryItemEntity> queryEntities(String namespace,
                                                                PackagedInventoryItemQueryCondition qc) {

        List<Key<PackagedInventoryItemEntity>> keys =
                PackagedInventoryItem.queryKeys(namespace, qc).list();

        ensureNotNull(keys);

        Set<Long> ids = new HashSet<>();
        for (Key<PackagedInventoryItemEntity> key : keys) {
            long id = key.getId();
            ids.add(id);
        }

        return ofy(namespace).load().type(PackagedInventoryItemEntity.class).ids(ids);
    }

}
