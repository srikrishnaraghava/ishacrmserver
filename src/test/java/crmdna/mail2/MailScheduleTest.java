package crmdna.mail2;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.list.List;
import crmdna.list.ListProp;
import crmdna.member.Member;
import crmdna.member.MemberProp;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class MailScheduleTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private GroupProp sgp;
    private GroupProp kl;

    private UserProp userWithUpdatePrivilage;
    private UserProp validUser;
    private UserProp userWithSgpEmailPrivilage;

    private MailContentProp newsletter;
    private ListProp newsletterListSgp;
    private ListProp ishakriyaListSgp;
    private ListProp newsletterListKl;

    private MemberProp sharmila, patma;

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
                GroupLevelPrivilege.UPDATE_MAIL_SCHEDULE, User.SUPER_USER);

        userWithSgpEmailPrivilage = User.create(client, "userwithsgpemail@invalid.com", sgp.groupId,
                User.SUPER_USER);
        User.addGroupLevelPrivilege(client, sgp.groupId, userWithSgpEmailPrivilage.email,
                GroupLevelPrivilege.SEND_EMAIL, User.SUPER_USER);

        validUser = User.create(client, "validuser@invalid.com", sgp.groupId,
                User.SUPER_USER);

        newsletter = MailContent.create(client, "Newsletter", sgp.groupId, "Mail content subject",
                "Mail content body", User.SUPER_USER);

        newsletterListSgp = List.createPublic(client, sgp.groupId, "Newsletter sgp list", User.SUPER_USER);
        ishakriyaListSgp = List.createPublic(client, sgp.groupId, "Isha kriya sgp list", User.SUPER_USER);
        newsletterListKl = List.createPublic(client, kl.groupId, "Newsletter kl list", User.SUPER_USER);

        ContactProp c = new ContactProp();
        c.asOfyyyymmdd = 20151118;
        c.firstName = "Sharmila";
        c.email = "sharmila@sharmila.com";
        sharmila = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);
        Member.subscribeList_to_be_removed(client, sharmila.memberId, ishakriyaListSgp.listId, User.SUPER_USER);
        Member.subscribeList_to_be_removed(client, sharmila.memberId, newsletterListSgp.listId, User.SUPER_USER);

        c = new ContactProp();
        c.asOfyyyymmdd = 20151118;
        c.firstName = "Patma";
        c.email = "patma@patma.com";
        patma = Member.create(client, kl.groupId, c, false, User.SUPER_USER);
        Member.subscribeList_to_be_removed(client, patma.memberId, newsletterListKl.listId, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test(expected = APIException.class)
    public void cannotScheduleEmailForInvalidClient() {
        Date after24Hours = new Date(new Date().getTime() + 86400 * 1000);
        MailSchedule.create("invalidclient", 1, after24Hours, newsletterListSgp.listId,
                "Isha", "Isha", User.SUPER_USER);
    }

    @Test
    public void userWOPermissionCannotScheduleEmail() {
        try {
            Date after24Hours = new Date(new Date().getTime() + 86400 * 1000);
            MailSchedule.create(client, newsletter.mailContentId, after24Hours,
                    newsletterListSgp.listId, "Isha", "Isha", validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }
    }

    @Test
    public void cannotScheduleForTimeInPast() {
        try {
            Date before24Hours = new Date(new Date().getTime() - 86400 * 1000);
            MailSchedule.create(client, newsletter.mailContentId, before24Hours,
                    newsletterListSgp.listId, "Isha", "Isha", User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void cannotCreateScheduledEmailForInvalidList() {
        try {
            Date after24Hours = new Date(new Date().getTime() + 86400 * 1000);
            long invalidListId = 1098;
            MailSchedule.create(client, newsletter.mailContentId, after24Hours,
                    invalidListId, "Isha", "Isha", User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void cannotCreateScheduleIfListHasNoMembers() {
        Date after24Hours = new Date(new Date().getTime() + 86400 * 1000);

        ListProp listProp = List.createPublic(client, sgp.groupId, "List", User.SUPER_USER);
        assertNotNull(listProp);

        try {
            MailSchedule.create(client, newsletter.mailContentId, after24Hours,
                    listProp.listId, "Isha", "Isha", User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void userWithPermissionCanScheduleEmail() {
        Date after24Hours = new Date(new Date().getTime() + 86400 * 1000);

        MailScheduleProp mailScheduleProp = MailSchedule.create(client, newsletter.mailContentId, after24Hours,
                newsletterListSgp.listId, "Isha", "Isha", userWithUpdatePrivilage.email);

        mailScheduleProp = MailSchedule.safeGet(client, mailScheduleProp.mailScheduleId);

        //check all fields populated correctly
        assertEquals(1, mailScheduleProp.mailScheduleId);
        assertEquals(newsletter.mailContentId, mailScheduleProp.mailContentId);
        assertEquals(after24Hours, mailScheduleProp.scheduledTime);
        assertEquals(1, mailScheduleProp.listIds.size());
        assertTrue(mailScheduleProp.listIds.contains(newsletterListSgp.listId));
        assertEquals(userWithUpdatePrivilage.email, mailScheduleProp.owner);
        assertEquals(newsletterListSgp.groupId, mailScheduleProp.groupId);
        assertEquals(false, mailScheduleProp.cancelled);
        assertEquals(false, mailScheduleProp.sendAttempted);
        assertNull(mailScheduleProp.sendAttemptedTime);
        assertNull(mailScheduleProp.sendSuccess);
        assertNull(mailScheduleProp.failureReason);
        assertEquals(mailScheduleProp.subject, MailContent.safeGet(client, newsletter.mailContentId).subject);
    }

    @Test
    public void mailScheduleIdAssignedInSequence() {
        Date after1Day = new Date(new Date().getTime() + 86400 * 1000);

        Set<Long> listIds = new HashSet<>();
        listIds.add(newsletterListSgp.listId);
        MailScheduleProp mailScheduleProp = MailSchedule.create(client, newsletter.mailContentId, after1Day,
                newsletterListSgp.listId, "Isha", "Isha", User.SUPER_USER);
        assertEquals(1, mailScheduleProp.mailScheduleId);

        Date after3Days = new Date(new Date().getTime() + 3 * 24 * 3600 * 1000);
        mailScheduleProp = MailSchedule.create(client, newsletter.mailContentId, after3Days,
                newsletterListSgp.listId, "Isha", "Isha", User.SUPER_USER);
        assertEquals(2, mailScheduleProp.mailScheduleId);

        Date after5Days = new Date(new Date().getTime() + 5 * 24 * 3600 * 1000);
        mailScheduleProp = MailSchedule.create(client, newsletter.mailContentId, after5Days,
                newsletterListSgp.listId, "Isha", "Isha", User.SUPER_USER);
        assertEquals(3, mailScheduleProp.mailScheduleId);
    }


    @Test
    public void canCancelOnesOwnScheduledEmail() {
        Date after24Hours = new Date(new Date().getTime() + 86400 * 1000);
        MailScheduleProp mailScheduleProp = MailSchedule.create(client, newsletter.mailContentId, after24Hours,
                newsletterListSgp.listId, "Isha", "Isha", userWithSgpEmailPrivilage.email);
        assertEquals(1, mailScheduleProp.mailScheduleId);

        MailSchedule.cancel(client, mailScheduleProp.mailScheduleId, userWithSgpEmailPrivilage.email);

        mailScheduleProp = MailSchedule.safeGet(client, mailScheduleProp.mailScheduleId);
        assertTrue(mailScheduleProp.cancelled);
    }

    @Test
    public void canUndoCancelOnesOwnScheduledEmail() {
        Date after24Hours = new Date(new Date().getTime() + 86400 * 1000);
        MailScheduleProp mailScheduleProp = MailSchedule.create(client, newsletter.mailContentId, after24Hours,
                newsletterListSgp.listId, "Isha", "Isha", userWithSgpEmailPrivilage.email);
        assertEquals(1, mailScheduleProp.mailScheduleId);

        MailSchedule.cancel(client, mailScheduleProp.mailScheduleId, userWithSgpEmailPrivilage.email);

        mailScheduleProp = MailSchedule.safeGet(client, mailScheduleProp.mailScheduleId);
        assertTrue(mailScheduleProp.cancelled);

        MailSchedule.undoCancel(client, mailScheduleProp.mailScheduleId, userWithSgpEmailPrivilage.email);
        mailScheduleProp = MailSchedule.safeGet(client, mailScheduleProp.mailScheduleId);
        assertFalse(mailScheduleProp.cancelled);
    }

    @Test
    public void userWOPermissionCannotCancelSomeonesScheduledEmail() {
        Date after24Hours = new Date(new Date().getTime() + 86400 * 1000);
        MailScheduleProp mailScheduleProp = MailSchedule.create(client, newsletter.mailContentId, after24Hours,
                newsletterListSgp.listId, "Isha", "Isha", userWithSgpEmailPrivilage.email);
        assertEquals(1, mailScheduleProp.mailScheduleId);

        try {
            MailSchedule.cancel(client, mailScheduleProp.mailScheduleId, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }
    }

    @Test
    public void userWithPermissionCanCancelSomeonesScheduledEmail() {
        Date after24Hours = new Date(new Date().getTime() + 86400 * 1000);
        MailScheduleProp mailScheduleProp = MailSchedule.create(client, newsletter.mailContentId, after24Hours,
                newsletterListSgp.listId, "Isha", "Isha", userWithSgpEmailPrivilage.email);
        assertEquals(1, mailScheduleProp.mailScheduleId);

        MailSchedule.cancel(client, mailScheduleProp.mailScheduleId, userWithUpdatePrivilage.email);

        mailScheduleProp = MailSchedule.safeGet(client, mailScheduleProp.mailScheduleId);
        assertTrue(mailScheduleProp.cancelled);
    }

    @Test
    public void cannotCancelIfAlreadyPickedUpForSending() {
        assertTrue(false);
    }

    @Test
    public void canGetAllOutstandingScheduledEmails() {
        assertTrue(false);
    }

    @Test
    public void canGetAllScheduledEmailsForAGroup() {
        assertTrue(false);
    }

    @Test
    public void scheduledEmailSentOutOnlyOnce() {
        assertTrue(false);
    }

    @Test
    public void notSentIfCancelled() {
        assertTrue(false);
    }

    @Test
    public void sentAtScheduledTimeIfNotCancelled() throws InterruptedException {
        Date after10Ms = new Date(new Date().getTime() + 10);
        MailScheduleProp mailScheduleProp = MailSchedule.create(client, newsletter.mailContentId, after10Ms,
                newsletterListSgp.listId, "Isha", "Isha", userWithSgpEmailPrivilage.email);

        Thread.sleep(200);

        MailTest.suppressEmailInTestEnv();
        MailSchedule.processScheduledEmails(client, new Date());

        mailScheduleProp = MailSchedule.safeGet(client, mailScheduleProp.mailScheduleId);
        assertTrue(mailScheduleProp.sendAttempted);

        SentMailQueryCondition smqc = new SentMailQueryCondition();
        smqc.email = sharmila.contact.email;
        smqc.mailContentId = newsletter.mailContentId;
        java.util.List<SentMailEntity> sentMailEntities = Mail.queryEntitiesSortedByTimeDesc(client, smqc, User.SUPER_USER);
        assertEquals(1, sentMailEntities.size());
    }

    @Test
    public void reportSentToOwnerAfterMailSent() {
        assertTrue(false);
    }

    @Test
    public void reportSentToOwnerAfterError() {
        assertTrue(false);
    }
}
