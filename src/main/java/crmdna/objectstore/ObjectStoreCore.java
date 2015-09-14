package crmdna.objectstore;

import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import java.util.Date;

import static crmdna.common.AssertUtils.ensure;
import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class ObjectStoreCore {
    static long put(String client, Object object, long expiryDurationMS) {
        ensureNotNull(client, "client is null");
        ensure(client.length() != 0, "client is empty");

        ensureNotNull(object, "object is null");

        ensure(expiryDurationMS > 0, "expiryDurationMS [" + expiryDurationMS + "]");

        ObjectEntity entity = new ObjectEntity();
        entity.object = object;
        entity.expiryMS = new Date().getTime() + expiryDurationMS;

        ofy(client).save().entity(entity).now();
        return entity.objectId;
    }

    static Object safeGet(String client, long objectId) {

        ensureNotNull(client, "client is null");
        ensure(client.length() != 0, "client is empty");

        Object entity = get(client, objectId);

        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Object [" + objectId + "] does not exist");

        return entity;
    }

    static Object get(String client, long objectId) {

        ObjectEntity entity = getEntity(client, objectId);

        if (entity == null)
            return null;

        if (entity.expiryMS < new Date().getTime())
            return null; // object has expired

        return entity.object;
    }

    private static ObjectEntity getEntity(String client, long objectId) {
        ensureNotNull(client, "client is null");
        ensure(client.length() != 0, "client is empty");

        ObjectEntity entity = ofy(client).load().type(ObjectEntity.class).id(objectId).now();

        return entity;
    }

    static void deleteExpiredObjects(String client) {
        // TODO
    }
}
