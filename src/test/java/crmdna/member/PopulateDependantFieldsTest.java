package crmdna.member;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.list.List;
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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class PopulateDependantFieldsTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    MemberProp sathya, syamala, sowmya;
    PracticeProp shambhavi, suryaNamaskar, yogaAsanas, angamardhana, ishaKriya;
    ProgramTypeProp innerEngineering7Day, hataYoga3Day, angamardhana2Day,
            ishaKriya1Hour;
    ProgramProp innerEngineering7DayNov2005, hataYoga3DayAug2012,
            angamardhana2DayAug2014, ishaKriya1HourAug2012;
    private GroupProp sgp;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER);
        suryaNamaskar = Practice.create(client, "Surya Namaskar",
                User.SUPER_USER);
        yogaAsanas = Practice.create(client, "Yoga Asanas", User.SUPER_USER);
        angamardhana = Practice.create(client, "Angamardhana", User.SUPER_USER);
        ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);

        innerEngineering7Day = ProgramType.create(client, "IE 7 day",
                Utils.getSet(shambhavi.practiceId), User.SUPER_USER);

        hataYoga3Day = ProgramType.create(client, "Hata Yoga 3 day",
                Utils.getSet(suryaNamaskar.practiceId, yogaAsanas.practiceId),
                User.SUPER_USER);

        ishaKriya1Hour = ProgramType.create(client, "Isha Kriya 1 hour",
                Utils.getSet(ishaKriya.practiceId), User.SUPER_USER);

        angamardhana2Day = ProgramType.create(client, "Angamardhana 2 day",
                Utils.getSet(angamardhana.practiceId), User.SUPER_USER);

        VenueProp dummyVenue = Venue.create(client, "Dummy", "Dummy", "Dummy",
                sgp.groupId, User.SUPER_USER);
        TeacherProp dummTeacher = Teacher.create(client, "", "", "dummy@dummy.com",
                sgp.groupId, User.SUPER_USER);

        innerEngineering7DayNov2005 = Program.create(client, sgp.groupId,
                innerEngineering7Day.programTypeId, dummyVenue.venueId,
                dummTeacher.teacherId, 20051104, 20051110, 1, null, 0.0, null,
                User.SUPER_USER);

        hataYoga3DayAug2012 = Program.create(client, sgp.groupId,
                hataYoga3Day.programTypeId, dummyVenue.venueId,
                dummTeacher.teacherId, 20120801, 20120803, 1, null, 0.0, null,
                User.SUPER_USER);

        angamardhana2DayAug2014 = Program.create(client, sgp.groupId,
                angamardhana2Day.programTypeId, dummyVenue.venueId,
                dummTeacher.teacherId, 20140801, 20140802, 1, null, 0.0, null,
                User.SUPER_USER);

        ishaKriya1HourAug2012 = Program.create(client, sgp.groupId,
                ishaKriya1Hour.programTypeId, dummyVenue.venueId,
                dummTeacher.teacherId, 20120801, 20120801, 1, null, 0.0, null,
                User.SUPER_USER);

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
    public void programTypeAndPracticesPopulatedBasedOnPrograms() {
        MemberEntity sathyaEntity = MemberLoader.safeGet(client,
                sathya.memberId, User.SUPER_USER);
        MemberEntity sowmyaEntity = MemberLoader.safeGet(client,
                sowmya.memberId, User.SUPER_USER);
        MemberEntity syamalaEntity = MemberLoader.safeGet(client,
                syamala.memberId, User.SUPER_USER);

        sathyaEntity.programIds.add(innerEngineering7DayNov2005.programId);
        sowmyaEntity.programIds.add(angamardhana2DayAug2014.programId);
        sowmyaEntity.programIds.add(hataYoga3DayAug2012.programId);
        syamalaEntity.programIds.add(ishaKriya1HourAug2012.programId);

        Member.populateDependantFields(client,
                Utils.getList(sathyaEntity, sowmyaEntity, syamalaEntity));

        assertTrue(sathyaEntity.practiceIds.contains(shambhavi.practiceId));
        assertTrue(sathyaEntity.programTypeIds
                .contains(innerEngineering7Day.programTypeId));

        assertTrue(sowmyaEntity.practiceIds.contains(suryaNamaskar.practiceId));
        assertTrue(sowmyaEntity.practiceIds.contains(yogaAsanas.practiceId));
        assertTrue(sowmyaEntity.practiceIds.contains(angamardhana.practiceId));
        assertTrue(sowmyaEntity.programTypeIds
                .contains(hataYoga3Day.programTypeId));
        assertTrue(sowmyaEntity.programTypeIds
                .contains(angamardhana2Day.programTypeId));

        assertTrue(syamalaEntity.practiceIds.contains(ishaKriya.practiceId));
    }

    @Test
    public void practicesPopulatedBasedOnList() {
        ListProp shambhaviList = List.createRestricted(client, sgp.groupId,
                "Shambhavi email list", Utils.getSet(shambhavi.practiceId),
                User.SUPER_USER);

        MemberEntity sathyaEntity = MemberLoader.safeGet(client,
                sathya.memberId, User.SUPER_USER);
        MemberEntity sowmyaEntity = MemberLoader.safeGet(client,
                sowmya.memberId, User.SUPER_USER);

        sathyaEntity.listIds.add(shambhaviList.listId);
        sowmyaEntity.listIds.add(shambhaviList.listId);

        Member.populateDependantFields(client,
                Utils.getList(sathyaEntity, sowmyaEntity));

        assertTrue(sathyaEntity.practiceIds.contains(shambhavi.practiceId));
        assertTrue(sowmyaEntity.practiceIds.contains(shambhavi.practiceId));
    }

    @Test
    public void practicesPopulatedBasedOnListAndPrograms() {
        ListProp shambhaviList = List.createRestricted(client, sgp.groupId,
                "Shambhavi email list", Utils.getSet(shambhavi.practiceId),
                User.SUPER_USER);

        MemberEntity sathyaEntity = MemberLoader.safeGet(client,
                sathya.memberId, User.SUPER_USER);
        MemberEntity sowmyaEntity = MemberLoader.safeGet(client,
                sowmya.memberId, User.SUPER_USER);

        sathyaEntity.listIds.add(shambhaviList.listId);
        sathyaEntity.programIds.add(ishaKriya1HourAug2012.programId);
        sowmyaEntity.listIds.add(shambhaviList.listId);

        Member.populateDependantFields(client,
                Utils.getList(sathyaEntity, sowmyaEntity));

        assertTrue(sathyaEntity.practiceIds.contains(shambhavi.practiceId));
        assertTrue(sathyaEntity.practiceIds.contains(ishaKriya.practiceId));
        assertTrue(sowmyaEntity.practiceIds.contains(shambhavi.practiceId));
    }
}
