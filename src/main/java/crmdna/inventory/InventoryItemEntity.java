package crmdna.inventory;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.common.UnitUtils.PhysicalQuantity;
import crmdna.common.UnitUtils.ReportingUnit;

@Entity
@Cache
public class InventoryItemEntity {
    @Id
    long inventoryItemId;

    @Index
    long groupId;
    @Index
    long inventoryItemTypeId;

    String displayName;
    PhysicalQuantity physicalQuantity;
    ReportingUnit reportingUnit;

    // dependents
    @Index
    String name;
    @Index
    String firstChar; // first char of name

    public InventoryItemProp toProp() {
        InventoryItemProp prop = new InventoryItemProp();

        prop.displayName = displayName;
        prop.groupId = groupId;
        prop.inventoryItemId = inventoryItemId;
        prop.inventoryItemTypeId = inventoryItemTypeId;
        prop.name = name;
        prop.physicalQuantity = physicalQuantity;
        prop.reportingUnit = reportingUnit;

        return prop;
    }
}
