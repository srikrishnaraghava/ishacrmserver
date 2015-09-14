package crmdna.interaction;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.calling.Campaign;
import crmdna.calling.CampaignProp;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.member.Member;
import crmdna.member.MemberProp;
import crmdna.practice.Practice;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.programtype.ProgramType;
import crmdna.programtype.ProgramTypeProp;
import crmdna.teacher.Teacher;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.UserProp;
import crmdna.venue.Venue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InteractionTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    MemberProp sathya, thulasi;
    private UserProp validUser, validUser2, userWithPermission;

    CampaignProp campaignIshaKriya20150823;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        // can call getAll without any groups
        List<GroupProp> centers = Group.getAll(client, false);
        assertEquals(0, centers.size());

        GroupProp chennai = Group.create(client, "Chennai", User.SUPER_USER);
        assertEquals(1, chennai.groupId);

        validUser = User.create(client, "valid@valid.com", chennai.groupId,
                User.SUPER_USER);
        assertEquals(1, User.get(client, validUser.email).toProp(client).userId);

        validUser2 = User.create(client, "valid2@valid.com", chennai.groupId,
                User.SUPER_USER);
        assertEquals(2,
                User.get(client, validUser2.email).toProp(client).userId);

        userWithPermission = User.create(client, "userWithPermission@valid.com", chennai.groupId, User.SUPER_USER);
        assertEquals(3,
                User.get(client, userWithPermission.email).toProp(client).userId);

        User.addClientLevelPrivilege(client, userWithPermission.email,
                ClientLevelPrivilege.UPDATE_INTERACTION, User.SUPER_USER);

        ContactProp contact = new ContactProp();
        contact.email = "sathya.t@ishafoundation.org";
        contact.asOfyyyymmdd = 20150801;
        sathya = Member.create(client, chennai.groupId, contact, false,
                User.SUPER_USER);

        contact = new ContactProp();
        contact.email = "thulasidhar@gmail.com";
        contact.asOfyyyymmdd = 20150801;
        thulasi = Member.create(client, chennai.groupId, contact, false,
                User.SUPER_USER);

        Venue.VenueProp pandianHall =
                Venue.create(client, "Pandian Hall", "Pandian Hall", "Pandian Hall", chennai.groupId,
                    User.SUPER_USER);
        Teacher.TeacherProp thulasi = Teacher.create(client, "", "", "thulasidhar@gmail.com", chennai.groupId, User.SUPER_USER);

        Practice.PracticeProp ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);

        ProgramTypeProp ishaKriyaTeacherLed = ProgramType.create(client, "Isha Kriya (Teacher led)",
                Utils.getSet(ishaKriya.practiceId), User.SUPER_USER);

        ProgramProp ishaKriya20150823 = Program.create(client, chennai.groupId, ishaKriyaTeacherLed.programTypeId,
                pandianHall.venueId, thulasi.teacherId, 20150823, 20150823, 1, null, 0, null, User.SUPER_USER);

        campaignIshaKriya20150823 = Campaign.create(client, ishaKriya20150823.programId, "Default",
                20150801, 20150823, User.SUPER_USER);

    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void createInteractionTest() {
        Date date = new Date();
        Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, date, campaignIshaKriya20150823.campaignId, false, validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.memberIds.add(sathya.memberId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(1, result.totalSize);
        assertEquals(1, result.interactionProps.size());
        InteractionProp prop = result.interactionProps.get(0);
        assertEquals(date.getTime(), prop.timestamp);
        assertEquals(sathya.memberId, prop.memberId);
        assertEquals(1, prop.subInteractionProps.size());
        assertEquals("test 1", prop.subInteractionProps.get(0).content);
        assertEquals(Interaction.InteractionType.PHONE.toString(), prop.interactionType);
        assertEquals(validUser.email, prop.user);
        assertEquals(campaignIshaKriya20150823.campaignId, prop.campaignId.longValue());

        //interaction score must be updated
        UserMemberProp userMemberProp = new UserMemberProp();
        userMemberProp.memberId = sathya.memberId;
        userMemberProp.userId = validUser.userId;
        InteractionScoreProp scoreProp = InteractionScore.get(client, Utils.getList(userMemberProp)).get(0);
        assertEquals(1, scoreProp.interactionScore);

        //exception if invalid campaignId
        try {
            Interaction.createInteraction(client, sathya.memberId, "test 2",
                    Interaction.InteractionType.EMAIL, date, (long)10001, false, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("Campaign id should be valid", APIResponse.Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void campaignDateValidationTest() {

        final int MILLISECONDS_IN_A_DAY = 86400 * 1000;
        Date aDayBeforeCampaignStart = new Date(DateUtils.toDate(campaignIshaKriya20150823.startYYYYMMDD).getTime() - MILLISECONDS_IN_A_DAY);

        try {
            Interaction.createInteraction(client, sathya.memberId, "test 1",
                    Interaction.InteractionType.PHONE, aDayBeforeCampaignStart,
                    campaignIshaKriya20150823.campaignId, true, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        Date aDayAfterCampaignEnd = new Date(DateUtils.toDate(campaignIshaKriya20150823.endYYYYMMDD).getTime() + MILLISECONDS_IN_A_DAY);

        try {
            Interaction.createInteraction(client, sathya.memberId, "test 1",
                    Interaction.InteractionType.PHONE, aDayAfterCampaignEnd,
                    campaignIshaKriya20150823.campaignId, true, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        //no exception if date validation is turned off
        Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, aDayAfterCampaignEnd,
                campaignIshaKriya20150823.campaignId, false, validUser.email);

        //create another interaction a day after campaign start
        Date aDayAfterCampaignStart = new Date(DateUtils.toDate(campaignIshaKriya20150823.startYYYYMMDD).getTime() + MILLISECONDS_IN_A_DAY);
        Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, aDayAfterCampaignStart,
                campaignIshaKriya20150823.campaignId, true, validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.campaignIds.add(campaignIshaKriya20150823.campaignId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(2, result.totalSize);
        assertEquals(2, result.interactionProps.size());
    }

    @Test
    public void updateInteractionTest() {
        Date date = new Date();
        InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, date, null, false, validUser.email);

        Interaction.updateInteraction(client, p.interactionId, thulasi.memberId, null, null, validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.memberIds.add(thulasi.memberId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(1, result.totalSize);
        assertEquals(1, result.interactionProps.size());
        InteractionProp prop = result.interactionProps.get(0);
        assertEquals(date.getTime(), prop.timestamp);
        assertEquals(thulasi.memberId, prop.memberId);
        assertEquals(1, prop.subInteractionProps.size());
        assertEquals("test 1", prop.subInteractionProps.get(0).content);
        assertEquals(Interaction.InteractionType.PHONE.toString(), prop.interactionType);
        assertEquals(validUser.email, prop.user);
    }

    @Test
    public void deleteInteractionTest() {
        Date date = new Date();
        InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, date, campaignIshaKriya20150823.campaignId, false, validUser.email);

        Interaction.deleteInteraction(client, p.interactionId, validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.memberIds.add(sathya.memberId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(0, result.totalSize);
    }

    @Test
    public void queryByMemberId() {
        //covered by createInteractionTest
    }

    @Test
    public void queryByUserId() {
        Date date = new Date();
        Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, date, null, false, validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.userIds.add(validUser.userId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(1, result.totalSize);
        assertEquals(1, result.interactionProps.size());
        InteractionProp prop = result.interactionProps.get(0);
        assertEquals(date.getTime(), prop.timestamp);
        assertEquals(sathya.memberId, prop.memberId);
        assertEquals(1, prop.subInteractionProps.size());
        assertEquals("test 1", prop.subInteractionProps.get(0).content);
        assertEquals(Interaction.InteractionType.PHONE.toString(), prop.interactionType);
        assertEquals(validUser.email, prop.user);
    }

    @Test
    public void queryByCampaignId() {
        Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.EMAIL, new Date(), campaignIshaKriya20150823.campaignId, false, validUser.email);

        //create one without campaign
        Interaction.createInteraction(client, sathya.memberId, "test 2",
                Interaction.InteractionType.EMAIL, new Date(), null, false, validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.campaignIds.add(campaignIshaKriya20150823.campaignId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(1, result.totalSize);
        InteractionProp prop = result.interactionProps.get(0);
        assertEquals("test 1", prop.subInteractionProps.get(0).content);
        assertEquals(campaignIshaKriya20150823.campaignId, prop.campaignId.longValue());
    }

    @Test
    public void countTest() {
        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.memberIds.add(sathya.memberId);

        assertEquals(0, Interaction.count(client, qc));

        Interaction.createInteraction(client, sathya.memberId, "test",
                Interaction.InteractionType.EMAIL, new Date(), null, false, validUser.email);
        assertEquals(1, Interaction.count(client, qc));

    }

    @Test public void sortedByDateDesc() throws InterruptedException {
        Date date1 = new Date();
        Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, date1, null, false, validUser.email);

        Thread.sleep(50);

        Date date1Plus50Ms = new Date();
        Interaction.createInteraction(client, sathya.memberId, "test 2",
                Interaction.InteractionType.PHONE, date1Plus50Ms, null, false, validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.userIds.add(validUser.userId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(2, result.totalSize);
        assertEquals(2, result.interactionProps.size());
        assertEquals(date1Plus50Ms.getTime(), result.interactionProps.get(0).timestamp);
        assertEquals(date1.getTime(), result.interactionProps.get(1).timestamp);
    }

    @Test
    public void queryUsingStartIndexAndNumResults() throws InterruptedException {
        Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, new Date(), null, false, validUser.email);
        Thread.sleep(50);
        Interaction.createInteraction(client, sathya.memberId, "test 2",
                Interaction.InteractionType.PHONE, new Date(), null, false, validUser.email);
        Thread.sleep(50);
        Interaction.createInteraction(client, sathya.memberId, "test 3",
                Interaction.InteractionType.PHONE, new Date(), null, false, validUser.email);

        //query for 2 interactions starting from index 0
        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.userIds.add(validUser.userId);
        qc.numResults = 2;
        qc.startIndex = 0;

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(3, result.totalSize);
        assertEquals(2, result.interactionProps.size());
        assertEquals("test 3", result.interactionProps.get(0).subInteractionProps.get(0).content);
        assertEquals("test 2", result.interactionProps.get(1).subInteractionProps.get(0).content);

        //query for 3 interactions starting from index 1
        qc.numResults = 3;
        qc.startIndex = 1;
        result = Interaction.query(client, qc, validUser.email);
        assertEquals(3, result.totalSize);
        assertEquals(2, result.interactionProps.size());
        assertEquals("test 2", result.interactionProps.get(0).subInteractionProps.get(0).content);
        assertEquals("test 1", result.interactionProps.get(1).subInteractionProps.get(0).content);
    }

    @Test
    public void createSubInteractionTest() {
        Date date = new Date();
        InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, date, null, false, validUser.email);

        Date datePlus50Ms = new Date(date.getTime() + 50);
        Interaction.createSubInteraction(client, p.interactionId, "test 2", datePlus50Ms, validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.memberIds.add(sathya.memberId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(1, result.interactionProps.size());
        List<SubInteractionProp> subInteractionProps = result.interactionProps.get(0).subInteractionProps;
        assertEquals(2, subInteractionProps.size());
        assertEquals("test 2", subInteractionProps.get(0).content);
        assertEquals(datePlus50Ms.getTime(), subInteractionProps.get(0).timestamp);
        assertEquals("test 1", subInteractionProps.get(1).content);
        assertEquals(date.getTime(), subInteractionProps.get(1).timestamp);

        //interaction score must be updated
        UserMemberProp userMemberProp = new UserMemberProp();
        userMemberProp.memberId = sathya.memberId;
        userMemberProp.userId = validUser.userId;
        InteractionScoreProp scoreProp = InteractionScore.get(client, Utils.getList(userMemberProp)).get(0);
        assertEquals(2, scoreProp.interactionScore);
    }

    @Test
    public void updateSubInteractionTest() {
        Date date = new Date();
        InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, date, null, false, validUser.email);

        Interaction.updateSubInteraction(client, p.interactionId, p.subInteractionProps.get(0).subInteractionId,
                "test 2", validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.memberIds.add(sathya.memberId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(1, result.interactionProps.size());
        List<SubInteractionProp> subInteractionProps = result.interactionProps.get(0).subInteractionProps;
        assertEquals(1, subInteractionProps.size());
        assertEquals("test 2", subInteractionProps.get(0).content);
    }

    @Test
    public void deleteSubInteractionTest() {
        Date date = new Date();
        InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, date, null, false, validUser.email);

        Interaction.deleteSubInteraction(client, p.interactionId,
                p.subInteractionProps.get(0).subInteractionId, validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.memberIds.add(sathya.memberId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(1, result.interactionProps.size());
        List<SubInteractionProp> subInteractionProps = result.interactionProps.get(0).subInteractionProps;
        assertEquals(0, subInteractionProps.size());
    }

    @Test
    public void onlyValidUserCanCreateInteraction() {
        try {
            InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                    Interaction.InteractionType.PHONE, new Date(), null, false, "invaliduser@dummy.com");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_INVALID_USER, ex.statusCode);
        }
    }

    @Test
    public void userCanDeleteOwnInteraction() {
        InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, new Date(), null, false, validUser.email);
        Interaction.deleteInteraction(client, p.interactionId, validUser.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.memberIds.add(sathya.memberId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(0, result.totalSize);
        assertEquals(0, result.interactionProps.size());
    }

    @Test
    public void permissionRequiredToUpdateSomeonesInteraction() {
        InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, new Date(), null, false, validUser.email);

        try {
            Interaction.updateInteraction(client, p.interactionId, thulasi.memberId, null, null, validUser2.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        Interaction.updateInteraction(client, p.interactionId, thulasi.memberId, null, null, userWithPermission.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.memberIds.add(sathya.memberId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(0, result.totalSize);

        qc.memberIds.clear();
        qc.memberIds.add(thulasi.memberId);
        result = Interaction.query(client, qc, validUser.email);
        assertEquals(1, result.totalSize);
        assertEquals(1, result.interactionProps.size());
        assertEquals("test 1", result.interactionProps.get(0).subInteractionProps.get(0).content);
    }

    @Test
    public void permissionRequiredToUpdateToSomeonesSubInteraction() {
        InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, new Date(), null, false, validUser.email);

        try {
            Interaction.updateSubInteraction(client, p.interactionId,
                    p.subInteractionProps.get(0).subInteractionId, "test 2", validUser2.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        Interaction.updateSubInteraction(client, p.interactionId,
                p.subInteractionProps.get(0).subInteractionId, "test 2", userWithPermission.email);

        InteractionQueryCondition qc = new InteractionQueryCondition();
        qc.memberIds.add(sathya.memberId);

        InteractionQueryResult result = Interaction.query(client, qc, validUser.email);
        assertEquals(1, result.totalSize);
        assertEquals("test 2", result.interactionProps.get(0).subInteractionProps.get(0).content);
    }

    @Test
    public void cannotExceedMaxContentLengthLimitWhenCreating() {
        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < InteractionCore.MAX_CONTENT_SIZE * 2; i ++) {
            largeContent.append("0");
        }

        try {
            Interaction.createInteraction(client, sathya.memberId, largeContent.toString(),
                    Interaction.InteractionType.EMAIL, new Date(), null, false, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_OVERFLOW, ex.statusCode);
        }
    }

    @Test
    public void cannotExceedMaxContentLengthLimitWhenUpdating() {
        InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, new Date(), null, false, validUser.email);

        StringBuilder largeContent = new StringBuilder();
        for (int i = 0; i < InteractionCore.MAX_CONTENT_SIZE * 2; i ++) {
            largeContent.append("0");
        }

        try {
            Interaction.updateSubInteraction(client, p.interactionId,
                    p.subInteractionProps.get(0).subInteractionId, largeContent.toString(), validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_OVERFLOW, ex.statusCode);
        }
    }

    @Test
    public void cannotExceedMaxNumSubinteractions() {
        InteractionProp p = Interaction.createInteraction(client, sathya.memberId, "test 1",
                Interaction.InteractionType.PHONE, new Date(), null, false, validUser.email);

        for (int i = 1; i < InteractionCore.MAX_SUB_INTERACTIONS; i++) {
            Interaction.createSubInteraction(client, p.interactionId, "test " + i, new Date(), validUser.email);
        }

        //creating another sub interaction should throw an exception
        try {
            Interaction.createSubInteraction(client, p.interactionId, "test", new Date(), validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_OVERFLOW, ex.statusCode);
        }
    }
}
