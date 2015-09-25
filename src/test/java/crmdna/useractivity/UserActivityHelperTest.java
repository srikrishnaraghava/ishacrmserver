package crmdna.useractivity;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.user.User;
import crmdna.user.UserProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class UserActivityHelperTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private final String invalidUser = "invalid@login.com";
    private UserProp validUserProp;
    private GroupProp sgp;
    private GroupProp kl;
    private GroupProp sydney;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        kl = Group.create(client, "Malaysia/KL", User.SUPER_USER);
        assertEquals(2, kl.groupId);

        sydney = Group.create(client, "Australia/Sydney", User.SUPER_USER);
        assertEquals(3, sydney.groupId);

        validUserProp = User.create(client, "valid@login.com", sgp.groupId,
                User.SUPER_USER);
        assertEquals(1,
                User.get(client, validUserProp.email).toProp(client).userId);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void getUserActivityTest() {
        // code commented out by sathya - 25 Sep 15

        /*
        // client has to be valid
        try {
            UserActivity.getUserActivity(invalidClient, validUserProp.userId,
                    null, null, validUserProp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // login user should be valid
        try {
            UserActivity.getUserActivity(client, validUserProp.userId, null,
                    null, invalidUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
        }

        // user for whom activity is queried should be valid
        try {
            UserActivity.getUserActivity(client, 100, // 100 should be an
                    // invalid user id
                    null, null, validUserProp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        UserActivity.recordUserActivity(client, EntityType.MEMBER, 3,
                UserAction.VIEW, null, validUserProp.email);
        UserActivity.recordUserActivity(client, EntityType.MEMBER, 4,
                UserAction.CREATE, "email: email@login.com",
                validUserProp.email);
        UserActivity.recordUserActivity(client, EntityType.MEMBER, 4,
                UserAction.UPDATE,
                "email: [email@login.com] ---> [email2@login.com]",
                validUserProp.email);

        ObjectifyFilter.complete();
        List<UserActivityProp> props = UserActivity.getUserActivity(client,
                validUserProp.userId, null, null, validUserProp.email);
        // sorted in descending order of timestamp
        assertEquals(3, props.size());
        assertEquals(validUserProp.userId, props.get(2).userId);
        assertEquals(UserAction.VIEW, props.get(2).userAction);
        assertEquals(3, props.get(2).entityId);
        assertEquals(EntityType.MEMBER, props.get(2).entityType);
        assertEquals(null, props.get(2).change);

        assertEquals(validUserProp.userId, props.get(1).userId);
        assertEquals(UserAction.CREATE, props.get(1).userAction);
        assertEquals(4, props.get(1).entityId);
        assertEquals(EntityType.MEMBER, props.get(1).entityType);
        assertEquals("email: email@login.com", props.get(1).change);

        assertEquals(validUserProp.userId, props.get(0).userId);
        assertEquals(UserAction.UPDATE, props.get(0).userAction);
        assertEquals(4, props.get(0).entityId);
        assertEquals(EntityType.MEMBER, props.get(0).entityType);
        assertEquals("email: [email@login.com] ---> [email2@login.com]",
                props.get(1).change);

                */
    }

    @Test
    public void recordUserActivityTest() throws InterruptedException {
        /*
        // code commented out by sathya - 25 Sep 15

        // client has to be valid
        try {
            UserActivity.recordUserActivity(invalidClient, EntityType.MEMBER,
                    3, UserAction.VIEW, null, validUserProp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // login should be valid
        try {
            UserActivity.recordUserActivity(client, EntityType.MEMBER, 3,
                    UserAction.VIEW, null, invalidUser);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // Date start = new Date();
        Thread.sleep(10);
        UserActivity.recordUserActivity(client, EntityType.MEMBER, 3,
                UserAction.VIEW, null, validUserProp.email);
        Thread.sleep(10);
        // Date end = new Date();

        ObjectifyFilter.complete();
        // List<UserActivityProp> props = UserActivity.getUserActivity(client,
        // validUserProp.userId, null, null, validUserProp.email);

        // work in progress
        assertTrue(false);

        */
    }
}
