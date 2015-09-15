package crmdna.client.isha;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.member.Member;
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
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.venue.Venue;
import crmdna.venue.Venue.VenueProp;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IshaUtilsTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    PracticeProp shambhavi;
    PracticeProp mysticEye;
    PracticeProp ishaKriya;

    MemberProp madhu, naveen, sanket, thulasi;

    ProgramProp sathsang201405;
    String sgpUser = "sgpuser@gmail.com";

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create("isha");

        shambhavi = Practice.create("isha", "Shambhavi", User.SUPER_USER);
        mysticEye = Practice.create("isha", "MysticEye", User.SUPER_USER);
        ishaKriya = Practice.create("isha", "IshaKriya", User.SUPER_USER);

        Set<Long> sathsangPracticeIds = new HashSet<>();
        sathsangPracticeIds.add(shambhavi.practiceId);
        sathsangPracticeIds.add(mysticEye.practiceId);

        IshaConfig.setSathsangPractices(sathsangPracticeIds, User.SUPER_USER);
        sathsangPracticeIds = IshaConfig.safeGet().sathsangPracticeIds;
        assertEquals(2, sathsangPracticeIds.size());
        assertTrue(sathsangPracticeIds.contains(shambhavi.practiceId));
        assertTrue(sathsangPracticeIds.contains(mysticEye.practiceId));

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

        GroupProp sgp = Group.create("isha", "Singapore", User.SUPER_USER);
        GroupProp chennai = Group.create("isha", "Chennai", User.SUPER_USER);

        VenueProp giis = Venue.create("isha", "GIIS", "GIIS", "GIIS", sgp.groupId,
                User.SUPER_USER);
        VenueProp expo = Venue.create("isha", "Expo", "Expo", "Expo", sgp.groupId,
                User.SUPER_USER);

        TeacherProp nidhi = Teacher.create("isha", "", "",
            "nidhi.jain@ishafoundation.org", sgp.groupId, User.SUPER_USER);
        TeacherProp sadhguru = Teacher.create("isha", "", "",
                "sadhguru@ishafoundation.org", sgp.groupId, User.SUPER_USER);

        ProgramProp shambhavi201405 = Program.create("isha", sgp.groupId,
                shambhavi2Day.programTypeId, giis.venueId, nidhi.teacherId,
                20140503, 20140504, 1, null, 0, null, User.SUPER_USER);

        ProgramProp mysticEye201401 = Program.create("isha", sgp.groupId,
                mysticEye1Day.programTypeId, expo.venueId, sadhguru.teacherId,
                20140503, 20140504, 1, null, 0, null, User.SUPER_USER);

        sathsang201405 = Program.create("isha", sgp.groupId,
                sathsang.programTypeId, giis.venueId, nidhi.teacherId,
                20140503, 20140503, 1, null, 0, null, User.SUPER_USER);

        // create members
        ContactProp c = new ContactProp(); // shambhavi but not mysticeye
        c.email = "thulasidhar@gmail.com";
        c.mobilePhone = "+6593875170";
        c.officePhone = "+6565882010";
        c.homePhone = "+6530133982";
        c.asOfyyyymmdd = 20141021;
        thulasi = Member.create("isha", sgp.groupId, c, false, User.SUPER_USER);
        Member.addOrDeleteProgram("isha", thulasi.memberId,
                shambhavi201405.programId, true, User.SUPER_USER);
        assertEquals(1, thulasi.memberId);

        c = new ContactProp();
        c.email = "naveen@gmail.com"; // both shambhavi and mystic eye
        c.asOfyyyymmdd = 20141021;
        naveen = Member.create("isha", sgp.groupId, c, false, User.SUPER_USER);
        Member.addOrDeleteProgram("isha", naveen.memberId,
                shambhavi201405.programId, true, User.SUPER_USER);
        Member.addOrDeleteProgram("isha", naveen.memberId,
                mysticEye201401.programId, true, User.SUPER_USER);
        assertEquals(2, naveen.memberId);

        c = new ContactProp();
        c.email = "sanket@sambaash.com"; // only mystic eye
        c.asOfyyyymmdd = 20141021;
        sanket = Member.create("isha", sgp.groupId, c, false, User.SUPER_USER);
        Member.addOrDeleteProgram("isha", sanket.memberId,
                mysticEye201401.programId, true, User.SUPER_USER);
        assertEquals(3, sanket.memberId);

        c = new ContactProp();
        c.email = "madhu@chrysler.com"; // non meditator
        c.asOfyyyymmdd = 20141021;
        madhu = Member.create("isha", chennai.groupId, c, false,
                User.SUPER_USER);
        assertEquals(4, madhu.memberId);

        User.create("isha", sgpUser, sgp.groupId, User.SUPER_USER);
        User.addClientLevelPrivilege("isha", sgpUser,
                ClientLevelPrivilege.UPDATE_GROUP, User.SUPER_USER);
    }

    @Test
    public void isMeditatorTest() {
        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(ishaKriya.practiceId);

        // only ishakriya
        assertEquals(false, IshaUtils.isMeditator(practiceIds));

        // ishakriya and shambhavi
        practiceIds.add(shambhavi.practiceId);
        assertEquals(true, IshaUtils.isMeditator(practiceIds));

        // ishakriya, shambhavi and mysticeye
        practiceIds.add(mysticEye.practiceId);
        assertEquals(true, IshaUtils.isMeditator(practiceIds));

        // only mysticeye
        practiceIds.clear();
        practiceIds.add(mysticEye.practiceId);
        assertEquals(true, IshaUtils.isMeditator(practiceIds));
    }

    @Test
    public void isSathsangTest() {
        String programName = "Sathsang 5 Apr 14 @ GIIS, Singapore";
        assertEquals(true, IshaUtils.isSathsang(programName));

        programName = "sathsang 5 Apr 14 @ GIIS, Singapore";
        assertEquals(true, IshaUtils.isSathsang(programName));

        programName = "SATHSANG 5 Apr 14 @ GIIS, Singapore";
        assertEquals(true, IshaUtils.isSathsang(programName));

        programName = "Isha Kriya 5 Apr 14 @ GIIS, Singapore";
        assertEquals(false, IshaUtils.isSathsang(programName));
    }
}
