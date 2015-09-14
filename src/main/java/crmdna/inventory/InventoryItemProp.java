package crmdna.inventory;

import crmdna.common.UnitUtils;
import crmdna.common.UnitUtils.PhysicalQuantity;
import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils.Currency;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static crmdna.common.AssertUtils.ensureNoNullElement;

public class InventoryItemProp implements Comparable<InventoryItemProp> {
    public long inventoryItemId;

    public long groupId;
    public long inventoryItemTypeId;

    public String displayName;
    public String name;
    public PhysicalQuantity physicalQuantity;
    public ReportingUnit reportingUnit;

    // dependents
    public String inventoryItemType;
    public double availableQtyInDefaultUnit;
    public double availableQtyInReportingUnit;
    public double avgPricePerReportingUnit;
    public Currency ccy;
    public List<ReportingUnit> reportingUnits;

    public static String toCSV(Iterable<InventoryItemProp> props) {
        ensureNoNullElement(props);

        StringBuilder builder = new StringBuilder();
        builder.append("Item,Item Type,Available Quantity,Unit,Avg Price per Unit,Currency,Item ID\n");

        for (InventoryItemProp prop : props) {
            builder.append(prop.displayName + "," + prop.inventoryItemType + ","
                    + prop.availableQtyInReportingUnit + "," + prop.reportingUnit + ","
                    + prop.avgPricePerReportingUnit + "," + (prop.ccy != null ? prop.ccy : "") + ","
                    + prop.inventoryItemId + "\n");
        }

        return builder.toString();
    }

    public static void populateDependents(String client, Iterable<InventoryItemProp> props) {

        Set<Long> inventoryItemIds = new HashSet<>();
        Set<Long> inventoryItemTypeIds = new HashSet<>();
        for (InventoryItemProp prop : props) {
            inventoryItemTypeIds.add(prop.inventoryItemTypeId);
            inventoryItemIds.add(prop.inventoryItemId);
        }

        Map<Long, InventoryItemTypeEntity> inventoryItemTypes =
                InventoryItemType.get(client, inventoryItemTypeIds);

        Map<Long, QuantityPriceProp> map =
                InventoryItemCore.getAvailableQtyAndAvgPrice(client, inventoryItemIds);
        // TODO: handle error cases. Error could happen if currency conversion
        // fails

        for (InventoryItemProp prop : props) {
            if (inventoryItemTypes.containsKey(prop.inventoryItemTypeId)) {
                prop.inventoryItemType = inventoryItemTypes.get(prop.inventoryItemTypeId).displayName;

                if (map.containsKey(prop.inventoryItemId)) {
                    QuantityPriceProp quantityPriceProp = map.get(prop.inventoryItemId);

                    prop.availableQtyInDefaultUnit = quantityPriceProp.availableQtyInDefaultUnit;
                    prop.availableQtyInReportingUnit =
                            UnitUtils.safeGetQtyInReportingUnit(prop.physicalQuantity,
                                    prop.availableQtyInDefaultUnit, prop.reportingUnit);

                    prop.avgPricePerReportingUnit =
                            UnitUtils.safeGetPricePerReportingUnit(prop.physicalQuantity,
                                    quantityPriceProp.avgPricePerDefaultUnit, prop.reportingUnit);

                    prop.ccy = quantityPriceProp.ccy;
                }

                prop.reportingUnits = UnitUtils.getReportingUnitsForPhysicalQuantity(prop.physicalQuantity);
            }
        }
    }

    @Override
    public int compareTo(InventoryItemProp o) {
        if ((inventoryItemType == null) || (o.inventoryItemType == null))
            return 0;

        if (inventoryItemType.equals(o.inventoryItemType))
            return displayName.toLowerCase().compareTo(o.displayName.toLowerCase());

        return inventoryItemType.compareTo(o.inventoryItemType);
    }
}
