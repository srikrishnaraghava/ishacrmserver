package crmdna.member;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.DateUtils.Month;
import crmdna.common.ICode;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
import crmdna.programtype.ProgramType;
import crmdna.programtype.ProgramTypeProp;
import crmdna.user.User;
import crmdna.user.UserCore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static crmdna.common.TestUtil.ensureResourceNotFoundException;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UnverifiedProgramTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private final String validUser = "valid@login.com";

    private ProgramTypeProp shambhavi2Day, mysticEye1Day;
    private PracticeProp shambhavi, ishaKriya, upaYogaBasic;

    private MemberProp sathya;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        GroupProp sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        User.create(client, validUser, sgp.groupId, User.SUPER_USER);
        assertEquals(1,
                UserCore.safeGet(client, validUser).toProp(client).userId);

        shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER);
        ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);
        upaYogaBasic = Practice.create(client, "Upa Yoga Basic",
                User.SUPER_USER);

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(shambhavi.practiceId);
        shambhavi2Day = ProgramType.create("isha", "Shambhavi", practiceIds,
                User.SUPER_USER);
        assertEquals(1, shambhavi2Day.programTypeId);

        practiceIds.clear();
        practiceIds.add(ishaKriya.practiceId);
        practiceIds.add(upaYogaBasic.practiceId);
        mysticEye1Day = ProgramType.create("isha", "MysticEye", practiceIds,
                User.SUPER_USER);
        assertEquals(2, mysticEye1Day.programTypeId);

        ContactProp contact = new ContactProp();
        contact.email = "sathya.t@ishafoundation.org";
        contact.asOfyyyymmdd = 20140722;

        sathya = Member.create(client, sgp.groupId, contact, true, validUser);
        assertEquals("first member has id 1", 1, sathya.memberId);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void addDeleteUnverifiedProgramTest() {
        // client should be valid
        ensureResourceNotFoundException(new ICode() {

            @Override
            public void run() {
                Member.addUnverifiedProgram(invalidClient, sathya.memberId,
                        shambhavi2Day.programTypeId, Month.DEC, 2003,
                        "chennai", "india", "sadhguru", validUser);
            }
        });

        // user can add unverified program
        Member.addUnverifiedProgram(client, sathya.memberId,
                shambhavi2Day.programTypeId, Month.DEC, 2003, "chennai",
                "india", "sadhguru", validUser);

        // member can add unverified program for himself
        List<UnverifiedProgramProp> list = Member.addUnverifiedProgram(client,
                sathya.memberId, mysticEye1Day.programTypeId, Month.JAN, 2014,
                "singapore", "singapore", "sadhguru", sathya.contact.email);

        // somebody else (non user) cannot add sathya's unverified program
        try {
            Member.addUnverifiedProgram(client, sathya.memberId,
                    mysticEye1Day.programTypeId, Month.JAN, 2014, "singapore",
                    "singapore", "sadhguru", "someoneelse@gmail.com");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
        }

        assertEquals(2, list.size());

        assertEquals(mysticEye1Day.programTypeId, list.get(0).programTypeId);
        assertEquals("singapore", list.get(0).city);
        assertEquals("singapore", list.get(0).country);
        assertEquals(Month.JAN, list.get(0).month);
        assertEquals(2014, list.get(0).year);
        assertEquals("sadhguru", list.get(0).teacher);
        assertEquals(2, list.get(0).unverifiedProgramId);

        assertEquals(shambhavi2Day.programTypeId, list.get(1).programTypeId);
        assertEquals("chennai", list.get(1).city);
        assertEquals("india", list.get(1).country);
        assertEquals(Month.DEC, list.get(1).month);
        assertEquals(2003, list.get(1).year);
        assertEquals("sadhguru", list.get(1).teacher);
        assertEquals(1, list.get(1).unverifiedProgramId);

        // member should have the correct practices and program types
        sathya = MemberLoader.safeGet(client, sathya.memberId, validUser)
                .toProp();
        assertTrue("practice tag to member",
                sathya.practiceIds.contains(shambhavi.practiceId));
        assertTrue("practice tag to member",
                sathya.practiceIds.contains(ishaKriya.practiceId));
        assertTrue("practice tag to member",
                sathya.practiceIds.contains(upaYogaBasic.practiceId));

        assertTrue("programType tag to member",
                sathya.programTypeIds.contains(shambhavi2Day.programTypeId));
        assertTrue("programType tag to member",
                sathya.programTypeIds.contains(mysticEye1Day.programTypeId));

        // unverified program should be included in detailed info
        sathya = MemberLoader.safeGetDetailedInfo(client, sathya.memberId,
                validUser);

        assertEquals("detailed info includes unverified programs", 2,
                sathya.memberProgramProps.size());

        // delete shambhavi 2day
        Member.deleteUnverifiedProgram(client, sathya.memberId, 1,
                sathya.contact.email);
        sathya = MemberLoader.safeGetDetailedInfo(client, sathya.memberId,
                validUser);
        assertTrue("programType removed",
                !sathya.programTypeIds.contains(shambhavi2Day.programTypeId));
        assertTrue("practice removed",
                !sathya.practiceIds.contains(shambhavi.practiceId));

        assertEquals("unverified program removed", 1,
                sathya.memberProgramProps.size());
    }
}
