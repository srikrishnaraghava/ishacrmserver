package crmdna.counter;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.counter.Counter.CounterType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class CounterTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    String client;
    String invalidClient = "invalid";

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        client = "isha";
        Client.create(client);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void incrementTest() {

        String key = "5_3";
        assertEquals(0, Counter.getCount(client, CounterType.CHECKIN, key));

        Counter.increment(client, CounterType.CHECKIN, key, 5);
        Counter.increment(client, CounterType.CHECKIN, key, -4);
        ObjectifyFilter.complete();
        assertEquals(1, Counter.getCount(client, CounterType.CHECKIN, key));

        try {
            Counter.increment(invalidClient, CounterType.CHECKIN, key, 5);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void incrementAndGetCurrentCountTest() {

        String key = "5_3";
        long count = Counter.incrementAndGetCurrentCount(client, CounterType.CHECKIN, key, 5);
        assertEquals(5, count);
        count = Counter.incrementAndGetCurrentCount(client, CounterType.CHECKIN, key, 6);
        assertEquals(11, count);
    }

    @Test
    public void getCountTest() {
        // same as increment test
    }
}
