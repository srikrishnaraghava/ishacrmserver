package crmdna.inventory;

import java.util.List;

public class PackagedInventoryBatchProp {

    public long batchId;
    public String batchName;
    public long createdMS;
    public String user;
    public double overheadPackingCostAtSrc;
    public double overheadLabellingCostAtSrc;
    public double overheadTransportAtSrc;
    public double overheadWarehouseCost;
    public double overheadManpowerCost;
    public double overheadShipmentCost;
    public double overheadTransportAtDst;
    public double overheadClearanceAtDst;
    public double overheadGST;
    public double overheadOther;
    public double forexUSD;
    public double forexINR;

    // dependents
    public List<PackagedInventoryItemProp> packagedInventoryItemProps;
}
