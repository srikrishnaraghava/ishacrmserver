package crmdna.user;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.user.User.App;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.User.GroupLevelPrivilege;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());
    // local implementation / test harness implementation becomes HRD
    // only if setApplyAllHighRepJobPolicy is set. If the implementation is not
    // HRD then
    // cross group transactions would fail (as master slave does not support it)

    String client = "isha";
    GroupProp sgp;
    GroupProp kl;
    UserProp userWithPermission;
    UserProp userWOPermission;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        kl = Group.create(client, "KL", User.SUPER_USER);
        assertEquals(2, kl.groupId);

        userWithPermission = User.create(client, "userwithpermission@dummy.com", sgp.groupId, User.SUPER_USER);
        assertEquals(1, userWithPermission.userId);
        User.addClientLevelPrivilege(client, userWithPermission.email, ClientLevelPrivilege.UPDATE_USER,
                User.SUPER_USER);

        userWOPermission = User.create(client, "userwopermission@dummy.com", sgp.groupId, User.SUPER_USER);
        assertEquals(2, userWOPermission.userId);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test(expected = APIException.class)
    public void cannotCreateUserForInvalidClient() {
        User.create("invalidClient", "dummy@dummy.com", sgp.groupId, User.SUPER_USER);
        assertTrue(false);
    }

    @Test(expected = APIException.class)
    public void cannotCreateUserForInvalidGroup() {
        long invalidGroupId = sgp.groupId + 100;
        User.create(client, "dummy@dummy.com", invalidGroupId, User.SUPER_USER);
        assertTrue(false);
    }

    @Test(expected = APIException.class)
    public void cannotCreateUserForInvalidEmail() {
        String invalidEmail = "invalid";
        User.create(client, invalidEmail, sgp.groupId, User.SUPER_USER);
        assertTrue(false);
    }

    @Test
    public void userWithPermissionCanCreateUser() {
        User.create(client, "dummy@dummy.com", sgp.groupId, userWithPermission.email);
        // no exception
    }

    @Test
    public void userIdsAreInSequence() {
        UserProp userProp1 = User.create(client, "dummy1@dummy.com", sgp.groupId, userWithPermission.email);
        UserProp userProp2 = User.create(client, "dummy2@dummy.com", sgp.groupId, userWithPermission.email);
        UserProp userProp3 = User.create(client, "dummy3@dummy.com", sgp.groupId, userWithPermission.email);
        UserProp userProp4 = User.create(client, "dummy4@dummy.com", sgp.groupId, userWithPermission.email);

        assertEquals(userProp1.userId + 1, userProp2.userId);
        assertEquals(userProp2.userId + 1, userProp3.userId);
        assertEquals(userProp3.userId + 1, userProp4.userId);
    }

    @Test(expected = APIException.class)
    public void userWOPermissionCannotCreateUser() {
        User.create(client, "dummy1@dummy.com", sgp.groupId, userWOPermission.email);
    }

    @Test(expected = APIException.class)
    public void cannotCreateUserForAlreadyExistingEmail() {
        String email = "dummy@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.create(client, email, sgp.groupId, User.SUPER_USER);
    }

    @Test(expected = APIException.class)
    public void safeGetThrowsIfUserMissing() {
        User.safeGet(client, "nonexisting@dummy.com");
        assertTrue(false);
    }

    @Test
    public void safeGetTest() {
        String user = "sathya.t@ishafoundation.org";
        String client = "isha";

        // client cannot be invalid
        try {
            User.safeGet("invalidClient", user);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            User.safeGet(client, user);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("user email should exist", Status.ERROR_INVALID_USER, ex.statusCode);
        }

        long groupId = Group.create(client, "sgp", User.SUPER_USER).groupId;
        UserCore.create(client, user, groupId);

        assertEquals("can get by email", user, User.safeGet(client, user).toProp(client).email);
    }

    @Test
    public void userWithPermssionCanUpdateEmail() {
        String existingEmail = "existing@dummy.com";
        User.create(client, existingEmail, sgp.groupId, User.SUPER_USER);

        String newEmail = "new@dummy.com";
        User.updateEmail(client, existingEmail, newEmail, userWithPermission.email);

        UserProp userProp = User.safeGet(client, newEmail).toProp(client);
        assertEquals(newEmail, userProp.email);

        try {
            User.safeGet(client, existingEmail);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
        }
    }

    @Test(expected = APIException.class)
    public void userWOPermssionCannotUpdateEmail() {
        String existingEmail = "existing@dummy.com";
        User.create(client, existingEmail, sgp.groupId, User.SUPER_USER);

        String newEmail = "new@dummy.com";
        User.updateEmail(client, existingEmail, newEmail, userWOPermission.email);
        assertTrue(false);
    }

    @Test
    public void userWithPermssionCanUpdateGroup() {
        String email = "existing@dummy.com";
        UserProp userProp = User.create(client, email, sgp.groupId, User.SUPER_USER);
        assertEquals(sgp.groupId, userProp.groupId);

        userProp = User.updateGroup(client, email, kl.groupId, userWithPermission.email);
        assertEquals(kl.groupId, userProp.groupId);
    }

    @Test(expected = APIException.class)
    public void userWOPermssionCannotUpdateGroup() {
        String email = "existing@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.updateGroup(client, email, kl.groupId, userWOPermission.email);
        assertTrue(false);
    }

    @Test(expected = APIException.class)
    public void ensureClientLevelPrivilegeThrowsIfNoPrivilege() {
        String email = "existing@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.ensureClientLevelPrivilege(client, email, ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT);
        assertTrue(false);
    }

    @Test
    public void ensureClientLevelPrivilegeDoesNotThrowIfUserHasPrivilege() {
        String email = "existing@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.addClientLevelPrivilege(client, email, ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT, User.SUPER_USER);

        User.ensureClientLevelPrivilege(client, email, ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT);
    }

    @Test(expected = APIException.class)
    public void ensureGroupLevelPrivilegeThrowsIfNoPrivilege() {
        String email = "existing@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.ensureGroupLevelPrivilege(client, sgp.groupId, email, GroupLevelPrivilege.SEND_EMAIL);
        assertTrue(false);
    }

    @Test
    public void ensureGroupLevelPrivilegeDoesNotThrowIfUserHasPrivilege() {
        String email = "existing@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.addGroupLevelPrivilege(client, sgp.groupId, email, GroupLevelPrivilege.SEND_EMAIL, User.SUPER_USER);

        User.ensureGroupLevelPrivilege(client, sgp.groupId, email, GroupLevelPrivilege.SEND_EMAIL);
    }

    @Test
    public void canClonePrivilages() {
        String email1 = "existing@dummy.com";
        User.create(client, email1, sgp.groupId, User.SUPER_USER);
        User.addClientLevelPrivilege(client, email1, ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT, User.SUPER_USER);

        User.addGroupLevelPrivilege(client, sgp.groupId, email1, GroupLevelPrivilege.SEND_EMAIL, User.SUPER_USER);
        User.addGroupLevelPrivilege(client, kl.groupId, email1, GroupLevelPrivilege.SEND_EMAIL, User.SUPER_USER);

        String email2 = "existing2@dummy.com";
        User.create(client, email2, kl.groupId, User.SUPER_USER);
        User.addClientLevelPrivilege(client, email2, ClientLevelPrivilege.UPDATE_CLIENT_CONTACT_EMAIL, User.SUPER_USER);
        User.addGroupLevelPrivilege(client, sgp.groupId, email2, GroupLevelPrivilege.UPDATE_LIST, User.SUPER_USER);

        User.clonePrivileges(client, email1, email2, User.SUPER_USER);

        UserProp userProp = User.safeGet(client, email2).toProp(client);
        assertEquals(email2, userProp.email);
        assertEquals(kl.groupId, userProp.groupId);
        assertTrue(userProp.clientLevelPrivileges.contains(ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT.toString()));
        assertEquals(1, userProp.clientLevelPrivileges.size());
        assertEquals(2, userProp.groupLevelPrivileges.size());
        assertTrue(userProp.groupLevelPrivileges.containsKey("Singapore"));
        assertEquals(1, userProp.groupLevelPrivileges.get("Singapore").size());
        assertTrue(userProp.groupLevelPrivileges.get("Singapore").contains(GroupLevelPrivilege.SEND_EMAIL.toString()));
        assertTrue(userProp.groupLevelPrivileges.containsKey("KL"));
        assertEquals(1, userProp.groupLevelPrivileges.get("KL").size());
        assertTrue(userProp.groupLevelPrivileges.get("KL").contains(GroupLevelPrivilege.SEND_EMAIL.toString()));
    }

    @Test(expected = APIException.class)
    public void userWOPermissionCannotClonePrivilages() {
        String email1 = "existing@dummy.com";
        User.create(client, email1, sgp.groupId, User.SUPER_USER);

        String email2 = "existing2@dummy.com";
        User.create(client, email2, kl.groupId, User.SUPER_USER);

        User.clonePrivileges(client, email1, email2, userWOPermission.email);
    }

    @Test(expected = APIException.class)
    public void ensureValidUserThrowsIfUserInvalid() {
        User.ensureValidUser(client, "somedummy@dummy.com");
        assertTrue(false);
    }

    @Test
    public void ensureValidUserDoesNotThrowIfUserValid() {
        User.ensureValidUser(client, userWOPermission.email);
        // no exception
    }

    @Test
    public void canAddClientLevelPrivilage() {
        String email = "existing@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.addClientLevelPrivilege(client, email, ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT, User.SUPER_USER);

        UserProp userProp = User.safeGet(client, email).toProp(client);
        assertEquals(email, userProp.email);
        assertTrue(userProp.clientLevelPrivileges.contains(ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT.toString()));
    }

    @Test
    public void canRemoveClientLevelPrivilage() {
        String email = "existing@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.addClientLevelPrivilege(client, email, ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT, User.SUPER_USER);

        UserProp userProp = User.safeGet(client, email).toProp(client);
        assertEquals(email, userProp.email);
        assertTrue(userProp.clientLevelPrivileges.contains(ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT.toString()));

        userProp = User.deleteClientLevelPrivilege(client, email, ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT,
                userWithPermission.email);
        assertEquals(email, userProp.email);
        assertEquals(0, userProp.clientLevelPrivileges.size());
    }

    @Test
    public void canAddGroupLevelPrivilage() {
        String email = "existing@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.addGroupLevelPrivilege(client, sgp.groupId, email, GroupLevelPrivilege.SEND_EMAIL,
                userWithPermission.email);

        UserProp userProp = User.safeGet(client, email).toProp(client);
        assertEquals(email, userProp.email);
        assertTrue(userProp.groupLevelPrivileges.containsKey("Singapore"));
        Set<String> privileges = userProp.groupLevelPrivileges.get("Singapore");
        assertEquals(1, privileges.size());
        assertTrue(privileges.contains(GroupLevelPrivilege.SEND_EMAIL.toString()));
    }

    @Test
    public void canRemoveGroupLevelPrivilage() {
        String email = "existing@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.addGroupLevelPrivilege(client, sgp.groupId, email, GroupLevelPrivilege.SEND_EMAIL,
                userWithPermission.email);

        UserProp userProp = User.safeGet(client, email).toProp(client);
        assertEquals(email, userProp.email);
        assertTrue(userProp.groupLevelPrivileges.containsKey("Singapore"));
        Set<String> privileges = userProp.groupLevelPrivileges.get("Singapore");
        assertEquals(1, privileges.size());
        assertTrue(privileges.contains(GroupLevelPrivilege.SEND_EMAIL.toString()));

        User.deleteGroupLevelPrivilege(client, sgp.groupId, email, GroupLevelPrivilege.SEND_EMAIL,
                userWithPermission.email);
        userProp = User.safeGet(client, email).toProp(client);
        assertTrue(userProp.groupLevelPrivileges.isEmpty());
    }

    @Test
    public void canAddMultipleClientAndGroupLevelPrivilages() {
        String email = "existing@dummy.com";
        User.create(client, email, sgp.groupId, User.SUPER_USER);

        User.addGroupLevelPrivilege(client, sgp.groupId, email, GroupLevelPrivilege.SEND_EMAIL,
                userWithPermission.email);
        User.addGroupLevelPrivilege(client, kl.groupId, email, GroupLevelPrivilege.SEND_EMAIL, userWithPermission.email);

        User.addClientLevelPrivilege(client, email, ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT, User.SUPER_USER);

        UserProp userProp = User.safeGet(client, email).toProp(client);
        assertTrue(userProp.groupLevelPrivileges.containsKey("Singapore"));
        assertTrue(userProp.groupLevelPrivileges.containsKey("KL"));

        assertTrue(userProp.groupLevelPrivileges.get("Singapore").contains(GroupLevelPrivilege.SEND_EMAIL.toString()));
        assertTrue(userProp.groupLevelPrivileges.get("KL").contains(GroupLevelPrivilege.SEND_EMAIL.toString()));

        assertTrue(userProp.clientLevelPrivileges.contains(ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT.toString()));
    }

    @Test
    public void ensureClientLevelPrivilegeAllowsSuperUser() {
        User.ensureClientLevelPrivilege(client, User.SUPER_USER, ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT);
        User.ensureClientLevelPrivilege(client, User.SUPER_USER, ClientLevelPrivilege.UPDATE_GROUP);
        User.ensureClientLevelPrivilege(client, User.SUPER_USER, ClientLevelPrivilege.UPDATE_USER);

        // no exception
    }

    @Test
    public void ensureGroupLevelPrivilegeAllowsSuperUser() {
        User.ensureGroupLevelPrivilege(client, sgp.groupId, User.SUPER_USER, GroupLevelPrivilege.SEND_EMAIL);
        User.ensureGroupLevelPrivilege(client, sgp.groupId, User.SUPER_USER, GroupLevelPrivilege.UPDATE_LIST);

        // no exception
    }

    @Test
    public void userWithPermissionCanAddApp() {
        String email = User.create(client, "newuser@dummy.com", sgp.groupId, User.SUPER_USER).email;

        User.addApp(client, email, App.CHECK_IN, userWithPermission.email);

        UserProp userProp = User.safeGet(client, email).toProp(client);
        assertEquals(1, userProp.apps.size());
        assertTrue(userProp.apps.contains(App.CHECK_IN.toString()));
    }

    @Test
    public void userWOPermissionCannotAddApp() {
        String email = User.create(client, "newuser@dummy.com", sgp.groupId, User.SUPER_USER).email;

        try {
            User.addApp(client, email, App.CHECK_IN, userWOPermission.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        UserProp userProp = User.safeGet(client, email).toProp(client);
        assertTrue(userProp.apps.isEmpty());
    }

    @Test
    public void userWithPermissionCanRemoveApp() {
        String email = User.create(client, "newuser@dummy.com", sgp.groupId, User.SUPER_USER).email;
        UserProp userProp = User.addApp(client, email, App.CHECK_IN, userWithPermission.email);
        assertEquals(1, userProp.apps.size());

        User.removeApp(client, email, App.CHECK_IN, userWithPermission.email);
        userProp = User.safeGet(client, email).toProp(client);
        assertTrue(userProp.apps.isEmpty());
    }

    @Test
    public void userWOPermissionCannotRemoveApp() {
        String email = User.create(client, "newuser@dummy.com", sgp.groupId, User.SUPER_USER).email;
        UserProp userProp = User.addApp(client, email, App.CHECK_IN, userWithPermission.email);
        assertEquals(1, userProp.apps.size());

        try {
            User.removeApp(client, email, App.CHECK_IN, userWOPermission.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        userProp = User.safeGet(client, email).toProp(client);
        assertEquals(1, userProp.apps.size());
    }

    @Test
    public void appsGetCloned() {
        String sourceEmail = User.create(client, "source@dummy.com", sgp.groupId, User.SUPER_USER).email;
        User.addApp(client, sourceEmail, App.CHECK_IN, userWithPermission.email);
        User.addApp(client, sourceEmail, App.INVENTORY, userWithPermission.email);

        String targetEmail = User.create(client, "target@dummy.com", sgp.groupId, User.SUPER_USER).email;

        User.clonePrivileges(client, sourceEmail, targetEmail, User.SUPER_USER);

        UserProp targetUserProp = User.safeGet(client, targetEmail).toProp(client);
        assertEquals(2, targetUserProp.apps.size());
        assertEquals(App.CHECK_IN.toString(), targetUserProp.apps.first());
        assertEquals(App.INVENTORY.toString(), targetUserProp.apps.last());
    }
}
