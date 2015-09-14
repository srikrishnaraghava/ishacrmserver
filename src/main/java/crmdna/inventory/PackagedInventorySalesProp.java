package crmdna.inventory;

import java.util.List;

public class PackagedInventorySalesProp {
    public long salesId;
    public String salesOrder;
    public boolean paidOnline;
    public long salesMS;
    public String user;

    // dependents
    public List<PackagedInventoryItemProp> packagedInventoryItemProps;
}
