package crmdna.client;

import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.OfyService.ofy;

public class CustomFields {
    private final static String KEY = "KEY";

    static void create(String client, String fieldName) {

        Client.ensureValid(client);

        CustomFieldsEntity entity = getCustomFieldsEntity(client);

        if (entity == null) {
            entity = new CustomFieldsEntity();
            entity.key = KEY;
            entity.fieldNames.add(fieldName);
            ofy(client).save().entity(entity).now();
            return;
        }

        // throw error if already present
        if (true == Utils.isPresentInListCaseInsensitive(entity.fieldNames, fieldName))
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "Custom field [" + fieldName + " already exists");

        // add the new custom field
        entity.fieldNames.add(fieldName);
        ofy(client).save().entity(entity).now();
    }

    static List<CustomFieldProp> getAll(String client) {
        Client.ensureValid(client);

        List<CustomFieldProp> list = new ArrayList<CustomFieldProp>();

        CustomFieldsEntity entity = getCustomFieldsEntity(client);

        if (null == entity)
            return list;

        for (int i = 0; i < entity.fieldNames.size(); i++) {
            CustomFieldProp customFieldProp = new CustomFieldProp();

            customFieldProp.id = i;
            customFieldProp.name = entity.fieldNames.get(i);
            customFieldProp.enabled = true;
            if (entity.disabledFieldIds.contains(i))
                customFieldProp.enabled = false;

            list.add(customFieldProp);
        }

        return list;
    }

    static void update(String client, int fieldId, String newFieldName, Boolean enabled) {

        Client.ensureValid(client);

        CustomFieldsEntity entity = getCustomFieldsEntity(client);

        if (entity == null) {
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "No custom field found");
        }

        if (fieldId > entity.fieldNames.size() - 1)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "There is no custom field with id [" + fieldId + "]");

        if (!entity.fieldNames.get(fieldId).equalsIgnoreCase(newFieldName)) {
            // not a rename, check if new name does not exist
            if (true == Utils.isPresentInListCaseInsensitive(entity.fieldNames, newFieldName))
                throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                        "Field name [" + newFieldName + "] is already present");
        }

        // null will preserve existing value
        if (null != newFieldName)
            entity.fieldNames.set(fieldId, newFieldName);

        if (null != enabled) {
            if (enabled)
                entity.disabledFieldIds.remove(fieldId);
            else
                entity.disabledFieldIds.add(fieldId);
        }

        ofy(client).save().entity(entity).now();
    }

    private static CustomFieldsEntity getCustomFieldsEntity(String client) {
        CustomFieldsEntity entity = ofy(client).load().type(CustomFieldsEntity.class).id(KEY).now();

        return entity;
    }

    public static class CustomFieldProp {
        int id;
        String name;
        boolean enabled;
    }
}
