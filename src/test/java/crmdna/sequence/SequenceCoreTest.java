package crmdna.sequence;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SequenceCoreTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

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
    public void getNextTest() {
        // client cannot be invalid
        try {
            SequenceCore.getNext("isha", "seq1", 5);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        Client.create("Isha");

        // error if incorrect input
        try {
            SequenceCore.getNext("isha", "seq1", 0);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // error if incorrect input
        try {
            SequenceCore.getNext("isha", "seq1", -1);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        List<Long> ids = SequenceCore.getNext("isha", "sEQ1", 5);
        assertEquals(5, ids.size());
        assertEquals(1, ids.get(0).longValue());
        assertEquals(2, ids.get(1).longValue());
        assertEquals(3, ids.get(2).longValue());
        assertEquals(4, ids.get(3).longValue());
        assertEquals(5, ids.get(4).longValue());

        // ensure it works for 1
        ids = SequenceCore.getNext("isha", "seq1", 1);
        assertEquals(1, ids.size());
        assertEquals(6, ids.get(0).longValue());

        assertEquals(7, SequenceCore.getNext("isha", "seq1", 1).iterator()
                .next().longValue());

        // for some other sequence types
        ids = SequenceCore.getNext("isha", "seq2", 2);
        assertEquals(2, ids.size());
        assertEquals(1, ids.get(0).longValue());
        assertEquals(2, ids.get(1).longValue());
    }
}
