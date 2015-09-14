package crmdna.client;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.TreeSet;

import static org.junit.Assert.assertEquals;

public class CrmDnaUserTest {

    private final LocalServiceTestHelper datastoreHelper =
            new LocalServiceTestHelper(new LocalDatastoreServiceTestConfig()
                    .setApplyAllHighRepJobPolicy());

    @Before
    public void setUp() {
        datastoreHelper.setUp();
    }

    @After
    public void tearDown() {
        datastoreHelper.tearDown();
    }

    @Test
    public void getClientsTest() {
        ClientProp client = Client.create("Isha");
        assertEquals("isha", client.name);

        String email = "sathya.t@ishafoundation.org";
        Client.addUser("isha", email);
        ObjectifyFilter.complete(); //force all async operations to complete

        TreeSet<ClientEntity> clients = CrmDnaUser.getClients(email);
        assertEquals(1, clients.size());
        assertEquals("isha", clients.first().name);
        assertEquals("Isha", clients.first().displayName);
    }
}
