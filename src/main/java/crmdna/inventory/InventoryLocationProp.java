package crmdna.inventory;


import java.util.Map;

public class InventoryLocationProp implements Comparable<InventoryLocationProp> {
    public long inventoryLocationId;
    public String displayName;
    public String address;

    //dependents
    public Map<Long, Integer> inventoryItemVsCount;

    @Override
    public int compareTo(InventoryLocationProp o) {
        return displayName.compareTo(o.displayName);
    }
}