package crmdna.inventory;

import crmdna.client.Client;
import crmdna.common.UnitUtils.ReportingUnit;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.hr.Department;
import crmdna.hr.DepartmentEntity;
import crmdna.inventory.InventoryItemCore.CheckInOrOut;
import crmdna.user.User.ResourceType;

import java.io.Serializable;
import java.util.*;

import static crmdna.common.AssertUtils.ensureNoNullElement;
import static crmdna.common.AssertUtils.ensureNotNull;

public class StockChangeProp implements Comparable<StockChangeProp>, Serializable {

    private static final long serialVersionUID = 1L;

    public Date timestamp;
    public long inventoryItemId;
    public String login;
    public Set<String> tags = new HashSet<>();
    public String comment;

    public CheckInOrOut checkInOrOut;
    public long checkInOrOutId;

    public String inventoryItem;
    public ReportingUnit reportingUnit;
    public double changeInReportingUnit;

    public double cost;
    public Currency ccy;

    public long departmentId;
    public String department;

    public static String toCSV(List<StockChangeProp> props) {
        ensureNoNullElement(props);

        StringBuilder builder = new StringBuilder();

        builder
                .append("Timestamp,Login,Item,Check In/Out,Change,Unit,Cost,Ccy,Department,Item ID,Check In/Out ID,Comment\n");

        for (StockChangeProp prop : props) {

            builder.append(prop.timestamp).append(",");
            builder.append(prop.login).append(",");
            builder.append(prop.inventoryItem).append(",");
            builder.append(prop.checkInOrOut).append(",");
            builder.append(prop.changeInReportingUnit).append(",");
            builder.append(prop.reportingUnit).append(",");
            builder.append(prop.cost).append(",");
            builder.append(prop.ccy).append(",");
            builder.append(prop.department != null ? prop.department : "").append(",");
            builder.append(prop.inventoryItemId).append(",");
            builder.append(prop.checkInOrOutId).append(",");
            builder.append(prop.comment != null ? prop.comment : "").append(",");
            builder.append("\n");
        }

        return builder.toString();
    }

    public static void populateDepartment(String client, List<StockChangeProp> stockChangeProps) {
        Client.ensureValid(client);

        ensureNotNull(stockChangeProps, "stockChangeProps is null");

        Set<Long> departmentIds = new HashSet<>();
        for (StockChangeProp stockChangeProp : stockChangeProps) {
            if (stockChangeProp.tags == null)
                continue;

            for (String tag : stockChangeProp.tags) {
                if (tag.contains(ResourceType.DEPARTMENT.toString())) {
                    String[] strings = tag.split("||");

                    if (strings.length >= 2) {
                        if (Utils.canParseAsLong(strings[1])) {
                            stockChangeProp.departmentId = Utils.safeParseAsInt(strings[1]);
                            departmentIds.add(stockChangeProp.departmentId);
                        }
                    }
                }
            }
        }

        Map<Long, DepartmentEntity> map = Department.get(client, departmentIds);

        for (StockChangeProp stockChangeProp : stockChangeProps) {
            if (stockChangeProp.departmentId == 0)
                continue;

            if (!map.containsKey(stockChangeProp.departmentId)) {
                stockChangeProp.department = "NA";
                continue;
            }

            stockChangeProp.department = map.get(stockChangeProp.departmentId).toProp().displayName;
        }
    }

    @Override
    public int compareTo(StockChangeProp o) {
        // default to be sorted in descending order
        return o.timestamp.compareTo(timestamp);
    }
}
