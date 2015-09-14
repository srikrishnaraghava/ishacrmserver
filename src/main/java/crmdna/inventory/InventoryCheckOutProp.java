package crmdna.inventory;

import crmdna.common.UnitUtils;
import crmdna.common.Utils.Currency;

import java.util.*;

import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class InventoryCheckOutProp {
    public long checkOutId;
    public Date timestamp;
    public long inventoryItemId;
    public double qtyInDefaultUnit;
    public double avgPricePerDefaultUnit;

    public Currency ccy;

    public List<CheckOutDetail> checkOutDetails;

    public String login;

    public Set<String> tags;

    // dependents
    public double qtyInReportingUnit;
    public double avgPricePerReportingUnit;

    // consider removing this
    public static void populateDependents_not_used(String client, List<InventoryCheckOutProp> props) {

        ensureNotNull(props, "props is null");

        List<Long> inventoryItemIds = new ArrayList<>();
        for (InventoryCheckOutProp prop : props) {
            inventoryItemIds.add(prop.inventoryItemId);
        }

        Map<Long, InventoryItemEntity> map =
                ofy(client).load().type(InventoryItemEntity.class).ids(inventoryItemIds);

        for (InventoryCheckOutProp prop : props) {
            if (!map.containsKey(prop.inventoryItemId)) {
                // should never happen
                continue;
            }

            InventoryItemEntity inventoryItemEntity = map.get(prop.inventoryItemId);

            prop.qtyInReportingUnit =
                    UnitUtils.safeGetQtyInReportingUnit(inventoryItemEntity.physicalQuantity,
                            prop.qtyInDefaultUnit, inventoryItemEntity.reportingUnit);
            prop.avgPricePerReportingUnit =
                    UnitUtils.safeGetPricePerReportingUnit(inventoryItemEntity.physicalQuantity,
                            prop.avgPricePerDefaultUnit, inventoryItemEntity.reportingUnit);
        }
    }
}
