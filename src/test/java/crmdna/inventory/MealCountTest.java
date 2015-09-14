package crmdna.inventory;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.UserCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MealCountTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String validUser = "valid@login.com";
    private final String userWithPermission = "withpermission@login.com";

    GroupProp mahamudra;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        GroupProp mahamudra = Group
                .create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, mahamudra.groupId);

        User.create(client, validUser, mahamudra.groupId, User.SUPER_USER);
        assertEquals(1,
                UserCore.safeGet(client, validUser).toProp(client).userId);

        User.create(client, userWithPermission, mahamudra.groupId,
                User.SUPER_USER);
        assertEquals(
                2,
                UserCore.safeGet(client, userWithPermission).toProp(client).userId);

        User.addClientLevelPrivilege(client, userWithPermission,
                ClientLevelPrivilege.UPDATE_MEAL_COUNT, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void setCountTest() {
        MealCount.setCount(client, 20140908, 100, null, null,
                userWithPermission);

        List<MealCountEntity> entities = MealCount
                .query(client, 20140908, null);
        assertEquals(1, entities.size());
        assertEquals(20140908, entities.get(0).yyyymmdd);
        assertEquals(100, entities.get(0).breakfastCount);
        assertEquals(0, entities.get(0).lunchCount);
        assertEquals(0, entities.get(0).dinnerCount);

        // need permission to set count
        try {
            MealCount.setCount(client, 20140908, 100, null, null, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        // date should be in valid format
        // need permission to set count
        try {
            MealCount.setCount(client, 2014090890, 100, null, null,
                    userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // set lunch and dinner count
        MealCount
                .setCount(client, 20140908, null, 200, 300, userWithPermission);
        entities = MealCount.query(client, 20140908, null);
        assertEquals(1, entities.size());
        assertEquals(20140908, entities.get(0).yyyymmdd);
        assertEquals(100, entities.get(0).breakfastCount);
        assertEquals(200, entities.get(0).lunchCount);
        assertEquals(300, entities.get(0).dinnerCount);
    }

    @Test
    public void queryTest() {
        MealCount.setCount(client, 20140908, 100, 200, 300, userWithPermission);
        MealCount.setCount(client, 20140909, 101, 201, 301, userWithPermission);

        List<MealCountEntity> entities = MealCount
                .query(client, 20140908, null);
        assertEquals(2, entities.size());
        assertEquals(20140909, entities.get(0).yyyymmdd);
        assertEquals(101, entities.get(0).breakfastCount);
        assertEquals(201, entities.get(0).lunchCount);
        assertEquals(301, entities.get(0).dinnerCount);

        assertEquals(100, entities.get(1).breakfastCount);
        assertEquals(200, entities.get(1).lunchCount);
        assertEquals(300, entities.get(1).dinnerCount);

        entities = MealCount.query(client, 20140908, 20140908);
        assertEquals(1, entities.size());
        assertEquals(20140908, entities.get(0).yyyymmdd);

        entities = MealCount.query(client, null, 20140909);
        assertEquals(2, entities.size());

        entities = MealCount.query(client, 20140801, null);
        assertEquals(2, entities.size());

        entities = MealCount.query(client, 20140801, 20140831);
        assertEquals(0, entities.size());

        // start should be valid
        try {
            MealCount.query(client, 201408019, null);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // end should be valid
        try {
            MealCount.query(client, 20140809, 201409093);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // end should be >= start
        try {
            MealCount.query(client, 20140809, 20140801);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }
}
