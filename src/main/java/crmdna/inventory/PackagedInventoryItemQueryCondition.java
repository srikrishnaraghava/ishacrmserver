package crmdna.inventory;

import java.util.HashSet;
import java.util.Set;

public class PackagedInventoryItemQueryCondition {
    public Set<Long> inventoryItemIds = new HashSet<>();
    public Integer expiryYYYYMMDD;
    public Long locationId;
    public Long salesId;
    public Long batchId;

}
