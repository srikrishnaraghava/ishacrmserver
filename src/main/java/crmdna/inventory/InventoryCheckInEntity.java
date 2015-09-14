package crmdna.inventory;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils.Currency;

import java.util.Date;

@Entity
@Cache
public class InventoryCheckInEntity implements Comparable<InventoryCheckInEntity> {
    @Id
    long checkInId;

    @Index
    long ms;
    @Index
    long inventoryItemId;
    double qtyInDefaultUnit;
    double pricePerDefaultUnit;
    Currency ccy;

    @Index
    boolean available;
    double availableQtyInDefaultUnit;
    String comment;

    @Index
    String login;

    public InventoryCheckInProp toProp(ReportingUnit defaultUnit) {
        InventoryCheckInProp prop = new InventoryCheckInProp();

        prop.checkInId = checkInId;
        prop.timestamp = new Date(ms);
        prop.inventoryItemId = inventoryItemId;
        prop.qtyInDefaultUnit = qtyInDefaultUnit;
        prop.pricePerDefaultUnit = pricePerDefaultUnit;
        prop.ccy = ccy;
        prop.available = available;
        prop.availableQtyInDefaultUnit = availableQtyInDefaultUnit;
        prop.defaultUnit = defaultUnit;
        prop.login = login;

        return prop;
    }

    @Override
    public int compareTo(InventoryCheckInEntity o) {
        return new Long(ms).compareTo(new Long(o.ms));
    }
}
