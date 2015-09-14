package crmdna.practice;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.practice.Practice.PracticeProp;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PracticeTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private final String validUser = "valid@login.com";
    private final String userWithPermission = "withpermission@login.com";

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        // can call getAll without any groups
        List<PracticeProp> props = Practice.getAll(client);
        assertEquals(0, props.size());

        long groupId = crmdna.group.Group.create(client, "Singapore",
                User.SUPER_USER).groupId;
        assertEquals(1, groupId);

        User.create(client, validUser, groupId, User.SUPER_USER);
        assertEquals(1, User.get(client, validUser).toProp(client).userId);

        User.create(client, userWithPermission, groupId, User.SUPER_USER);
        assertEquals(2,
                User.get(client, userWithPermission).toProp(client).userId);

        User.addClientLevelPrivilege(client, userWithPermission,
                ClientLevelPrivilege.UPDATE_PRACTICE, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void createTest() {

        PracticeProp prop = Practice.create(client, "Isha Kriya",
                userWithPermission);
        assertEquals("first entity has id 1", 1, prop.practiceId);

        prop = Practice.safeGet(client, prop.practiceId).toProp();
        assertEquals("name is populated correctly", "ishakriya", prop.name);
        assertEquals("display name is populated correctly", "Isha Kriya",
                prop.displayName);

        // cannot create duplicate
        try {
            Practice.create("isha", "ISHA KRIYA", User.SUPER_USER);
            assertTrue(false);
        } catch (APIException e) {
            assertEquals("cannot create duplicate practice",
                    Status.ERROR_RESOURCE_ALREADY_EXISTS, e.statusCode);
        }

        // ids should be in sequence
        prop = Practice.create(client, "Shoonya", userWithPermission);
        assertEquals("practice id is in sequence", 2, prop.practiceId);
        prop = Practice.create(client, "Shambhavi", userWithPermission);
        assertEquals("practice id is in sequence", 3, prop.practiceId);
        prop = Practice.create(client, "Surya Kriya", userWithPermission);
        assertEquals("practice id is in sequence", 4, prop.practiceId);

        prop = Practice.safeGet(client, 2).toProp();
        assertEquals("name is populated correctly", "shoonya", prop.name);
        assertEquals("display name is populated correctly", "Shoonya",
                prop.displayName);
        prop = Practice.safeGet(client, 3).toProp();
        assertEquals("display name is populated correctly", "Shambhavi",
                prop.displayName);
        prop = Practice.safeGet(client, 4).toProp();
        assertEquals("display name is populated correctly", "Surya Kriya",
                prop.displayName);
        assertEquals("name is populated correctly", "suryakriya", prop.name);

        // access control
        try {
            Practice.create("isha", "Hata Yoga", validUser);
            assertTrue(false);
        } catch (APIException e) {
            assertEquals("permission required to edit practice",
                    Status.ERROR_INSUFFICIENT_PERMISSION, e.statusCode);
        }

        // client should be valid
        try {
            Practice.create(invalidClient, "Aum Chanting", User.SUPER_USER);
            assertTrue(false);
        } catch (APIException e) {
            assertEquals("client should be valid",
                    Status.ERROR_RESOURCE_NOT_FOUND, e.statusCode);
        }
    }

    @Test
    public void safeGetTest() {

        PracticeProp prop = Practice.create(client, "Shoonya",
                userWithPermission);
        assertTrue(prop.practiceId != 0);

        prop = Practice.safeGet("isha", prop.practiceId).toProp();
        assertEquals("name is populated correctly", "shoonya", prop.name);
        assertEquals("display name is populated correctly", "Shoonya",
                prop.displayName);

        // exception for non existing
        try {
            Practice.safeGet("isha", prop.practiceId + 20939); // non existant
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("exception thrown for non exisitng practise id",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void renameTest() {
        PracticeProp shoonya = Practice.create("isha", "Shoonya",
                userWithPermission);
        Practice.create(client, "Shambhavi", userWithPermission);

        try {
            Practice.rename("isha", shoonya.practiceId, "Shambhavi",
                    userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("cannot rename to existing",
                    Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        // can change case and rename
        Practice.rename("isha", shoonya.practiceId, "shoonya",
                userWithPermission);
        shoonya = Practice.safeGet("isha", shoonya.practiceId).toProp();
        assertEquals("name correctly populated after rename", "shoonya",
                shoonya.name);
        assertEquals("name correctly populated after rename", "shoonya",
                shoonya.displayName);

        Practice.rename("isha", shoonya.practiceId, "Shoonya Meditation",
                userWithPermission);
        shoonya = Practice.safeGet("isha", shoonya.practiceId).toProp();
        assertEquals("name populated correctly", "shoonyameditation",
                shoonya.name);
        assertEquals("display name populated correctly", "Shoonya Meditation",
                shoonya.displayName);

        try {
            Practice.rename("isha", shoonya.practiceId, "Shoonya", validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("permission required to rename practice",
                    Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        try {
            Practice.rename(invalidClient, shoonya.practiceId, "Shoonya",
                    userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void getAllTest() {
        Practice.create("isha", "Shoonya", userWithPermission);
        Practice.create("isha", "Shambhavi", userWithPermission);
        ObjectifyFilter.complete();

        List<PracticeProp> props = Practice.getAll("isha");
        assertEquals(2, props.size());
        // should be sorted
        assertEquals("shambhavi", props.get(0).name);
        assertEquals("Shambhavi", props.get(0).displayName);
        assertEquals("shoonya", props.get(1).name);
        assertEquals("Shoonya", props.get(1).displayName);
    }

    @Test
    public void toPropTest() {
        PracticeEntity entity = new PracticeEntity();
        entity.practiceId = 123l;
        entity.displayName = "Shoonya";
        entity.name = entity.displayName.toLowerCase();

        PracticeProp prop = entity.toProp();
        assertEquals("practise id correctly populated", 123, prop.practiceId);
        assertEquals("display name correctly populated", "Shoonya",
                prop.displayName);
        assertEquals("name correctly populated", "shoonya", prop.name);
    }

    @Test
    public void practiseNameSavedInLCWithSpecialCharRemoved() {
        PracticeProp practiceProp = Practice.create("isha",
                "Shambhavi_Mahamudra (kriya)", User.SUPER_USER);
        practiceProp = Practice.safeGet(client, practiceProp.practiceId)
                .toProp();
        assertEquals("shambhavimahamudrakriya", practiceProp.name);
    }

    @Test
    public void practiseNameSavedInLCWithSpecialCharRemovedAfterUpdate() {
        PracticeProp practiceProp = Practice.create("isha",
                "Shambhavi Mahamudra", User.SUPER_USER);
        practiceProp = Practice.safeGet(client, practiceProp.practiceId)
                .toProp();
        assertEquals("shambhavimahamudra", practiceProp.name);

        Practice.rename(client, practiceProp.practiceId,
                "Shambhavi_Mahamudra (Kriya)", User.SUPER_USER);
        practiceProp = Practice.safeGet(client, practiceProp.practiceId)
                .toProp();
        assertEquals("shambhavimahamudrakriya", practiceProp.name);
        assertEquals("Shambhavi_Mahamudra (Kriya)", practiceProp.displayName);
    }

    @Test
    public void displayNameSavedAsIs() {
        PracticeProp practiceProp = Practice.create("isha",
                "Shambhavi_Mahamudra (kriya)", User.SUPER_USER);
        practiceProp = Practice.safeGet(client, practiceProp.practiceId)
                .toProp();
        assertEquals("Shambhavi_Mahamudra (kriya)", practiceProp.displayName);
    }

    @Test
    public void safeGetByIdOrNameTest() {
        PracticeProp shoonya = Practice.create(client, "Shoonya", User.SUPER_USER);

        assertEquals(shoonya.practiceId, Practice.safeGetByIdOrName(client, shoonya.practiceId + "").practiceId);

        assertEquals(shoonya.practiceId, Practice.safeGetByIdOrName(client, shoonya.name).practiceId);

        try {
            Practice.safeGetByIdOrName(client, "dummy");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }
}
