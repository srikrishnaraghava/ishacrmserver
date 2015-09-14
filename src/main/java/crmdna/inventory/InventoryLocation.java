package crmdna.inventory;

import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.sequence.Sequence;
import crmdna.user.User;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.OfyService.ofy;

public class InventoryLocation {

    public static InventoryLocationProp create(String client, String displayName, String address,
                                               String login) {

        InventoryLocationEntity entity = new InventoryLocationEntity();

        User.ensureClientLevelPrivilege(client, login,
                User.ClientLevelPrivilege.UPDATE_INVENTORY_ITEM_TYPE);

        entity.inventoryLocationId = Sequence.getNext(client, Sequence.SequenceType.INVENTORY_LOCATION);
        entity.displayName = displayName;
        entity.name = Utils.removeSpaceUnderscoreBracketAndHyphen(displayName);
        entity.address = address;

        ofy(client).save().entity(entity).now();

        return entity.toProp();
    }

    public static List<InventoryLocationProp> getAll(String client, Boolean populateItemCount,
                                                     String login) {

        Client.ensureValid(client);

        List<InventoryLocationEntity> entities =
                ofy(client).load().type(InventoryLocationEntity.class).list();

        List<InventoryLocationProp> props = new ArrayList<>();
        for (InventoryLocationEntity entity : entities) {
            InventoryLocationProp prop = entity.toProp();

            if ((populateItemCount != null) && populateItemCount) {
                List<InventoryItemProp> itemProps =
                        InventoryItem.query(client, new InventoryItemQueryCondition(), login);
                for (InventoryItemProp itemProp : itemProps) {

                    PackagedInventoryItemQueryCondition qc = new PackagedInventoryItemQueryCondition();
                    qc.locationId = prop.inventoryLocationId;
                    qc.inventoryItemIds.add(itemProp.inventoryItemId);

                    prop.inventoryItemVsCount.put(itemProp.inventoryItemId,
                            PackagedInventoryItem.queryKeys(client, qc).list().size());
                }
            }

            props.add(prop);
        }

        return props;
    }


    public static InventoryLocationEntity safeGet(String client, long inventoryLocationId) {

        Client.ensureValid(client);

        InventoryLocationEntity entity =
                ofy(client).load().type(InventoryLocationEntity.class).id(inventoryLocationId).now();

        if (null == entity)
            throw new APIException().status(APIResponse.Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Packaged Inventory item id [" + inventoryLocationId + "] does not exist");

        return entity;
    }

}
