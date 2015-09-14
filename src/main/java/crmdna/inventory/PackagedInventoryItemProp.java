package crmdna.inventory;


public class PackagedInventoryItemProp implements Comparable<PackagedInventoryItemProp> {
    public long packagedInventoryItemId;
    public long inventoryItemId;
    public String inventoryItemDisplayName;

    public long locationId;
    public String salesOrder;
    public String batch;
    public long lastUpdatedMS;

    public long expiryYYYYMMDD;

    public double costPrice;
    public double sellingPrice;

    @Override
    public int compareTo(PackagedInventoryItemProp o) {
        return packagedInventoryItemId == o.packagedInventoryItemId ? 0 : 1;
    }
}
