package crmdna.inventory;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.HashMap;

@Entity
@Cache
public class InventoryLocationEntity {
    @Id
    long inventoryLocationId;

    String displayName;
    @Index
    String name;

    String address;

    public InventoryLocationProp toProp() {
        InventoryLocationProp prop = new InventoryLocationProp();

        prop.displayName = displayName;
        prop.inventoryLocationId = inventoryLocationId;
        prop.address = address;

        prop.inventoryItemVsCount = new HashMap<>();

        return prop;
    }
}
