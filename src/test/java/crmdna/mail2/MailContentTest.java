package crmdna.mail2;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.Lists;
import com.googlecode.objectify.ObjectifyFilter;
import com.microtripit.mandrillapp.lutung.model.MandrillApiError;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.list.ListHelper;
import crmdna.list.ListProp;
import crmdna.member.Member;
import crmdna.member.MemberProp;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;

import static org.junit.Assert.*;

public class MailContentTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private GroupProp sgp;
    private GroupProp kl;

    private UserProp userWithUpdatePrivilage;
    private UserProp validUser;
    private UserProp validUser2;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        kl = Group.create(client, "KL", User.SUPER_USER);
        assertEquals(2, kl.groupId);

        Group.addOrDeleteAllowedEmailSender(client, sgp.groupId,
                "singapore@ishayoga.org", "Isha Singapore", true,
                User.SUPER_USER);

        userWithUpdatePrivilage = User.create(client,
                "userwithpermission@invalid.com", sgp.groupId, User.SUPER_USER);
        User.addGroupLevelPrivilege(client, sgp.groupId,
                userWithUpdatePrivilage.email,
                GroupLevelPrivilege.UPDATE_MAIL_CONTENT, User.SUPER_USER);

        validUser = User.create(client, "validuser@invalid.com", sgp.groupId,
                User.SUPER_USER);
        validUser2 = User.create(client, "validuser2@invalid.com", sgp.groupId,
                User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void queryByOwner() throws InterruptedException {

        MailContentProp sepSathsang = MailContent.create(client, "title1",
                sgp.groupId, "sathsang on 6 sep", "we invite you...",
                validUser.email);

        Thread.sleep(200);
        MailContentProp octSathsang = MailContent.create(client, "title2",
                sgp.groupId, "sathsang on 4 oct", "we invite you...",
                validUser.email);

        List<MailContentEntity> entities = MailContent.query(client,
                validUser.email, null, null, User.SUPER_USER);

        assertEquals(2, entities.size());
        assertEquals(octSathsang.mailContentId, entities.get(0).mailContentId);
        assertEquals("sathsang on 4 oct", entities.get(0).subject);
        assertEquals("we invite you...", entities.get(0).body);
        assertEquals("title2", entities.get(0).displayName);

        assertEquals(sepSathsang.mailContentId, entities.get(1).mailContentId);
        assertEquals("sathsang on 6 sep", entities.get(1).subject);
        assertEquals("we invite you...", entities.get(1).body);
        assertEquals("title1", entities.get(1).displayName);
    }

    @Test
    public void queryByTS() throws InterruptedException {

        long ts1 = new Date().getTime();
        MailContentProp sepSathsang = MailContent.create(client,
                "sep sathsang", sgp.groupId, "sathsang on 6 sep",
                "we invite you...", userWithUpdatePrivilage.email);

        Thread.sleep(200);
        long ts2 = new Date().getTime();
        MailContentProp octSathsang = MailContent.create(client,
                "oct sathsang", sgp.groupId, "sathsang on 4 oct",
                "we invite you...", validUser.email);
        Thread.sleep(200);
        long ts3 = new Date().getTime();

        // only sep sathsang
        List<MailContentEntity> entities = MailContent.query(client, null, ts1,
                ts2, validUser.email);

        assertEquals(1, entities.size());
        assertEquals(sepSathsang.mailContentId, entities.get(0).mailContentId);
        assertEquals("sathsang on 6 sep", entities.get(0).subject);
        assertEquals("we invite you...", entities.get(0).body);

        // ts2 and later. only oct sathsang
        entities = MailContent.query(client, null, ts2, null,
                userWithUpdatePrivilage.email);
        assertEquals(1, entities.size());
        assertEquals(octSathsang.mailContentId, entities.get(0).mailContentId);
        assertEquals("sathsang on 4 oct", entities.get(0).subject);
        assertEquals("we invite you...", entities.get(0).body);

        // between ts1 and ts3. both sathsangs
        entities = MailContent.query(client, null, ts1, ts3,
                userWithUpdatePrivilage.email);
        assertEquals(2, entities.size());
    }

    @Test
    public void canCreateMailContentForGroup() {
        MailContentProp sepSathsang = MailContent.create(client,
                "Sep Sathsang", sgp.groupId, "sathsang on 6 sep",
                "we invite you...", validUser.email);

        sepSathsang = MailContent.safeGet(client, sepSathsang.mailContentId)
                .toProp();
        assertEquals(1, sepSathsang.mailContentId);
        assertEquals("Sep Sathsang", sepSathsang.displayName);
        assertEquals("sepsathsang", sepSathsang.name);
        assertEquals("sathsang on 6 sep", sepSathsang.subject);
        assertEquals("we invite you...", sepSathsang.body);
        assertEquals(sgp.groupId, sepSathsang.groupId);
    }

    @Test
    public void canUpdateMailContentForGroup() {
        MailContentProp sepSathsang = MailContent.create(client,
                "sep sathsang", sgp.groupId, "sathsang on 6 sep",
                "we invite you...", validUser.email);

        MailContent.update(client, sepSathsang.mailContentId, null,
                "sathsang on 6 sep 6pm", null, false, validUser.email);

        sepSathsang = MailContent.safeGet(client, sepSathsang.mailContentId)
                .toProp();
        assertEquals("sep sathsang", sepSathsang.displayName);
        assertEquals("sathsang on 6 sep 6pm", sepSathsang.subject);
        assertEquals("we invite you...", sepSathsang.body);
    }

    @Test
    public void userWithPermissionCanCreateMailContentForClient() {

        User.addClientLevelPrivilege(client, userWithUpdatePrivilage.email,
                ClientLevelPrivilege.UPDATE_MAIL_CONTENT, User.SUPER_USER);

        MailContentProp mailContentProp = MailContent.create(client,
                "Verification email", 0, "Please verify your account",
                "Please enter code...", userWithUpdatePrivilage.email);

        MailContent.safeGet(client, mailContentProp.mailContentId);
    }

    @Test
    public void userWOPermissionCannotCreateMailContentForClient() {

        try {
            MailContent.create(client, "Verification email", 0,
                    "Please verify your account", "Please enter code...",
                    validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }
    }

    @Test
    public void mailContentCannotBeUpdatedAfterSendingEmail()
            throws IOException, MandrillApiError {
        MailContentProp sepSathsang = MailContent.create(client,
                "sep sathsang", sgp.groupId, "sathsang on 5 sep",
                "we invite you...", validUser.email);

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        MailTest.suppressEmailInTestEnv();

        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = sepSathsang.mailContentId;
        msi.senderEmail = "singapore@IshaYogA.org";
        msi.suppressIfAlreadySent = false;

        List<SentMailEntity> entities = Mail.send(client, msi, mailMap, User.SUPER_USER);

        assertEquals(1, entities.size());

        ObjectifyFilter.complete();

        try {
            MailContent.update(client, sepSathsang.mailContentId, null,
                    "sathsang on 6 sep", "we invite you...", false,
                    validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_OPERATION_NOT_ALLOWED, ex.statusCode);
        }
    }

    @Test
    public void mailContentCanBeUpdatedAfterSendingEmailIfFlagIsTrue()
            throws IOException, MandrillApiError {
        MailContentProp sepSathsang = MailContent.create(client,
                "sep sathsang", sgp.groupId, "sathsang on 5 sep",
                "we invite you...", validUser.email);

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        MailTest.suppressEmailInTestEnv();

        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = sepSathsang.mailContentId;
        msi.senderEmail = "singapore@IshaYogA.org";
        msi.suppressIfAlreadySent = false;

        List<SentMailEntity> entities = Mail.send(client, msi, mailMap, User.SUPER_USER);

        assertEquals(1, entities.size());

        ObjectifyFilter.complete();

        MailContent.update(client, sepSathsang.mailContentId, null,
                "sathsang on 6 sep", "we cordially invite you...", true,
                validUser.email);
        // no exception
        sepSathsang = MailContent.safeGet(client, sepSathsang.mailContentId)
                .toProp();
        assertEquals("sathsang on 6 sep", sepSathsang.subject);
        assertEquals("we cordially invite you...", sepSathsang.body);
    }

    @Test
    public void mailContentCannotBeDeletedAfterSendingEmail()
            throws MandrillApiError, IOException {
        MailContentProp sepSathsang = MailContent.create(client,
                "sep sathsang", sgp.groupId, "sathsang on 5 sep",
                "we invite you...", validUser.email);

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        MailTest.suppressEmailInTestEnv();

        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = sepSathsang.mailContentId;
        msi.senderEmail = "singapore@IshaYogA.org";
        msi.suppressIfAlreadySent = false;

        List<SentMailEntity> entities = Mail.send(client, msi, mailMap, User.SUPER_USER);

        assertEquals(1, entities.size());

        ObjectifyFilter.complete();

        try {
            MailContent.delete(client, sepSathsang.mailContentId,
                    validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_OPERATION_NOT_ALLOWED, ex.statusCode);
        }
    }

    @Test
    public void mailContentCanBeDeletedByPrivilagedUser() {
        MailContentProp sepSathsang = MailContent.create(client,
                "sep sathsang", sgp.groupId, "sathsang on 5 sep",
                "we invite you...", validUser.email);

        MailContent.delete(client, sepSathsang.mailContentId,
                userWithUpdatePrivilage.email);

        ObjectifyFilter.complete();

        // MailContent should be deleted
        try {
            MailContent.safeGet(client, sepSathsang.mailContentId);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

    }

    @Test
    public void mailContentCanBeDeletedByOwner() {
        MailContentProp sepSathsang = MailContent.create(client,
                "sep sathsang", sgp.groupId, "sathsang on 5 sep",
                "we invite you...", validUser.email);

        MailContent.delete(client, sepSathsang.mailContentId, validUser.email);

        ObjectifyFilter.complete();

        // MailContent should be deleted
        try {
            MailContent.safeGet(client, sepSathsang.mailContentId);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test(expected = APIException.class)
    public void displayNameUniqueForGroup() {
        MailContent.create(client, "sep sathsang", sgp.groupId,
                "sathsang on 5 sep", "we invite you...",
                userWithUpdatePrivilage.email);

        MailContent.create(client, "sep sathsang", sgp.groupId,
                "sathsang on 5 sep", "we cordially invite you...",
                userWithUpdatePrivilage.email);
    }

    @Test
    public void displayNameCanRepeatInADifferentGroup() {
        MailContent.create(client, "sep sathsang", sgp.groupId,
                "sathsang on 5 sep", "we invite you...",
                userWithUpdatePrivilage.email);

        MailContent.create(client, "sep sathsang", kl.groupId,
                "sathsang on 5 sep", "we cordially invite you...",
                userWithUpdatePrivilage.email);

        // no exception
    }

    @Test(expected = APIException.class)
    public void displayNameCannotBeUpdatedToExisting() {
        MailContentProp sepSathsang = MailContent.create(client,
                "sep sathsang", sgp.groupId, "sathsang on 5 sep",
                "we invite you...", userWithUpdatePrivilage.email);

        MailContent.create(client, "oct sathsang", sgp.groupId,
                "sathsang on 5 sep", "we cordially invite you...",
                userWithUpdatePrivilage.email);

        MailContent.update(client, sepSathsang.mailContentId, "oct sathsang",
                null, null, false, userWithUpdatePrivilage.email);
    }

    @Test
    public void displayNameSetForBespokeEmail() throws MandrillApiError,
            IOException {
        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        String from = "sathyanarayanant@gmail.com";
        MailTest.suppressEmailInTestEnv();
        Group.addOrDeleteAllowedEmailSender(client, sgp.groupId, from,
                "Sathya t", true, User.SUPER_USER);

        List<SentMailEntity> entities = Mail.sendBespoke(client, sgp.groupId,
                mailMap, "test subject", "test body", from, null,
                User.SUPER_USER);
        assertEquals(1, entities.size());
        MailContentProp mailContentProp = MailContent.safeGet(client,
                entities.get(0).mailContentId).toProp();
        assertTrue(mailContentProp.displayName.contains("Bespoke_"));
    }

    @Test(expected = APIException.class)
    public void onlyValidUserCanCreateMailContent() {
        MailContent.create(client, "Test mail", sgp.groupId, "test", "test",
                "invalid@invalid.com");
    }

    @Test
    public void privilageRequiredToUpdateMailContentCreatedBySomeoneElse() {
        MailContentProp sepSathsang = MailContent.create(client,
                "sep sathsang", sgp.groupId, "sathsang on 5 sep",
                "we invite you...", validUser.email);

        try {
            MailContent.update(client, sepSathsang.mailContentId,
                    "Sep Sathsang", "Sathsang on 6 sep",
                    "We cordially invite you...", false, validUser2.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        MailContent.update(client, sepSathsang.mailContentId, "Sep Sathsang",
                "Sathsang on 6 sep", "We cordially invite you...", false,
                userWithUpdatePrivilage.email);
        sepSathsang = MailContent.safeGet(client, sepSathsang.mailContentId)
                .toProp();
        assertEquals("Sep Sathsang", sepSathsang.displayName);
        assertEquals("Sathsang on 6 sep", sepSathsang.subject);
        assertEquals("We cordially invite you...", sepSathsang.body);
    }

    @Test
    public void privilageRequiredToDeleteMailContentCreatedBySomeoneElse() {
        MailContentProp sepSathsang = MailContent.create(client,
                "sep sathsang", sgp.groupId, "sathsang on 5 sep",
                "we invite you...", validUser.email);

        try {
            MailContent.delete(client, sepSathsang.mailContentId,
                    validUser2.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        MailContent.delete(client, sepSathsang.mailContentId,
                userWithUpdatePrivilage.email);
        try {
            MailContent.safeGet(client, sepSathsang.mailContentId).toProp();
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void canGetByName() {

        MailContentProp mailContentProp = MailContent.create(client,
                "Account Verification", sgp.groupId, "Account verification",
                "Please verify..", User.SUPER_USER);

        MailContentEntity mailContentEntity = MailContent.getByName(client,
                "Account verification", sgp.groupId);
        assertEquals(mailContentProp.mailContentId,
                mailContentEntity.mailContentId);
    }

    @Test
    public void getByNameReturnsNullIfNameDoesNotExist() {
        MailContentEntity mailContentEntity = MailContent.getByName(client,
                "Dummy", sgp.groupId);
        assertNull(mailContentEntity);
    }


}
