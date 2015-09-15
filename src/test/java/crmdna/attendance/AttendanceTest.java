package crmdna.attendance;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.gson.Gson;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.client.isha.IshaConfig;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group.GroupProp;
import crmdna.member.Member;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.programtype.ProgramType;
import crmdna.programtype.ProgramTypeProp;
import crmdna.teacher.Teacher;
import crmdna.teacher.Teacher.TeacherProp;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.venue.Venue;
import crmdna.venue.Venue.VenueProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AttendanceTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String validUser = "valid@login.com";
    private final String sgpUser = "sgpuser@login.com";
    private final String klUser = "kluser@login.com";
    private final String sgpAndKlUser = "sgpandkl@login.com";

    GroupProp sgp;
    GroupProp kl;

    PracticeProp suryaNamaskar;
    PracticeProp yogaAsanas;
    PracticeProp shambhavi;
    PracticeProp aumChanting;
    PracticeProp ishaKriya;

    ProgramTypeProp innerEngineering7Day;
    ProgramTypeProp suryaNamaskarAndAsanas;
    ProgramTypeProp ishaKriyaTeacherLed;
    ProgramTypeProp sathsang;
    ProgramTypeProp shambhavi2Day;

    VenueProp giis;
    VenueProp chaichee;
    VenueProp gujarathiBhavan;
    VenueProp yuhuaCC;
    VenueProp woodlandsCC;

    TeacherProp muthu;
    TeacherProp sharmila;

    MemberProp paramesh, thulasi, duane;

    ProgramProp ishaKriya20131229;
    ProgramProp sathsang20140405;
    ProgramProp shambhavi201405;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        sgp = crmdna.group.Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        kl = crmdna.group.Group.create(client, "KL", User.SUPER_USER);
        assertEquals(2, kl.groupId);

        User.create(client, validUser, sgp.groupId, User.SUPER_USER);
        assertEquals(1, User.get(client, validUser).toProp(client).userId);

        User.create(client, sgpUser, sgp.groupId, User.SUPER_USER);
        assertEquals(2, User.get(client, sgpUser).toProp(client).userId);

        User.addGroupLevelPrivilege(client, sgp.groupId, sgpUser,
                GroupLevelPrivilege.CHECK_IN, User.SUPER_USER);

        User.create(client, klUser, kl.groupId, User.SUPER_USER);
        assertEquals(3, User.get(client, klUser).toProp(client).userId);

        User.addGroupLevelPrivilege(client, kl.groupId, klUser,
                GroupLevelPrivilege.CHECK_IN, User.SUPER_USER);

        User.create(client, sgpAndKlUser, sgp.groupId, User.SUPER_USER);
        assertEquals(4, User.get(client, sgpAndKlUser).toProp(client).userId);

        User.addGroupLevelPrivilege(client, sgp.groupId, sgpAndKlUser,
                GroupLevelPrivilege.CHECK_IN, User.SUPER_USER);
        User.addGroupLevelPrivilege(client, kl.groupId, sgpAndKlUser,
                GroupLevelPrivilege.CHECK_IN, User.SUPER_USER);

        suryaNamaskar = Practice.create(client, "Surya Namaskar",
                User.SUPER_USER);
        yogaAsanas = Practice.create(client, "Yoga Asanas", User.SUPER_USER);
        shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER);
        ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);
        aumChanting = Practice.create(client, "Aum Chanting", User.SUPER_USER);

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(shambhavi.practiceId);
        practiceIds.add(aumChanting.practiceId);
        innerEngineering7Day = ProgramType.create(client,
                "Inner Engineering 7 day", practiceIds, User.SUPER_USER);

        practiceIds.clear();
        practiceIds.add(suryaNamaskar.practiceId);
        practiceIds.add(yogaAsanas.practiceId);
        suryaNamaskarAndAsanas = ProgramType.create(client,
                "Hata Yoga (Surya Namaskar & Asanas)", practiceIds,
                User.SUPER_USER);

        practiceIds.clear();
        practiceIds.add(ishaKriya.practiceId);
        ishaKriyaTeacherLed = ProgramType.create(client, "Isha Kriya",
                practiceIds, User.SUPER_USER);

        sathsang = ProgramType
                .create(client, "Sathsang", null, User.SUPER_USER);

        practiceIds.clear();
        practiceIds.add(shambhavi.practiceId);
        shambhavi2Day = ProgramType.create(client, "Shambhavi", practiceIds,
                User.SUPER_USER);

        giis = Venue.create(client, "GIIS", "GIIS", "GIIS", sgp.groupId,
                User.SUPER_USER);
        chaichee = Venue.create(client, "Chai Chee", "Chai Chee", "Chai Chee", sgp.groupId,
                User.SUPER_USER);
        gujarathiBhavan = Venue.create(client, "Gujarathi Bhavan", "GB",
                "Gujarathi Bhavan", sgp.groupId, User.SUPER_USER);
        yuhuaCC = Venue.create(client, "Yuhua CC", "Yuhua CC", "Yuhua CC", sgp.groupId,
                User.SUPER_USER);
        woodlandsCC = Venue.create(client, "Woodlands CC", "Woodlands CC", "Woodlands CC",
                sgp.groupId, User.SUPER_USER);

        muthu = Teacher.create(client, "Muthu", "Kumar", "muthu_sys@gmail.com", sgp.groupId,
                User.SUPER_USER);
        sharmila = Teacher.create(client, "", "", "sharmila@bhairaviyoga.sg",
                sgp.groupId, User.SUPER_USER);

        // add members
        ContactProp contactDetailProp = new ContactProp();
        contactDetailProp.email = "paramesh@ishafoundation.com";
        contactDetailProp.asOfyyyymmdd = 20140703;

        paramesh = Member.create(client, sgp.groupId, contactDetailProp, false,
                User.SUPER_USER);
        assertEquals(1, paramesh.memberId);

        contactDetailProp = new ContactProp();
        contactDetailProp.email = "thulasidhar@gmail.com";
        contactDetailProp.asOfyyyymmdd = 20140703;

        thulasi = Member.create(client, sgp.groupId, contactDetailProp, false,
                User.SUPER_USER);
        assertEquals(2, thulasi.memberId);

        contactDetailProp = new ContactProp();
        contactDetailProp.email = "duane.bong@barclays.com";
        contactDetailProp.asOfyyyymmdd = 20140703;

        duane = Member.create(client, sgp.groupId, contactDetailProp, false,
                User.SUPER_USER);
        duane = MemberLoader.safeGet(client, duane.memberId, User.SUPER_USER)
                .toProp();
        assertEquals(3, duane.memberId);
        assertEquals("duane.bong@barclays.com", duane.contact.email);

        ishaKriya20131229 = Program.create(client, sgp.groupId,
                ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                muthu.teacherId, 20131229, 20131229, 2, null, 0, null,
                User.SUPER_USER);

        sathsang20140405 = Program.create(client, sgp.groupId,
                sathsang.programTypeId, giis.venueId, sharmila.teacherId,
                20140405, 20140405, 1, null, 0, null, User.SUPER_USER);

        shambhavi201405 = Program.create(client, sgp.groupId,
                shambhavi2Day.programTypeId, giis.venueId, sharmila.teacherId,
                20140503, 20140504, 1, null, 0, null, User.SUPER_USER);

        ObjectifyFilter.complete();

        Member.addOrDeleteProgram(client, thulasi.memberId,
                shambhavi201405.programId, true, User.SUPER_USER);
        Member.addOrDeleteProgram(client, paramesh.memberId,
                shambhavi201405.programId, true, User.SUPER_USER);

        Set<Long> sathsangPracticeIds = new HashSet<>();
        sathsangPracticeIds.add(shambhavi.practiceId);
        sathsangPracticeIds.add(ishaKriya.practiceId);

        IshaConfig.setSathsangPractices(sathsangPracticeIds, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void checkInCheckOutAndGetNumCheckInsTest() {
        IAttendance impl = AttendanceFactory.getImpl(client);

        // num checkins should be 0
        int numCheckins = impl.getNumCheckins(ishaKriya20131229.programId,
                20131229, 1);
        assertEquals(0, numCheckins);

        // date should be valid
        try {
            impl.checkin(paramesh.memberId, ishaKriya20131229.programId,
                    20131230, 1, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // member should be valid
        try {
            impl.checkin(100, // invalid member id
                    ishaKriya20131229.programId, 20131229, 1, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // program id should be valid
        try {
            impl.checkin(paramesh.memberId, ishaKriya20131229.programId + 100,
                    20131229, 1, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // batch no should be valid
        try {
            impl.checkin(paramesh.memberId, ishaKriya20131229.programId,
                    20131229, 3, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // user should have permission - kl user cannot checkin for singapore
        // program
        try {
            impl.checkin(paramesh.memberId, ishaKriya20131229.programId,
                    20131229, 1, klUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        numCheckins = impl.checkin(paramesh.memberId,
                ishaKriya20131229.programId, 20131229, 1, sgpUser);
        assertEquals(1, numCheckins);
        // this program should be tagged to member
        paramesh = MemberLoader.safeGet(client, paramesh.memberId,
                User.SUPER_USER).toProp();
        assertTrue(paramesh.programIds.contains(ishaKriya20131229.programId));

        // cannot checkin the same member again in same batch
        try {
            impl.checkin(paramesh.memberId, ishaKriya20131229.programId,
                    20131229, 1, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        // cannot checkin the same member again in batch 2
        try {
            impl.checkin(paramesh.memberId, ishaKriya20131229.programId,
                    20131229, 2, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        // can checkout
        numCheckins = impl.checkout(paramesh.memberId,
                ishaKriya20131229.programId, 20131229, User.SUPER_USER);
        // batch 1 count should go to zero
        assertEquals(0, numCheckins);
        // Member should not show this member as completed
        paramesh = MemberLoader.safeGet(client, paramesh.memberId,
                User.SUPER_USER).toProp();
        ObjectifyFilter.complete();
        assertTrue(!paramesh.programIds.contains(ishaKriya20131229.programId));

        // can check in the same member in batch 2
        numCheckins = impl.checkin(paramesh.memberId,
                ishaKriya20131229.programId, 20131229, 2, User.SUPER_USER);
        // batch 2 count should be 1, batch 1 should remain at 0
        assertEquals(1, numCheckins);
        assertEquals(0,
                impl.getNumCheckins(ishaKriya20131229.programId, 20131229, 1));
        paramesh = MemberLoader.safeGet(client, paramesh.memberId,
                User.SUPER_USER).toProp();
        assertTrue(paramesh.programIds.contains(ishaKriya20131229.programId));

        // can checkin another member in batch 2
        numCheckins = impl.checkin(thulasi.memberId,
                ishaKriya20131229.programId, 20131229, 2, User.SUPER_USER);
        // batch 2 count should be 2
        assertEquals(2, numCheckins);
        thulasi = MemberLoader.safeGet(client, thulasi.memberId,
                User.SUPER_USER).toProp();
        assertTrue(thulasi.programIds.contains(ishaKriya20131229.programId));

        // for a program with multiple sessions, member record will be marked
        // as complete only when checked in for all sessions
        ProgramProp suryaKriya201403 = Program.create(client, sgp.groupId,
                ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                sharmila.teacherId, 20140308, 20140309, 2, null, 0, null,
                User.SUPER_USER);
        numCheckins = impl.checkin(paramesh.memberId,
                suryaKriya201403.programId, 20140308, 1, sgpUser);
        assertEquals(1, numCheckins);
        paramesh = MemberLoader.safeGet(client, paramesh.memberId, sgpUser)
                .toProp();
        assertTrue(!paramesh.programIds.contains(suryaKriya201403.programId));

        // now check in for the second session
        numCheckins = impl.checkin(paramesh.memberId,
                suryaKriya201403.programId, 20140309, 1, sgpUser);
        assertEquals(1, numCheckins);
        paramesh = MemberLoader.safeGet(client, paramesh.memberId, sgpUser)
                .toProp();
        assertTrue(paramesh.programIds.contains(suryaKriya201403.programId));

        // checkout of second session
        impl.checkout(paramesh.memberId, suryaKriya201403.programId, 20140309,
                sgpUser);
        paramesh = MemberLoader.safeGet(client, paramesh.memberId, sgpUser)
                .toProp();
        assertTrue(!paramesh.programIds.contains(suryaKriya201403.programId));
    }

    @Test
    public void getMembersForCheckInTest() {
        IAttendance impl = AttendanceFactory.getImpl(client);

        List<CheckInMemberProp> props = impl.getMembersForCheckIn(".com",
                sathsang20140405.programId, 20140405, 100, User.SUPER_USER);
        System.out.println("props: " + new Gson().toJson(props));
        assertEquals(3, props.size());

        // should be sorted
        assertEquals(duane.memberId, props.get(0).memberId);
        assertEquals(false, props.get(0).allow);
        assertEquals(0, props.get(0).practices.size());
        assertTrue(props.get(0).notAllowingReason.toLowerCase().contains(
                "not meditator"));
        assertEquals(paramesh.memberId, props.get(1).memberId);
        assertEquals(true, props.get(1).allow);
        assertEquals(1, props.get(1).practices.size());
        assertEquals("Shambhavi", props.get(1).practices.first());
        assertEquals(thulasi.memberId, props.get(2).memberId);
        assertEquals(true, props.get(2).allow);
        assertEquals(1, props.get(2).practices.size());
        assertEquals("Shambhavi", props.get(2).practices.first());

        // check in thulasi
        impl.checkin(thulasi.memberId, sathsang20140405.programId, 20140405, 1,
                User.SUPER_USER);
        props = impl.getMembersForCheckIn(".com", sathsang20140405.programId,
                20140405, 100, User.SUPER_USER);
        assertEquals(thulasi.memberId, props.get(2).memberId);
        assertEquals(false, props.get(2).allow);
        assertTrue(props.get(2).notAllowingReason.toLowerCase().contains(
                "checked in"));

        // now check out thulasi
        impl.checkout(thulasi.memberId, sathsang20140405.programId, 20140405,
                User.SUPER_USER);
        props = impl.getMembersForCheckIn(".com", sathsang20140405.programId,
                20140405, 100, User.SUPER_USER);
        assertEquals(thulasi.memberId, props.get(2).memberId);
        assertEquals(true, props.get(2).allow);
    }
}
