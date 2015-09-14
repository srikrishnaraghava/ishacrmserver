package crmdna.mail2;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.Sets;
import com.googlecode.objectify.ObjectifyFilter;
import com.microtripit.mandrillapp.lutung.model.MandrillApiError;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.EmailConfig;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.list.*;
import crmdna.member.Member;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.member.MemberQueryCondition;
import crmdna.program.Program;
import crmdna.programtype.ProgramType;
import crmdna.teacher.Teacher;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserProp;
import crmdna.venue.Venue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.List;

import static crmdna.common.AssertUtils.ensureEqual;
import static org.junit.Assert.*;

public class MailTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private GroupProp sgp;
    private UserProp userWithPermssion;
    private UserProp userWOPermssion;

    public static void suppressEmailInTestEnv() {
        System.setProperty(Mail.SYSTEM_PROPERTY_SUPPRESS_EMAIL, "TRUE");
    }

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        EmailConfig emailConfig = Group.addOrDeleteAllowedEmailSender(client, sgp.groupId, "SinGapore@IshayoGa.org",
                "Isha Singapore", true, User.SUPER_USER);
        assertEquals(1, emailConfig.allowedFromEmailVsName.size());
        assertEquals("Isha Singapore", emailConfig.allowedFromEmailVsName.get("singapore@ishayoga.org"));
        userWithPermssion = User.create(client, "userwithpermission@invalid.com", sgp.groupId, User.SUPER_USER);
        User.addGroupLevelPrivilege(client, sgp.groupId, userWithPermssion.email, GroupLevelPrivilege.SEND_EMAIL,
                User.SUPER_USER);

        userWOPermssion = User.create(client, "userwithoutpermission@invalid.com", sgp.groupId, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
        System.clearProperty(Mail.SYSTEM_PROPERTY_SUPPRESS_EMAIL);
    }

    @Test
    public void sendEmailToNewMember() throws MandrillApiError, IOException {

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");
        String from = "singapore@ishayoga.org";
        suppressEmailInTestEnv();
        List<SentMailEntity> sentMailEntities = Mail.sendBespoke(client, sgp.groupId, mailMap,
                "update member profile", "member profile", from, Utils.getSet("singapore", "memberprofile"),
                User.SUPER_USER);
        ensureEqual(1, sentMailEntities.size());
        ObjectifyFilter.complete();

        SentMailEntity sentMailEntity = Mail.safeGet(client, sentMailEntities.get(0).sentMailId);
        assertEquals(from, sentMailEntity.from);
        assertEquals("sathya.t@ishafoundation.org", sentMailEntity.email);
        MailContentEntity mailContentEntity = MailContent.safeGet(client, sentMailEntity.mailContentId);
        assertEquals("update member profile", mailContentEntity.subject);
        assertEquals("member profile", mailContentEntity.body);

        MemberProp memberProp = MemberLoader.safeGet(client, sentMailEntity.memberId, User.SUPER_USER).toProp();
        assertEquals("Sathya", memberProp.contact.firstName);
        assertEquals("Thilakan", memberProp.contact.lastName);
        assertEquals(1, memberProp.groupIds.size());
        assertTrue(memberProp.groupIds.contains(sgp.groupId));

        Set<String> tags = TagSet.safeGet(client, sentMailEntity.tagSetId).tags;
        assertEquals(2, tags.size());
        assertTrue(tags.contains("singapore"));
        assertTrue(tags.contains("memberprofile"));
    }

    @Test
    public void cannotSendEmailFromInvalidFrom() throws MandrillApiError, IOException {

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        String from = "sathyanarayanant@gmail.com";
        suppressEmailInTestEnv();

        try {
            Mail.sendBespoke(client, sgp.groupId, mailMap, "update member profile", "member profile", from,
                    Utils.getSet("singapore", "memberprofile"), User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void mailIdShowsNS() throws MandrillApiError, IOException {

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        suppressEmailInTestEnv();
        String from = "singapore@ishayoga.org";
        long currentMS = new Date().getTime();

        SentMailEntity sentMailEntity = Mail.sendBespoke(client, sgp.groupId, mailMap, "update member profile",
                "member profile", from, Utils.getSet("singapore", "memberprofile"), User.SUPER_USER).get(0);

        final int MILLION = 1000000;
        assertTrue(sentMailEntity.sentMailId >= currentMS * MILLION);
        assertTrue(sentMailEntity.sentMailId < (currentMS + 20) * MILLION);

        ObjectifyFilter.complete();
    }

    @Test
    public void sendEmailToExistingMember() throws MandrillApiError, IOException {

        ContactProp contact = new ContactProp();
        contact.asOfyyyymmdd = 20140823;
        contact.email = "sathya.t@ishafoundation.org";
        contact.firstName = "Sathyanarayanan";
        MemberProp sathya = Member.create(client, sgp.groupId, contact, false, User.SUPER_USER);
        assertEquals(1, sathya.memberId);

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        String from = "singapore@ishayoga.org";
        suppressEmailInTestEnv();
        List<SentMailEntity> sentMailEntities = Mail.sendBespoke(client, sgp.groupId, mailMap,
                "update member profile", "member profile", from, Utils.getSet("singapore", "memberprofile"),
                User.SUPER_USER);
        ensureEqual(1, sentMailEntities.size());

        SentMailEntity sentMailEntity = Mail.safeGet(client, sentMailEntities.get(0).sentMailId);
        assertEquals(from, sentMailEntity.from);
        assertEquals("sathya.t@ishafoundation.org", sentMailEntity.email);
        MailContentEntity mailContentEntity = MailContent.safeGet(client, sentMailEntity.mailContentId);
        assertEquals("update member profile", mailContentEntity.subject);
        assertEquals("member profile", mailContentEntity.body);

        MemberProp memberProp = MemberLoader.safeGet(client, sentMailEntity.memberId, User.SUPER_USER).toProp();
        assertEquals(sathya.memberId, memberProp.memberId);

        // first name and last name should not get updated
        assertEquals("Sathyanarayanan", memberProp.contact.firstName);
        assertEquals(null, memberProp.contact.lastName);
        assertEquals(1, memberProp.groupIds.size());
        assertTrue(memberProp.groupIds.contains(sgp.groupId));

        Set<String> tags = TagSet.safeGet(client, sentMailEntity.tagSetId).tags;
        assertEquals(2, tags.size());
        assertTrue(tags.contains("singapore"));
        assertTrue(tags.contains("memberprofile"));
    }

    @Test
    public void isEmailSuppressedTest() {
        assertTrue(!Mail.isEmailSuppressed());

        System.setProperty(Mail.SYSTEM_PROPERTY_SUPPRESS_EMAIL, "TRUE");
        assertTrue(Mail.isEmailSuppressed());

        System.clearProperty(Mail.SYSTEM_PROPERTY_SUPPRESS_EMAIL);
        assertTrue(!Mail.isEmailSuppressed());

        System.setProperty(Mail.SYSTEM_PROPERTY_SUPPRESS_EMAIL, "trUe");
        assertTrue(Mail.isEmailSuppressed());

        System.setProperty(Mail.SYSTEM_PROPERTY_SUPPRESS_EMAIL, "1");
        assertTrue(Mail.isEmailSuppressed());
    }

    @Test
    public void stagsTagIsCaseInsensitive() throws MandrillApiError, IOException {

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        suppressEmailInTestEnv();
        String from = "singapore@isHayoga.org";
        Mail.sendBespoke(client, sgp.groupId, mailMap, "update member profile", "member profile", from,
                Utils.getSet("singapore", "memberprofile"), User.SUPER_USER).get(0);

        ObjectifyFilter.complete();

        MailStatsProp mailStatsProp = Mail.getStatsByTag(client, Utils.getSet("sinGapore"));
        assertEquals(1, mailStatsProp.numRecipientsSendAttempted);
    }

    @Test
    public void multipleStatsTags() throws MandrillApiError, IOException {

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        suppressEmailInTestEnv();
        String from = "singapore@ishayoga.org";

        Mail.sendBespoke(client, sgp.groupId, mailMap, "update member profile", "member profile", from,
                Utils.getSet("singapore", "memberprofile"), User.SUPER_USER).get(0);

        Mail.sendBespoke(client, sgp.groupId, mailMap, "registration", "thanks for registration", from,
                Utils.getSet("singapore", "registration"), User.SUPER_USER).get(0);

        MailMap mailMap2 = new MailMap();
        mailMap2.add("thulasidhar@gmail.com", "Thulasi", "Thilakan");

        Mail.sendBespoke(client, sgp.groupId, mailMap, "update member profile", "member profile", from,
                Utils.getSet("singapore", "memberprofile"), User.SUPER_USER).get(0);

        ObjectifyFilter.complete();

        MailStatsProp mailStatsProp = Mail.getStatsByTag(client, Utils.getSet("sinGapore"));
        assertEquals(3, mailStatsProp.numRecipientsSendAttempted);

        mailStatsProp = Mail.getStatsByTag(client, Utils.getSet("sinGapore", "Memberprofile"));
        assertEquals(2, mailStatsProp.numRecipientsSendAttempted);

        mailStatsProp = Mail.getStatsByTag(client, Utils.getSet("sinGapore", "registration"));
        assertEquals(1, mailStatsProp.numRecipientsSendAttempted);

        mailStatsProp = Mail.getStatsByTag(client, Utils.getSet("memberProFile"));
        assertEquals(2, mailStatsProp.numRecipientsSendAttempted);
    }

    @Test
    public void nonExistantTags() {
        MailStatsProp mailStatsProp = Mail.getStatsByTag(client, Utils.getSet("sinGapore", "kl"));
        assertEquals(0, mailStatsProp.numRecipientsSendAttempted);
    }

    @Test
    public void mailContentIdSavedCorrectly() throws MandrillApiError, IOException {
        long mailContentId = MailContent.create(client, "test 1", sgp.groupId, "test subject", "test body",
                User.SUPER_USER).mailContentId;
        assertEquals("test subject", MailContent.safeGet(client, mailContentId).subject);
        assertEquals("test body", MailContent.safeGet(client, mailContentId).body);
        assertEquals("test 1", MailContent.safeGet(client, mailContentId).displayName);

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");
        mailMap.add("thulasidhar@gmail.com", "Thulasidhar", "Kosalram");

        String from = "singapore@ishayoga.org";

        long currentNs = new Date().getTime() * 1000000;
        suppressEmailInTestEnv();

        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = mailContentId;
        msi.senderEmail = from;
        msi.suppressIfAlreadySent = true;
        msi.tags = Utils.getSet("singapore", "memberprofile");

        Mail.send(client, msi, mailMap, userWithPermssion.email);

        ObjectifyFilter.complete();
        SentMailQueryCondition qc = new SentMailQueryCondition();
        qc.mailContentId = mailContentId;

        List<SentMailEntity> sentMailEntities = Mail.queryEntitiesSortedByTimeDesc(client, qc, User.SUPER_USER);
        assertEquals(2, sentMailEntities.size());

        SentMailEntity sathyaSentMail;
        SentMailEntity thulasiSentMail;
        if (sentMailEntities.get(0).email.equals("sathya.t@ishafoundation.org")) {
            sathyaSentMail = sentMailEntities.get(0);
            thulasiSentMail = sentMailEntities.get(1);
        } else {
            sathyaSentMail = sentMailEntities.get(1);
            thulasiSentMail = sentMailEntities.get(0);
        }

        SentMailEntity sentMailEntity = sathyaSentMail;
        assertTrue(sentMailEntity.sentMailId >= currentNs);
        assertEquals(mailContentId, sentMailEntity.mailContentId);
        assertTrue(TagSet.safeGet(client, sentMailEntity.tagSetId).tags.contains("singapore"));
        assertTrue(TagSet.safeGet(client, sentMailEntity.tagSetId).tags.contains("memberprofile"));
        assertEquals("Sathya",
                MemberLoader.safeGet(client, sentMailEntity.memberId, User.SUPER_USER).toProp().contact.firstName);
        assertEquals("Thilakan",
                MemberLoader.safeGet(client, sentMailEntity.memberId, User.SUPER_USER).toProp().contact.lastName);
        assertEquals("sathya.t@ishafoundation.org",
                MemberLoader.safeGet(client, sentMailEntity.memberId, User.SUPER_USER).toProp().contact.email);

        sentMailEntity = thulasiSentMail;
        assertTrue(sentMailEntity.sentMailId >= currentNs);
        assertEquals(mailContentId, sentMailEntity.mailContentId);
        assertTrue(TagSet.safeGet(client, sentMailEntity.tagSetId).tags.contains("singapore"));
        assertTrue(TagSet.safeGet(client, sentMailEntity.tagSetId).tags.contains("memberprofile"));
        assertEquals("Thulasidhar",
                MemberLoader.safeGet(client, sentMailEntity.memberId, User.SUPER_USER).toProp().contact.firstName);
        assertEquals("Kosalram",
                MemberLoader.safeGet(client, sentMailEntity.memberId, User.SUPER_USER).toProp().contact.lastName);
        assertEquals("thulasidhar@gmail.com", MemberLoader.safeGet(client, sentMailEntity.memberId, User.SUPER_USER)
                .toProp().contact.email);
    }

    @Test
    public void repeatEmailNotSentWhenSuppressed() throws MandrillApiError, IOException {
        long mailContentId = MailContent.create(client, "test1", sgp.groupId, "test subject", "test body",
                User.SUPER_USER).mailContentId;
        assertEquals("test subject", MailContent.safeGet(client, mailContentId).subject);
        assertEquals("test body", MailContent.safeGet(client, mailContentId).body);
        assertEquals("test1", MailContent.safeGet(client, mailContentId).displayName);

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        String from = "singapore@ishayoga.org";

        suppressEmailInTestEnv();
        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = mailContentId;
        msi.senderEmail = from;
        msi.suppressIfAlreadySent = true;
        msi.tags = Sets.newHashSet("singapore", "memberprofile");

        List<SentMailEntity> entities = Mail.send(client, msi, mailMap, userWithPermssion.email);
        assertEquals(1, entities.size());
        ObjectifyFilter.complete();

        entities = Mail.send(client, msi, mailMap, userWithPermssion.email);
        assertEquals(0, entities.size());
    }

    @Test
    public void repeatEmailSentWhenNotSuppressed() throws MandrillApiError, IOException {
        long mailContentId = MailContent.create(client, "test1", sgp.groupId, "test subject", "test body",
                User.SUPER_USER).mailContentId;
        assertEquals("test subject", MailContent.safeGet(client, mailContentId).subject);
        assertEquals("test body", MailContent.safeGet(client, mailContentId).body);
        assertEquals("test1", MailContent.safeGet(client, mailContentId).displayName);

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        String from = "singapore@ishayoga.org";

        suppressEmailInTestEnv();
        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = mailContentId;
        msi.senderEmail = from;
        msi.suppressIfAlreadySent = false;
        msi.tags = Sets.newHashSet("singapore", "memberprofile");

        List<SentMailEntity> entities = Mail.send(client, msi, mailMap, userWithPermssion.email);
        assertEquals(1, entities.size());
        ObjectifyFilter.complete();

        entities = Mail.send(client, msi, mailMap, userWithPermssion.email);
        assertEquals(1, entities.size());
    }

    @Test
    public void sendEmailWithoutAnyTags() throws MandrillApiError, IOException {
        long mailContentId = MailContent.create(client, "test1", sgp.groupId, "test subject", "test body",
                User.SUPER_USER).mailContentId;
        assertEquals("test subject", MailContent.safeGet(client, mailContentId).subject);
        assertEquals("test body", MailContent.safeGet(client, mailContentId).body);

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        String from = "singapore@ishayoga.org";

        suppressEmailInTestEnv();

        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = mailContentId;
        msi.senderEmail = from;
        msi.suppressIfAlreadySent = false;

        List<SentMailEntity> entities = Mail.send(client, msi, mailMap, userWithPermssion.email);

        assertEquals(1, entities.size());
        assertEquals("sathya.t@ishafoundation.org", entities.get(0).email);
        assertEquals(null, entities.get(0).tagSetId);
        ObjectifyFilter.complete();

        SentMailQueryCondition qc = new SentMailQueryCondition();
        qc.email = "sathya.t@ishafoundation.org";

        entities = Mail.queryEntitiesSortedByTimeDesc(client, qc, User.SUPER_USER);
        assertEquals(1, entities.size());
        assertEquals("sathya.t@ishafoundation.org", entities.get(0).email);
        assertTrue(entities.get(0).memberId != null);
        assertEquals(null, entities.get(0).tagSetId);
    }

    @Test
    public void queryByEmail() throws MandrillApiError, IOException, InterruptedException {
        long mailContentId1 = MailContent.create(client, "test1", sgp.groupId, "test subject", "test body",
                User.SUPER_USER).mailContentId;

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        suppressEmailInTestEnv();
        String from = "singapore@ishayoga.org";

        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = mailContentId1;
        msi.senderEmail = from;
        msi.suppressIfAlreadySent = false;

        long ts1 = new Date().getTime();
        // send five times
        long sentMailId1 = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0).sentMailId;
        Thread.sleep(50);
        long sentMailId2 = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0).sentMailId;
        Thread.sleep(50);
        long sentMailId3 = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0).sentMailId;
        Thread.sleep(50);
        long sentMailId4 = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0).sentMailId;
        Thread.sleep(50);
        long sentMailId5 = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0).sentMailId;
        Thread.sleep(50);

        long ts2 = new Date().getTime();
        assertTrue(ts2 > ts1);

        long mailContentId2 = MailContent.create(client, "test2", sgp.groupId, "test subject 2", "test body 2",
                User.SUPER_USER).mailContentId;

        msi.mailContentId = mailContentId2;
        // send mailContentId2 five times
        long sentMailId6 = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0).sentMailId;
        Thread.sleep(50);
        long sentMailId7 = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0).sentMailId;
        Thread.sleep(50);
        long sentMailId8 = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0).sentMailId;
        Thread.sleep(50);
        long sentMailId9 = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0).sentMailId;
        Thread.sleep(50);
        long sentMailId10 = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0).sentMailId;
        Thread.sleep(50);

        ObjectifyFilter.complete();

        SentMailQueryCondition qc = new SentMailQueryCondition();
        qc.email = "sathya.t@ishafoundation.org";

        List<SentMailEntity> entities = Mail.queryEntitiesSortedByTimeDesc(client, qc, User.SUPER_USER);
        assertEquals(10, entities.size());

        // should be sorted in descending
        assertEquals(sentMailId10, entities.get(0).sentMailId);
        assertEquals(sentMailId9, entities.get(1).sentMailId);
        assertEquals(sentMailId8, entities.get(2).sentMailId);
        assertEquals(sentMailId7, entities.get(3).sentMailId);
        assertEquals(sentMailId6, entities.get(4).sentMailId);
        assertEquals(sentMailId5, entities.get(5).sentMailId);
        assertEquals(sentMailId4, entities.get(6).sentMailId);
        assertEquals(sentMailId3, entities.get(7).sentMailId);
        assertEquals(sentMailId2, entities.get(8).sentMailId);
        assertEquals(sentMailId1, entities.get(9).sentMailId);

        // include only 3
        qc.numResults = 3;

        entities = Mail.queryEntitiesSortedByTimeDesc(client, qc, User.SUPER_USER);
        assertEquals(3, entities.size());

        assertEquals(sentMailId10, entities.get(0).sentMailId);
        assertEquals(sentMailId9, entities.get(1).sentMailId);
        assertEquals(sentMailId8, entities.get(2).sentMailId);

        // include 6 between ts1 and ts2
        // there are only 5 mails sent between this time
        qc.startMS = ts1;
        qc.endMS = ts2;
        qc.numResults = 6;

        entities = Mail.queryEntitiesSortedByTimeDesc(client, qc, User.SUPER_USER);
        assertEquals(5, entities.size());

        assertEquals(sentMailId5, entities.get(0).sentMailId);
        assertEquals(sentMailId4, entities.get(1).sentMailId);
        assertEquals(sentMailId3, entities.get(2).sentMailId);
        assertEquals(sentMailId2, entities.get(3).sentMailId);
        assertEquals(sentMailId1, entities.get(4).sentMailId);
    }

    @Test
    public void getStatsByMailContent() throws MandrillApiError, IOException, InterruptedException {
        long mailContentId = MailContent.create(client, "test1", sgp.groupId, "test subject",
                "test body <a href=\"http:\\www.google.com\">link</a>", User.SUPER_USER).mailContentId;

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        suppressEmailInTestEnv();
        String from = "singapore@ishayoga.org";

        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = mailContentId;
        msi.senderEmail = from;
        msi.suppressIfAlreadySent = false;


        // send five times

        SentMailEntity sentMailEntity = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0);
        assertEquals(mailContentId, sentMailEntity.mailContentId);

        sentMailEntity = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0);
        assertEquals(mailContentId, sentMailEntity.mailContentId);

        sentMailEntity = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0);
        assertEquals(mailContentId, sentMailEntity.mailContentId);

        sentMailEntity = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0);
        assertEquals(mailContentId, sentMailEntity.mailContentId);

        sentMailEntity = Mail.send(client, msi, mailMap, userWithPermssion.email).get(0);
        assertEquals(mailContentId, sentMailEntity.mailContentId);

        ObjectifyFilter.complete();

        MailStatsProp mailStatsProp = Mail.getStatsByMailContent(client, mailContentId, userWithPermssion.email);
        assertEquals(5, mailStatsProp.numRecipientsSendAttempted);
        assertEquals(1, mailStatsProp.urlVsNumRecipientsThatClicked.size());
        assertEquals(0, mailStatsProp.urlVsNumRecipientsThatClicked.get("http:\\www.google.com").intValue());

    }

    @Test(expected = APIException.class)
    public void permissionRequiredToSendEmail() throws MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        long mailContentId = MailContent.create(client, "test1", sgp.groupId, "test subject", "test body",
                User.SUPER_USER).mailContentId;

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = mailContentId;
        msi.senderEmail = "singapore@ishayoga.org";
        msi.suppressIfAlreadySent = true;

        Mail.send(client, msi, mailMap, userWOPermssion.email);

        assertTrue(false);
    }

    @Test
    public void memberCreatedWhenFlagSet() throws MandrillApiError, IOException {
        MailTest.suppressEmailInTestEnv();
        long mailContentId = MailContent.create(client, "test1", sgp.groupId, "test subject", "test body",
                User.SUPER_USER).mailContentId;

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        MailSendInput msi = new MailSendInput();
        msi.createMember = true;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = mailContentId;
        msi.senderEmail = "singapore@ishayoga.org";
        msi.suppressIfAlreadySent = true;

        List<SentMailEntity> sentMailEntities = Mail.send(client, msi, mailMap, User.SUPER_USER);

        ObjectifyFilter.complete();

        assertEquals(1, sentMailEntities.size());
        assertNotNull(sentMailEntities.get(0).memberId);
        assertEquals("sathya.t@ishafoundation.org", sentMailEntities.get(0).email);

        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        int count = MemberLoader.getCount(mqc, User.SUPER_USER);

        assertEquals(1, count);
    }

    @Test
    public void memberNotCreatedWhenFlagNotSet() throws MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        long mailContentId = MailContent.create(client, "test1", sgp.groupId, "test subject", "test body",
                User.SUPER_USER).mailContentId;

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = mailContentId;
        msi.senderEmail = "singapore@ishayoga.org";
        msi.suppressIfAlreadySent = true;

        List<SentMailEntity> sentMailEntities = Mail.send(client, msi, mailMap, User.SUPER_USER);

        ObjectifyFilter.complete();

        assertEquals(1, sentMailEntities.size());
        assertNull(sentMailEntities.get(0).memberId);
        assertEquals("sathya.t@ishafoundation.org", sentMailEntities.get(0).email);

        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        int count = MemberLoader.getCount(mqc, User.SUPER_USER);

        assertEquals(0, count);
    }

    @Test
    public void canSendEmailToEnabledList() throws MandrillApiError, IOException {

        suppressEmailInTestEnv();

        ListProp newsletter = crmdna.list.List.createPublic(client, sgp.groupId, "Newsletter", User.SUPER_USER);

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");
        mailMap.add("thulasidhar@gmail.com", "Thulasi", "Kosalram");

        Member.bulkSubscribeList(client, newsletter.listId, mailMap, User.SUPER_USER);
        ObjectifyFilter.complete();

        MailContentProp mailContentProp = MailContent.create(client, "Aug 14 Newsletter", sgp.groupId,
                "Aug 14 Newsletter", "Hello everyone,...", User.SUPER_USER);

        List<SentMailEntity> sentMailEntities = Mail.sendToList(client, newsletter.listId,
                mailContentProp.mailContentId, "singapore@ishayoga.org", null, User.SUPER_USER, "Isha", "Isha");
        ObjectifyFilter.complete();

        assertEquals(2, sentMailEntities.size());

        // sort in ascending order of email
        Collections.sort(sentMailEntities, new Comparator<SentMailEntity>() {

            @Override
            public int compare(SentMailEntity arg0, SentMailEntity arg1) {
                // TODO Auto-generated method stub
                return arg0.email.compareTo(arg1.email);
            }
        });
        assertEquals("sathya.t@ishafoundation.org", sentMailEntities.get(0).email);
        assertEquals("thulasidhar@gmail.com", sentMailEntities.get(1).email);
    }

    @Test
    public void cannotSendEmailToDisabledList() throws MandrillApiError, IOException {

        suppressEmailInTestEnv();

        ListProp newsletter = crmdna.list.List.createPublic(client, sgp.groupId, "Newsletter", User.SUPER_USER);
        crmdna.list.List.disable(client, newsletter.listId, User.SUPER_USER);

        MailContentProp mailContentProp = MailContent.create(client, "Aug 14 Newsletter", sgp.groupId,
                "Aug 14 Newsletter", "Hello everyone,...", User.SUPER_USER);

        try {
            Mail.sendToList(client, newsletter.listId, mailContentProp.mailContentId, "singapore@ishayoga.org", null,
                    User.SUPER_USER, "Isha", "Isha");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_PRECONDITION_FAILED, ex.statusCode);
        }
    }

    @Test
    public void mailNotSentToUnsubscribedEmails() throws IOException, MandrillApiError {
        suppressEmailInTestEnv();

        MailContentProp newsletter = MailContent.create(client, "Aug 14 Newsletter", sgp.groupId,
                "Aug 14 Newsletter", "Hello everyone,...", User.SUPER_USER);

        //create two members m1 and m2
        //m1 is subscribed to singapore
        //m2 is unsubscribed to singapore

        ContactProp c1 = new ContactProp();
        c1.firstName = "Member1";
        c1.email = "m1@invalid.com";
        c1.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        MemberProp m1 = Member.create(client, sgp.groupId, c1, false, User.SUPER_USER);

        ContactProp c2 = new ContactProp();
        c2.firstName = "Member2";
        c2.email = "m2@invalid.com";
        c2.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        MemberProp m2 = Member.create(client, sgp.groupId, c2, false, User.SUPER_USER);

        Member.subscribeGroup(client, m1.memberId, sgp.groupId, User.SUPER_USER);
        Member.subscribeGroup(client, m2.memberId, sgp.groupId, User.SUPER_USER);
        Member.unsubscribeGroup(client, m2.memberId, sgp.groupId, User.SUPER_USER);

        m1 = MemberLoader.safeGet(client, m1.memberId, User.SUPER_USER).toProp();
        assertTrue(m1.subscribedGroupIds.contains(sgp.groupId));

        m2 = MemberLoader.safeGet(client, m2.memberId, User.SUPER_USER).toProp();
        assertTrue(m2.unsubscribedGroupIds.contains(sgp.groupId));

        MailMap mailMap = new MailMap();
        mailMap.add("m1@invalid.com", "First1", "Last1");
        mailMap.add("m2@invalid.com", "First2", "Last2");

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.groupId = sgp.groupId;
        msi.isTransactionEmail = false;
        msi.mailContentId = newsletter.mailContentId;
        msi.senderEmail = "singapore@ishayoga.org";
        msi.suppressIfAlreadySent = true;

        List<SentMailEntity> sentEmailEntities = Mail.send(client, msi, mailMap, User.SUPER_USER);

        assertEquals(1, sentEmailEntities.size());
        assertEquals("m1@invalid.com", sentEmailEntities.get(0).email);
    }

    @Test
    public void cannotSendMarketingEmailAtClientLevel() throws IOException, MandrillApiError {

        suppressEmailInTestEnv();

        long mailContentId = MailContent.create(client, "test1", sgp.groupId, "test subject", "test body",
                User.SUPER_USER).mailContentId;

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.isTransactionEmail = false;
        msi.mailContentId = mailContentId;
        msi.senderEmail = "singapore@ishayoga.org";
        msi.suppressIfAlreadySent = true;

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");
        mailMap.add("thulasidhar@gmail.com", "Thulasi", "Kosalram");

        try {
            Mail.send(client, msi, mailMap, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void cannotSendTransactionEmailsInBulk() throws IOException, MandrillApiError {
        suppressEmailInTestEnv();

        long mailContentId = MailContent.create(client, "test1", sgp.groupId, "test subject", "test body",
                User.SUPER_USER).mailContentId;

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.isTransactionEmail = true;
        msi.mailContentId = mailContentId;
        msi.senderEmail = "singapore@ishayoga.org";
        msi.suppressIfAlreadySent = true;

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");
        mailMap.add("thulasidhar@gmail.com", "Thulasi", "Kosalram");

        try {
            Mail.send(client, msi, mailMap, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        //try group level transaction email
        msi.groupId = sgp.groupId;
        try {
            Mail.send(client, msi, mailMap, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void sendToParticipantsIfPresentInListTest() throws IOException, MandrillApiError {
        suppressEmailInTestEnv();

        long mailContentId = MailContent.create(client, "Welcome to isha", sgp.groupId, "welcome", "welcome",
                User.SUPER_USER).mailContentId;

        long programId = createIshaUpaYogaProgram();

        long listId = crmdna.list.List.createRestricted(client, sgp.groupId, "Isha Upa Yoga",
                null, User.SUPER_USER).listId;

        ContactProp c = new ContactProp();
        c.email = "sathya.t@ishafoundation.org";
        c.asOfyyyymmdd = 20150911;

        MemberProp sathya = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);
        Member.subscribeGroup(client, sathya.memberId, sgp.groupId, User.SUPER_USER);
        sathya = Member.addOrDeleteProgram(client, sathya.memberId, programId, true, User.SUPER_USER);
        assertTrue(sathya.programIds.contains(programId));
        assertTrue(sathya.subscribedGroupIds.contains(sgp.groupId));

        List<SentMailEntity> sentMailEntities = Mail.sendToParticipantsIfPresentInList(client, programId,
                listId, mailContentId,
                "singapore@ishayoga.org", "Isha", "Isha", userWithPermssion.email);

        //no mail should be sent as the participants are not added to list
        assertEquals(0, sentMailEntities.size());

        //now add participants to list
        Member.addOrDeleteList(client, sathya.memberId, listId, true, User.SUPER_USER);

        //mail should go out now
        sentMailEntities = Mail.sendToParticipantsIfPresentInList(client, programId,
                listId, mailContentId,
                "singapore@ishayoga.org", "Isha", "Isha", userWithPermssion.email);
        assertEquals(1, sentMailEntities.size());
        assertEquals(sathya.memberId, sentMailEntities.get(0).mailContentId);

        //send one more time, it should be suppressed
        sentMailEntities = Mail.sendToParticipantsIfPresentInList(client, programId,
                listId, mailContentId,
                "singapore@ishayoga.org", "Isha", "Isha", userWithPermssion.email);
        assertEquals(0, sentMailEntities.size());
    }

    private long createIshaUpaYogaProgram() {
        long venueId = Venue.create(client, "Venue1", "Full address",
                sgp.groupId, User.SUPER_USER).venueId;

        long teacherId = Teacher.create(client, "firstname", "lastname",
                "email@dummy.com", sgp.groupId, User.SUPER_USER).teacherId;

        long programTypeId = ProgramType.create(client, "Isha Upa Yoga", null, User.SUPER_USER).programTypeId;

        long programId = Program.create(client, sgp.groupId, programTypeId, venueId, teacherId, 20150911, 20150911,
                1, null, 0, Utils.Currency.SGD, User.SUPER_USER).programId;

        return programId;
    }
}
