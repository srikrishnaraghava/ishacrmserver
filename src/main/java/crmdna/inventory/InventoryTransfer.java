package crmdna.inventory;

import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;

import static crmdna.common.OfyService.ofy;

public class InventoryTransfer {

    public static InventoryTransferEntity safeGet(String client, long inventoryTransferId) {

        Client.ensureValid(client);

        InventoryTransferEntity entity =
                ofy(client).load().type(InventoryTransferEntity.class).id(inventoryTransferId).now();

        if (null == entity)
            throw new APIException().status(APIResponse.Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Packaged Inventory item id [" + inventoryTransferId + "] does not exist");

        return entity;
    }
}
