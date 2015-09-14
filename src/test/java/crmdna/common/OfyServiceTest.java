package crmdna.common;

import com.google.appengine.api.NamespaceManager;
import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static crmdna.common.OfyService.ofy;
import static crmdna.common.OfyService.ofyCrmDna;
import static org.junit.Assert.assertEquals;

public class OfyServiceTest {

    private final LocalServiceTestHelper datastoreHelper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()
                    .setApplyAllHighRepJobPolicy());
    //local implementation / test harness implementation becomes HRD
    //only if setApplyAllHighRepJobPolicy is set. If the implementation is not HRD then
    //cross group transactions would fail (as master slave does not support it)


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
    public void ofyTest() {

        ofy("testclient");
        assertEquals("namespace set correctly", "testclient", NamespaceManager.get());

        ofyCrmDna();
        assertEquals("namespace set correctly to crmdna", "CRMDNA", NamespaceManager.get());
    }
}