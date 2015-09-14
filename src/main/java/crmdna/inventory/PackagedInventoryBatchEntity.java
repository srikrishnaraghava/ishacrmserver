package crmdna.inventory;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;

import java.util.ArrayList;

@Entity
@Cache
public class PackagedInventoryBatchEntity {
    @Id
    long batchId;

    @Index
    String batchName;

    @Index
    long createdMS;

    @Index
    String user;

    double overheadPackingCostAtSrc;
    double overheadLabellingCostAtSrc;
    double overheadTransportAtSrc;
    double overheadWarehouseCost;
    double overheadManpowerCost;
    double overheadShipmentCost;
    double overheadTransportAtDst;
    double overheadClearanceAtDst;
    double overheadGST;
    double overheadOther;
    double forexUSD;
    double forexINR;

    public PackagedInventoryBatchProp toProp() {
        PackagedInventoryBatchProp prop = new PackagedInventoryBatchProp();

        prop.batchId = batchId;
        prop.batchName = batchName;
        prop.createdMS = createdMS;
        prop.user = user;
        prop.overheadPackingCostAtSrc = overheadPackingCostAtSrc;
        prop.overheadLabellingCostAtSrc = overheadLabellingCostAtSrc;
        prop.overheadTransportAtSrc = overheadTransportAtSrc;
        prop.overheadWarehouseCost = overheadWarehouseCost;
        prop.overheadManpowerCost = overheadManpowerCost;
        prop.overheadShipmentCost = overheadShipmentCost;
        prop.overheadTransportAtDst = overheadTransportAtDst;
        prop.overheadClearanceAtDst = overheadClearanceAtDst;
        prop.overheadGST = overheadGST;
        prop.overheadOther = overheadOther;
        prop.forexUSD = forexUSD;
        prop.forexINR = forexINR;

        prop.packagedInventoryItemProps = new ArrayList<>();

        return prop;
    }
}
