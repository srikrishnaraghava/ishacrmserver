package crmdna.common;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.user.User.ResourceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static crmdna.common.TestUtil.ensureAPIException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemcacheLockTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private String client1 = "client1";
    private String client2 = "client2";
    private String invalidClient = "invalidClient";

    // local implementation / test harness implementation becomes HRD
    // only if setApplyAllHighRepJobPolicy is set. If the implementation is not HRD then
    // cross group transactions would fail (as master slave does not support it)


    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client1);
        Client.create(client2);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @SuppressWarnings("resource")
    @Test
    public void test() throws Exception {
        try (MemcacheLock lock =
                     new MemcacheLock(client1, ResourceType.MEMBER, "sathya.t@ishafoundation.org")) {

            // cannot acquire the same lock again
            ensureAPIException(Status.ERROR_RESOURCE_ALREADY_EXISTS, new ICode() {

                @Override
                public void run() {
                    new MemcacheLock(client1, ResourceType.MEMBER, "sathya.t@ishafoundation.org");
                }
            });

            // just try one more time
            // cannot acquire the same lock again
            ensureAPIException(Status.ERROR_RESOURCE_ALREADY_EXISTS, new ICode() {

                @Override
                public void run() {
                    new MemcacheLock(client1, ResourceType.MEMBER, "sathya.t@ishafoundation.org");
                }
            });

            // can acquire lock for same resource for a different client
            try (MemcacheLock lock2 =
                         new MemcacheLock(client2, ResourceType.MEMBER, "sathya.t@ishafoundation.org")) {
            }

            // can acquire lock for different resource
            try (MemcacheLock lock2 =
                         new MemcacheLock(client1, ResourceType.MEMBER, "sathya.isha@gmail.com")) {
            }
        }

        // can acquire lock again
        try (MemcacheLock lock =
                     new MemcacheLock(client1, ResourceType.MEMBER, "sathya.t@ishafoundation.org")) {
        }

        // can acquire repeated locks when uniqueKey is null
        try (MemcacheLock lock = new MemcacheLock(client1, ResourceType.MEMBER, null)) {

            try (MemcacheLock lock2 = new MemcacheLock(client1, ResourceType.MEMBER, null)) {

                try (MemcacheLock lock3 = new MemcacheLock(client1, ResourceType.MEMBER, null)) {
                }
            }

            new MemcacheLock(client1, ResourceType.MEMBER, null);
            new MemcacheLock(client1, ResourceType.MEMBER, null);
            new MemcacheLock(client1, ResourceType.MEMBER, null);
        }

        // client should be valid
        try (MemcacheLock lock4 = new MemcacheLock(invalidClient, ResourceType.MEMBER, null)) {
            assertTrue(false);
        } catch (APIException e) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, e.statusCode);
        }
    }
}
