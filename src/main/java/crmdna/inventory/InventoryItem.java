package crmdna.inventory;

import com.google.appengine.api.datastore.*;
import com.google.appengine.api.datastore.Query.FilterOperator;
import com.google.gson.Gson;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.DSUtils;
import crmdna.common.UnitUtils;
import crmdna.common.UnitUtils.PhysicalQuantity;
import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils.Currency;
import crmdna.group.Group;
import crmdna.inventory.InventoryItemCore.CheckInOrOut;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;

import java.util.*;

import static crmdna.common.AssertUtils.*;
import static crmdna.common.OfyService.ofy;
import static crmdna.common.ProjectionQuery.pq;

public class InventoryItem {

    public static InventoryItemProp create(String client, long groupId, long inventoryItemTypeId,
                                           String displayName, PhysicalQuantity physicalQuantity, ReportingUnit reportingUnit,
                                           String login) {

        Client.ensureValid(client);
        Group.safeGet(client, groupId);
        InventoryItemType.safeGet(client, inventoryItemTypeId);

        User.ensureGroupLevelPrivilege(client, groupId, login,
                GroupLevelPrivilege.UPDATE_INVENTORY_ITEM);

        return InventoryItemCore.create(client, groupId, inventoryItemTypeId, displayName,
                physicalQuantity, reportingUnit);
    }

    public static InventoryItemProp update(String client, long inventoryItemId,
                                           Long newInventoryItemTypeId, String newDisplayName, ReportingUnit newReportingUnit,
                                           String login) {

        Client.ensureValid(client);

        InventoryItemEntity entity = safeGet(client, inventoryItemId);
        User.ensureGroupLevelPrivilege(client, entity.groupId, login,
                GroupLevelPrivilege.UPDATE_INVENTORY_ITEM);

        Group.safeGet(client, entity.groupId);

        return InventoryItemCore.update(client, inventoryItemId, newInventoryItemTypeId,
                newDisplayName, newReportingUnit);
    }

    public static InventoryItemEntity safeGet(String client, long inventoryItemId) {

        Client.ensureValid(client);

        return InventoryItemCore.safeGet(client, inventoryItemId);
    }

    public static Map<Long, InventoryItemEntity> get(String client, Set<Long> inventoryItemIds) {

        Client.ensureValid(client);

        return InventoryItemCore.get(client, inventoryItemIds);
    }

    public static InventoryItemEntity safeGetByName(String client, String name) {

        Client.ensureValid(client);

        return InventoryItemCore.safeGetByName(client, name);
    }

    public static List<InventoryItemProp> query_to_be_removed(String client,
                                                              InventoryItemQueryCondition qc, String login) {

        List<Key<InventoryItemEntity>> keys = InventoryItemCore.queryKeys(client, qc).list();

        List<Long> ids = new ArrayList<>();
        List<InventoryItemProp> props = new ArrayList<>(keys.size());

        for (Key<InventoryItemEntity> key : keys) {
            long id = key.getId();
            ids.add(id);

            InventoryItemProp prop = new InventoryItemProp();
            prop.inventoryItemId = id;
            props.add(prop);
        }

        List<com.google.appengine.api.datastore.Key> rawKeys = new ArrayList<>();
        for (Key<?> key : keys) {
            rawKeys.add(key.getRaw());
        }

        System.out.println("ids = " + new Gson().toJson(ids));

        ensureEqual(keys.size(), props.size());

        AsyncDatastoreService datastore = DatastoreServiceFactory.getAsyncDatastoreService();

        // com.google.appengine.api.datastore.Query.Filter filter = new
        // com.google.appengine.api.datastore.Query.FilterPredicate(
        // "inventoryItemId", FilterOperator.IN, ids);

        com.google.appengine.api.datastore.Query.Filter filter =
                new com.google.appengine.api.datastore.Query.FilterPredicate(Entity.KEY_RESERVED_PROPERTY,
                        FilterOperator.IN, rawKeys);

        com.google.appengine.api.datastore.Query q =
                new com.google.appengine.api.datastore.Query(InventoryItemEntity.class.getSimpleName());

        q.setFilter(filter).addProjection(new PropertyProjection("groupId", Long.class));

        Iterable<Entity> groupIds = datastore.prepare(q).asList(FetchOptions.Builder.withLimit(10000));

        q = new com.google.appengine.api.datastore.Query("InventoryItemEntity");
        q.setFilter(filter).addProjection(new PropertyProjection("inventoryItemTypeId", Long.class));

        Iterable<Entity> inventoryItemTypeIds = datastore.prepare(q).asIterable();

        int i = 0;
        for (Entity entity : groupIds) {

            InventoryItemProp prop = props.get(i);
            prop.groupId = (long) entity.getProperty("groupId");
            i++;
        }
        System.out.println("i = " + i + ", key.size() = " + keys.size());

        i = 0;
        for (Entity entity : inventoryItemTypeIds) {

            InventoryItemProp prop = props.get(i);
            prop.inventoryItemTypeId = (long) entity.getProperty("inventoryItemTypeId");
            i++;
        }
        ensureEqual(keys.size(), i);

        System.out.println("before populateDependents. props: " + new Gson().toJson(props));

        InventoryItemProp.populateDependents(client, props);

        Collections.sort(props);

        return props;
    }

    public static List<InventoryItemProp> query(String client, InventoryItemQueryCondition qc,
                                                String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        return InventoryItemCore.query(client, qc);
    }

    public static Map<Long, InventoryItemEntity> queryEntities(String client,
                                                               InventoryItemQueryCondition qc, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        return InventoryItemCore.queryEntities(client, qc);
    }

    public static List<InventoryItemProp> query2_to_be_removed(String client,
                                                               InventoryItemQueryCondition qc, String login) {

        List<Key<InventoryItemEntity>> keys = InventoryItemCore.queryKeys(client, qc).list();

        List<Long> ids = new ArrayList<>();
        List<InventoryItemProp> props = new ArrayList<>(keys.size());

        for (Key<InventoryItemEntity> key : keys) {
            long id = key.getId();
            ids.add(id);

            InventoryItemProp prop = new InventoryItemProp();
            prop.inventoryItemId = id;
            props.add(prop);
        }

        String kind = "InventoryItemEntity";
        // List<Long> groupIds = DSUtils.executeProjectionQuery(kind, keys,
        // "groupId", Long.class);

        List<Long> groupIds =
                DSUtils.executeProjectionQuery2(InventoryItemEntity.class, keys, "groupId", Long.class);

        List<Long> inventoryItemTypeIds =
                DSUtils.executeProjectionQuery(kind, keys, "inventoryItemTypeId", Long.class);

        ensureEqual(keys.size(), groupIds.size(),
                "Records returned by projection query [" + groupIds.size()
                        + "] is different from number of keys [" + keys.size() + "]");
        ensureEqual(keys.size(), inventoryItemTypeIds.size(), "Projection query result mismatch");

        for (int i = 0; i < keys.size(); i++) {
            InventoryItemProp prop = props.get(i);
            prop.groupId = groupIds.get(i);
            prop.inventoryItemTypeId = inventoryItemTypeIds.get(i);
        }

        InventoryItemProp.populateDependents(client, props);

        Collections.sort(props);

        return props;
    }

    public static List<InventoryItemProp> query3_to_be_removed(String client,
                                                               InventoryItemQueryCondition qc, String login) {

        List<Key<InventoryItemEntity>> keys = InventoryItemCore.queryKeys(client, qc).list();

        List<Long> ids = new ArrayList<>();
        List<InventoryItemProp> props = new ArrayList<>(keys.size());

        for (Key<InventoryItemEntity> key : keys) {
            long id = key.getId();
            ids.add(id);

            InventoryItemProp prop = new InventoryItemProp();
            prop.inventoryItemId = id;
            props.add(prop);
        }

        List<Long> groupIds =
                pq(InventoryItemEntity.class, Long.class).keys(keys).property("groupId").execute();

        List<Long> inventoryItemTypeIds =
                pq(InventoryItemEntity.class, Long.class).keys(keys).property("inventoryItemTypeId")
                        .execute();

        ensureEqual(keys.size(), groupIds.size(),
                "Records returned by projection query [" + groupIds.size()
                        + "] is different from number of keys [" + keys.size() + "]");
        ensureEqual(keys.size(), inventoryItemTypeIds.size(), "Projection query result mismatch");

        for (int i = 0; i < keys.size(); i++) {
            InventoryItemProp prop = props.get(i);
            prop.groupId = groupIds.get(i);
            prop.inventoryItemTypeId = inventoryItemTypeIds.get(i);
        }

        InventoryItemProp.populateDependents(client, props);

        Collections.sort(props);

        return props;
    }

    public static InventoryCheckInProp checkIn(String client, long inventoryItemId, Date date,
                                               double qtyInReportingUnit, ReportingUnit reportingUnit, double pricePerReportingUnit,
                                               Currency ccy, String changeDescription, final String login) {

        Client.ensureValid(client);

        InventoryItemEntity inventoryItemEntity = InventoryItem.safeGet(client, inventoryItemId);

        User.ensureGroupLevelPrivilege(client, inventoryItemEntity.groupId, login,
                GroupLevelPrivilege.UPDATE_INVENTORY_QUANTITY);

        return InventoryItemCore.checkIn(client, inventoryItemId, date, qtyInReportingUnit,
                reportingUnit, pricePerReportingUnit, ccy, changeDescription, login);

    }

    public static InventoryCheckOutProp checkOut(String client, final long inventoryItemId,
                                                 Date date, double qtyInReportingUnit, final ReportingUnit reportingUnit,
                                                 Double pricePerReportingUnit, Currency ccy, String comment, Set<String> tags,
                                                 final String login) {

        Client.ensureValid(client);

        InventoryItemEntity inventoryItemEntity = InventoryItem.safeGet(client, inventoryItemId);

        User.ensureGroupLevelPrivilege(client, inventoryItemEntity.groupId, login,
                GroupLevelPrivilege.UPDATE_INVENTORY_QUANTITY);

        return InventoryItemCore.checkOut(client, inventoryItemId, date, qtyInReportingUnit,
                reportingUnit, pricePerReportingUnit, ccy, comment, tags, login);
    }

    public static void delete(String client, long inventoryItemId, String login) {
        Client.ensureValid(client);

        InventoryItemEntity entity = safeGet(client, inventoryItemId);

        User.ensureGroupLevelPrivilege(client, entity.groupId, login,
                GroupLevelPrivilege.UPDATE_INVENTORY_ITEM);

        InventoryItemCore.delete(client, inventoryItemId);
    }

    public static List<StockChangeProp> queryStockChanges(String client,
                                                          StockChangeQueryCondition qc, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);
        ensureNotNull(qc);

        Group.safeGet(client, qc.groupId);
        ensure(qc.startMS > 0);
        ensure(qc.endMS > 0);
        ensure(qc.endMS >= qc.startMS, "endMS should be greater or equal to startMS");
        ensure(qc.groupId != 0, "qc.groupId is 0");

        Group.safeGet(client, qc.groupId);

        ensure(qc.includeCheckIn || qc.includeCheckOut,
                "Either checkIn or checkOut should be specified");

        if (qc.inventoryItemIds == null)
            qc.inventoryItemIds = new HashSet<>();
        if (qc.inventoryItemTypeIds == null)
            qc.inventoryItemTypeIds = new HashSet<>();

        ensure(qc.inventoryItemIds.isEmpty() || qc.inventoryItemTypeIds.isEmpty(),
                "Both inventoryItemIds and inventoryItemTypeIds cannot be specified");

        InventoryItemQueryCondition iiqc = new InventoryItemQueryCondition();
        iiqc.groupId = qc.groupId;
        iiqc.inventoryItemTypeIds = qc.inventoryItemTypeIds;

        Map<Long, InventoryItemEntity> allInventoryItemEntitiesForGroup =
                InventoryItem.queryEntities(client, iiqc, login);

        if ((qc.inventoryItemIds != null) && !qc.inventoryItemIds.isEmpty()) {
            ensure(allInventoryItemEntitiesForGroup.keySet().containsAll(qc.inventoryItemIds),
                    "All specified inventoryItemIds should belong to group [" + qc.groupId + "]");
        } else {
            qc.inventoryItemIds = allInventoryItemEntitiesForGroup.keySet();
        }

        // return if no inventory item is available for the group
        if (qc.inventoryItemIds.isEmpty())
            return new ArrayList<>();


        List<InventoryCheckInEntity> checkIns = new ArrayList<>();
        if (qc.includeCheckIn) {
            Query<InventoryCheckInEntity> q = ofy(client).load().type(InventoryCheckInEntity.class);

            q =
                    q.filter("ms >=", qc.startMS).filter("ms <=", qc.endMS)
                            .filter("inventoryItemId in", qc.inventoryItemIds);

            if ((qc.logins != null) && !qc.logins.isEmpty())
                q = q.filter("login in", qc.logins);

            q = q.order("-ms");

            checkIns = q.list();
        }

        List<InventoryCheckOutEntity> checkOuts = new ArrayList<>();
        if (qc.includeCheckOut) {
            Query<InventoryCheckOutEntity> q = ofy(client).load().type(InventoryCheckOutEntity.class);

            q =
                    q.filter("ms >=", qc.startMS).filter("ms <=", qc.endMS)
                            .filter("inventoryItemId in", qc.inventoryItemIds);

            if ((qc.logins != null) && !qc.logins.isEmpty())
                q = q.filter("login in", qc.logins);

            if ((qc.tags != null) && !qc.tags.isEmpty())
                q = q.filter("tags", qc.tags);

            q = q.order("-ms");

            checkOuts = q.list();
        }

        List<StockChangeProp> stockChangeProps = new ArrayList<>();
        for (InventoryCheckInEntity checkInEntity : checkIns) {
            ensure(qc.inventoryItemIds.contains(checkInEntity.inventoryItemId), "Inventory id ["
                    + checkInEntity.inventoryItemId + "] missing");

            InventoryItemEntity inventoryItemEntity =
                    allInventoryItemEntitiesForGroup.get(checkInEntity.inventoryItemId);

            StockChangeProp prop = new StockChangeProp();
            prop.timestamp = new Date(checkInEntity.ms);
            prop.login = checkInEntity.login;
            prop.inventoryItemId = checkInEntity.inventoryItemId;

            prop.inventoryItem = inventoryItemEntity.displayName;
            prop.changeInReportingUnit =
                    UnitUtils.safeGetQtyInReportingUnit(inventoryItemEntity.physicalQuantity,
                            checkInEntity.qtyInDefaultUnit, inventoryItemEntity.reportingUnit);
            prop.reportingUnit = inventoryItemEntity.reportingUnit;

            prop.cost = checkInEntity.pricePerDefaultUnit * checkInEntity.qtyInDefaultUnit;
            prop.ccy = checkInEntity.ccy;

            prop.checkInOrOut = CheckInOrOut.CHECK_IN;
            prop.checkInOrOutId = checkInEntity.checkInId;
            prop.comment = checkInEntity.comment;

            stockChangeProps.add(prop);
        }

        for (InventoryCheckOutEntity checkOutEntity : checkOuts) {
            ensure(allInventoryItemEntitiesForGroup.containsKey(checkOutEntity.inventoryItemId),
                    "Inventory id [" + checkOutEntity.inventoryItemId + "] missing in map");
            InventoryItemEntity inventoryItemEntity =
                    allInventoryItemEntitiesForGroup.get(checkOutEntity.inventoryItemId);

            StockChangeProp prop = new StockChangeProp();
            prop.timestamp = new Date(checkOutEntity.ms);
            prop.login = checkOutEntity.login;
            prop.inventoryItemId = checkOutEntity.inventoryItemId;

            prop.inventoryItem = inventoryItemEntity.displayName;
            prop.changeInReportingUnit =
                    UnitUtils.safeGetQtyInReportingUnit(inventoryItemEntity.physicalQuantity,
                            checkOutEntity.qtyInDefaultUnit, inventoryItemEntity.reportingUnit);
            prop.reportingUnit = inventoryItemEntity.reportingUnit;

            prop.cost = checkOutEntity.avgPricePerDefaultUnit * checkOutEntity.qtyInDefaultUnit;
            prop.ccy = checkOutEntity.ccy;

            prop.checkInOrOut = CheckInOrOut.CHECK_OUT;
            prop.checkInOrOutId = checkOutEntity.checkOutId;
            prop.comment = checkOutEntity.comment;
            prop.tags = checkOutEntity.tags;

            stockChangeProps.add(prop);
        }

        Collections.sort(stockChangeProps);
        return stockChangeProps;
    }
}
