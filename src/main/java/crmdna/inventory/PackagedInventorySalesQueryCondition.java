package crmdna.inventory;

import java.util.HashSet;
import java.util.Set;

public class PackagedInventorySalesQueryCondition {
    public Set<Long> salesIds = new HashSet<>();
    public Long startMS;
    public Long endMS;
}
