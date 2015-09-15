package crmdna.program;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group.GroupProp;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
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

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ProgramTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
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

    VenueProp giis;
    VenueProp chaichee;
    VenueProp gujarathiBhavan;
    VenueProp yuhuaCC;
    VenueProp woodlandsCC;

    TeacherProp tina;
    TeacherProp thulasi;
    TeacherProp sathya;

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

        User.addGroupLevelPrivilege(client, sgp.groupId, sgpUser, GroupLevelPrivilege.UPDATE_PROGRAM,
                User.SUPER_USER);
        User.addGroupLevelPrivilege(client, sgp.groupId, sgpUser, GroupLevelPrivilege.CHECK_IN,
                User.SUPER_USER);

        User.create(client, klUser, kl.groupId, User.SUPER_USER);
        assertEquals(3, User.get(client, klUser).toProp(client).userId);

        User.addGroupLevelPrivilege(client, kl.groupId, klUser, GroupLevelPrivilege.UPDATE_PROGRAM,
                User.SUPER_USER);
        User.addGroupLevelPrivilege(client, kl.groupId, klUser, GroupLevelPrivilege.CHECK_IN,
                User.SUPER_USER);

        User.create(client, sgpAndKlUser, sgp.groupId, User.SUPER_USER);
        assertEquals(4, User.get(client, sgpAndKlUser).toProp(client).userId);

        User.addGroupLevelPrivilege(client, sgp.groupId, sgpAndKlUser, GroupLevelPrivilege.CHECK_IN,
                User.SUPER_USER);
        User.addGroupLevelPrivilege(client, kl.groupId, sgpAndKlUser, GroupLevelPrivilege.CHECK_IN,
                User.SUPER_USER);

        suryaNamaskar = Practice.create(client, "Surya Namaskar", User.SUPER_USER);
        yogaAsanas = Practice.create(client, "Yoga Asanas", User.SUPER_USER);
        shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER);
        ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);
        aumChanting = Practice.create(client, "Aum Chanting", User.SUPER_USER);

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(shambhavi.practiceId);
        practiceIds.add(aumChanting.practiceId);
        innerEngineering7Day =
                ProgramType.create(client, "Inner Engineering 7 day", practiceIds, User.SUPER_USER);

        practiceIds.clear();
        practiceIds.add(suryaNamaskar.practiceId);
        practiceIds.add(yogaAsanas.practiceId);
        suryaNamaskarAndAsanas =
                ProgramType.create(client, "Hata Yoga (Surya Namaskar & Asanas)", practiceIds,
                        User.SUPER_USER);

        practiceIds.clear();
        practiceIds.add(ishaKriya.practiceId);
        ishaKriyaTeacherLed = ProgramType.create(client, "Isha Kriya", practiceIds, User.SUPER_USER);

        giis = Venue.create(client, "GIIS", "GIIS", "GIIS", sgp.groupId, User.SUPER_USER);
        chaichee = Venue.create(client, "Chai Chee", "Chai Chee", "Chai Chee", sgp.groupId, User.SUPER_USER);
        gujarathiBhavan =
                Venue.create(client, "Gujarathi Bhavan", "Gujarathi Bhavan", "Gujarathi Bhavan", sgp.groupId, User.SUPER_USER);
        yuhuaCC = Venue.create(client, "Yuhua CC", "Yuhua CC", "Yuhua CC", sgp.groupId, User.SUPER_USER);
        woodlandsCC =
                Venue.create(client, "Woodlands CC", "Woodlands CC", "Woodlands CC", sgp.groupId, User.SUPER_USER);

        tina = Teacher.create(client, "", "", "tina@ishafoundation.org", sgp.groupId, User.SUPER_USER);
        thulasi = Teacher.create(client, "", "", "thulasidhar@gmail.com", sgp.groupId, User.SUPER_USER);
        sathya = Teacher.create(client, "", "", "sathya.t@ishafoundation.org", sgp.groupId, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void createTest() {

        try {
            Program.create(invalidClient, sgp.groupId, ishaKriyaTeacherLed.programTypeId,
                    yuhuaCC.venueId, sathya.teacherId, 20131229, 20131229, 1, null, 0, null, klUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // user should have permission to create program for a group
        try {
            Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                    sathya.teacherId, 20131229, 20131229, 1, null, 0, null, klUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("user should have permission", Status.ERROR_INSUFFICIENT_PERMISSION,
                    ex.statusCode);
        }

        // group id should be valid
        try {
            Program.create(client, sgp.groupId + 1000, ishaKriyaTeacherLed.programTypeId,
                    yuhuaCC.venueId, sathya.teacherId, 20131229, 20131229, 1, null, 0, null, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("group id should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // teacher should be valid
        try {
            Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                    sathya.teacherId + 1000, 20131229, 20131229, 1, null, 0, null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("teacher id should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // program type should be valid
        try {
            Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId + 100, yuhuaCC.venueId,
                    sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("program type should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // venue should be valid
        try {
            Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId + 100,
                    sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("venue should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // start date and end date should be correct format
        try {
            Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                    sathya.teacherId, 29122013, 29122013, 1, null, 0, null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("start date should be in yyyymmdd", Status.ERROR_RESOURCE_INCORRECT,
                    ex.statusCode);
        }

        // start date should be lesser than or equal to end date
        try {
            Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                    sathya.teacherId, 20131229, 20131228, 1, null, 0, null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("start date should be on or before end date", Status.ERROR_RESOURCE_INCORRECT,
                    ex.statusCode);
        }

        // num batches should be valid
        try {
            Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                    sathya.teacherId, 20131229, 20131229, -1, null, 0, null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("batch should be valid", Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // fees cannot be negative
        try {
            Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                    sathya.teacherId, 20131229, 20131229, -1, null, -150, null, sgpUser); // negative
            // fees
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("fees cannot be negative", Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // ccy cannot be null when fees is specified
        try {
            Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                    sathya.teacherId, 20131229, 20131229, -1, null, 150, null, sgpUser); // null
            // ccy
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("ccy cannot be null when fees is specified", Status.ERROR_RESOURCE_INCORRECT,
                    ex.statusCode);
        }

        Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                sathya.teacherId, 20131229, 20131229, 2, null, 150, Currency.SGD, sgpUser);

        List<ProgramProp> programProps = Program.query(client, null, null, null, null, null, 100);
        // specifying null should return all programs

        assertEquals(1, programProps.size());

        ProgramProp programProp = programProps.get(0);
        assertEquals(1, programProp.programId);
        assertEquals(ishaKriyaTeacherLed.programTypeId, programProp.programTypeProp.programTypeId);
        assertEquals(yuhuaCC.venueId, programProp.venueProp.venueId);
        assertEquals(sathya.teacherId, programProp.teacherProp.teacherId);
        assertEquals(20131229, programProp.startYYYYMMDD);
        assertEquals(20131229, programProp.endYYYYMMDD);
        assertEquals(2, programProp.numBatches);
        assertEquals(null, programProp.description);
        assertEquals("Isha Kriya 29 Dec 13 @ Yuhua CC", programProp.name);
        assertTrue(150 == programProp.fee);
        assertEquals(Currency.SGD, programProp.ccy);

        // cannot create duplicate
        try {
            Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                    sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("cannot create duplicate", Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        // fees can be zero.
        programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                        sathya.teacherId, 20140105, 20140105, 2, null, 0, null, sgpUser);
        assertTrue(0.0 == programProp.fee);
        assertEquals(null, programProp.ccy);
    }

    @Test
    public void getTest() {
        ProgramProp programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                        sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);
        assertEquals(1, programProp.programId);

        programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, woodlandsCC.venueId,
                        sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);
        assertEquals(2, programProp.programId);

        List<Long> programIds = new ArrayList<>();
        programIds.add((long) 1);
        programIds.add((long) 2);
        programIds.add((long) 3); // non existing

        Map<Long, ProgramProp> map = Program.getProps(client, programIds);
        assertEquals(2, map.size());

        assertEquals(yuhuaCC.venueId, map.get(1l).venueProp.venueId);
        assertEquals(woodlandsCC.venueId, map.get(2l).venueProp.venueId);
        assertEquals(null, map.get(3));

        // simple get
        programProp = Program.get(client, 1).toProp(client);
        assertTrue(programProp != null);
        assertEquals(1, programProp.programId);

        // should return null for non existent program id
        assertTrue(null == Program.get(client, 1002)); // non existent
    }

    @Test
    public void safeGetTest() {
        ProgramProp programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                        sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);

        ProgramEntity programEntity = Program.safeGet(client, programProp.programId);
        assertEquals(sgp.groupId, programEntity.groupId);
        assertEquals(ishaKriyaTeacherLed.programTypeId, programEntity.programTypeId);
        assertEquals(yuhuaCC.venueId, programEntity.venueId);
        assertEquals(sathya.teacherId, programEntity.teacherId);

        programProp = programEntity.toProp(client);
        assertEquals(1, programProp.programId);
        assertEquals(ishaKriyaTeacherLed.programTypeId, programProp.programTypeProp.programTypeId);
        assertEquals(yuhuaCC.venueId, programProp.venueProp.venueId);
        assertEquals(sathya.teacherId, programProp.teacherProp.teacherId);
        assertEquals(20131229, programProp.startYYYYMMDD);
        assertEquals(20131229, programProp.endYYYYMMDD);
        assertEquals(null, programProp.description);
        assertEquals("Isha Kriya 29 Dec 13 @ Yuhua CC", programProp.name);
    }

    @Test
    public void updateTest() {

        ProgramProp programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                        sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);
        assertEquals(1, programProp.programId);

        // can pass null for non changing values
        assertEquals(sathya.teacherId, programProp.teacherProp.teacherId);
        Program.update(client, programProp.programId, null, tina.teacherId, null, null, null,
                "after update 1", null, null, sgpUser);
        programProp = Program.safeGet(client, programProp.programId).toProp(client);
        assertEquals(tina.teacherId, programProp.teacherProp.teacherId);
        assertEquals("after update 1", programProp.description);

        // change batch to 2 and venue to woodlands
        Program.update(client, programProp.programId, woodlandsCC.venueId, null, null, null, 2, null,
                null, null, sgpUser);
        ProgramEntity programEntity = Program.safeGet(client, programProp.programId);
        assertEquals(woodlandsCC.venueId, programEntity.venueId);
        programProp = programEntity.toProp(client);
        assertEquals(1, programProp.programId);
        assertEquals(tina.teacherId, programProp.teacherProp.teacherId);
        assertEquals(woodlandsCC.venueId, programProp.venueProp.venueId);
        assertEquals(20131229, programProp.startYYYYMMDD);
        assertEquals(20131229, programProp.endYYYYMMDD);
        assertEquals(2, programProp.numBatches);
        assertEquals("after update 1", programProp.description);

        // change fees to 10 SGD
        Program.update(client, programProp.programId, woodlandsCC.venueId, null, null, null, 2, null,
                10.0, Currency.SGD, sgpUser);
        programProp = Program.safeGet(client, programProp.programId).toProp(client);
        assertTrue(10.0 == programProp.fee);
        assertEquals(Currency.SGD, programProp.ccy);

        Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, woodlandsCC.venueId,
                sathya.teacherId, 20140105, 20140105, 1, null, 0, null, sgpUser);

        // cannot update to another existing program
        try {
            Program.update(client, programProp.programId, woodlandsCC.venueId, sathya.teacherId,
                    20140105, 20140105, 1, null, null, null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        // basic validation checks
        programProp =
                Program.create(client, sgp.groupId, suryaNamaskarAndAsanas.programTypeId, yuhuaCC.venueId,
                        sathya.teacherId, 20140101, 20140103, 1, null, 0, null, sgpUser);

        try {
            Program.update(client, programProp.programId, (long) 100, null, null, null, null, null, null,
                    null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("Venue should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Program.update(client, programProp.programId, (long) 100, null, null, null, null, null, null,
                    null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("Teacher should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Program.update(client, programProp.programId, null, null, null, null, -2, null, null, null,
                    sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("NumBatches cannot be negative", Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        try {
            Program.update(client, programProp.programId, null, null, 20140101, 20131231, null, null,
                    null, null, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("Start and end dates should be valid", Status.ERROR_RESOURCE_INCORRECT,
                    ex.statusCode);
        }
    }

    @Test
    public void setSpecialInstructionTest() {
        try {
            Program.setSpecialInstruction(invalidClient, 100, "special instruction");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Program.setSpecialInstruction(client, 100, "special instruction");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("program Id should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        ProgramProp programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                        sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);
        assertEquals(1, programProp.programId);
        assertEquals(null, programProp.specialInstruction);

        programProp =
                Program.setSpecialInstruction(client, programProp.programId, "special instruction");
        assertEquals("special instruction", programProp.specialInstruction);

        // null should preserve existing value
        Program.setSpecialInstruction(client, programProp.programId, null);
        programProp = Program.safeGet(client, programProp.programId).toProp(client);
        assertEquals("special instruction", programProp.specialInstruction);
    }

    @Test
    public void setSessionTimingsTest() {
        try {
            Program.setSessionTimings(invalidClient, 100, null, null, null, null, null);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Program.setSessionTimings(client, 100, null, null, null, null, null);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("program Id should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        ProgramProp programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                        sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);
        assertEquals(1, programProp.programId);
        assertEquals(null, programProp.batch1SessionTimings);
        assertEquals(null, programProp.batch2SessionTimings);
        assertEquals(null, programProp.batch3SessionTimings);
        assertEquals(null, programProp.batch4SessionTimings);
        assertEquals(null, programProp.batch5SessionTimings);

        List<String> b1Timings = new ArrayList<>();
        b1Timings.add("10am - 11am");
        List<String> b2Timings = new ArrayList<>();
        b2Timings.add("12pm - 1pm");
        List<String> b3Timings = new ArrayList<>();
        b3Timings.add("2pm - 3pm");
        List<String> b4Timings = new ArrayList<>();
        b4Timings.add("4pm - 5pm");
        List<String> b5Timings = new ArrayList<>();
        b5Timings.add("6pm - 7pm");

        programProp =
                Program.setSessionTimings(client, 1, b1Timings, b2Timings, b3Timings, b4Timings, b5Timings);
        assertEquals(b1Timings.size(), programProp.batch1SessionTimings.size());
        assertEquals(b1Timings.get(0), programProp.batch1SessionTimings.get(0));
        // there is only one batch. other timings should be ignored
        assertEquals(null, programProp.batch2SessionTimings);
        assertEquals(null, programProp.batch3SessionTimings);
        assertEquals(null, programProp.batch4SessionTimings);
        assertEquals(null, programProp.batch5SessionTimings);

        // change program to have 4 batches
        programProp = Program.update(client, 1l, null, null, null, null, 4, null, 0.0, null, sgpUser);
        assertEquals(4, programProp.numBatches);

        programProp =
                Program.setSessionTimings(client, 1, b1Timings, b2Timings, b3Timings, b4Timings, b5Timings);
        assertEquals(b1Timings.size(), programProp.batch1SessionTimings.size());
        assertEquals(b1Timings.get(0), programProp.batch1SessionTimings.get(0));
        assertEquals(b2Timings.size(), programProp.batch2SessionTimings.size());
        assertEquals(b2Timings.get(0), programProp.batch2SessionTimings.get(0));
        assertEquals(b3Timings.size(), programProp.batch3SessionTimings.size());
        assertEquals(b3Timings.get(0), programProp.batch3SessionTimings.get(0));
        assertEquals(b4Timings.size(), programProp.batch4SessionTimings.size());
        assertEquals(b4Timings.get(0), programProp.batch4SessionTimings.get(0));
        assertEquals(null, programProp.batch5SessionTimings);

        b4Timings.clear();
        b4Timings.add("7pm - 8pm");
        // null preserves existing values
        programProp = Program.setSessionTimings(client, 1, null, null, null, b4Timings, null);
        assertEquals(b1Timings.size(), programProp.batch1SessionTimings.size());
        assertEquals(b1Timings.get(0), programProp.batch1SessionTimings.get(0));
        assertEquals(b2Timings.size(), programProp.batch2SessionTimings.size());
        assertEquals(b2Timings.get(0), programProp.batch2SessionTimings.get(0));
        assertEquals(b3Timings.size(), programProp.batch3SessionTimings.size());
        assertEquals(b3Timings.get(0), programProp.batch3SessionTimings.get(0));
        assertEquals(b4Timings.size(), programProp.batch4SessionTimings.size());
        assertEquals(b4Timings.get(0), programProp.batch4SessionTimings.get(0));
        assertEquals(null, programProp.batch5SessionTimings);
    }

    @Test
    public void queryTest() {
        try {
            Program.query(invalidClient, null, null, null, null, null, 10);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        List<ProgramProp> programProps = Program.query(client, null, null, null, null, null, 10);
        assertEquals(0, programProps.size());

        ProgramProp programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                        sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);
        assertEquals(1, programProp.programId);

        programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, woodlandsCC.venueId,
                        sathya.teacherId, 20131229, 20131229, 1, null, 0, null, sgpUser);
        assertEquals(2, programProp.programId);

        programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, woodlandsCC.venueId,
                        sathya.teacherId, 20140105, 20140105, 1, null, 0, null, sgpUser);
        assertEquals(3, programProp.programId);

        programProp =
                Program.create(client, sgp.groupId, innerEngineering7Day.programTypeId, giis.venueId,
                        tina.teacherId, 20110505, 20110511, 1, null, 0, null, sgpUser);
        assertEquals(4, programProp.programId);

        programProp =
                Program.create(client, kl.groupId, innerEngineering7Day.programTypeId, giis.venueId,
                        tina.teacherId, 20110405, 20110411, 1, null, 0, null, klUser);
        assertEquals(5, programProp.programId);

        // get all programs, should be sorted by -start, +program type, +group,
        // +venue
        programProps = Program.query(client, null, null, null, null, null, null);
        assertEquals(5, programProps.size());
        assertEquals(3, programProps.get(0).programId);
        assertEquals(2, programProps.get(1).programId);
        assertEquals(1, programProps.get(2).programId);
        assertEquals(4, programProps.get(3).programId);
        assertEquals(5, programProps.get(4).programId);

        // limit size
        programProps = Program.query(client, null, null, null, null, null, 3);
        assertEquals(3, programProps.size());
        assertEquals(3, programProps.get(0).programId);
        assertEquals(2, programProps.get(1).programId);
        assertEquals(1, programProps.get(2).programId);

        // limit size to zero
        programProps = Program.query(client, null, null, null, null, null, 0);
        assertEquals(0, programProps.size());

        // get all singapore programs
        Set<Long> groupIds = new HashSet<>();
        groupIds.add(sgp.groupId);
        programProps = Program.query(client, null, null, null, groupIds, null, 10);
        assertEquals(4, programProps.size());
        assertEquals(3, programProps.get(0).programId);
        assertEquals(2, programProps.get(1).programId);
        assertEquals(1, programProps.get(2).programId);
        assertEquals(4, programProps.get(3).programId);

        // all isha kriya programs in singapore in 2014
        Set<Long> ishaKriyaTeacherLedProgramTypeId = new HashSet<>();
        groupIds.add(ishaKriyaTeacherLed.programTypeId);
        programProps =
                Program.query(client, 20140101, 20141231, ishaKriyaTeacherLedProgramTypeId, groupIds,
                        null, null);
        assertEquals(1, programProps.size());
        assertEquals(3, programProps.get(0).programId);

        // get all programs in woodlands cc
        programProps = Program.query(client, null, null, null, null, woodlandsCC.venueId, 10);
        assertEquals(2, programProps.size());
        assertEquals(3, programProps.get(0).programId);
        assertEquals(2, programProps.get(1).programId);

        Set<Long> innerEngineering7DayProgramTypeId = new HashSet<>();
        groupIds.add(innerEngineering7Day.programTypeId);
        // get all IE 7 day programs
        programProps =
                Program.query(client, null, null, innerEngineering7DayProgramTypeId, null, null, 10);
        assertEquals(2, programProps.size());
        assertEquals(4, programProps.get(0).programId);
        assertEquals(5, programProps.get(1).programId);

        // get all IE 7 day programs in Singapore
        programProps =
                Program.query(client, null, null, innerEngineering7DayProgramTypeId, groupIds, null, 10);
        assertEquals(1, programProps.size());
        assertEquals(4, programProps.get(0).programId);

        // get all IE 7 days programs in Singapore and KL
        groupIds.clear();
        groupIds.add(sgp.groupId);
        groupIds.add(kl.groupId);
        programProps =
                Program.query(client, null, null, innerEngineering7DayProgramTypeId, groupIds, null, 10);
        assertEquals(2, programProps.size());
        assertEquals(4, programProps.get(0).programId);
        assertEquals(5, programProps.get(1).programId);

        // all programs in 2011
        programProps = Program.query(client, 20110101, 20111231, null, null, null, 10);
        assertEquals(2, programProps.size());
        assertEquals(4, programProps.get(0).programId);
        assertEquals(5, programProps.get(1).programId);

        // all programs after 20131229
        programProps = Program.query(client, 20131229, null, null, null, null, 10);
        assertEquals(3, programProps.size());
        assertEquals(3, programProps.get(0).programId);
        assertEquals(2, programProps.get(1).programId);
        assertEquals(1, programProps.get(2).programId);

        // all programs before 2014
        programProps = Program.query(client, null, 20131231, null, null, null, 10);
        assertEquals(4, programProps.size());
        assertEquals(2, programProps.get(0).programId);
        assertEquals(1, programProps.get(1).programId);
        assertEquals(4, programProps.get(2).programId);
        assertEquals(5, programProps.get(3).programId);

        // can pass 0 sized list for groupIds
        groupIds.clear();
        programProps = Program.query(client, null, 20131231, null, groupIds, null, 10);
        assertEquals(4, programProps.size());
        assertEquals(2, programProps.get(0).programId);
        assertEquals(1, programProps.get(1).programId);
        assertEquals(4, programProps.get(2).programId);
        assertEquals(5, programProps.get(3).programId);
    }

    @Test
    public void getOngoingProgramsTest() {
        try {
            Program.getOngoingPrograms(invalidClient, 20140302, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        List<ProgramProp> programProps = Program.getOngoingPrograms(client, 20140309, sgpUser);
        assertEquals(0, programProps.size());

        // date should be in correct format
        try {
            programProps = Program.getOngoingPrograms(client, 201403029, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        ProgramProp programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                        sathya.teacherId, 20140309, 20140309, 1, null, 0, null, sgpUser);
        assertEquals(1, programProp.programId);

        programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, woodlandsCC.venueId,
                        sathya.teacherId, 20140309, 20140309, 1, null, 0, null, sgpUser);
        assertEquals(2, programProp.programId);

        programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, woodlandsCC.venueId,
                        sathya.teacherId, 20140316, 20140316, 1, null, 0, null, sgpUser);
        assertEquals(3, programProp.programId);

        programProp =
                Program.create(client, sgp.groupId, innerEngineering7Day.programTypeId, giis.venueId,
                        tina.teacherId, 20140312, 20140318, 1, null, 0, null, sgpUser);
        assertEquals(4, programProp.programId);

        programProp =
                Program.create(client, kl.groupId, innerEngineering7Day.programTypeId, giis.venueId,
                        tina.teacherId, 20140305, 20140311, 1, null, 0, null, klUser);
        assertEquals(5, programProp.programId);

        programProps = Program.getOngoingPrograms(client, 20140309, sgpUser);
        assertEquals(2, programProps.size());
        assertEquals(2, programProps.get(0).programId);
        assertEquals(1, programProps.get(1).programId);

        programProps = Program.getOngoingPrograms(client, 20140309, klUser);
        assertEquals(1, programProps.size());
        assertEquals(5, programProps.get(0).programId);

        programProps = Program.getOngoingPrograms(client, 20140309, sgpAndKlUser);
        assertEquals(3, programProps.size());
        assertEquals(2, programProps.get(0).programId);
        assertEquals(1, programProps.get(1).programId);
        assertEquals(5, programProps.get(2).programId);
    }

    @Test
    public void getOngoingSessionsTest() {
        try {
            Program.getOngoingSessions(invalidClient, 20140302, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        List<SessionProp> sessionProps = Program.getOngoingSessions(client, 20140309, sgpUser);
        assertEquals(0, sessionProps.size());

        // date should be in correct format
        try {
            sessionProps = Program.getOngoingSessions(client, 201403029, sgpUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        ProgramProp programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, yuhuaCC.venueId,
                        sathya.teacherId, 20140309, 20140309, 1, null, 0, null, sgpUser);
        assertEquals(1, programProp.programId);

        programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, woodlandsCC.venueId,
                        sathya.teacherId, 20140309, 20140309, 1, null, 0, null, sgpUser);
        assertEquals(2, programProp.programId);

        programProp =
                Program.create(client, sgp.groupId, ishaKriyaTeacherLed.programTypeId, woodlandsCC.venueId,
                        sathya.teacherId, 20140316, 20140316, 1, null, 0, null, sgpUser);
        assertEquals(3, programProp.programId);

        programProp =
                Program.create(client, sgp.groupId, innerEngineering7Day.programTypeId, giis.venueId,
                        tina.teacherId, 20140312, 20140318, 1, null, 0, null, sgpUser);
        assertEquals(4, programProp.programId);

        programProp =
                Program.create(client, kl.groupId, innerEngineering7Day.programTypeId, giis.venueId,
                        tina.teacherId, 20140305, 20140311, 2, null, 0, null, klUser);
        assertEquals(5, programProp.programId);

        sessionProps = Program.getOngoingSessions(client, 20140309, sgpUser);
        assertEquals(2, sessionProps.size());
        assertEquals(2, sessionProps.get(0).programId);
        assertEquals(1, sessionProps.get(1).programId);

        sessionProps = Program.getOngoingSessions(client, 20140309, klUser);
        assertEquals(2, sessionProps.size());
        assertEquals(5, sessionProps.get(0).programId);
        assertEquals(1, sessionProps.get(0).batchNo);
        assertEquals(5, sessionProps.get(1).programId);
        assertEquals(2, sessionProps.get(1).batchNo);

        sessionProps = Program.getOngoingSessions(client, 20140309, sgpAndKlUser);
        assertEquals(4, sessionProps.size());
    }
}
