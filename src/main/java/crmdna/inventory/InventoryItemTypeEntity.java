package crmdna.inventory;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

@Entity
@Cache
public class InventoryItemTypeEntity {
    @Id
    long inventoryItemTypeId;
    String displayName;
    @Index
    String name;

    public InventoryItemTypeProp toProp() {
        InventoryItemTypeProp prop = new InventoryItemTypeProp();
        prop.inventoryItemTypeId = inventoryItemTypeId;
        prop.displayName = displayName;

        return prop;
    }
}
