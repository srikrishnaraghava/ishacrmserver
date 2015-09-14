package crmdna.inventory;


public class InventoryItemTypeProp implements Comparable<InventoryItemTypeProp> {
    public long inventoryItemTypeId;
    public String displayName;

    @Override
    public int compareTo(InventoryItemTypeProp o) {
        return displayName.compareTo(o.displayName);
    }
}