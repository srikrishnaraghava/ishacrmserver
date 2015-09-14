package crmdna.inventory;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.Set;

@Entity
@Cache
public class InventoryTransferEntity {
    @Id
    long inventoryTransferId;

    @Index
    Set<Long> packagedInventoryItemIds;

    @Index
    long transferMS;

    @Index
    long fromLocationId;

    @Index
    long toLocationId;

    public InventoryTransferProp toProp() {
        InventoryTransferProp prop = new InventoryTransferProp();

        prop.inventoryTransferId = inventoryTransferId;
        prop.packagedInventoryItemIds = packagedInventoryItemIds;
        prop.transferMS = transferMS;
        prop.fromLocationId = fromLocationId;
        prop.toLocationId = toLocationId;

        return prop;
    }
}
