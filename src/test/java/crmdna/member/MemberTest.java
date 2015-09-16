package crmdna.member;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.Contact.Gender;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.programtype.ProgramType;
import crmdna.programtype.ProgramTypeProp;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
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

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemberTest {

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
    public void createTest() {
        // client should be valid
        try {
            Member.create(invalidClient, sgp.groupId, null, false, invalidUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // should not be invalid user
        try {
            Member.create(client, sgp.groupId, null, false, invalidUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
        }

        // group id should be valid
        try {
            Member.create(client, 100, null, false, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // contact details should be valid
        ContactProp contact = new ContactProp();
        contact.firstName = "sathya"; // no email or phone number
        contact.gender = Gender.MALE;
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());

        try {
            Member.create(client, sgp.groupId, contact, false, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        contact.email = "sathya.t@ishafoundation.org";
        MemberProp prop = Member.create(client, sgp.groupId, contact, false,
                validUser);

        prop = MemberLoader.safeGet(client, prop.memberId, validUser).toProp();
        assertEquals("sathya.t@ishafoundation.org", prop.contact.email);
        assertEquals(Gender.MALE, prop.contact.gender);

        // cannot add the same email again
        try {
            Member.create(client, sgp.groupId, contact, false, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        // can add member without email but with valid phone number
        contact.email = null;
        contact.homePhone = "+6565227030";
        contact.gender = null;
        prop = Member.create(client, sgp.groupId, contact, false, validUser);

        prop = MemberLoader.safeGet(client, prop.memberId, userWithPermission)
                .toProp();
        assertEquals("+6565227030", prop.contact.homePhone);
        assertEquals(null, prop.contact.email);
        assertEquals(null, prop.contact.gender);

        // can add email again if allowDuplicateEmail is true
        contact.email = "sathya.t@ishafoundation.org";
        prop = Member.create(client, sgp.groupId, contact, true, validUser);
        assertEquals("sathya.t@ishafoundation.org", prop.contact.email);
    }

    @Test
    public void updateContactDetailsTest() {
        ContactProp contact = new ContactProp();
        contact.firstName = "sathya";
        contact.email = "sathya.t@ishafoundation.org";
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        MemberProp prop = Member.create(client, sgp.groupId, contact, false,
                validUser);
        assertEquals(1, prop.memberId);

        // change first name
        contact = new ContactProp();
        contact.firstName = "Sathyanarayanan";
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        Member.updateContactDetails(client, prop.memberId, contact, validUser);
        prop = MemberLoader.safeGet(client, prop.memberId, userWithPermission)
                .toProp();
        assertEquals("Sathyanarayanan", prop.contact.firstName);
        assertEquals(null, prop.contact.gender);

        // multiple changes
        contact = new ContactProp();
        contact.firstName = "Sathya";
        contact.lastName = "Thilakan";
        contact.mobilePhone = "+6598361844";
        contact.homePhone = "+6565227030";
        contact.officePhone = "+6563083013";
        contact.gender = Gender.MALE;
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        Member.updateContactDetails(client, prop.memberId, contact, validUser);
        prop = MemberLoader.safeGet(client, prop.memberId, userWithPermission)
                .toProp();
        assertEquals("Sathya", prop.contact.firstName);
        assertEquals("Thilakan", prop.contact.lastName);
        assertEquals("+6598361844", prop.contact.mobilePhone);
        assertEquals("+6565227030", prop.contact.homePhone);
        assertEquals("+6563083013", prop.contact.officePhone);
        assertEquals(Gender.MALE, prop.contact.gender);

        // phone no cannot be invalid
        contact = new ContactProp();
        contact.mobilePhone = "9384938ad";
        try {
            Member.updateContactDetails(client, prop.memberId, contact,
                    validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // email cannot be invalid
        contact = new ContactProp();
        contact.email = "sathyafkdjf";
        try {
            Member.updateContactDetails(client, prop.memberId, contact,
                    validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // email cannot clash with another member
        contact = new ContactProp();
        contact.firstName = "sathya isha";
        contact.email = "sathya.isha@gmail.com";
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        Member.create(client, sgp.groupId, contact, false, validUser);

        contact = new ContactProp();
        contact.email = "sathya.isha@gmail.com";
        try {
            Member.updateContactDetails(client, prop.memberId, contact,
                    validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }
    }

    @Test
    public void addDeleteUniverifiedProgramTest() {
        // available in a seperate file: UnverifiedProgramTest
    }

    // @Test
    // public void queryTest() {
    //
    // ContactProp contact = new ContactProp();
    // contact.email = "Sathya.t@IshaFoundation.org";
    // contact.mobilePhone = "+6598361844";
    // contact.homePhone = "+6565227030";
    //
    // MemberProp sathya = Member.create(client, sgp.groupId, contact,
    // User.SUPER_USER);
    // assertEquals(1, sathya.memberId);
    //
    // try {
    // Member.query("isha", "SathYa", null, null, null, null, invalidUser);
    // assertTrue(false);
    // } catch (APIException ex) {
    // assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
    // }
    //
    // // search string cannot be less than 3 char
    // try {
    // Member.query("isha", "Sa", null, null, null, null, validUser);
    // assertTrue(false);
    // } catch (APIException ex) {
    // assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
    // }
    //
    // List<MemberProp> searchResults = Member.query("isha", "SathYa", null,
    // null, null, null, validUser);
    // assertEquals(1, searchResults.size());
    //
    // searchResults = Member.query("isha", "sathyat", null, null, null, null,
    // validUser);
    // assertEquals(0, searchResults.size());
    //
    // // add few more members
    // contact = new ContactProp();
    // contact.firstName = "Amit";
    // contact.email = "amit@gmail.com";
    // MemberProp amit = Member.create(client, sgp.groupId, contact,
    // User.SUPER_USER);
    // assertEquals(2, amit.memberId);
    //
    // contact = new ContactProp();
    // contact.firstName = "Shiva";
    // contact.email = "sjawaji@gmail.com";
    // MemberProp shiva = Member.create(client, sydney.groupId, contact,
    // User.SUPER_USER);
    // assertEquals(3, shiva.memberId);
    //
    // contact = new ContactProp();
    // contact.firstName = "Anantha";
    // contact.email = "anantha@gmail.com";
    // MemberProp anantha = Member.create(client, kl.groupId, contact,
    // User.SUPER_USER);
    // assertEquals(4, anantha.memberId);
    //
    // // search for gmail without specifying group
    // searchResults = Member.query("isha", "Gmail", null, null, null, null,
    // validUser);
    // assertEquals(3, searchResults.size());
    // // should be sorted by name
    // assertEquals("Amit", searchResults.get(0).contact.firstName);
    // assertEquals("Anantha", searchResults.get(1).contact.firstName);
    // assertEquals("Shiva", searchResults.get(2).contact.firstName);
    //
    // // search for gmail only for sgp and kl
    // Set<Long> groupIds = new HashSet<>();
    // groupIds.add(sgp.groupId);
    // groupIds.add(kl.groupId);
    // searchResults = Member.query("isha", "Gmail", groupIds, null, null,
    // null, validUser);
    // assertEquals(2, searchResults.size());
    // // should be sorted by name
    // assertEquals("Amit", searchResults.get(0).contact.firstName);
    // assertEquals("Anantha", searchResults.get(1).contact.firstName);
    //
    // // search phone number
    // searchResults = Member.query("isha", "844", groupIds, null, null, null,
    // validUser); // matches sathya
    // assertEquals(1, searchResults.size());
    // assertEquals("sathya.t@ishafoundation.org", searchResults.get(0).name);
    //
    // // create a shambhavi program and mystic eye program
    // // shambhavi;//i = Practice.create(client, "Shambhavi",
    // // User.SUPER_USER);
    // PracticeProp mysticEyePractice = Practice.create(client,
    // "Mystic Eye Pracice", User.SUPER_USER);
    // Set<Long> practiceIds = new HashSet<>();
    // practiceIds.add(shambhavi.practiceId);
    // ProgramTypeProp ie7day = ProgramType.create(client, "IE 7 Day",
    // practiceIds, User.SUPER_USER);
    //
    // practiceIds.clear();
    // practiceIds.add(mysticEyePractice.practiceId);
    // ProgramTypeProp mysticEye = ProgramType.create(client, "MysticEye1Day",
    // practiceIds, User.SUPER_USER);
    // VenueProp notFixed = Venue.create(client, "Not Fixed", "NA",
    // sgp.groupId, User.SUPER_USER);
    // TeacherProp notFixed2 = Teacher.create(client, "dummy@dummy.com",
    // sgp.groupId, User.SUPER_USER);
    // ProgramProp ie7day2011 = Program.create(client, sgp.groupId,
    // ie7day.programTypeId, notFixed.venueId, notFixed2.teacherId,
    // 20110505, 20110511, 1, null, 0, null, User.SUPER_USER);
    // ProgramProp mysticEyeJan14 = Program.create(client, sgp.groupId,
    // mysticEye.programTypeId, notFixed.venueId, notFixed2.teacherId,
    // 20140112, 20140112, 1, null, 0, null, User.SUPER_USER);
    // Member.addOrDeleteProgram(client, sathya.memberId,
    // ie7day2011.programId, true, User.SUPER_USER);
    // Member.addOrDeleteProgram(client, amit.memberId, ie7day2011.programId,
    // true, User.SUPER_USER);
    // Member.addOrDeleteProgram(client, shiva.memberId, ie7day2011.programId,
    // true, User.SUPER_USER);
    // Member.addOrDeleteProgram(client, anantha.memberId,
    // ie7day2011.programId, true, User.SUPER_USER);
    //
    // // mystic eye only for amit
    // Member.addOrDeleteProgram(client, amit.memberId,
    // mysticEyeJan14.programId, true, User.SUPER_USER);
    //
    // // search for both mysticeye - only amit
    // practiceIds.add(shambhavi.practiceId);
    // practiceIds.add(mysticEyePractice.practiceId);
    // List<MemberProp> memberProps = Member.query(client, "amit", null,
    // practiceIds, null, null, User.SUPER_USER);
    // assertEquals(1, memberProps.size());
    //
    // // search for all members that are initiated into shambhavi
    // practiceIds.clear();
    // practiceIds.add(shambhavi.practiceId);
    // memberProps = Member.query(client, null, null, practiceIds, null, null,
    // User.SUPER_USER);
    // System.out.println("memberProps = " + new Gson().toJson(memberProps));
    // assertEquals(4, memberProps.size());
    //
    // // limit size to 1
    // memberProps = Member.query(client, null, null, practiceIds, null, 1,
    // User.SUPER_USER);
    // assertEquals(1, memberProps.size());
    //
    // // limit to 4
    // memberProps = Member.query(client, null, null, practiceIds, null, 4,
    // User.SUPER_USER);
    // assertEquals(4, memberProps.size());
    //
    // // query by program
    // assertTrue(false);
    // }

    @Test
    public void addOrDeleteGroupTest() {
        ContactProp contact = new ContactProp();
        contact.email = "Sathya.t@IshaFoundation.org";
        contact.mobilePhone = "+6598361844";
        contact.homePhone = "+6565227030";
        contact.asOfyyyymmdd = 20141021;

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
        prop = MemberLoader.safeGet(client, prop.memberId, userWithPermission)
                .toProp();
        assertEquals(2, prop.groupIds.size());
        assertTrue(prop.groupIds.contains(sgp.groupId));
        assertTrue(prop.groupIds.contains(kl.groupId));

        // can add the same center again
        Member.addOrDeleteGroup(client, prop.memberId, kl.groupId, true,
                validUser);
        ObjectifyFilter.complete();
        prop = MemberLoader.safeGet(client, prop.memberId, userWithPermission)
                .toProp();
        assertEquals(2, prop.groupIds.size());
        assertTrue(prop.groupIds.contains(sgp.groupId));
        assertTrue(prop.groupIds.contains(kl.groupId));

        // remove center
        Member.addOrDeleteGroup(client, prop.memberId, kl.groupId, false,
                validUser);
        ObjectifyFilter.complete();
        prop = MemberLoader.safeGet(client, prop.memberId, userWithPermission)
                .toProp();
        assertEquals(1, prop.groupIds.size());
        assertTrue(prop.groupIds.contains(sgp.groupId));
        assertTrue(!prop.groupIds.contains(kl.groupId));

        // can remove center again
        Member.addOrDeleteGroup(client, prop.memberId, kl.groupId, false,
                validUser);
        ObjectifyFilter.complete();
        prop = MemberLoader.safeGet(client, prop.memberId, userWithPermission)
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
    public void getMatchingMemberIdsTest() {
        ContactProp contactDetailProp = new ContactProp();
        List<ContactProp> contactDetailProps = new ArrayList<>();

        List<Long> matching = Member.getMatchingMemberIds(client,
                contactDetailProps);
        assertEquals(0, matching.size());

        contactDetailProps.clear();
        contactDetailProp.email = "sathya.t@ishafoundation.org";
        contactDetailProps.add(contactDetailProp);
        matching = Member.getMatchingMemberIds(client, contactDetailProps);
        assertEquals(1, matching.size());
        assertEquals(null, matching.get(0));

        ContactProp c = new ContactProp();
        c.firstName = "Sathyanarayanan";
        c.lastName = "Thilakan";
        c.email = "sathya.t@ishafoundation.org";
        c.mobilePhone = "+6593232152";
        c.asOfyyyymmdd = 20141021;
        MemberProp sathya = Member.create(client, sgp.groupId, c, false,
                User.SUPER_USER);

        c = new ContactProp();
        c.firstName = "Thulasidhar";
        c.lastName = "Kosalram";
        c.email = "thulasidhar@gmail.com";
        c.mobilePhone = "+6593705371";
        c.asOfyyyymmdd = 20141021;
        MemberProp thulasi = Member.create(client, sgp.groupId, c, false,
                User.SUPER_USER);

        c = new ContactProp();
        c.firstName = "Sharmila";
        c.lastName = "Napa";
        c.email = "sharmila@bhairaviyoga.sg";
        c.asOfyyyymmdd = 20141021;
        MemberProp sharmila = Member.create(client, sgp.groupId, c, false,
                User.SUPER_USER);

        // create contacts
        ContactProp syamala = new ContactProp();
        syamala.firstName = "syamala";
        syamala.mobilePhone = "+6593232152";

        ContactProp thilakan = new ContactProp();
        thilakan.firstName = "thilakan";
        thilakan.email = "rthilakan@gmail.com";

        ContactProp sowmya = new ContactProp();
        sowmya.firstName = "sowmya ramakrishnan";
        sowmya.email = "sathya.t@ishafoundation.org";

        ContactProp sathyaContactDetail = new ContactProp();
        sathyaContactDetail.firstName = "sathya";
        sathyaContactDetail.mobilePhone = "+6593232152";

        contactDetailProps.clear();
        contactDetailProps.add(syamala);
        contactDetailProps.add(thilakan);
        contactDetailProps.add(sowmya);
        contactDetailProps.add(sathyaContactDetail);
        contactDetailProps.add(thulasi.contact);
        contactDetailProps.add(sharmila.contact);

        List<Long> matchingMemberIds = Member.getMatchingMemberIds(client,
                contactDetailProps);
        assertEquals(6, matchingMemberIds.size());
        assertEquals(null, matchingMemberIds.get(0));
        assertEquals(null, matchingMemberIds.get(1));
        assertEquals(null, matchingMemberIds.get(2));
        assertEquals(new Long(sathya.memberId), matchingMemberIds.get(3));
        assertEquals(new Long(thulasi.memberId), matchingMemberIds.get(4));
        assertEquals(new Long(sharmila.memberId), matchingMemberIds.get(5));
    }

    @Test
    public void rebuildTest() {
        assertTrue(false);
    }

    @Test
    public void getCSVTest() {
        PracticeProp shambhavi = Practice.safeGetByIdOrName(client, "shambhavi").toProp();
        PracticeProp shoonya = Practice.create(client, "Shoonya", User.SUPER_USER);
        PracticeProp bsp = Practice.create(client, "Bhava Spandana", User.SUPER_USER);
        PracticeProp samyama = Practice.create(client, "Samyama", User.SUPER_USER);

        List<MemberProp> memberProps = new ArrayList<>();
        MemberProp sathya = new MemberProp();
        sathya.contact = new ContactProp();
        sathya.memberId = Sequence.getNext(client, SequenceType.MEMBER);
        sathya.contact.firstName = "Sathya,";
        sathya.contact.lastName = "Thilakan";
        sathya.contact.email = "sathya.t@ishafoundation.org";
        sathya.contact.mobilePhone = "+6598361844";
        sathya.practiceIds.add(shambhavi.practiceId);
        sathya.practiceIds.add(shoonya.practiceId);
        sathya.practiceIds.add(bsp.practiceId);
        sathya.practiceIds.add(samyama.practiceId);

        MemberProp sowmya = new MemberProp();
        sowmya.memberId = Sequence.getNext(client, SequenceType.MEMBER);
        sowmya.contact = new ContactProp();
        sowmya.contact.firstName = "Sowmya";
        sowmya.contact.email = "sowmya@sowmya.com";
        sowmya.contact.mobilePhone = "+6598361844";
        sowmya.practiceIds.add(shambhavi.practiceId);

        memberProps.add(sathya);
        memberProps.add(sowmya);

        String csv = Member.getCSV(client, memberProps);
        //System.out.println("csv: " + csv);

        StringBuilder expected = new StringBuilder();
        expected.append("First Name, Last Name, Email, Mobile, Home Phone, Office Phone, Shambhavi ?, Shoonya ?, BSP ?, Samyama ?, Isha CRM Member Id\n");
        expected.append("Sathya,Thilakan,sathya.t@ishafoundation.org,+6598361844,,,Yes,Yes,Yes,Yes,1\n");
        expected.append("Sowmya,,sowmya@sowmya.com,+6598361844,,,Yes,No,No,No,2\n");

        assertEquals(expected.toString(), csv);

        //todo add occupation and company name
    }
}
