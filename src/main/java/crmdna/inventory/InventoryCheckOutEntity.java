package crmdna.inventory;

import com.googlecode.objectify.annotation.*;
import crmdna.common.UnitUtils;
import crmdna.common.UnitUtils.PhysicalQuantity;
import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils.Currency;

import java.util.*;

@Entity
@Cache
public class InventoryCheckOutEntity {
    @Id
    long checkOutId;

    @Index
    long ms;
    @Index
    long inventoryItemId;
    double qtyInDefaultUnit;
    double avgPricePerDefaultUnit;
    Currency ccy;

    String comment;

    @Index
    String login;

    @Index
    Set<String> tags = new HashSet<>();

    @Serialize
    List<CheckOutDetail> checkOutDetails = new ArrayList<>();

    public InventoryCheckOutProp toProp(PhysicalQuantity physicalQuantity, ReportingUnit reportingUnit) {
        InventoryCheckOutProp prop = new InventoryCheckOutProp();

        prop.checkOutId = checkOutId;
        prop.timestamp = new Date(ms);
        prop.ccy = ccy;
        prop.inventoryItemId = inventoryItemId;
        prop.avgPricePerDefaultUnit = avgPricePerDefaultUnit;
        prop.qtyInDefaultUnit = qtyInDefaultUnit;
        prop.checkOutDetails = checkOutDetails;
        prop.login = login;
        prop.tags = tags;

        prop.qtyInReportingUnit =
                UnitUtils.safeGetQtyInReportingUnit(physicalQuantity, prop.qtyInDefaultUnit, reportingUnit);
        prop.avgPricePerReportingUnit =
                UnitUtils.safeGetPricePerReportingUnit(physicalQuantity, avgPricePerDefaultUnit,
                        reportingUnit);

        return prop;
    }
}
