package crmdna.inventory;

import java.util.HashSet;
import java.util.Set;

import static crmdna.common.AssertUtils.ensure;

public class StockChangeQueryCondition {

    public boolean includeCheckIn;
    public boolean includeCheckOut;
    public Set<Long> inventoryItemTypeIds = new HashSet<>();
    public Set<Long> inventoryItemIds = new HashSet<>();
    public Set<String> tags = new HashSet<>();
    public Set<String> logins = new HashSet<>();
    long groupId;
    long startMS;
    long endMS;

    public StockChangeQueryCondition(long groupId, long startMS, long endMS) {
        ensure(groupId > 0, "Invalid group id [" + groupId + "]");
        ensure(endMS >= startMS, "endMS [" + endMS + "] should be >= startMS [" + startMS + "]");

        this.groupId = groupId;
        this.startMS = startMS;
        this.endMS = endMS;
    }
}
