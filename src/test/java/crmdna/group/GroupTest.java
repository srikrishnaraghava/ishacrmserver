package crmdna.group;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.EmailConfig;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group.EmailType;
import crmdna.group.Group.GroupProp;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static crmdna.common.OfyService.ofy;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class GroupTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private final String validUser = "valid@login.com";
    private final String userWithPermission = "withpermission@login.com";
    GroupProp chennai;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        // can call getAll without any groups
        List<GroupProp> centers = Group.getAll(client, false);
        assertEquals(0, centers.size());

        chennai = Group.create(client, "Chennai", User.SUPER_USER);
        assertEquals(1, chennai.groupId);

        User.create(client, validUser, chennai.groupId, User.SUPER_USER);
        assertEquals(1, User.get(client, validUser).toProp(client).userId);

        User.create(client, userWithPermission, chennai.groupId,
                User.SUPER_USER);
        assertEquals(2,
                User.get(client, userWithPermission).toProp(client).userId);

        User.addClientLevelPrivilege(client, userWithPermission,
                ClientLevelPrivilege.UPDATE_GROUP, User.SUPER_USER);
        User.addClientLevelPrivilege(client, userWithPermission,
                ClientLevelPrivilege.UPDATE_USER, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void createTest() {

        GroupProp prop = Group.create(client, "Singapore", userWithPermission);
        assertTrue(prop.groupId == 2); // one already created in the set up
        // method

        prop = Group.safeGet(client, prop.groupId).toProp();
        assertEquals("singapore", prop.name);
        assertEquals("Singapore", prop.displayName);

        // cannot create duplicate
        try {
            Group.create("isha", "SINGAPORE", User.SUPER_USER);
            assertTrue(false);
        } catch (APIException e) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, e.statusCode);
        }

        // group ids should be in sequence
        prop = Group.create(client, "Malaysia/KL", userWithPermission);
        assertEquals(3, prop.groupId);
        prop = Group.create(client, "Australia/Sydney", userWithPermission);
        assertEquals(4, prop.groupId);
        prop = Group.create(client, "Australia/Melbourne", userWithPermission);
        assertEquals(5, prop.groupId);

        prop = Group.safeGet(client, 3).toProp();
        assertEquals("malaysia/kl", prop.name);
        prop = Group.safeGet(client, 4).toProp();
        assertEquals("Australia/Sydney", prop.displayName);
        prop = Group.safeGet(client, 5).toProp();
        assertEquals("Australia/Melbourne", prop.displayName);
        assertEquals("australia/melbourne", prop.name);

        // access control
        try {
            Group.create("isha", "Malaysia/Johor", validUser);
            assertTrue(false);
        } catch (APIException e) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, e.statusCode);
        }

        // client should be valid
        try {
            Group.create(invalidClient, "Singapore", User.SUPER_USER);
            assertTrue(false);
        } catch (APIException e) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, e.statusCode);
        }
    }

    @Test
    public void safeGetTest() {

        GroupProp group = Group.create(client, "Singapore", userWithPermission);
        assertTrue(group.groupId != 0);

        group = Group.safeGet("isha", group.groupId).toProp();
        assertEquals("singapore", group.name);
        assertEquals("Singapore", group.displayName);

        // exception for non existing group
        try {
            Group.safeGet("isha", group.groupId + 20939); // non existant
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void safeGetByIdOrNameTest() {

        GroupProp group = Group.create(client, "Singapore", userWithPermission);
        assertTrue(group.groupId != 0);

        group = Group.safeGetByIdOrName("isha", group.groupId + "").toProp();
        assertEquals("get by id", "singapore", group.name);
        assertEquals("get by id", "Singapore", group.displayName);

        // exception for non existing group
        try {
            Group.safeGetByIdOrName("isha", group.groupId + 20939 + ""); // non
            // existant
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        group = Group.safeGetByIdOrName("isha", "singapore").toProp();
        assertEquals("get by name", "singapore", group.name);
        assertEquals("get by name", "Singapore", group.displayName);
    }

    @Test
    public void ensureValidGroupIdsTest() {
        GroupProp group1 = Group.create(client, "group1", User.SUPER_USER);
        GroupProp group2 = Group.create(client, "group2", User.SUPER_USER);
        GroupProp group3 = Group.create(client, "group3", User.SUPER_USER);

        List<Long> groupIds = new ArrayList<>();
        groupIds.add(group1.groupId);
        groupIds.add(group2.groupId);
        groupIds.add(group3.groupId);

        Group.ensureValidGroupIds(client, groupIds); // no exception

        groupIds.add((long) 100);

        try {
            Group.ensureValidGroupIds(client, groupIds);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void renameTest() {
        GroupProp sgp = Group.create("isha", "Singapore", userWithPermission);
        Group.create("isha", "Sydney", userWithPermission);

        try {
            Group.rename("isha", sgp.groupId, "sydney", userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        // can change case and rename
        sgp = Group
                .rename("isha", sgp.groupId, "singapore", userWithPermission);
        sgp = Group.safeGet("isha", sgp.groupId).toProp();
        assertEquals("singapore", sgp.name);
        assertEquals("singapore", sgp.displayName);

        sgp = Group.rename("isha", sgp.groupId, "Sgp", userWithPermission);
        sgp = Group.safeGet("isha", sgp.groupId).toProp();
        assertEquals("sgp", sgp.name);
        assertEquals("Sgp", sgp.displayName);

        try {
            Group.rename("isha", sgp.groupId, "Sgp", validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

    }

    @Test
    public void getAllTest() {
        Group.create("isha", "Sydney", userWithPermission);
        Group.create("isha", "Singapore", userWithPermission);
        ObjectifyFilter.complete();

        List<GroupProp> groups = Group.getAll("isha", false);
        assertEquals(3, groups.size()); // chennai already created in set up
        // method
        // should be sorted
        assertEquals("singapore", groups.get(1).name);
        assertEquals("Singapore", groups.get(1).displayName);
        assertEquals("sydney", groups.get(2).name);
        assertEquals("Sydney", groups.get(2).displayName);
    }

    @Test
    public void getAllGroupIdsTest() {
        Set<Long> all = Group.getAllGroupIds("isha");

        assertEquals(1, all.size());
        assertTrue(all.contains(chennai.groupId));

        GroupProp sydney = Group.create("isha", "Sydney", userWithPermission);
        GroupProp sgp = Group.create("isha", "Singapore", userWithPermission);
        ObjectifyFilter.complete();

        all = Group.getAllGroupIds("isha");
        assertEquals(3, all.size());
        assertTrue(all.contains(chennai.groupId));
        assertTrue(all.contains(sydney.groupId));
        assertTrue(all.contains(sgp.groupId));
    }

    // public void deleteTest() {
    // GroupProp sgp = Group.create(client, "Sgp", User.SUPER_USER);
    // GroupProp kl = Group.create(client, "KL", User.SUPER_USER);
    //
    // // test permission
    // try {
    // Group.delete(client, sgp.groupId, validUser);
    // assertTrue(false);
    // } catch (APIException ex) {
    // assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
    // }
    //
    // UserProp userProp = User.create(client, "sgpuser@login.com",
    // sgp.groupId, User.SUPER_USER);
    // ContactProp contact = new ContactProp();
    // contact.email = "email1@email.com";
    // MemberProp memberProp = Member.create(client, sgp.groupId, contact,
    // false,
    // validUser);
    // Member.addOrDeleteGroup(client, memberProp.memberId, kl.groupId, true,
    // validUser);
    //
    // // There should not be any user for that center
    // try {
    // Group.delete(client, sgp.groupId, userWithPermission);
    // assertTrue(false);
    // } catch (APIException ex) {
    // assertEquals(Status.ERROR_PRECONDITION_FAILED, ex.statusCode);
    // }
    //
    // User.updateGroup(client, userProp.email, kl.groupId, userWithPermission);
    // assertEquals(0, User.getAllForGroup(client, sgp.groupId).size());
    //
    // // There should not be any member for that center
    // try {
    // Group.delete(client, sgp.groupId, userWithPermission);
    // assertTrue(false);
    // } catch (APIException ex) {
    // assertEquals(Status.ERROR_PRECONDITION_FAILED, ex.statusCode);
    // }
    //
    // Member.addOrDeleteGroup(client, memberProp.memberId, sgp.groupId,
    // false, validUser);
    // assertEquals(0, Member.getAllForGroup(client, sgp.groupId, validUser)
    // .size());
    //
    // assertEquals("force failure", true, false);
    // }

    @Test
    public void setAndGetPaypalApiCredentialsTest() {
        //privilage required to set api key
        try {
            Group.setPaypalApiCredentials(client, chennai.groupId, "api-login", "api-pwd",
                    "api-secret", false, false, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        User.addGroupLevelPrivilege(client, chennai.groupId, validUser,
                User.GroupLevelPrivilege.UPDATE_PAYMENT_CONFIG, User.SUPER_USER);

        Group.setPaypalApiCredentials(client, chennai.groupId, "api-login", "api-pwd",
                "api-secret", false, false, validUser);
        //no exception

        //privilege required to get api key
        try {
            Group.getPaypalApiCredentials(client, chennai.groupId, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        User.addClientLevelPrivilege(client, validUser,
                ClientLevelPrivilege.VIEW_API_KEY, User.SUPER_USER);
        PaypalApiCredentialsProp p = Group.getPaypalApiCredentials(client,
                chennai.groupId, validUser);
        assertEquals("api-login", p.login);
        assertEquals("api-pwd", p.pwd);
        assertEquals("api-secret", p.secret);
        assertEquals(false, p.sandbox);
        assertEquals(false, p.disable);
    }

    @Test
    public void setMandrillApiKeyTest() {
        GroupProp sgp = Group.create(client, "Sgp", User.SUPER_USER);

        // throws exception for invalid key
        try {
            Group.setMandrillApiKey(client, sgp.groupId, "invalid key",
                    User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        //this test will fail when not connected to internet
        Group.setMandrillApiKey(client, sgp.groupId, "SGDxaNgCVEF6trBYKtugag",
                User.SUPER_USER);
        EmailConfig emailConfig = Group.getEmailConfig(client, sgp.groupId,
                User.SUPER_USER);
        assertEquals("SGDxaNgCVEF6trBYKtugag", emailConfig.mandrillApiKey);
    }

    @Test
    public void addOrDeleteAllowedFromEmailTest() {
        GroupProp sgp = Group.create(client, "Sgp", User.SUPER_USER);

        Group.addOrDeleteAllowedEmailSender(client, sgp.groupId,
                "Singapore@ishayoga.org", "Isha Singapore", true,
                User.SUPER_USER);
        Group.addOrDeleteAllowedEmailSender(client, sgp.groupId,
                "Singapore@innerengineering.com", "Isha Singapore", true,
                User.SUPER_USER);

        EmailConfig emailConfig = Group.getEmailConfig(client, sgp.groupId,
                User.SUPER_USER);
        Map<String, String> emailVsName = emailConfig.allowedFromEmailVsName;
        assertEquals(2, emailVsName.size());
        assertEquals("Isha Singapore",
                emailVsName.get("singapore@ishayoga.org"));
        assertEquals("Isha Singapore",
                emailVsName.get("singapore@innerengineering.com"));

        // remove
        Group.addOrDeleteAllowedEmailSender(client, sgp.groupId,
                "SinGapore@ishayoga.org", null, false, User.SUPER_USER);
        emailConfig = Group
                .getEmailConfig(client, sgp.groupId, User.SUPER_USER);
        emailVsName = emailConfig.allowedFromEmailVsName;
        assertEquals(1, emailVsName.size());
    }

    @Test
    public void getEmailConfigTest() {
        Group.addOrDeleteAllowedEmailSender(client, chennai.groupId, "chennai@ishafoundation.org",
                "Isha Chennai", true, User.SUPER_USER);
        EmailConfig emailConfig = Group.getEmailConfig(client, chennai.groupId, validUser);

        assertEquals(emailConfig.mandrillApiKey, EmailConfig.TEXT_API_KEY_MASKED);
        assertEquals(1, emailConfig.allowedFromEmailVsName.size());
        assertEquals("Isha Chennai", emailConfig.allowedFromEmailVsName.get("chennai@ishafoundation.org"));
    }

    @Test
    public void setEmailTemplateTest() {
        GroupProp sgp = Group.create(client, "Sgp", User.SUPER_USER);

        Group.setEmailHtmlTemplate(client, sgp.groupId,
                EmailType.REGISTRATION_CONFIRMATION, "registration template",
                User.SUPER_USER);

        String htmlTemplate = Group.getEmailTemplate(client, sgp.groupId,
                EmailType.REGISTRATION_CONFIRMATION);
        assertEquals("registration template", htmlTemplate);

        Group.setEmailHtmlTemplate(client, sgp.groupId,
                EmailType.REGISTRATION_REMINDER, "reminder template",
                User.SUPER_USER);
        htmlTemplate = Group.getEmailTemplate(client, sgp.groupId,
                EmailType.REGISTRATION_REMINDER);
        assertEquals("reminder template", htmlTemplate);
    }

    @Test
    public void toPropTest() {
        GroupEntity entity = new GroupEntity();
        entity.groupId = 123l;
        entity.displayName = "Singapore";
        entity.name = entity.displayName.toLowerCase();

        GroupProp prop = entity.toProp();
        assertEquals(123, prop.groupId);
        assertEquals("Singapore", prop.displayName);
        assertEquals("singapore", prop.name);
    }

    @Test
    public void entityCanBeCopiedToAnotherClient() {
        String client1 = "client1";
        String client2 = "client2";
        Client.create(client1);
        Client.create(client2);

        GroupProp groupProp = Group.create(client1, "group1", User.SUPER_USER);
        assertEquals(1, groupProp.groupId);

        try {
            groupProp = Group.safeGet(client2, 1).toProp();
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        GroupEntity groupEntity = Group.safeGet(client1, 1);

        ofy(client2).save().entity(groupEntity).now();

        groupProp = Group.safeGet(client2, 1).toProp();
        assertEquals(1, groupProp.groupId);
        assertEquals("group1", groupProp.displayName);

        groupEntity = Group.safeGet(client1, 1);
        //entity still exists in client1
    }

    @Test
    public void safeGetSenderNameFromEmailTest() {
        String client = "isha";

        Group.addOrDeleteAllowedEmailSender(client, chennai.groupId, "chennai@ishayoga.org",
                "Isha Chennai", true, User.SUPER_USER);

        String name = Group.safeGetSenderNameFromEmail(client, chennai.groupId, "chennai@ishayoga.org");
        assertEquals("Isha Chennai", name);

        try {
            Group.safeGetSenderNameFromEmail(client, chennai.groupId, "dummy@dummy.com");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }
}
