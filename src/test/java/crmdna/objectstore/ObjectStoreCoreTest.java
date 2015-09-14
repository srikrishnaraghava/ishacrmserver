package crmdna.objectstore;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.ICode;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.Serializable;

import static crmdna.common.TestUtil.ensureResourceNotFoundException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ObjectStoreCoreTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void putGetAndSafeGetTest() throws InterruptedException {
        long objectId = ObjectStoreCore.put(client, new Long(100), 100);
        assertTrue(objectId != 0);

        long l = (long) ObjectStoreCore.get(client, objectId);
        assertEquals(100, l);

        objectId = ObjectStoreCore.put(client, "sathya", 100);
        assertTrue(objectId != 0);
        String s = (String) ObjectStoreCore.get(client, objectId);
        assertEquals("sathya", s);

        //sleep for 50ms
        Thread.sleep(50);
        assertEquals("sathya", ObjectStoreCore.get(client, objectId));

        //sleep for another 150ms. object should get expired
        Thread.sleep(150);
        assertEquals(null, ObjectStoreCore.get(client, objectId));

        //safeget should throw exception
        final long id = objectId;
        ensureResourceNotFoundException(new ICode() {

            @Override
            public void run() {
                ObjectStoreCore.safeGet(client, id);
            }
        });

        //put an object
        Prop prop = new Prop();
        prop.id = 100;
        prop.name = "Singapore";
        objectId = ObjectStoreCore.put(client, prop, 100);
        prop = (Prop) ObjectStoreCore.get(client, objectId);
        assertTrue(prop != null);
        assertEquals(100, prop.id);
        assertEquals("Singapore", prop.name);
    }

    public static class Prop implements Serializable {
        private static final long serialVersionUID = 1L;

        public long id;
        public String name;
    }
}
