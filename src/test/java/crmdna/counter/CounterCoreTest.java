package crmdna.counter;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class CounterCoreTest {

    private final LocalServiceTestHelper datastoreHelper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()
                    .setApplyAllHighRepJobPolicy());


    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void incrementTest() {
        CounterCore c1 = new CounterCore(1);

        String ns1 = "ns1";
        String counterName1 = "checkin_5_3";

        assertEquals(0, c1.getCount(ns1, counterName1));

        c1.increment(ns1, counterName1, 5);
        c1.increment(ns1, counterName1, -3);

        ObjectifyFilter.complete();
        assertEquals(2, c1.getCount(ns1, counterName1));

        String ns2 = "ns2";
        c1.increment(ns2, counterName1, 5);
        c1.increment(ns2, counterName1, -2);
        assertEquals(3, c1.getCount(ns2, counterName1));

        //counter 5
        CounterCore c5 = new CounterCore(5);

        String counterName2 = "checkin_5_4";
        c5.increment(ns1, counterName2, 6);
        c5.increment(ns1, counterName2, -1);
        ObjectifyFilter.complete();
        assertEquals(5, c5.getCount(ns1, counterName2));

        //can increase shards
        c5.increment(ns1, counterName1, 10);
        ObjectifyFilter.complete();
        assertEquals(12, c5.getCount(ns1, counterName1));
    }

    @Test
    public void getCountTest() {
        //same as increment test
    }
}
