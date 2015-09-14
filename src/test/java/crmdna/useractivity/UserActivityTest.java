package crmdna.useractivity;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.useractivity.UserActivityCore.UserActivityProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserActivityTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";

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
    public void getUserActivityTest() throws InterruptedException {
        // client has to be valid
        final long userId = 1;
        try {
            UserActivityCore.getUserActivity(invalidClient, userId, null, null);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // zero records before recording any activity
        List<UserActivityProp> props = UserActivityCore.getUserActivity(client, userId, null, null);
        assertEquals(0, props.size());

        Date start = new Date();
        UserActivityCore.recordUserActivity(client, "GROUP", 3, "CREATE", null, userId + 2); // some
        // other
        // user id
        UserActivityCore.recordUserActivity(client, "MEMBER", 3, "VIEW", null, userId);
        int deltaMS = 100;
        Thread.sleep(deltaMS);
        Date startPlusDelta = new Date();
        Thread.sleep(deltaMS);
        UserActivityCore.recordUserActivity(client, "MEMBER", 4, "CREATE", "email: email@login.com",
                userId);
        Thread.sleep(deltaMS);
        Date startPlus3Delta = new Date();
        UserActivityCore.recordUserActivity(client, "MEMBER", 4, "UPDATE",
                "email: [email@login.com] ---> [email2@login.com]", userId);
        Thread.sleep(deltaMS);

        ObjectifyFilter.complete();
        props = UserActivityCore.getUserActivity(client, userId, null, null);

        // do not specify start and end dates - should return all records
        // sorted in descending order of timestamp
        assertEquals(3, props.size());
        assertEquals(userId, props.get(2).userId);
        assertEquals("VIEW", props.get(2).userAction);
        assertEquals(3, props.get(2).entityId);
        assertEquals("MEMBER", props.get(2).entityType);
        assertEquals(null, props.get(2).change);

        assertEquals(userId, props.get(1).userId);
        assertEquals("CREATE", props.get(1).userAction);
        assertEquals(4, props.get(1).entityId);
        assertEquals("MEMBER", props.get(1).entityType);
        assertEquals("email: email@login.com", props.get(1).change);

        assertEquals(userId, props.get(0).userId);
        assertEquals("UPDATE", props.get(0).userAction);
        assertEquals(4, props.get(0).entityId);
        assertEquals("MEMBER", props.get(0).entityType);
        assertEquals("email: [email@login.com] ---> [email2@login.com]", props.get(0).change);

        // specify start and end date
        props = UserActivityCore.getUserActivity(client, userId, start, startPlusDelta);
        assertEquals(1, props.size());
        assertEquals(userId, props.get(0).userId);
        assertEquals("VIEW", props.get(0).userAction);
        assertEquals(3, props.get(0).entityId);
        assertEquals("MEMBER", props.get(0).entityType);
        assertEquals(null, props.get(0).change);

        // specify start but not end
        props = UserActivityCore.getUserActivity(client, userId, startPlus3Delta, null);
        assertEquals(1, props.size());
        assertEquals(userId, props.get(0).userId);
        assertEquals("UPDATE", props.get(0).userAction);
        assertEquals(4, props.get(0).entityId);
        assertEquals("MEMBER", props.get(0).entityType);
        assertEquals("email: [email@login.com] ---> [email2@login.com]", props.get(0).change);

        // specify end but not start
        props = UserActivityCore.getUserActivity(client, userId, null, startPlusDelta);
        assertEquals(1, props.size());
        assertEquals(userId, props.get(0).userId);
        assertEquals("VIEW", props.get(0).userAction);
        assertEquals(3, props.get(0).entityId);
        assertEquals("MEMBER", props.get(0).entityType);
        assertEquals(null, props.get(0).change);
    }

    @Test
    public void recordUserActivityTest() throws InterruptedException {
        final long userId = 1;
        // client has to be valid
        try {
            UserActivityCore.recordUserActivity(invalidClient, "MEMBER", 3, "VIEW", null, userId);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        Date start = new Date();
        Thread.sleep(100);
        UserActivityCore.recordUserActivity(client, "MEMBER", 3, "VIEW", null, userId);
        Thread.sleep(100);
        Date end = new Date();

        ObjectifyFilter.complete();
        List<UserActivityProp> props = UserActivityCore.getUserActivity(client, userId, null, null);

        assertEquals(1, props.size());
        // System.out.println("props.get(0: " + new Gson().toJson(props.get(0)) +
        // ", start: " + start + ", end: " + end);
        assertEquals("MEMBER", props.get(0).entityType);
        assertEquals(3, props.get(0).entityId);
        assertEquals(userId, props.get(0).userId);
        assertTrue(props.get(0).timestamp.getTime() > start.getTime());
        assertTrue(props.get(0).timestamp.getTime() < end.getTime());
    }

    @Test
    public void getEntityActivityTest() throws InterruptedException {
        final long userId = 1;

        // client has to be valid
        try {
            UserActivityCore.getEntityActivity(invalidClient, "MEMBER", 3, null, null);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // no records if non existing entity
        List<UserActivityProp> userActivityProps =
                UserActivityCore.getEntityActivity(client, "MEMBER", 3, null, null);
        assertEquals(0, userActivityProps.size());

        int deltaMS = 100;
        UserActivityCore.recordUserActivity(client, "MEMBER", 2, "VIEW", null, userId);
        Thread.sleep(deltaMS);
        Date startPlusDelta = new Date();
        Thread.sleep(deltaMS);
        UserActivityCore.recordUserActivity(client, "MEMBER", 3, "UPDATE", "update 1", userId);
        Thread.sleep(deltaMS);
        Date startPlus3Delta = new Date();
        UserActivityCore.recordUserActivity(client, "MEMBER", 3, "UPDATE", "update 2", userId);
        Thread.sleep(100);

        // do not specify start and end
        userActivityProps = UserActivityCore.getEntityActivity(client, "MEMBER", 3, null, null);
        assertEquals(2, userActivityProps.size());
        assertEquals("MEMBER", userActivityProps.get(0).entityType);
        assertEquals(3, userActivityProps.get(0).entityId);
        assertEquals(userId, userActivityProps.get(0).userId);
        assertEquals("UPDATE", userActivityProps.get(0).userAction);
        assertEquals("update 2", userActivityProps.get(0).change);

        assertEquals("MEMBER", userActivityProps.get(1).entityType);
        assertEquals(3, userActivityProps.get(1).entityId);
        assertEquals(userId, userActivityProps.get(1).userId);
        assertEquals("UPDATE", userActivityProps.get(1).userAction);
        assertEquals("update 1", userActivityProps.get(1).change);

        // specify only start date
        userActivityProps =
                UserActivityCore.getEntityActivity(client, "MEMBER", 3, startPlus3Delta, null);
        assertEquals(1, userActivityProps.size());
        assertEquals("MEMBER", userActivityProps.get(0).entityType);
        assertEquals(3, userActivityProps.get(0).entityId);
        assertEquals(userId, userActivityProps.get(0).userId);
        assertEquals("UPDATE", userActivityProps.get(0).userAction);
        assertEquals("update 2", userActivityProps.get(0).change);

        // specify only end date
        userActivityProps =
                UserActivityCore.getEntityActivity(client, "MEMBER", 3, null, startPlusDelta);
        assertEquals(0, userActivityProps.size());
    }
}
