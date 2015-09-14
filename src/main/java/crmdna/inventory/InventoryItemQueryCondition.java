package crmdna.inventory;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class InventoryItemQueryCondition {
    public Long groupId;
    public Set<Long> inventoryItemTypeIds = new HashSet<>();
    public List<String> firstChars = new ArrayList<>();
}
