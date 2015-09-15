package crmdna.calling;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.interaction.Interaction;
import crmdna.interaction.InteractionProp;
import crmdna.member.Member;
import crmdna.member.MemberProp;
import crmdna.practice.Practice;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.programtype.ProgramType;
import crmdna.programtype.ProgramTypeProp;
import crmdna.teacher.Teacher;
import crmdna.user.User;
import crmdna.user.UserProp;
import crmdna.venue.Venue;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by sathya on 23/8/15.
 */
public class CampaignTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    String client;
    String invalidClient = "invalid";
    Group.GroupProp sgp, kl;
    UserProp validUser, userWithCampaignPermissionInSgp;

    ProgramProp ishaKriya20150823, ishaKriya20150830;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        client = "isha";
        Client.create(client);

        sgp = crmdna.group.Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        kl = Group.create(client, "KL", User.SUPER_USER);
        assertEquals(2, kl.groupId);

        validUser = User.create(client, "validuser@dummy.com", sgp.groupId, User.SUPER_USER);
        assertEquals(1, User.get(client, validUser.email).toProp(client).userId);

        userWithCampaignPermissionInSgp = User.create(client, "withpermission@dummy.com", sgp.groupId, User.SUPER_USER);
        assertEquals(2, User.get(client, userWithCampaignPermissionInSgp.email).toProp(client).userId);

        User.addGroupLevelPrivilege(client, sgp.groupId, userWithCampaignPermissionInSgp.email,
                User.GroupLevelPrivilege.UPDATE_CAMPAIGN,
                User.SUPER_USER);

        Venue.VenueProp woodlandsCC =
                Venue.create(client, "Woodlands CC", "Woodlands CC", "Woodlands CC", sgp.groupId, User.SUPER_USER);
        Teacher.TeacherProp thulasi = Teacher.create(client, "", "", "thulasidhar@gmail.com", sgp.groupId, User.SUPER_USER);

        Practice.PracticeProp ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);

        ProgramTypeProp ishaKriyaTeacherLed = ProgramType.create(client, "Isha Kriya (Teacher led)",
                Utils.getSet(ishaKriya.practiceId), User.SUPER_USER);

        ishaKriya20150823 = Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId,
                woodlandsCC.venueId, thulasi.teacherId, 20150823, 20150823, 1, null, 0, null, User.SUPER_USER);

        ishaKriya20150830 = Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId,
                woodlandsCC.venueId, thulasi.teacherId, 20150830, 20150830, 1, null, 0, null, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void createTest() {

        Campaign.create(client, ishaKriya20150823.programId, "Default",
                20150801, 20150823, userWithCampaignPermissionInSgp.email);

        //create with different name
        CampaignProp prop = Campaign.create(client, ishaKriya20150823.programId, "Campaign-1", 20150815,
                20150822, userWithCampaignPermissionInSgp.email);
        assertEquals("campaign id in sequence", 2, prop.campaignId);
        assertEquals("Campaign-1", prop.displayName);
        assertEquals("campaign1", prop.campaignName);
        assertTrue(prop.enabled);
        assertEquals(ishaKriya20150823.programId, prop.programId);
        assertEquals(20150815, prop.startYYYYMMDD);
        assertEquals(20150822, prop.endYYYYMMDD);

        //cannot create if name clash for same program
        try {
            prop = Campaign.create(client, ishaKriya20150823.programId, "Campaign_1", 20150815,
                    20150822, userWithCampaignPermissionInSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        //can create with same name for different program
        prop = Campaign.create(client, ishaKriya20150830.programId, "Campaign_1",
                20150801, 20150830, userWithCampaignPermissionInSgp.email);
        assertEquals(ishaKriya20150830.programId, prop.programId);
        //no exception

        //start date, end date should be valid
        try {
            Campaign.create(client, ishaKriya20150830.programId, "Campaign-2",
                    20150000, 20150830, userWithCampaignPermissionInSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("Invalid start date", APIResponse.Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        try {
            Campaign.create(client, ishaKriya20150830.programId, "Campaign-2",
                    20150801, 20150832, userWithCampaignPermissionInSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("Invalid end date", APIResponse.Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        try {
            Campaign.create(client, ishaKriya20150830.programId, "Campaign-2",
                    20150831, 20150801, userWithCampaignPermissionInSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("End date before start date", APIResponse.Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void renameTest() {

        long campaignId =
                Campaign.create(client, ishaKriya20150823.programId, "Default", 20150801, 20150825,
                        userWithCampaignPermissionInSgp.email).campaignId;

        //permission required to rename
        try {
            Campaign.rename(client, campaignId, "Campaign-1", validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        //user with permission can rename
        Campaign.rename(client, campaignId, "Campaign-1", userWithCampaignPermissionInSgp.email);
        CampaignProp prop = Campaign.safeGet(client, campaignId).toProp();
        assertEquals(campaignId, prop.campaignId);
        assertEquals("Campaign-1", prop.displayName);
        assertEquals("campaign1", prop.campaignName);

        //can change case
        Campaign.rename(client, campaignId, "campaign_1", userWithCampaignPermissionInSgp.email);
        prop = Campaign.safeGet(client, campaignId).toProp();
        assertEquals(campaignId, prop.campaignId);
        assertEquals("campaign_1", prop.displayName);
        assertEquals("campaign1", prop.campaignName);

        //cannot rename to another existing name
        prop = Campaign.create(client, ishaKriya20150823.programId, "Default", 20150801, 20150831,
                userWithCampaignPermissionInSgp.email);
        assertEquals("Default", prop.displayName);

        try {
            Campaign.rename(client, campaignId, "Default", userWithCampaignPermissionInSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }
    }

    @Test
    public void enableDisableTest() {
        long campaignId =
                Campaign.create(client, ishaKriya20150823.programId, "Default", 20150801, 20150825,
                        userWithCampaignPermissionInSgp.email).campaignId;

        //permission required to enable/disable
        try {
            Campaign.enable(client, campaignId, false, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        Campaign.enable(client, campaignId, false, userWithCampaignPermissionInSgp.email);
        assertEquals(false, Campaign.safeGet(client, campaignId).toProp().enabled);

        try {
            Campaign.enable(client, campaignId, true, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        Campaign.enable(client, campaignId, true, userWithCampaignPermissionInSgp.email);
        assertEquals(true, Campaign.safeGet(client, campaignId).toProp().enabled);
    }

    @Test
    public void updateDates() {

        long campaignId =
                Campaign.create(client, ishaKriya20150823.programId, "Default",
                        20150801, 20150831,
                        userWithCampaignPermissionInSgp.email).campaignId;

        try {
            Campaign.updateDates(client, campaignId, 20150631, null, userWithCampaignPermissionInSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("invalid start date", APIResponse.Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        try {
            Campaign.updateDates(client, campaignId, 20150801, 201508222, userWithCampaignPermissionInSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("invalid end date", APIResponse.Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        try {
            Campaign.updateDates(client, campaignId, 20150801, 20150722, userWithCampaignPermissionInSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("end date before start date", APIResponse.Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        //all ok
        Campaign.updateDates(client, campaignId, 20150801, 20150822, userWithCampaignPermissionInSgp.email);

        CampaignProp prop = Campaign.safeGet(client, campaignId).toProp();
        assertEquals(20150801, prop.startYYYYMMDD);
        assertEquals(20150822, prop.endYYYYMMDD);
    }

    @Test
    public void permissionRequiredToDelete() {
        long campaignId =
                Campaign.create(client, ishaKriya20150823.programId, "Default",
                        20150801, 20150831,
                        userWithCampaignPermissionInSgp.email).campaignId;

        try {
            Campaign.delete(client, campaignId, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("permission required to delete",
                    APIResponse.Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        Campaign.delete(client, campaignId, userWithCampaignPermissionInSgp.email);
        //check it is deleted
        try {
            Campaign.safeGet(client, campaignId);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void canDeleteOnlyIfNoInteractions() {

        long campaignId =
                Campaign.create(client, ishaKriya20150823.programId, "Default",
                        20150801, 20150831,
                        userWithCampaignPermissionInSgp.email).campaignId;

        ContactProp c = new ContactProp();
        c.email = "sathya.t@ishafoundation.org";
        c.asOfyyyymmdd = 20150825;
        MemberProp m = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);
        InteractionProp interactionProp = Interaction.createInteraction(client, m.memberId,
                "test 1", Interaction.InteractionType.EMAIL,
                new Date(), campaignId, false, validUser.email);

        try {
            Campaign.delete(client, campaignId, userWithCampaignPermissionInSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("Cannot delete a campaign with interactions",
                    APIResponse.Status.ERROR_PRECONDITION_FAILED, ex.statusCode);
        }

        Interaction.deleteInteraction(client, interactionProp.interactionId, validUser.email);

        Campaign.delete(client, campaignId, userWithCampaignPermissionInSgp.email);

        try {
            Campaign.safeGet(client, campaignId);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(APIResponse.Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void queryTest() {

        CampaignQueryCondition qc = new CampaignQueryCondition();
        List<CampaignEntity> campaignProps = Campaign.query(client, qc, validUser.email);
        assertTrue("Empty list when no campaigns", campaignProps.isEmpty());

        CampaignProp c1 = Campaign.create(client, ishaKriya20150823.programId, "C1",
                20150801, 20150820,
                userWithCampaignPermissionInSgp.email);

        CampaignProp c2 = Campaign.create(client, ishaKriya20150823.programId, "C2",
                20150808, 20150823,
                userWithCampaignPermissionInSgp.email);

        //try for group kl. should be empty list
        qc = new CampaignQueryCondition();
        qc.groupIds.add(kl.groupId);
        campaignProps = Campaign.query(client, qc, validUser.email);
        assertTrue(campaignProps.isEmpty());

        //try for multiple groups
        qc.groupIds.add(sgp.groupId);
        campaignProps = Campaign.query(client, qc, validUser.email);
        assertEquals(2, campaignProps.size());

        //specify end date
        qc = new CampaignQueryCondition();
        qc.groupIds.add(kl.groupId);
        qc.endDateGreaterThanYYYYMMDD = 20150820;
        campaignProps = Campaign.query(client, qc, validUser.email);
        assertTrue(campaignProps.isEmpty());

        qc = new CampaignQueryCondition();
        qc.programIds.add(ishaKriya20150823.programId);
        qc.endDateGreaterThanYYYYMMDD = 20150821;
        campaignProps = Campaign.query(client, qc, validUser.email);
        assertEquals(1, campaignProps.size());
        assertEquals(c2.campaignId, campaignProps.get(0).campaignId);

        //query by enabled
        qc = new CampaignQueryCondition();
        qc.programIds.add(ishaKriya20150823.programId);
        qc.programIds.add(ishaKriya20150830.programId);
        qc.enabled = false;
        campaignProps = Campaign.query(client, qc, validUser.email);
        assertTrue(campaignProps.isEmpty());
    }
}
