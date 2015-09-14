package crmdna.inventory;

import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.group.Group;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

// TODO: to be removed
class InventoryItemSaver {
    private String client;
    private InventoryItemEntity entity;

    private InventoryItemSaver() {
    }

    static InventoryItemSaver inventoryItemSaver(String client, InventoryItemEntity entity) {

        Client.ensureValid(client);
        ensureNotNull(entity);

        InventoryItemSaver saver = new InventoryItemSaver();
        saver.client = client;
        saver.entity = entity;

        return saver;
    }

    void populateDependentsAndSave() {
        Client.ensureValid(client);
        ensureNotNull(entity);

        ensureNotNull(entity.displayName, "Display name cannot be null");
        ensure(entity.displayName.length() > 0, "Display name cannot be empty");

        // first character should be an alphabet
        char c = entity.displayName.toLowerCase().charAt(0);
        ensure(c >= 'a' && c <= 'z', "First character should be an alphabet");

        Group.safeGet(client, entity.groupId);

        InventoryItemType.safeGet(client, entity.inventoryItemTypeId);

        // populate dependents
        entity.name = entity.displayName.toLowerCase();
        entity.name = Utils.removeSpaceUnderscoreBracketAndHyphen(entity.name);
        entity.firstChar = entity.name.substring(0, 1);

        ofy(client).save().entity(entity);
    }

    void save() {
        Client.ensureValid(client);
        ensureNotNull(entity);

        ofy(client).save().entity(entity);
    }
}
