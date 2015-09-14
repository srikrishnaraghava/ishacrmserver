package crmdna.participant;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
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
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.venue.Venue;
import crmdna.venue.Venue.VenueProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ParticipantTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private final String validUser = "valid@login.com";
    private final String sgpUser = "sgpuser@login.com";
    private final String klUser = "kluser@login.com";

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

    VenueProp giis;
    VenueProp chaichee;
    VenueProp gujarathiBhavan;
    VenueProp yuhuaCC;
    VenueProp woodlandsCC;

    TeacherProp tina;
    TeacherProp thulasi;
    TeacherProp muthu;

    ProgramProp ishaKriya18Aug2013;
    ProgramProp ishaKriya25Aug2013;

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
                GroupLevelPrivilege.UPDATE_PROGRAM, User.SUPER_USER);

        User.create(client, klUser, kl.groupId, User.SUPER_USER);
        assertEquals(3, User.get(client, klUser).toProp(client).userId);

        User.addGroupLevelPrivilege(client, kl.groupId, klUser,
                GroupLevelPrivilege.UPDATE_PROGRAM, User.SUPER_USER);

        suryaNamaskar = Practice.create(client, "Surya Namaskar",
                User.SUPER_USER);
        yogaAsanas = Practice.create(client, "Yoga Asanas", User.SUPER_USER);
        shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER);
        ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);
        aumChanting = Practice.create(client, "Aum Chanting", User.SUPER_USER);

        Set<Long> practiceIds = new HashSet<Long>();
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

        giis = Venue.create(client, "GIIS", "GIIS", sgp.groupId,
                User.SUPER_USER);
        chaichee = Venue.create(client, "Chai Chee", "Chai Chee", sgp.groupId,
                User.SUPER_USER);
        gujarathiBhavan = Venue.create(client, "Gujarathi Bhavan",
                "Gujarathi Bhavan", sgp.groupId, User.SUPER_USER);
        yuhuaCC = Venue.create(client, "Yuhua CC", "Yuhua CC", sgp.groupId,
                User.SUPER_USER);
        woodlandsCC = Venue.create(client, "Woodlands CC", "Woodlands CC",
                sgp.groupId, User.SUPER_USER);

        tina = Teacher.create(client, "", "", "tina@ishafoundation.org", sgp.groupId,
                User.SUPER_USER);
        thulasi = Teacher.create(client, "", "", "thulasidhar@gmail.com", sgp.groupId,
                User.SUPER_USER);
        muthu = Teacher.create(client, "", "", "muthu_sys@yahoo.com", sgp.groupId,
                User.SUPER_USER);

        ishaKriya18Aug2013 = Program.create(client, sgp.groupId,
                ishaKriyaTeacherLed.programTypeId, woodlandsCC.venueId,
                muthu.teacherId, 20130818, 20130818, 1, null, 0, null,
                User.SUPER_USER);

        ishaKriya25Aug2013 = Program.create(client, sgp.groupId,
                ishaKriyaTeacherLed.programTypeId, woodlandsCC.venueId,
                muthu.teacherId, 20130825, 20130825, 1, null, 0, null,
                User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    // @Test
    // public void uploadAllTest() {
    // ContactProp c1 = new ContactProp();
    // c1.email = "oasisram@gmail.com";
    // c1.firstName = "Ramakrishnan";
    //
    // List<ContactProp> contactDetailProps = new ArrayList<>();
    // contactDetailProps.add(c1);
    //
    // UploadReportProp uploadReportProp = Participant.uploadAll(client,
    // contactDetailProps, ishaKriya18Aug2013.programId, false,
    // User.SUPER_USER);
    //
    // assertEquals(1, uploadReportProp.numParticipants);
    // assertEquals(1, uploadReportProp.newMemberEmails);
    // assertEquals(0, uploadReportProp.existingMemberEmails);
    //
    // List<ParticipantProp> participantProps = Participant.getAll(client,
    // ishaKriya18Aug2013.programId, User.SUPER_USER);
    //
    // assertEquals(1, participantProps.size());
    // ParticipantProp participantProp = participantProps.get(0);
    //
    // assertEquals(1, participantProp.participantId); // first participant
    // // should have id 1
    //
    // assertEquals("Ramakrishnan", participantProp.contactDetail.firstName);
    // assertEquals(null, participantProp.contactDetail.lastName);
    // assertEquals(null, participantProp.contactDetail.mobilePhone);
    // assertEquals(1, participantProp.memberId); // first member
    // assertEquals(ishaKriya18Aug2013.programId, participantProp.programId);
    //
    // // should be able to get the associated member
    // MemberProp memberProp = Member.safeGet(client,
    // participantProp.memberId, User.SUPER_USER).toProp();
    // assertEquals(participantProp.memberId, memberProp.memberId);
    // assertEquals(participantProp.contactDetail.firstName,
    // memberProp.contact.firstName);
    // assertEquals(null, memberProp.contact.lastName);
    // assertEquals("oasisram@gmail.com", memberProp.contact.email);
    // assertTrue(memberProp.programIds.contains(ishaKriya18Aug2013.programId));
    // assertTrue(memberProp.programTypeIds
    // .contains(ishaKriyaTeacherLed.programTypeId));
    // assertTrue(memberProp.practiceIds.contains(ishaKriya.practiceId));
    //
    // // add the same person as participant for another program. This time he
    // specifies
    // //his contact number and house address
    // c1.mobilePhone = "+6591846937";
    // c1.homeAddress.address = "Block 292B, #09-210, Compassvale Street";
    // c1.homeAddress.country = "Singapore";
    // uploadReportProp = Participant.uploadAll(client, contactDetailProps,
    // ishaKriya25Aug2013.programId, false, User.SUPER_USER);
    // assertEquals(1, uploadReportProp.numParticipants);
    // assertEquals(1, uploadReportProp.existingMemberEmails);
    // assertEquals(0, uploadReportProp.newMemberEmails);
    //
    // // should be tagged to the same member
    // participantProps = Participant.getAll(client,
    // ishaKriya25Aug2013.programId, User.SUPER_USER);
    //
    // assertEquals(1, participantProps.size());
    // participantProp = participantProps.get(0);
    //
    // assertEquals(2, participantProp.participantId); // id should be sequence
    // assertEquals("Ramakrishnan", participantProp.contactDetail.firstName);
    // assertEquals(null, participantProp.contactDetail.lastName);
    // assertEquals(null, participantProp.contactDetail.mobilePhone);
    // assertEquals(1, participantProp.memberId); // same member
    // assertEquals(ishaKriya25Aug2013.programId, participantProp.programId);
    //
    // // delete all participants, update participants list and upload again
    //
    // Participant.deleteAll(client, ishaKriya25Aug2013.programId,
    // User.SUPER_USER);
    //
    // // add somebody else who has given the same email
    // c1 = new ContactProp();
    // c1.email = "oasisram@gmail.com";
    // c1.firstName = "Hemamalini";
    // c1.lastName = "Krishnamurthy";
    // c1.mobilePhone = "+6593232152";
    // c1.homePhone = "+6565072230";
    // contactDetailProps.add(c1);
    // uploadReportProp = Participant.uploadAll(client, contactDetailProps,
    // ishaKriya25Aug2013.programId, false, User.SUPER_USER);
    //
    // assertEquals(2, uploadReportProp.numParticipants);
    // assertEquals(1, uploadReportProp.existingMemberEmails);
    // assertEquals(1, uploadReportProp.newMemberEmails);
    //
    // participantProps = Participant.getAll(client,
    // ishaKriya18Aug2013.programId, User.SUPER_USER);
    //
    // assertEquals(3, participantProp.participantId); // id should be sequence
    // assertEquals("Hemamalini", participantProp.contactDetail.firstName);
    // assertEquals("Krishnamurthy", participantProp.contactDetail.lastName);
    // assertEquals(null, participantProp.contactDetail.mobilePhone);
    // // should be tagged to a different member
    // assertEquals(2, participantProp.memberId); // same member
    //
    // memberProp = Member.safeGet(client, participantProp.memberId,
    // User.SUPER_USER).toProp();
    // assertEquals(participantProp.memberId, memberProp.memberId);
    // assertEquals(participantProp.contactDetail.firstName,
    // memberProp.contact.firstName);
    // assertEquals("oasisram@gmail.com", memberProp.contact.email);
    // assertTrue(memberProp.programIds.contains(ishaKriya18Aug2013.programId));
    // assertTrue(memberProp.programTypeIds
    // .contains(ishaKriyaTeacherLed.programTypeId));
    // assertTrue(memberProp.practiceIds.contains(ishaKriya.practiceId));
    //
    // // // add same person with a typo in the name
    // // c1 = new ContactDetailProp();
    // // c1.email = "oasisram@gmail.com";
    // // c1.firstName = "Ramkrishnan";
    // // // should get linked to the same member
    // // participantProp = Participant.createWithoutDuplicateCheck(client, c1,
    // // ishaKriya25Aug2013.programId, User.SUPER_USER);
    // // memberProp = Member.safeGet(client, participantProp.memberId,
    // // User.SUPER_USER).toProp();
    // // assertEquals(participantProp.memberId, memberProp.memberId);
    // // assertEquals(participantProp.contactDetail.firstName,
    // // memberProp.contact.firstName);
    // // assertEquals("oasisram@gmail.com", memberProp.contact.email);
    // //
    // assertTrue(memberProp.programIds.contains(ishaKriya18Aug2013.programId));
    // // assertTrue(memberProp.programTypeIds
    // // .contains(ishaKriyaTeacherLed.programTypeId));
    // // assertTrue(memberProp.practiceIds.contains(ishaKriya.practiceId));
    //
    // assertTrue(false);
    // }

    @Test
    public void getTest() {
        assertEquals("force failure", true, false);
    }

    @Test
    public void safeGetTest() {
        assertTrue(false);
    }

    @Test
    public void updateTest() {
        assertTrue(false);
    }

    @Test
    public void getQSMatchesTest() {
        assertTrue(false);
    }

    @Test
    public void bulkUploadTest() {
        assertTrue(false);
    }

    @Test
    public void deleteTest() {
        assertTrue(false);
    }
}
