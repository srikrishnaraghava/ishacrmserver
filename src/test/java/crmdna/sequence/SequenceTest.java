package crmdna.sequence;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.sequence.Sequence.SequenceType;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SequenceTest {

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
            Sequence.getNext("isha", SequenceType.MEMBER, 5);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        Client.create("Isha");

        // error if incorrect input
        try {
            Sequence.getNext("isha", SequenceType.MEMBER, -1);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        List<Long> ids = Sequence.getNext("isha", SequenceType.MEMBER, 5);
        assertEquals(5, ids.size());
        assertEquals(1, ids.get(0).longValue());
        assertEquals(2, ids.get(1).longValue());
        assertEquals(3, ids.get(2).longValue());
        assertEquals(4, ids.get(3).longValue());
        assertEquals(5, ids.get(4).longValue());

        // ensure it works for 1
        ids = Sequence.getNext("isha", SequenceType.MEMBER, 1);
        assertEquals(1, ids.size());
        assertEquals(6, ids.get(0).longValue());

        assertEquals(7, Sequence.getNext("isha", SequenceType.MEMBER));

        // for other sequence types
        ids = Sequence.getNext("isha", SequenceType.GROUP, 2);
        assertEquals(2, ids.size());
        assertEquals(1, ids.get(0).longValue());
        assertEquals(2, ids.get(1).longValue());

        ids = Sequence.getNext("isha", SequenceType.VENUE, 3);
        assertEquals(3, ids.size());
        assertEquals(1, ids.get(0).longValue());
        assertEquals(2, ids.get(1).longValue());
        assertEquals(3, ids.get(2).longValue());

        ids = Sequence.getNext("isha", SequenceType.PROGRAM, 3);
        assertEquals(3, ids.size());
        assertEquals(1, ids.get(0).longValue());
        assertEquals(2, ids.get(1).longValue());
        assertEquals(3, ids.get(2).longValue());
    }

    @Test
    public void getNextReturnsEmptyListWhenNumElementsIsZero() {
        Client.create("Isha");

        List<Long> ids = Sequence.getNext("isha", SequenceType.PROGRAM, 0);

        assertTrue(ids.isEmpty());
    }
}
