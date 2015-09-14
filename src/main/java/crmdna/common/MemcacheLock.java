package crmdna.common;

import com.google.appengine.api.memcache.MemcacheService;
import com.google.appengine.api.memcache.MemcacheServiceFactory;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.user.User.ResourceType;

import static crmdna.common.AssertUtils.ensureNotNull;

public class MemcacheLock implements AutoCloseable {

    private String key = null;
    private String client;

    public MemcacheLock(String client, ResourceType resourceType, String uniqueKey) {
        Client.ensureValid(client);
        this.client = client;
        ensureNotNull(resourceType, "resourceType is null");

        if (uniqueKey == null)
            return;

        MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService(client);

        key = resourceType + "_" + uniqueKey;

        long val = memcacheService.increment(key, (long) 1, (long) 0);
        // note: call to increment is atomic

        if (val != 1)
            throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                    "Unique constraint violated (when acquiring memcache lock) for key [" + key
                            + "], client [" + client + "]");
    }

    @Override
    public void close() {
        if (key != null) {
            MemcacheService memcacheService = MemcacheServiceFactory.getMemcacheService(client);
            memcacheService.delete(key);
        }
    }
}
