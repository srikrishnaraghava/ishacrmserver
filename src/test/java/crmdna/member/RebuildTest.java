package crmdna.member;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.Utils;
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
import crmdna.venue.Venue;
import crmdna.venue.Venue.VenueProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RebuildTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    MemberProp sathya, syamala, sowmya;
    PracticeProp shambhavi, suryaNamaskar, yogaAsanas, angamardhana, ishaKriya;
    ProgramTypeProp innerEngineering7Day, hataYoga3Day, angamardhana2Day, ishaKriya1Hour;
    ProgramProp innerEngineering7DayNov2005, hataYoga3DayAug2012, angamardhana2DayAug2014,
            ishaKriya1HourAug2012;
    private GroupProp sgp;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER);
        suryaNamaskar = Practice.create(client, "Surya Namaskar", User.SUPER_USER);
        yogaAsanas = Practice.create(client, "Yoga Asanas", User.SUPER_USER);
        angamardhana = Practice.create(client, "Angamardhana", User.SUPER_USER);
        ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);

        innerEngineering7Day =
                ProgramType.create(client, "IE 7 day", Utils.getSet(shambhavi.practiceId), User.SUPER_USER);

        hataYoga3Day =
                ProgramType.create(client, "Hata Yoga 3 day",
                        Utils.getSet(suryaNamaskar.practiceId, yogaAsanas.practiceId), User.SUPER_USER);

        ishaKriya1Hour =
                ProgramType.create(client, "Isha Kriya 1 hour", Utils.getSet(ishaKriya.practiceId),
                        User.SUPER_USER);

        angamardhana2Day =
                ProgramType.create(client, "Angamardhana 2 day", Utils.getSet(angamardhana.practiceId),
                        User.SUPER_USER);

        VenueProp dummyVenue = Venue.create(client, "Dummy", "Dummy", sgp.groupId, User.SUPER_USER);
        TeacherProp dummTeacher =
                Teacher.create(client, "", "", "dummy@dummy.com", sgp.groupId, User.SUPER_USER);

        innerEngineering7DayNov2005 =
                Program.create(client, sgp.groupId, innerEngineering7Day.programTypeId, dummyVenue.venueId,
                        dummTeacher.teacherId, 20051104, 20051110, 1, null, 0.0, null, User.SUPER_USER);

        hataYoga3DayAug2012 =
                Program.create(client, sgp.groupId, hataYoga3Day.programTypeId, dummyVenue.venueId,
                        dummTeacher.teacherId, 20120801, 20120803, 1, null, 0.0, null, User.SUPER_USER);

        angamardhana2DayAug2014 =
                Program.create(client, sgp.groupId, angamardhana2Day.programTypeId, dummyVenue.venueId,
                        dummTeacher.teacherId, 20140801, 20140802, 1, null, 0.0, null, User.SUPER_USER);

        ishaKriya1HourAug2012 =
                Program.create(client, sgp.groupId, ishaKriya1Hour.programTypeId, dummyVenue.venueId,
                        dummTeacher.teacherId, 20120801, 20120801, 1, null, 0.0, null, User.SUPER_USER);

        ContactProp c = new ContactProp();
        c.email = "sathya@sathya.com";
        c.asOfyyyymmdd = 20141022;
        sathya = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);

        c.email = "syamala@syamala.com";
        syamala = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);

        c.email = "sowmya@sowmya.com";
        sowmya = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void rebuildRemovesNonExistantGroups() {
        assertTrue(false);
    }

    @Test
    public void rebuildRemovesNonExistantPrograms() {
        assertTrue(false);
    }

    @Test
    public void rebuildRemovesNonExistantUVP() {
        assertTrue(false);
    }

    @Test
    public void rebuildRemovesNonExistantSubscribedLists() {
        assertTrue(false);
    }

    @Test
    public void rebuildRemovesNonExistantUnsubscribedLists() {
        assertTrue(false);
    }
}
