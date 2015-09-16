package crmdna.client;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.user.User;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class ClientTest {

    private final LocalServiceTestHelper helper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());


    @Before
    public void setUp() {
        helper.setUp();
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        helper.tearDown();
    }

    @Test
    public void createTest() {
        ClientProp clientProp = Client.create("Isha");
        assertEquals("isha", clientProp.name);
        assertEquals("Isha", clientProp.displayName);

        ClientEntity clientEntity = Client.safeGet(clientProp.name);
        // should not throw exception
        assertTrue(clientEntity.name.equals("isha"));
        assertTrue(clientEntity.displayName.equals("Isha"));


        // cannot create the same name again
        try {
            clientProp = Client.create("Isha");
            assertTrue(false);
        } catch (APIException ex) {
            assertTrue(ex.statusCode == Status.ERROR_RESOURCE_ALREADY_EXISTS);
        }

        // cannot create in different case
        try {
            clientProp = Client.create("iSHa");
            assertTrue(false);
        } catch (APIException ex) {
            assertTrue(ex.statusCode == Status.ERROR_RESOURCE_ALREADY_EXISTS);
        }

        // big name should get truncated
        clientProp = Client.create("Isha Foundation");
        System.out.println("entity: " + new Gson().toJson(clientProp));
        assertEquals("isha fou", clientProp.name);
        assertEquals("Isha Foundation", clientProp.displayName);

        // cannot create same name again
        try {
            clientProp = Client.create("Isha Foundation Yoga");
            assertTrue(false);
        } catch (APIException e) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, e.statusCode);
        }
    }

    @Test
    public void safeGetTest() {
        ClientProp clientProp = Client.create("Isha");

        clientProp = Client.safeGet("isha").toProp();
        // should not throw exception
        assertTrue(clientProp.name.equals("isha"));
        assertTrue(clientProp.displayName.equals("Isha"));

        // exception for non existant client
        try {
            Client.safeGet("nonexist"); // non existant
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // exception when client is null
        try {
            Client.safeGet(null); // non existant
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void getTest() {
        assertNull(Client.get("isha"));

        Client.create("isha");
        ClientProp prop = Client.get("isha").toProp();
        assertEquals("isha", prop.name);
        assertEquals("isha", prop.displayName);
    }

    @Test
    public void getEntitiesTest() {
        Client.create("isha");
        Client.create("barclays");

        ObjectifyFilter.complete();
        Map<String, ClientEntity> map = Client.getEntities(Utils.getSet("isha", "barclays", "dummy"));

        assertEquals(2, map.size());
        assertTrue(map.containsKey("isha"));
        assertTrue(map.containsKey("barclays"));
    }

    @Test
    public void updateDisplayName() {
        ClientProp clientProp = Client.create("Isha");
        assertEquals("isha", clientProp.name);

        clientProp = Client.updateDisplayName("isha", "Isha Foundation");
        clientProp = Client.safeGet("isha").toProp();
        assertTrue(clientProp.name.equals("isha"));
        assertTrue(clientProp.displayName.equals("Isha Foundation"));
    }

    @Test
    public void ensureValidClientTest() {
        // throws exception is client does not exist
        try {
            Client.ensureValid("isha"); // non existant
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        Client.create("isha");
        Client.ensureValid("isha");
        // no exception
    }

    @Test
    public void getAllTest() {
        List<ClientProp> all = Client.getAll();
        assertEquals(0, all.size());

        Client.create("Isha Foundation");
        Client.create("Barclays Capital");

        all = Client.getAll();
        assertEquals(2, all.size());

        // should be sorted by name
        assertEquals("barclays", all.get(0).name);
        assertEquals("Barclays Capital", all.get(0).displayName);
        assertEquals("isha fou", all.get(1).name);
        assertEquals("Isha Foundation", all.get(1).displayName);
    }

    @Test
    public void safeGetSenderNameFromEmailTest() {
        String client = "isha";

        Client.create(client);
        Client.addOrDeleteAllowedEmailSender(client, "info@ishayoga.org", "Isha Yoga", true, User.SUPER_USER);

        String name = Client.safeGetSenderNameFromEmail(client, "info@ishayoga.org");
        assertEquals("Isha Yoga", name);

        try {
            Client.safeGetSenderNameFromEmail(client, "dummy@dummy.com");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }
}
