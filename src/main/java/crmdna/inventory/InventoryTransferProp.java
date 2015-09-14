package crmdna.inventory;

import java.util.Set;

public class InventoryTransferProp implements Comparable<InventoryTransferProp> {
    public long inventoryTransferId;
    public Set<Long> packagedInventoryItemIds;
    public long transferMS;
    public long fromLocationId;
    public long toLocationId;

    @Override
    public int compareTo(InventoryTransferProp o) {
        return (inventoryTransferId == o.inventoryTransferId ? 0 : 1);
    }
}