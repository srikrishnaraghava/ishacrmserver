package crmdna.inventory;

import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils.Currency;

import java.util.Date;

public class InventoryCheckInProp {
    public long checkInId;
    public Date timestamp;
    public long inventoryItemId;
    public double qtyInDefaultUnit;
    public double pricePerDefaultUnit;
    public ReportingUnit defaultUnit;
    public Currency ccy;
    public String login;

    public boolean available;
    public double availableQtyInDefaultUnit;
}
