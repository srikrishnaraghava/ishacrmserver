package crmdna.inventory;

import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.sequence.Sequence;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.OfyService.ofy;

public class PackagedInventoryBatch {

    public static PackagedInventoryBatchProp create(String client, String batchName,
        Double overheadPackingCostAtSrc, Double overheadLabellingCostAtSrc,
        Double overheadTransportAtSrc, Double overheadWarehouseCost, Double overheadManpowerCost,
        Double overheadShipmentCost, Double overheadTransportAtDst, Double overheadClearanceAtDst,
        Double overheadGST, Double overheadOther, Double forexUSD, Double forexINR, String login) {

        PackagedInventoryBatchEntity entity = new PackagedInventoryBatchEntity();
        entity.batchId = Sequence.getNext(client, Sequence.SequenceType.PACKAGED_INVENTORY_BATCH);
        entity.batchName = batchName;
        entity.createdMS = System.currentTimeMillis();
        entity.user = login;
        entity.overheadPackingCostAtSrc = overheadPackingCostAtSrc;
        entity.overheadLabellingCostAtSrc = overheadLabellingCostAtSrc;
        entity.overheadTransportAtSrc = overheadTransportAtSrc;
        entity.overheadWarehouseCost = overheadWarehouseCost;
        entity.overheadManpowerCost = overheadManpowerCost;
        entity.overheadShipmentCost = overheadShipmentCost;
        entity.overheadTransportAtDst = overheadTransportAtDst;
        entity.overheadClearanceAtDst = overheadClearanceAtDst;
        entity.overheadGST = overheadGST;
        entity.overheadOther = overheadOther;
        entity.forexUSD = forexUSD;
        entity.forexINR = forexINR;

        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static PackagedInventoryBatchEntity safeGet(String client, long batchId) {

        Client.ensureValid(client);

        PackagedInventoryBatchEntity entity =
                ofy(client).load().type(PackagedInventoryBatchEntity.class).id(batchId).now();

        if (null == entity)
            throw new APIException().status(APIResponse.Status.ERROR_RESOURCE_NOT_FOUND)
                    .message("Batch id [" + batchId + "] does not exist");

        return entity;
    }

    public static List<PackagedInventoryBatchProp> getAll(String client) {

        Client.ensureValid(client);

        List<PackagedInventoryBatchEntity> entities =
                ofy(client).load().type(PackagedInventoryBatchEntity.class).order("-createdMS").list();

        List<PackagedInventoryBatchProp> props = new ArrayList<>();
        for (PackagedInventoryBatchEntity entity : entities) {
            PackagedInventoryBatchProp prop = entity.toProp();
            props.add(prop);
        }

        return props;
    }

}
