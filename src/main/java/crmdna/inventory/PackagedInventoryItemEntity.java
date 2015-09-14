package crmdna.inventory;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.common.Utils;

@Entity
@Cache
public class PackagedInventoryItemEntity {
    @Id
    long packagedInventoryItemId;
    @Index
    long inventoryItemId;

    @Index
    long locationId;

    @Index
    int expiryYYYYMMDD;

    double costPrice;
    double sellingPrice;
    Utils.Currency currency;

    @Index
    long salesId;

    @Index
    long batchId;

    long lastUpdatedMS;

    public PackagedInventoryItemProp toProp(String client) {
        PackagedInventoryItemProp prop = new PackagedInventoryItemProp();

        prop.packagedInventoryItemId = packagedInventoryItemId;
        prop.inventoryItemId = inventoryItemId;
        prop.locationId = locationId;
        prop.expiryYYYYMMDD = expiryYYYYMMDD;
        prop.costPrice = costPrice;
        prop.sellingPrice = sellingPrice;

        prop.lastUpdatedMS = lastUpdatedMS;

        if (salesId > 0) {
            prop.salesOrder = PackagedInventorySales.safeGet(client, salesId).salesOrder;
        } else {
            prop.salesOrder = "";
        }

        prop.batch = PackagedInventoryBatch.safeGet(client, batchId).batchName;

        return prop;
    }
}
