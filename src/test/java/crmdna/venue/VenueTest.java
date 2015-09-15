package crmdna.venue;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group.GroupProp;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.venue.Venue.VenueProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class VenueTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
        new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private final String validUser = "valid@login.com";
    private final String userWithPermission = "withpermission@login.com";

    private GroupProp sgp, kl;

    @Before public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        sgp = crmdna.group.Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        kl = crmdna.group.Group.create(client, "KL", User.SUPER_USER);
        assertEquals(2, kl.groupId);

        User.create(client, validUser, sgp.groupId, User.SUPER_USER);
        assertEquals(1, User.get(client, validUser).toProp(client).userId);

        User.create(client, userWithPermission, sgp.groupId, User.SUPER_USER);
        assertEquals(2, User.get(client, userWithPermission).toProp(client).userId);

        User.addClientLevelPrivilege(client, userWithPermission, ClientLevelPrivilege.UPDATE_VENUE,
            User.SUPER_USER);
    }

    @After public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test public void createTest() {
        try {
            Venue.create(invalidClient, "Singapore Gujarathi Bhavan", "Gujarathi Bhavan",
                "13 Cuff Road", sgp.groupId, userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("error for invalid client", Status.ERROR_RESOURCE_NOT_FOUND,
                ex.statusCode);
        }

        try {
            Venue.create(client, "Singapore Gujarathi Bhavan", "Gujarathi Bhavan", "13 Cuff Road",
                sgp.groupId, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("user needs permission to create", Status.ERROR_INSUFFICIENT_PERMISSION,
                ex.statusCode);
        }

        VenueProp venueProp = Venue
            .create(client, "Singapore Gujarathi Bhavan", "Gujarathi Bhavan", "13 Cuff Road",
                sgp.groupId, userWithPermission);
        venueProp = Venue.safeGet(client, venueProp.venueId).toProp();
        assertEquals("first id is 1", 1, venueProp.venueId);
        assertEquals("name saved correctly in lower case without spaces",
            "singaporegujarathibhavan", venueProp.name);
        assertEquals("display name saved correctly", "Singapore Gujarathi Bhavan",
            venueProp.displayName);
        assertEquals("short name saved correctly", "Gujarathi Bhavan", venueProp.shortName);
        assertEquals("group id saved correctly", sgp.groupId, venueProp.groupId);
        assertEquals("address saved correctly", "13 Cuff Road", venueProp.address);

        // cannot create duplicate
        try {
            Venue.create(client, "singapore Gujarathi bhavan", "Gujarathi Bhavan", "13 Cuff Road",
                sgp.groupId, userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("cannot create duplicate", Status.ERROR_RESOURCE_ALREADY_EXISTS,
                ex.statusCode);
        }

        venueProp = Venue.create(client, "Chai Chee", "Technopark", "Technopark", sgp.groupId,
            userWithPermission);
        venueProp = Venue.safeGet(client, venueProp.venueId).toProp();
        assertEquals("id assigned in sequence", 2, venueProp.venueId);
    }

    @Test public void safeGetTest() {
        try {
            Venue.safeGet(invalidClient, 1);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Venue.safeGet(client, 1);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("venue id should be valid", Status.ERROR_RESOURCE_NOT_FOUND,
                ex.statusCode);
        }

        VenueProp venueProp = Venue
            .create(client, "Singapore Gujarathi Bhavan", "Gujarathi Bhavan", "13 Cuff Road",
                sgp.groupId, userWithPermission);
        venueProp = Venue.safeGet(client, venueProp.venueId).toProp();
        assertEquals("first id is 1", 1, venueProp.venueId);
        assertEquals("name saved correctly in lower case without spaces",
            "singaporegujarathibhavan", venueProp.name);
        assertEquals("display name saved correctly", "Singapore Gujarathi Bhavan",
            venueProp.displayName);
        assertEquals("short name saved correctly", "Gujarathi Bhavan", venueProp.shortName);
        assertEquals("group id saved correctly", sgp.groupId, venueProp.groupId);
        assertEquals("address saved correctly", "13 Cuff Road", venueProp.address);
    }

    @Test public void safeGetByNameTest() {
        try {
            Venue.safeGetByIdOrName(invalidClient, "singapore gujarathi bhavan");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Venue.safeGetByIdOrName(client, "singapore gujarathi bhavan");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("venue should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        Venue.create(client, "Singapore Gujarathi Bhavan", "Gujarathi Bhavan", "13 Cuff Road",
            sgp.groupId, userWithPermission);
        VenueProp venueProp = Venue.safeGetByIdOrName(client, "singaporegujarathI BhaVan").toProp();
        assertEquals("first id is 1", 1, venueProp.venueId);
        assertEquals("name saved correctly in lower case without spaces",
            "singaporegujarathibhavan", venueProp.name);
        assertEquals("display name saved correctly", "Singapore Gujarathi Bhavan",
            venueProp.displayName);
        assertEquals("short name saved correctly", "Gujarathi Bhavan", venueProp.shortName);
        assertEquals("group id saved correctly", sgp.groupId, venueProp.groupId);
        assertEquals("address saved correctly", "13 Cuff Road", venueProp.address);
    }

    @Test public void updateTest() {
        try {
            Venue.update(invalidClient, 1, "Gujarathi Bhavan", "Gujarathi Bhavan",
                "13 cuff road, singapore", null, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        VenueProp venueProp = Venue
            .create(client, "Singapore Gujarathi Bhavan", "Gujarathi Bhavan", "13 Cuff Road",
                sgp.groupId, userWithPermission);
        assertEquals(1, venueProp.venueId);

        try {
            Venue.update(client, 1, "Gujarathi Bhavan", "Gujarathi Bhavan",
                "13 cuff road, singapore", null, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("user should have permission", Status.ERROR_INSUFFICIENT_PERMISSION,
                ex.statusCode);
        }

        Venue.update(client, 1, "Gujarathi Bhavan", "GB", "13 cuff road, singapore", null,
            userWithPermission);
        venueProp = Venue.safeGet(client, venueProp.venueId).toProp();

        assertEquals("id is correct", 1, venueProp.venueId);
        assertEquals("name saved correctly in lower case", "gujarathibhavan", venueProp.name);
        assertEquals("display name saved correctly", "Gujarathi Bhavan", venueProp.displayName);
        assertEquals("short name saved correctly", "GB", venueProp.shortName);
        assertEquals("group id saved correctly", sgp.groupId, venueProp.groupId);
        assertEquals("address saved correctly", "13 cuff road, singapore", venueProp.address);

        // can change name alone
        Venue.update(client, 1, "Sgp Gujarathi Bhavan", null, null, null, userWithPermission);
        venueProp = Venue.safeGet(client, venueProp.venueId).toProp();
        assertEquals("id is correct", 1, venueProp.venueId);
        assertEquals("name saved correctly in lower case", "sgpgujarathibhavan", venueProp.name);
        assertEquals("display name saved correctly", "Sgp Gujarathi Bhavan", venueProp.displayName);
        assertEquals("short name saved correctly", "GB", venueProp.shortName);
        assertEquals("group id is unchanged", sgp.groupId, venueProp.groupId);
        assertEquals("address is unchanged", "13 cuff road, singapore", venueProp.address);

        // can change address alone
        Venue.update(client, 1, null, null, "13 cuff road, singapore 123234", null,
            userWithPermission);
        venueProp = Venue.safeGet(client, venueProp.venueId).toProp();
        assertEquals("id is correct", 1, venueProp.venueId);
        assertEquals("name saved correctly in lower case", "sgpgujarathibhavan", venueProp.name);
        assertEquals("display name saved correctly", "Sgp Gujarathi Bhavan", venueProp.displayName);
        assertEquals("short name saved correctly", "GB", venueProp.shortName);
        assertEquals("group id is unchanged", sgp.groupId, venueProp.groupId);
        assertEquals("address is unchanged", "13 cuff road, singapore 123234", venueProp.address);

        // venue id should be valid
        try {
            Venue.update(client, 3, "Gujarathi Bhavan", "GB", "13 cuff road, singapore", null,
                userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("venue id should be valid", Status.ERROR_RESOURCE_NOT_FOUND,
                ex.statusCode);
        }
    }

    @Test public void getAllTest() {
        try {
            Venue.getAll(invalidClient);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        List<VenueProp> venueProps = Venue.getAll(client);
        assertEquals("getAll works when no venues are present", 0, venueProps.size());

        Venue.create(client, "Chai Chee", "Technopark", "Chai Chee Technopark", sgp.groupId,
            userWithPermission);
        Venue.create(client, "KLCC", "KLCC", "KL center", kl.groupId, userWithPermission);
        Venue.create(client, "Singapore Gujarathi Bhavan", "Gujarathi Bhavan", "13 Cuff Road",
            sgp.groupId, userWithPermission);

        venueProps = Venue.getAll(client);
        assertEquals("all venues returnes", 3, venueProps.size());
        // should be sorted
        assertEquals("name is correct", "chaichee", venueProps.get(0).name);
        assertEquals("displayname is correct", "Chai Chee", venueProps.get(0).displayName);
        assertEquals("shortname is correct", "Technopark", venueProps.get(0).shortName);
        assertEquals("address is correct", "Chai Chee Technopark", venueProps.get(0).address);
        assertEquals("group id is correct", sgp.groupId, venueProps.get(0).groupId);

        assertEquals("name is correct", "klcc", venueProps.get(1).name);
        assertEquals("displayname is correct", "KLCC", venueProps.get(1).displayName);
        assertEquals("shortname is correct", "KLCC", venueProps.get(1).shortName);
        assertEquals("address is correct", "KL center", venueProps.get(1).address);
        assertEquals("group id is correct", kl.groupId, venueProps.get(1).groupId);

        assertEquals("name is correct", "singaporegujarathibhavan", venueProps.get(2).name);
        assertEquals("displayname is correct", "Singapore Gujarathi Bhavan",
            venueProps.get(2).displayName);
        assertEquals("shortname is correct", "Gujarathi Bhavan", venueProps.get(2).shortName);
        assertEquals("address is correct", "13 Cuff Road", venueProps.get(2).address);
        assertEquals("group id is correct", sgp.groupId, venueProps.get(2).groupId);
    }

    @Test public void getAllForGroupTest() {
        try {
            Venue.getAllForGroup(invalidClient, sgp.groupId);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Venue.getAllForGroup(client, 100); // invalid group
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("group should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        List<VenueProp> venueProps = Venue.getAllForGroup(client, sgp.groupId);
        assertEquals("works when no venues are present", 0, venueProps.size());

        Venue.create(client, "Singapore Gujarathi Bhavan", "GB", "13 Cuff Road", sgp.groupId,
            userWithPermission);
        Venue.create(client, "Chai Chee", "Technopark", "Chai Chee Technopark", sgp.groupId,
            userWithPermission);
        Venue.create(client, "KLCC", "KLCC", "KL center", kl.groupId, userWithPermission);

        venueProps = Venue.getAllForGroup(client, sgp.groupId);
        assertEquals("all venues returned", 2, venueProps.size());
        // should be sorted
        assertEquals("name is correct", "chaichee", venueProps.get(0).name);
        assertEquals("displayname is correct", "Chai Chee", venueProps.get(0).displayName);
        assertEquals("shortname is correct", "Technopark", venueProps.get(0).shortName);
        assertEquals("address is correct", "Chai Chee Technopark", venueProps.get(0).address);
        assertEquals("group id is correct", sgp.groupId, venueProps.get(0).groupId);

        assertEquals("name is correct", "singaporegujarathibhavan", venueProps.get(1).name);
        assertEquals("displayname is correct", "Singapore Gujarathi Bhavan",
            venueProps.get(1).displayName);
        assertEquals("shortname is correct", "GB", venueProps.get(1).shortName);
        assertEquals("address is correct", "13 Cuff Road", venueProps.get(1).address);
        assertEquals("group id is correct", sgp.groupId, venueProps.get(1).groupId);

        venueProps = Venue.getAllForGroup(client, kl.groupId);
        assertEquals("all venues returned", 1, venueProps.size());
        assertEquals("name is correct", "klcc", venueProps.get(0).name);
        assertEquals("displayname is correct", "KLCC", venueProps.get(0).displayName);
        assertEquals("shortname is correct", "KLCC", venueProps.get(0).shortName);
        assertEquals("address is correct", "KL center", venueProps.get(0).address);
        assertEquals("group id is correct", kl.groupId, venueProps.get(0).groupId);
    }
}
