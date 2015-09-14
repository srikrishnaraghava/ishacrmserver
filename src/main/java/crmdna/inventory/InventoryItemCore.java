package crmdna.inventory;

import com.google.appengine.api.memcache.MemcacheServiceFactory;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.cmd.Query;
import com.googlecode.objectify.cmd.QueryKeys;
import crmdna.client.Client;
import crmdna.common.UnitUtils;
import crmdna.common.UnitUtils.PhysicalQuantity;
import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;

import java.util.*;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class InventoryItemCore {

    static InventoryItemProp create(String namespace, long groupId, long inventoryItemTypeId,
                                    String displayName, PhysicalQuantity physicalQuantity, ReportingUnit reportingUnit) {

        ensureNotNull(displayName, "Display name cannot be null");
        ensure(displayName.length() > 0, "Display name cannot be empty");

        // first character should be an alphabet
        char c = displayName.toLowerCase().charAt(0);
        ensure(c >= 'a' && c <= 'z', "First character should be an alphabet");

        UnitUtils.ensureValidReportingUnit(physicalQuantity, reportingUnit);

        String name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName.toLowerCase());
        List<Key<InventoryItemEntity>> keys =
                ofy(namespace).load().type(InventoryItemEntity.class).filter("name", name)
                        .filter("groupId", groupId).keys().list();

        if (keys.size() != 0)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already an inventory item with the same name for group [" + groupId + "]");

        String key = getUniqueKey(namespace, groupId, name);
        long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "There is already a inventory item with the same name (in cache) for group [" + groupId
                            + "]");

        InventoryItemEntity entity = new InventoryItemEntity();
        entity.inventoryItemId = Sequence.getNext(namespace, SequenceType.INVENTORY_ITEM);
        entity.groupId = groupId;
        entity.displayName = displayName;
        entity.physicalQuantity = physicalQuantity;
        entity.inventoryItemTypeId = inventoryItemTypeId;
        entity.groupId = groupId;
        entity.reportingUnit = reportingUnit;

        // populate dependents
        entity.name = name;
        entity.firstChar = entity.name.substring(0, 1);

        ofy(namespace).save().entity(entity).now();

        return entity.toProp();
    }

    static InventoryItemProp update(String namespace, long inventoryItemId,
                                    Long newInventoryItemTypeId, String newDisplayName, ReportingUnit newReportingUnit) {

        InventoryItemEntity entity = safeGet(namespace, inventoryItemId);

        String name = null;
        if (newDisplayName != null) {
            ensure(newDisplayName.length() > 0, "Display name cannot be empty");
            char c = newDisplayName.toLowerCase().charAt(0);
            ensure(c >= 'a' && c <= 'z', "First character of display name should be an alphabet");

            name = Utils.removeSpaceUnderscoreBracketAndHyphen(newDisplayName.toLowerCase());

            if (!entity.name.equals(name)) {
                // ensure name doesn't clash with another existing entity in the
                // same group
                List<Key<InventoryItemEntity>> keys =
                        ofy(namespace).load().type(InventoryItemEntity.class).filter("name", name)
                                .filter("groupId", entity.groupId).keys().list();

                if (keys.size() != 0)
                    throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                            "There is already an inventory item with the same name for group [" + entity.groupId
                                    + "]");

                String key = getUniqueKey(namespace, entity.groupId, name);
                long val = MemcacheServiceFactory.getMemcacheService().increment(key, 1, (long) 0);

                if (val != 1) {
                    throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                            "There is already a inventory item with the same name for group [" + entity.groupId
                                    + "]");
                }
            }
        }

        if (newInventoryItemTypeId != null) {
            InventoryItemType.safeGet(namespace, newInventoryItemTypeId);
        }

        if (newReportingUnit != null) {
            UnitUtils.ensureValidReportingUnit(entity.physicalQuantity, newReportingUnit);
        }

        // all ok. populate and save
        if (newDisplayName != null) {
            entity.displayName = newDisplayName;

            entity.name = name;
            entity.firstChar = entity.name.substring(0, 1);
        }

        if (newInventoryItemTypeId != null)
            entity.inventoryItemTypeId = newInventoryItemTypeId;

        if (newReportingUnit != null)
            entity.reportingUnit = newReportingUnit;

        ofy(namespace).save().entity(entity).now();
        return entity.toProp();
    }

    private static String getUniqueKey(String namespace, long groupId, String name) {

        return namespace + "_" + SequenceType.INVENTORY_ITEM + "_" + groupId + "_" + name;
    }

    static InventoryItemEntity safeGet(String namespace, long inventoryItemId) {

        InventoryItemEntity entity =
                ofy(namespace).load().type(InventoryItemEntity.class).id(inventoryItemId).now();
        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Inventory item id [" + inventoryItemId + "] does not exist");

        return entity;
    }

    static InventoryItemEntity safeGetByName(String namespace, String name) {

        ensureNotNull(name);

        name = Utils.removeSpaceUnderscoreBracketAndHyphen(name.toLowerCase());
        List<InventoryItemEntity> entities =
                ofy(namespace).load().type(InventoryItemEntity.class).filter("name", name).list();

        if (entities.size() == 0)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Inventory item [" + name + "] does not exist");

        if (entities.size() > 1)
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Found [" + entities.size() + "] matches for inventory item [" + name
                            + "]. Please specify Id");
        return entities.get(0);
    }

    static Map<Long, InventoryItemEntity> get(String namespace, Set<Long> inventoryItemIds) {

        Map<Long, InventoryItemEntity> map =
                ofy(namespace).load().type(InventoryItemEntity.class).ids(inventoryItemIds);

        return map;
    }

    static List<InventoryItemProp> query(String namespace, InventoryItemQueryCondition qc) {

        List<Key<InventoryItemEntity>> keys = queryKeys(namespace, qc).list();

        ensureNotNull(keys);

        Collection<InventoryItemEntity> entities = ofy(namespace).load().keys(keys).values();

        List<InventoryItemProp> props = new ArrayList<>(keys.size());
        for (InventoryItemEntity entity : entities) {
            props.add(entity.toProp());
        }

        InventoryItemProp.populateDependents(namespace, props);

        Collections.sort(props);
        return props;
    }

    static QueryKeys<InventoryItemEntity> queryKeys(String namespace, InventoryItemQueryCondition qc) {

        ensureNotNull(qc);

        Query<InventoryItemEntity> query = ofy(namespace).load().type(InventoryItemEntity.class);

        if (qc.groupId != null) {
            query = query.filter("groupId", qc.groupId);
        }

        if ((qc.inventoryItemTypeIds != null) && !qc.inventoryItemTypeIds.isEmpty()) {
            query = query.filter("inventoryItemTypeId in", qc.inventoryItemTypeIds);
        }

        if ((qc.firstChars != null) && !qc.firstChars.isEmpty()) {
            query = query.filter("firstChar in", qc.firstChars);
        }

        return query.keys();
    }

    static Map<Long, InventoryItemEntity> queryEntities(String namespace,
                                                        InventoryItemQueryCondition qc) {

        List<Key<InventoryItemEntity>> keys = InventoryItemCore.queryKeys(namespace, qc).list();

        ensureNotNull(keys);

        Set<Long> ids = new HashSet<>();
        for (Key<InventoryItemEntity> key : keys) {
            long id = key.getId();
            ids.add(id);
        }

        Map<Long, InventoryItemEntity> map =
                ofy(namespace).load().type(InventoryItemEntity.class).ids(ids);

        return map;
    }

    static InventoryCheckInProp checkIn(String namespace, long inventoryItemId, Date date,
                                        double qtyInReportingUnit, ReportingUnit reportingUnit, double pricePerReportingUnit,
                                        Currency ccy, String changeDescription, String login) {

        InventoryItemEntity inventoryItemEntity = InventoryItemCore.safeGet(namespace, inventoryItemId);

        ensure(qtyInReportingUnit > 0, "invalid quantityInReporting [" + qtyInReportingUnit + "]");

        double qtyInDefaultUnit =
                UnitUtils.safeGetQtyInDefaultUnit(inventoryItemEntity.physicalQuantity, qtyInReportingUnit,
                        reportingUnit);

        ensure(pricePerReportingUnit >= 0, "invalid pricePerReportingUnit [" + pricePerReportingUnit
                + "]");

        InventoryCheckInEntity inventoryCheckInEntity = new InventoryCheckInEntity();
        inventoryCheckInEntity.checkInId = Sequence.getNext(namespace, SequenceType.INVENTORY_CHECKIN);

        if (date == null)
            inventoryCheckInEntity.ms = new Date().getTime();
        else
            inventoryCheckInEntity.ms = date.getTime();

        inventoryCheckInEntity.availableQtyInDefaultUnit = qtyInDefaultUnit;
        inventoryCheckInEntity.qtyInDefaultUnit = qtyInDefaultUnit;
        inventoryCheckInEntity.available = true;
        inventoryCheckInEntity.ccy = ccy;
        inventoryCheckInEntity.inventoryItemId = inventoryItemId;
        inventoryCheckInEntity.pricePerDefaultUnit =
                UnitUtils.safeGetPricePerDefaultUnit(inventoryItemEntity.physicalQuantity,
                        pricePerReportingUnit, reportingUnit);
        inventoryCheckInEntity.login = login;

        ofy(namespace).save().entity(inventoryCheckInEntity).now();

        return inventoryCheckInEntity.toProp(UnitUtils
                .getDefaultUnit(inventoryItemEntity.physicalQuantity));
    }

    private static Map<Long, Double> getAvailableQtyInDefaultUnit(
            List<InventoryCheckInEntity> entities) {

        HashMap<Long, Double> map = new HashMap<Long, Double>();

        for (InventoryCheckInEntity entity : entities) {

            if (!map.containsKey(entity.inventoryItemId))
                map.put(entity.inventoryItemId, 0.0);

            double value = map.get(entity.inventoryItemId);
            value += entity.availableQtyInDefaultUnit;

            map.put(entity.inventoryItemId, value);
        }

        return map;
    }

    static Map<Long, QuantityPriceProp> getAvailableQtyAndAvgPrice(String namespace,
                                                                   Set<Long> inventoryItemIds) {

        HashMap<Long, QuantityPriceProp> map = new HashMap<>();

        List<InventoryCheckInEntity> checkIns =
                getCheckInsWithAvailableQtyFIFO(namespace, inventoryItemIds);

        Map<Long, List<InventoryCheckInEntity>> entityMap = new HashMap<>();

        for (InventoryCheckInEntity entity : checkIns) {
            long key = entity.inventoryItemId;
            if (!entityMap.containsKey(key))
                entityMap.put(key, new ArrayList<InventoryCheckInEntity>());

            List<InventoryCheckInEntity> list = entityMap.get(key);
            ensureNotNull(list);
            list.add(entity);
        }

        for (Long inventoryItemId : entityMap.keySet()) {
            QuantityPriceProp prop = new QuantityPriceProp();

            List<Double> prices = new ArrayList<>();
            List<Double> quantities = new ArrayList<>();

            List<InventoryCheckInEntity> list = entityMap.get(inventoryItemId);
            for (InventoryCheckInEntity entity : list) {
                prices.add(entity.pricePerDefaultUnit);
                quantities.add(entity.availableQtyInDefaultUnit);

                prop.availableQtyInDefaultUnit += entity.availableQtyInDefaultUnit;
                // TODO: handle case where currencies are different

                prop.ccy = entity.ccy;
            }

            prop.avgPricePerDefaultUnit = Utils.getWeightedAvg(prices, quantities);
            map.put(inventoryItemId, prop);
        }

        return map;
    }

    private static List<InventoryCheckInEntity> getCheckInsWithAvailableQtyFIFO(String namespace,
                                                                                Set<Long> inventoryItemIds) {

        if ((inventoryItemIds == null) || inventoryItemIds.isEmpty())
            return new ArrayList<>();

        List<InventoryCheckInEntity> list =
                ofy(namespace).load().type(InventoryCheckInEntity.class).filter("available", true)
                        .filter("inventoryItemId in", inventoryItemIds).list();

        Collections.sort(list);

        return list;
    }

    static InventoryCheckOutProp checkOut(String namespace, final long inventoryItemId, Date date,
                                          double qtyInReportingUnit, final ReportingUnit reportingUnit, Double pricePerReportingUnit,
                                          Currency ccy, String comment, Set<String> tags, String login) {

        InventoryItemEntity inventoryItemEntity = safeGet(namespace, inventoryItemId);

        ensureNotNull(reportingUnit, "reportingUnit is null");
        ensure(qtyInReportingUnit > 0, "qtyInReportingUnit [" + qtyInReportingUnit
                + "] is negative or zero");

        List<InventoryCheckInEntity> checkIns =
                getCheckInsWithAvailableQtyFIFO(namespace, Utils.getSet(inventoryItemId));

        Map<Long, Double> map = getAvailableQtyInDefaultUnit(checkIns);

        if (!map.containsKey(inventoryItemId))
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Nothing available to checkout");

        double availableQtyInReportingUnit =
                UnitUtils.safeGetQtyInReportingUnit(inventoryItemEntity.physicalQuantity,
                        map.get(inventoryItemId), reportingUnit);

        if (qtyInReportingUnit > availableQtyInReportingUnit) {
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Only [" + availableQtyInReportingUnit + "] " + reportingUnit + " available");
        }

        double qtyInDefaultUnit =
                UnitUtils.safeGetQtyInDefaultUnit(inventoryItemEntity.physicalQuantity, qtyInReportingUnit,
                        reportingUnit);

        List<InventoryCheckInEntity> toSave = new ArrayList<>();

        List<Double> prices = new ArrayList<>();
        List<Double> quantities = new ArrayList<>();

        InventoryCheckOutEntity checkOut = new InventoryCheckOutEntity();
        checkOut.checkOutId = Sequence.getNext(namespace, SequenceType.INVENTORY_CHECKOUT);
        if (date == null)
            checkOut.ms = new Date().getTime();
        else
            checkOut.ms = date.getTime();

        checkOut.qtyInDefaultUnit = qtyInDefaultUnit;
        checkOut.ccy = ccy;
        checkOut.inventoryItemId = inventoryItemId;
        checkOut.login = login;

        if ((tags != null) && !tags.isEmpty())
            checkOut.tags = tags;

        for (InventoryCheckInEntity checkIn : checkIns) {
            ensure(qtyInDefaultUnit >= 0, "qtyInDefaultUnit [" + qtyInDefaultUnit + "] is less than 0");

            if (qtyInDefaultUnit == 0) // break out of loop
                break;

            double qtyCheckedOutInDefaultUnit =
                    Math.min(qtyInDefaultUnit, checkIn.availableQtyInDefaultUnit);

            ensure(qtyCheckedOutInDefaultUnit > 0, "qtyCheckedOutInDefaultUnit ["
                    + qtyCheckedOutInDefaultUnit + "] is less than 0");

            prices.add(checkIn.pricePerDefaultUnit);
            quantities.add(qtyCheckedOutInDefaultUnit);

            checkIn.availableQtyInDefaultUnit -= qtyCheckedOutInDefaultUnit;
            ensure(checkIn.availableQtyInDefaultUnit >= 0, "checkIn.availableQtyInDefaultUnit ["
                    + checkIn.availableQtyInDefaultUnit + "] is less than 0");

            if (checkIn.availableQtyInDefaultUnit == 0)
                checkIn.available = false;

            qtyInDefaultUnit -= qtyCheckedOutInDefaultUnit;

            toSave.add(checkIn);

            CheckOutDetail checkOutDetail = new CheckOutDetail();
            checkOutDetail.checkInPricePerDefaultUnit = checkIn.pricePerDefaultUnit;
            checkOutDetail.checkInCcy = checkIn.ccy;
            checkOutDetail.checkOutCcy = ccy;
            checkOutDetail.qtyInDefaultUnit = qtyCheckedOutInDefaultUnit;

            if (pricePerReportingUnit != null)
                checkOutDetail.checkOutPricePerDefaultUnit = pricePerReportingUnit;
            else {
                checkOutDetail.checkOutPricePerDefaultUnit = checkIn.pricePerDefaultUnit;
            }

            checkOut.checkOutDetails.add(checkOutDetail);
        }

        // find weighted average of price
        if (pricePerReportingUnit == null) {
            // checkout at checkin price
            checkOut.avgPricePerDefaultUnit = Utils.getWeightedAvg(prices, quantities);
        } else {
            checkOut.avgPricePerDefaultUnit =
                    UnitUtils.safeGetPricePerDefaultUnit(inventoryItemEntity.physicalQuantity,
                            pricePerReportingUnit, reportingUnit);
        }

        ofy(namespace).save().entities(toSave);

        ofy(namespace).save().entity(checkOut);

        return checkOut.toProp(inventoryItemEntity.physicalQuantity, inventoryItemEntity.reportingUnit);
    }

    static void delete(String namespace, long inventoryItemId) {
        Client.ensureValid(namespace);

        InventoryItemEntity entity = safeGet(namespace, inventoryItemId);

        ofy(namespace).delete().entity(entity).now();
    }

    public enum CheckInOrOut {
        CHECK_IN, CHECK_OUT
    }
}
