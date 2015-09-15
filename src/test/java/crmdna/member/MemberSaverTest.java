package crmdna.member;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.OfyService;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.programtype.ProgramType;
import crmdna.programtype.ProgramTypeProp;
import crmdna.teacher.Teacher;
import crmdna.teacher.Teacher.TeacherProp;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.UserCore;
import crmdna.venue.Venue;
import crmdna.venue.Venue.VenueProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemberSaverTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private final String validUser = "valid@login.com";
    private final String validUser2 = "valid2@login.com";
    private final String invalidUser = "invalid@login.com";
    private final String userWithPermission = "userwithpermission@login.com";
    ProgramProp shambhavi201405, mysticEye201401, sathsang201405;
    MemberProp thulasi;
    String sgpUser = "sgpuser@valid.com";
    private GroupProp sgp;
    private GroupProp kl;
    private GroupProp sydney;
    private GroupProp iyc;
    private PracticeProp shambhavi, mysticEye, ishaKriya;
    private ProgramTypeProp shambhavi2Day, mysticEye1Day;

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

        iyc = Group.create(client, "India/Isha Yoga Center", User.SUPER_USER);
        assertEquals(4, iyc.groupId);

        User.create(client, validUser, sgp.groupId, User.SUPER_USER);
        assertEquals(1,
                UserCore.safeGet(client, validUser).toProp(client).userId);

        User.create(client, validUser2, sgp.groupId, User.SUPER_USER);
        assertEquals(2,
                UserCore.safeGet(client, validUser2).toProp(client).userId);

        User.create(client, userWithPermission, sgp.groupId, User.SUPER_USER);
        assertEquals(
                3,
                UserCore.safeGet(client, userWithPermission).toProp(client).userId);
        User.addClientLevelPrivilege(client, userWithPermission,
                ClientLevelPrivilege.UPDATE_INTERACTION, User.SUPER_USER);

        shambhavi = Practice.create("isha", "Shambhavi", User.SUPER_USER);
        mysticEye = Practice.create("isha", "MysticEye", User.SUPER_USER);
        ishaKriya = Practice.create("isha", "IshaKriya", User.SUPER_USER);

        Set<Long> sathsangPracticeIds = new HashSet<>();
        sathsangPracticeIds.add(shambhavi.practiceId);
        sathsangPracticeIds.add(mysticEye.practiceId);

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(shambhavi.practiceId);
        ProgramTypeProp shambhavi2Day = ProgramType.create("isha", "Shambhavi",
                practiceIds, User.SUPER_USER);

        practiceIds.clear();
        practiceIds.add(mysticEye.practiceId);
        ProgramTypeProp mysticEye1Day = ProgramType.create("isha", "MysticEye",
                practiceIds, User.SUPER_USER);

        ProgramTypeProp sathsang = ProgramType.create("isha", "Sathsang", null,
                User.SUPER_USER);

        GroupProp chennai = Group.create("isha", "Chennai", User.SUPER_USER);

        VenueProp giis = Venue.create("isha", "GIIS", "GIIS", "GIIS", sgp.groupId,
                User.SUPER_USER);
        VenueProp expo = Venue.create("isha", "Expo", "Expo", "Expo", sgp.groupId,
                User.SUPER_USER);

        TeacherProp nidhi = Teacher.create("isha", "Nidhi", "Jain",
                "nidhi.jain@ishafoundation.org", sgp.groupId, User.SUPER_USER);
        TeacherProp sadhguru = Teacher.create("isha", "Sadhguru", "",
                "sadhguru@ishafoundation.org", sgp.groupId, User.SUPER_USER);

        shambhavi201405 = Program.create("isha", sgp.groupId,
                shambhavi2Day.programTypeId, giis.venueId, nidhi.teacherId,
                20140503, 20140504, 1, null, 0, null, User.SUPER_USER);

        mysticEye201401 = Program.create("isha", sgp.groupId,
                mysticEye1Day.programTypeId, expo.venueId, sadhguru.teacherId,
                20140503, 20140504, 1, null, 0, null, User.SUPER_USER);

        sathsang201405 = Program.create("isha", sgp.groupId,
                sathsang.programTypeId, giis.venueId, nidhi.teacherId,
                20140503, 20140503, 1, null, 0, null, User.SUPER_USER);

        User.create("isha", sgpUser, sgp.groupId, User.SUPER_USER);

        User.addClientLevelPrivilege("isha", sgpUser,
                ClientLevelPrivilege.UPDATE_GROUP, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void addOrDeleteGroupTest() {
        ContactProp contact = new ContactProp();
        contact.email = "Sathya.t@IshaFoundation.org";
        contact.mobilePhone = "+6598361844";
        contact.homePhone = "+6565227030";
        contact.asOfyyyymmdd = 20141007;

        MemberProp prop = Member.create(client, sgp.groupId, contact, false,
                User.SUPER_USER);

        // user cannot be invalid
        try {
            Member.addOrDeleteGroup(client, prop.memberId, sgp.groupId, true,
                    invalidUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
        }

        // member should exist
        try {
            Member.addOrDeleteGroup(client, 3456, kl.groupId, true, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // center should exist
        try {
            Member.addOrDeleteGroup(client, prop.memberId, 100, true, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // add kl
        Member.addOrDeleteGroup(client, prop.memberId, kl.groupId, true,
                validUser);
        ObjectifyFilter.complete();
        prop = MemberLoader.safeGet(client, prop.memberId, User.SUPER_USER)
                .toProp();
        assertEquals(2, prop.groupIds.size());
        assertTrue(prop.groupIds.contains(sgp.groupId));
        assertTrue(prop.groupIds.contains(kl.groupId));

        // can add the same center again
        Member.addOrDeleteGroup(client, prop.memberId, kl.groupId, true,
                validUser);
        ObjectifyFilter.complete();
        prop = MemberLoader.safeGet(client, prop.memberId, User.SUPER_USER)
                .toProp();
        assertEquals(2, prop.groupIds.size());
        assertTrue(prop.groupIds.contains(sgp.groupId));
        assertTrue(prop.groupIds.contains(kl.groupId));

        // remove center
        Member.addOrDeleteGroup(client, prop.memberId, kl.groupId, false,
                validUser);
        ObjectifyFilter.complete();
        prop = MemberLoader.safeGet(client, prop.memberId, User.SUPER_USER)
                .toProp();
        assertEquals(1, prop.groupIds.size());
        assertTrue(prop.groupIds.contains(sgp.groupId));
        assertTrue(!prop.groupIds.contains(kl.groupId));

        // can remove center again
        Member.addOrDeleteGroup(client, prop.memberId, kl.groupId, false,
                validUser);
        ObjectifyFilter.complete();
        prop = MemberLoader.safeGet(client, prop.memberId, User.SUPER_USER)
                .toProp();
        assertEquals(1, prop.groupIds.size());
        assertTrue(prop.groupIds.contains(sgp.groupId));
        assertTrue(!prop.groupIds.contains(kl.groupId));

        // there should be atleast 1 center
        try {
            Member.addOrDeleteGroup(client, prop.memberId, sgp.groupId, false,
                    validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_PRECONDITION_FAILED, ex.statusCode);
        }
    }

    @Test
    public void populateDependantIndexFieldsTest() {

        // moved to a seperate java clas
    }

    @Test
    public void addOrDeleteProgramTest() {
        ContactProp contactDetailProp = new ContactProp();
        contactDetailProp.email = "simansmile@yahoo.com";
        contactDetailProp.asOfyyyymmdd = 20141027;

        MemberProp narasimhan = Member.create(client, sgp.groupId,
                contactDetailProp, false, User.SUPER_USER);

        contactDetailProp.email = "thulasidhar@gmail.com";
        MemberProp thulasi = Member.create(client, sgp.groupId,
                contactDetailProp, false, User.SUPER_USER);

        VenueProp adiYogiAlayam = Venue.create(client, "Adi yogi aalayam", "AYA",
                "Adi yogi aalamay", iyc.groupId, User.SUPER_USER);

        PracticeProp suryaKriya = Practice.create(client, "surya kriya",
                User.SUPER_USER);
        PracticeProp upaYoga = Practice.create(client, "upa yoga",
                User.SUPER_USER);
        PracticeProp angamardhana = Practice.create(client, "angamardhana",
                User.SUPER_USER);
        PracticeProp yogaAsanas = Practice.create(client, "yoga asanas",
                User.SUPER_USER);

        Set<Long> practiceIds = new HashSet<Long>();
        practiceIds.add(suryaKriya.practiceId);
        practiceIds.add(upaYoga.practiceId);
        practiceIds.add(angamardhana.practiceId);
        practiceIds.add(yogaAsanas.practiceId);

        ProgramTypeProp hatayoga21day = ProgramType.create(client,
                "Hata yoga 21 day", practiceIds, User.SUPER_USER);

        TeacherProp sheela = Teacher.create(client, "Sheela", "",
                "sheela.r@ishafoundation.org", iyc.groupId, User.SUPER_USER);

        ProgramProp hatayoga21day2013 = Program.create(client, iyc.groupId,
                hatayoga21day.programTypeId, adiYogiAlayam.venueId,
                sheela.teacherId, 20130701, 20130721, 1, null, 0, null,
                User.SUPER_USER);

        List<Long> memberIds = new ArrayList<Long>();

        // can call with empty list

        Member.addOrDeleteProgram(client, thulasi.memberId,
                hatayoga21day2013.programId, true, User.SUPER_USER);
        Member.addOrDeleteProgram(client, narasimhan.memberId,
                hatayoga21day2013.programId, true, User.SUPER_USER);

        // clear session cache
        ObjectifyFilter.complete();
        OfyService.ofy(client).clear();

        MemberProp thulasiProp = MemberLoader.safeGet(client, thulasi.memberId,
                User.SUPER_USER).toProp();
        assertEquals(1, thulasiProp.programIds.size());
        assertTrue(thulasiProp.programIds.contains(hatayoga21day2013.programId));
        assertEquals(1, thulasiProp.programTypeIds.size());
        assertTrue(thulasiProp.programTypeIds
                .contains(hatayoga21day.programTypeId));
        assertEquals(4, thulasiProp.practiceIds.size());
        assertTrue(thulasiProp.practiceIds.contains(suryaKriya.practiceId));
        assertTrue(thulasiProp.practiceIds.contains(upaYoga.practiceId));
        assertTrue(thulasiProp.practiceIds.contains(angamardhana.practiceId));
        assertTrue(thulasiProp.practiceIds.contains(yogaAsanas.practiceId));

        MemberProp narasimhanProp = MemberLoader.safeGet(client,
                narasimhan.memberId, User.SUPER_USER).toProp();
        assertEquals(1, narasimhanProp.programIds.size());
        assertTrue(narasimhanProp.programIds
                .contains(hatayoga21day2013.programId));
        assertEquals(1, narasimhanProp.programTypeIds.size());
        assertTrue(narasimhanProp.programTypeIds
                .contains(hatayoga21day.programTypeId));
        assertEquals(4, narasimhanProp.practiceIds.size());
        assertTrue(narasimhanProp.practiceIds.contains(suryaKriya.practiceId));
        assertTrue(narasimhanProp.practiceIds.contains(upaYoga.practiceId));
        assertTrue(narasimhanProp.practiceIds.contains(angamardhana.practiceId));
        assertTrue(narasimhanProp.practiceIds.contains(yogaAsanas.practiceId));

        // add again
        Member.addOrDeleteProgram(client, narasimhan.memberId,
                hatayoga21day.programTypeId, true, validUser);
        ObjectifyFilter.complete();
        OfyService.ofy(client).clear();

        thulasiProp = MemberLoader.safeGet(client, thulasi.memberId,
                User.SUPER_USER).toProp();
        assertEquals(1, thulasiProp.programIds.size());
        assertTrue(thulasiProp.programIds.contains(hatayoga21day2013.programId));
        assertEquals(1, thulasiProp.programTypeIds.size());
        assertTrue(thulasiProp.programTypeIds
                .contains(hatayoga21day.programTypeId));
        assertEquals(4, thulasiProp.practiceIds.size());
        assertTrue(thulasiProp.practiceIds.contains(suryaKriya.practiceId));
        assertTrue(thulasiProp.practiceIds.contains(upaYoga.practiceId));
        assertTrue(thulasiProp.practiceIds.contains(angamardhana.practiceId));
        assertTrue(thulasiProp.practiceIds.contains(yogaAsanas.practiceId));

        narasimhanProp = MemberLoader.safeGet(client, narasimhan.memberId,
                User.SUPER_USER).toProp();
        assertEquals(1, narasimhanProp.programIds.size());
        assertTrue(narasimhanProp.programIds
                .contains(hatayoga21day2013.programId));
        assertEquals(1, narasimhanProp.programTypeIds.size());
        assertTrue(narasimhanProp.programTypeIds
                .contains(hatayoga21day.programTypeId));
        assertEquals(4, narasimhanProp.practiceIds.size());
        assertTrue(narasimhanProp.practiceIds.contains(suryaKriya.practiceId));
        assertTrue(narasimhanProp.practiceIds.contains(upaYoga.practiceId));
        assertTrue(narasimhanProp.practiceIds.contains(angamardhana.practiceId));
        assertTrue(narasimhanProp.practiceIds.contains(yogaAsanas.practiceId));

        // delete member
        Member.addOrDeleteProgram(client, narasimhan.memberId,
                hatayoga21day.programTypeId, false, validUser);
        ObjectifyFilter.complete();
        OfyService.ofy(client).clear();

        // exception if client is invalid
        try {
            Member.addOrDeleteProgram(invalidClient, narasimhan.memberId,
                    hatayoga21day2013.programId, true, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // program id should be valid
        try {
            Member.addOrDeleteProgram(client, narasimhan.memberId, 938, true,
                    User.SUPER_USER); // invalid
            // program
            // id
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // user should be a valid user
        try {
            Member.addOrDeleteProgram(client, narasimhan.memberId,
                    hatayoga21day2013.programId, true, invalidUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
        }
    }
}
