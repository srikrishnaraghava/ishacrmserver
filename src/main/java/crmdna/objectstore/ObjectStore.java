package crmdna.objectstore;

import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

public class ObjectStore {

    public static long put(String client, Object object, long expiryDuration, TimeUnit unit) {

        Client.ensureValid(client);

        long expiryDurationMS;
        if (unit == TimeUnit.MILLISECONDS)
            expiryDurationMS = expiryDuration;
        else if (unit == TimeUnit.SECONDS)
            expiryDurationMS = expiryDuration * 1000;
        else if (unit == TimeUnit.HOURS)
            expiryDurationMS = expiryDuration * 3600 * 1000;
        else if (unit == TimeUnit.DAYS)
            expiryDurationMS = expiryDuration * 86400 * 1000;
        else {
            // should never come here
            throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                    "Unsupported time unit [" + unit + "]");
        }

        return ObjectStoreCore.put(client, object, expiryDurationMS);
    }

    public static Object safeGet(String client, long objectId) {

        Client.ensureValid(client);

        return ObjectStoreCore.safeGet(client, objectId);
    }

    public static Object get(String client, long objectId) {

        Client.ensureValid(client);

        return ObjectStoreCore.get(client, objectId);
    }

    public enum TimeUnit {
        MILLISECONDS, SECONDS, HOURS, DAYS
    }
}
