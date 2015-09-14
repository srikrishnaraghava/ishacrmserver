package crmdna.inventory;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;

@Entity
@Cache
public class PackagedInventorySalesEntity {
    @Id
    long salesId;

    @Index
    String salesOrder;

    @Index
    boolean paidOnline;

    @Index
    long salesMS;

    @Index
    String user;

    public PackagedInventorySalesProp toProp() {
        PackagedInventorySalesProp prop = new PackagedInventorySalesProp();

        prop.salesOrder = salesOrder;
        prop.salesId = salesId;
        prop.paidOnline = paidOnline;
        prop.salesMS = salesMS;
        prop.user = user;

        prop.packagedInventoryItemProps = new ArrayList<>();

        return prop;
    }
}
