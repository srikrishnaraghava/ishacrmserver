package crmdna.member;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.Key;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.DateUtils.Month;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.list.ListProp;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.programtype.ProgramType;
import crmdna.programtype.ProgramTypeProp;
import crmdna.teacher.Teacher;
import crmdna.teacher.Teacher.TeacherProp;
import crmdna.user.User;
import crmdna.venue.Venue;
import crmdna.venue.Venue.VenueProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MemberLoaderTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private GroupProp sgp;
    private GroupProp kl;

    private MemberProp sathya;
    private MemberProp thulasi;
    private MemberProp chitra;

    private String validUser = "validuser@valid.com";

    private PracticeProp suryaKriya;
    private ProgramTypeProp suryaKriya2Day;
    private TeacherProp sharmila;
    private VenueProp grassRootsClub;
    private ProgramProp suryaKriyaDec2013;

    private ProgramTypeProp sahajaSthithiYoga;
    private PracticeProp shoonya, shakthiChalanaKriya;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals("first group id is 1", 1, sgp.groupId);

        kl = Group.create(client, "KL", User.SUPER_USER);
        assertEquals("second group id is 2", 2, kl.groupId);

        ContactProp contact = new ContactProp();
        contact.firstName = "Sathyanarayanan";
        contact.lastName = "Thilakan";
        contact.email = "sathya.t@ishafoundation.org";
        contact.asOfyyyymmdd = 20140727;

        sathya = Member.create(client, sgp.groupId, contact, false, User.SUPER_USER);
        assertEquals("first member id is 1", 1, sathya.memberId);

        contact = new ContactProp();
        contact.firstName = "Thulasidhar";
        contact.lastName = "Kosalram";
        contact.email = "thulasidhar@gmail.com";
        contact.asOfyyyymmdd = 20140727;

        thulasi = Member.create(client, sgp.groupId, contact, false, User.SUPER_USER);
        assertEquals("second member id is 2", 2, thulasi.memberId);

        contact = new ContactProp();
        contact.firstName = "Chitra";
        contact.lastName = "Nair";
        contact.email = "chithra.nair@gmail.com";
        contact.asOfyyyymmdd = 20140727;

        chitra = Member.create(client, kl.groupId, contact, false, User.SUPER_USER);
        assertEquals("third member id is 3", 3, chitra.memberId);

        User.create(client, validUser, sgp.groupId, User.SUPER_USER);

        suryaKriya = Practice.create(client, "Surya Kriya", User.SUPER_USER);
        assertEquals("first practice id is 1", 1, suryaKriya.practiceId);

        shoonya = Practice.create(client, "Shoonya", User.SUPER_USER);
        assertEquals("second practice id is 2", 2, shoonya.practiceId);

        shakthiChalanaKriya = Practice.create(client, "Shakthi chalana kriya", User.SUPER_USER);
        assertEquals("third practice id is 3", 3, shakthiChalanaKriya.practiceId);

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(suryaKriya.practiceId);
        suryaKriya2Day =
                ProgramType.create(client, "Surya Kriya (2 day)", practiceIds, User.SUPER_USER);
        assertEquals("first program type id is 1", 1, suryaKriya2Day.programTypeId);

        practiceIds.clear();
        practiceIds.add(shoonya.practiceId);
        practiceIds.add(shakthiChalanaKriya.practiceId);
        sahajaSthithiYoga =
                ProgramType.create(client, "Sahaja sthithi yoga", practiceIds, User.SUPER_USER);

        grassRootsClub =
                Venue.create(client, "Grass roots club", "Grass roots club", sgp.groupId, User.SUPER_USER);
        assertEquals("first venueid is 1", 1, grassRootsClub.venueId);

        sharmila = Teacher.create(client, "Sharmila", "Napa", "sharmila.napa@gmail.com", sgp.groupId, User.SUPER_USER);
        assertEquals("first teacher id is 1", 1, sharmila.teacherId);

        suryaKriyaDec2013 =
                Program.create(client, sgp.groupId, suryaKriya2Day.programTypeId, grassRootsClub.venueId,
                        sharmila.teacherId, 20131226, 20131227, 1, null, 0.0, null, User.SUPER_USER);
        assertEquals("first program id is 1", suryaKriyaDec2013.programId, 1);

        contact = new ContactProp();
        contact.firstName = "Sathya";
        contact.lastName = "Thilakan";
        contact.email = "sathya.t@ishafoundation.org";
        contact.asOfyyyymmdd = 20140727;
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void safeGetTest() {

        // valid user can get member info
        MemberEntity entity = MemberLoader.safeGet(client, thulasi.memberId, validUser);
        assertEquals("valid user can retrieve member record", thulasi.contact.email, entity.email);
        assertEquals("firstname correct", thulasi.contact.firstName, entity.firstName);
        assertEquals("lastname correct", thulasi.contact.lastName, entity.lastName);

        // member can retrieve own record
        entity = MemberLoader.safeGet(client, thulasi.memberId, thulasi.contact.email);
        assertEquals("member can retrieve own record", thulasi.contact.email, entity.email);
        assertEquals("member can retrieve own record", thulasi.contact.firstName, entity.firstName);
        assertEquals("member can retrieve own record", thulasi.contact.lastName, entity.lastName);

        // sathya cannot retrieve thulasi's record
        try {
            entity = MemberLoader.safeGet(client, thulasi.memberId, sathya.contact.email);
            assertTrue("member cannot retrieve someone else's record", false);
        } catch (APIException ex) {
            assertEquals("member cannot retrieve someone else's record", Status.ERROR_INVALID_USER,
                    ex.statusCode);
        }

        // case when entity doesn't have email address
        ContactProp contact = new ContactProp();
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        contact.firstName = "Syamala";
        contact.mobilePhone = "+918754509947";
        MemberProp syamala = Member.create(client, sgp.groupId, contact, false, validUser);
        assertTrue(syamala.memberId != 0);

        // sathya cannot retrieve thulasi's record
        try {
            entity = MemberLoader.safeGet(client, syamala.memberId, sathya.contact.email);
            assertTrue("member cannot retrieve someone else's record", false);
        } catch (APIException ex) {
            assertEquals("member cannot retrieve someone else's record", Status.ERROR_INVALID_USER,
                    ex.statusCode);
        }

        // client should be valid
        try {
            MemberLoader.safeGet(invalidClient, thulasi.memberId, User.SUPER_USER);
            assertTrue("client should be valid", false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void canSafeGetById() {
        MemberEntity memberEntity =
                MemberLoader.safeGetByIdOrEmail(client, sathya.memberId + "", User.SUPER_USER);
        assertEquals(sathya.memberId, memberEntity.memberId);
    }

    @Test
    public void canSafeGetByEmail() {
        MemberEntity memberEntity =
                MemberLoader.safeGetByIdOrEmail(client, "sathya.t@ishafoundation.org", User.SUPER_USER);

        assertEquals("sathya.t@ishafoundation.org", memberEntity.email);
    }

    public void safeGetByEmailThrowsIfMultipleMembers() {
        ContactProp contactProp = new ContactProp();
        contactProp.email = "duplicate@duplicate.com";
        contactProp.asOfyyyymmdd = 20141014;

        MemberProp memberProp1 = Member.create(client, sgp.groupId, contactProp, true, User.SUPER_USER);
        MemberProp memberProp2 = Member.create(client, sgp.groupId, contactProp, true, User.SUPER_USER);

        assertEquals(memberProp1.memberId + 1, memberProp2.memberId);

        try {
            MemberLoader.safeGetByIdOrEmail(client, contactProp.email, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test(expected = APIException.class)
    public void safeGetByEmailThrowsIfNoMatchingMembers() {
        MemberLoader.safeGetByIdOrEmail(client, "dummydummydummy@invaliddummy.com", User.SUPER_USER);
    }

    @Test
    public void queryKeysTest() {
        MemberQueryCondition mqc = new MemberQueryCondition(client, 10);
        mqc.searchStr = "@gmail";

        List<Key<MemberEntity>> keys = MemberLoader.queryKeys(mqc, validUser);
        // should return thulasi and chittra
        assertEquals("can query by searchStr", 2, keys.size());

        mqc.searchStr = "sathya thil";
        // should return sathya
        keys = MemberLoader.queryKeys(mqc, validUser);
        // should return thulasi and chittra
        assertEquals("can query by searchStr", 1, keys.size());

        mqc.groupIds.add(sgp.groupId);
        keys = MemberLoader.queryKeys(mqc, validUser);
        // should return only thulasi
        assertEquals("can query by searchStr and group id", 1, keys.size());

        mqc = new MemberQueryCondition(client, 10);
        mqc.firstName3Chars.add("thu");
        mqc.firstName3Chars.add("sat");

        keys = MemberLoader.queryKeys(mqc, validUser);
        // should return thulasi and sathya
        assertEquals("can query by firstName3Chars", 2, keys.size());
    }

    @Test
    public void queryByNameFirstCharTest() {

        MemberQueryCondition mqc = new MemberQueryCondition(client, 100);

        //a - should match no one
        mqc.nameFirstChar = "a";
        List<MemberProp> memberProps = MemberLoader.queryProps(mqc, User.SUPER_USER);
        assertEquals(0, memberProps.size());

        //s - should match sathya.t@ishafoundation.org
        mqc.nameFirstChar = "s";
        memberProps = MemberLoader.queryProps(mqc, User.SUPER_USER);
        assertEquals(1, memberProps.size());
        assertEquals("sathya.t@ishafoundation.org", memberProps.get(0).contact.email);
    }

    @Test
    public void getCountTest() {
        MemberQueryCondition mqc = new MemberQueryCondition(client, 10);
        mqc.searchStr = "@gmail";

        int count = MemberLoader.getCount(mqc, validUser);
        // should return thulasi and chittra
        assertEquals("can query by searchStr", 2, count);

        mqc.searchStr = "sathya thil";
        // should return sathya
        count = MemberLoader.getCount(mqc, validUser);
        // should return thulasi and chittra
        assertEquals("can query by searchStr", 1, count);

        mqc.groupIds.add(sgp.groupId);
        count = MemberLoader.getCount(mqc, validUser);
        // should return only thulasi
        assertEquals("can query by searchStr and group id", 1, count);

        mqc = new MemberQueryCondition(client, 10);
        mqc.firstName3Chars.add("thu");
        mqc.firstName3Chars.add("sat");

        count = MemberLoader.getCount(mqc, validUser);
        // should return thulasi and sathya
        assertEquals("can query by firstName3Chars", 2, count);
    }

    @Test
    public void quickSearchTest() {
        List<MemberProp> props = MemberLoader.quickSearch(client, "gmail", null, 10, validUser);

        assertEquals("correct no of records", 2, props.size());
        assertEquals("sorted by name", chitra.memberId, props.get(0).memberId);
        assertEquals("first name populalated", chitra.contact.firstName, props.get(0).contact.firstName);
        assertEquals("last name populalated", chitra.contact.lastName, props.get(0).contact.lastName);
        assertEquals("email populalated", chitra.contact.email, props.get(0).contact.email);

        assertEquals("sorted by name", thulasi.memberId, props.get(1).memberId);
        assertEquals("first name populalated", thulasi.contact.firstName,
                props.get(1).contact.firstName);
        assertEquals("last name populalated", thulasi.contact.lastName, props.get(1).contact.lastName);
        assertEquals("email populalated", thulasi.contact.email, props.get(1).contact.email);

        assertTrue("group populated in member prop", props.get(0).groups.contains(kl.displayName));
        assertTrue("group populated in member prop", props.get(1).groups.contains(sgp.displayName));
    }

    @Test
    public void safeGetDetailedInfoTest() {
        // sathya has a verified program surya kriya and unverified program
        // sahaja sthithi yoga
        Member.addOrDeleteProgram(client, sathya.memberId, suryaKriyaDec2013.programId, true,
                User.SUPER_USER);

        Member.addUnverifiedProgram(client, sathya.memberId, sahajaSthithiYoga.programTypeId,
                Month.MAY, 2000, "Chennai", "India", "Swaminathan", validUser);

        //create some lists and add to the member
        ListProp sgpList = crmdna.list.List.createPublic(client, sgp.groupId, "SGP List", User.SUPER_USER);
        ListProp klList = crmdna.list.List.createPublic(client, kl.groupId, "KL List", User.SUPER_USER);

        Member.addOrDeleteList(client, sathya.memberId, sgpList.listId, true, User.SUPER_USER);
        Member.subscribeGroup(client, sathya.memberId, sgp.groupId, User.SUPER_USER);
        Member.addOrDeleteList(client, sathya.memberId, klList.listId, true, User.SUPER_USER);
        Member.subscribeGroup(client, sathya.memberId, kl.groupId, User.SUPER_USER);
        Member.unsubscribeGroup(client, sathya.memberId, kl.groupId, User.SUPER_USER);

        sathya = MemberLoader.safeGetDetailedInfo(client, sathya.memberId, validUser);
        assertTrue("can get detailed info", sathya != null);

        assertTrue("practices populated", sathya.practices.contains(suryaKriya.displayName));
        assertTrue("practices populated", sathya.practices.contains(shoonya.displayName));
        assertTrue("practices populated", sathya.practices.contains(shakthiChalanaKriya.displayName));
        assertEquals("practices populated", 3, sathya.practices.size());

        assertEquals("both verified and unverified programs populated", 2,
                sathya.memberProgramProps.size());
        assertEquals("programs sorted by date", suryaKriya2Day.programTypeId,
                sathya.memberProgramProps.get(0).programTypeId);
        assertEquals("month ok", Month.DEC, sathya.memberProgramProps.get(0).month);
        assertEquals("year ok", 2013, sathya.memberProgramProps.get(0).year);
        assertEquals("teacher ok", sharmila.firstName + " " + sharmila.lastName, sathya.memberProgramProps.get(0).teacher);
        assertEquals("group ok", sgp.displayName, sathya.memberProgramProps.get(0).groupOrCity);

        assertEquals("programs sorted by date", sahajaSthithiYoga.programTypeId,
                sathya.memberProgramProps.get(1).programTypeId);
        assertEquals("month ok", Month.MAY, sathya.memberProgramProps.get(1).month);
        assertEquals("year ok", 2000, sathya.memberProgramProps.get(1).year);
        assertEquals("teacher ok", "Swaminathan", sathya.memberProgramProps.get(1).teacher);
        assertEquals("group ok", "Chennai", sathya.memberProgramProps.get(1).groupOrCity);

        //subscribed lists
        assertEquals(2, sathya.listProps.size());
        //assertEquals("Singapore", sathya.subscribedListProps.get(0).groupName);
        //assertEquals("SGP List", sathya.subscribedListProps.get(0).displayName);

        //subscribed and unsubscribed groups
        assertEquals(1, sathya.subscribedGroupIds.size());
        assertTrue(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertEquals(1, sathya.unsubscribedGroupIds.size());
        assertTrue(sathya.unsubscribedGroupIds.contains(kl.groupId));
    }

    @Test
    public void listsSortedInSafeGetDetailedInfo() {
        //create some lists and add to the member
        ListProp sgpList1 = crmdna.list.List.createPublic(client, sgp.groupId, "SGP List 1", User.SUPER_USER);
        ListProp sgpList2 = crmdna.list.List.createPublic(client, sgp.groupId, "SGP List 2", User.SUPER_USER);
        ListProp klList1 = crmdna.list.List.createPublic(client, kl.groupId, "KL List 1", User.SUPER_USER);
        ListProp klList2 = crmdna.list.List.createPublic(client, kl.groupId, "KL List 2", User.SUPER_USER);

        Member.addOrDeleteList(client, sathya.memberId, sgpList1.listId, true, User.SUPER_USER);
        Member.addOrDeleteList(client, sathya.memberId, sgpList2.listId, true, User.SUPER_USER);
        Member.addOrDeleteList(client, sathya.memberId, klList1.listId, true, User.SUPER_USER);
        Member.addOrDeleteList(client, sathya.memberId, klList2.listId, true, User.SUPER_USER);

        sathya = MemberLoader.safeGetDetailedInfo(client, sathya.memberId, validUser);

        // subscribed list should be sorted by list name
        List<ListProp> listProps = sathya.listProps;
        assertEquals(4, listProps.size());
        assertEquals("KL List 1", listProps.get(0).displayName);
        assertEquals("KL List 2", listProps.get(1).displayName);
        assertEquals("SGP List 1", listProps.get(2).displayName);
        assertEquals("SGP List 2", listProps.get(3).displayName);
    }

    @Test
    public void getUnsubscribedEmailsTest() {
        Member.subscribeGroup(client, sathya.memberId, sgp.groupId, User.SUPER_USER);
        Member.unsubscribeGroup(client, sathya.memberId, sgp.groupId, User.SUPER_USER);

        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertTrue(sathya.unsubscribedGroupIds.contains(sgp.groupId));

        TreeSet<String> unsubscribedEmails =
                MemberLoader.getUnsubscribedEmails(client, sgp.groupId, validUser);
        assertTrue(unsubscribedEmails.contains(sathya.contact.email));
        assertEquals(1, unsubscribedEmails.size());
    }
}
