package crmdna.programtype;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
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
import crmdna.teacher.Teacher;
import crmdna.teacher.Teacher.TeacherProp;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
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

public class ProgramTypeTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private final String validUser = "valid@login.com";
    private final String userWithPermission = "withpermission@login.com";

    GroupProp sgp;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        // can call getAll without any records
        List<ProgramTypeProp> props = ProgramType.getAll(client);
        assertEquals(0, props.size());

        sgp = crmdna.group.Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        User.create(client, validUser, sgp.groupId, User.SUPER_USER);
        assertEquals(1, User.get(client, validUser).toProp(client).userId);

        User.create(client, userWithPermission, sgp.groupId, User.SUPER_USER);
        assertEquals(2, User.get(client, userWithPermission).toProp(client).userId);

        User.addClientLevelPrivilege(client, userWithPermission, ClientLevelPrivilege.UPDATE_PROGRAM_TYPE,
                User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void createTest() {

        PracticeProp ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);
        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(ishaKriya.practiceId);
        ProgramTypeProp prop = ProgramType.create(client, "Isha Kriya Online", practiceIds, userWithPermission);
        assertEquals("first entity has id 1", 1, prop.programTypeId);

        prop = ProgramType.safeGet(client, prop.programTypeId).toProp(client);
        assertEquals("name is populated correctly", "ishakriyaonline", prop.name);
        assertEquals("display name is populated correctly", "Isha Kriya Online", prop.displayName);
        assertEquals(1, prop.practiceProps.size());
        assertEquals("ishakriya", prop.practiceProps.get(0).name);

        // cannot create duplicate
        try {
            ProgramType.create("isha", "ISHA KRIYA onLine", practiceIds, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException e) {
            assertEquals("cannot create duplicate", Status.ERROR_RESOURCE_ALREADY_EXISTS, e.statusCode);
        }

        // ids should be in sequence
        prop = ProgramType.create(client, "Shambhavi 2 day", practiceIds, userWithPermission);
        assertEquals("id is in sequence", 2, prop.programTypeId);
        prop = ProgramType.create(client, "Inner Engineering Retreat", practiceIds, userWithPermission);
        assertEquals("practice id is in sequence", 3, prop.programTypeId);
        prop = ProgramType.create(client, "Surya Kriya", practiceIds, userWithPermission);
        assertEquals("practice id is in sequence", 4, prop.programTypeId);

        prop = ProgramType.safeGet(client, 2).toProp(client);
        assertEquals("name is populated correctly", "shambhavi2day", prop.name);
        assertEquals("display name is populated correctly", "Shambhavi 2 day", prop.displayName);
        prop = ProgramType.safeGet(client, 3).toProp(client);
        assertEquals("display name is populated correctly", "Inner Engineering Retreat", prop.displayName);
        prop = ProgramType.safeGet(client, 4).toProp(client);
        assertEquals("display name is populated correctly", "Surya Kriya", prop.displayName);
        assertEquals("name is populated correctly", "suryakriya", prop.name);

        // access control
        try {
            ProgramType.create("isha", "Hata Yoga - Asanas", practiceIds, validUser);
            assertTrue(false);
        } catch (APIException e) {
            assertEquals("permission required to edit program type", Status.ERROR_INSUFFICIENT_PERMISSION, e.statusCode);
        }

        // client should be valid
        try {
            ProgramType.create(invalidClient, "Hata Yoga - Upa Yoga", practiceIds, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException e) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, e.statusCode);
        }
    }

    @Test
    public void safeGetTest() {
        long shoonya = Practice.create(client, "Shoonya", User.SUPER_USER).practiceId;
        long shakthiChalana = Practice.create(client, "Shakthi Chalana Kriya", User.SUPER_USER).practiceId;
        long suryaNamaskar = Practice.create(client, "Surya Namaskar", User.SUPER_USER).practiceId;

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(suryaNamaskar);
        practiceIds.add(shoonya);
        practiceIds.add(shakthiChalana);

        ProgramTypeProp prop = ProgramType.create(client, "Shoonya Intensive", practiceIds, userWithPermission);
        assertTrue(prop.programTypeId != 0);

        prop = ProgramType.safeGet("isha", prop.programTypeId).toProp(client);
        assertEquals("name is populated correctly", "shoonyaintensive", prop.name);
        assertEquals("display name is populated correctly", "Shoonya Intensive", prop.displayName);

        // practice should be sorted by name
        assertEquals("shakthichalanakriya", prop.practiceProps.get(0).name);
        assertEquals("shoonya", prop.practiceProps.get(1).name);
        assertEquals("suryanamaskar", prop.practiceProps.get(2).name);

        // exception for non existing
        try {
            ProgramType.safeGet("isha", prop.programTypeId + 20939); // non
            // existant
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("exception thrown for non exisitng practise id", Status.ERROR_RESOURCE_NOT_FOUND,
                    ex.statusCode);
        }
    }

    @Test
    public void safeGetByIdOrNameTest() {
        long shoonya = Practice.create(client, "Shoonya", User.SUPER_USER).practiceId;
        long shakthiChalana = Practice.create(client, "Shakthi Chalana Kriya", User.SUPER_USER).practiceId;
        long suryaNamaskar = Practice.create(client, "Surya Namaskar", User.SUPER_USER).practiceId;

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(suryaNamaskar);
        practiceIds.add(shoonya);
        practiceIds.add(shakthiChalana);

        ProgramTypeProp prop = ProgramType.create(client, "Shoonya Intensive", practiceIds, userWithPermission);
        assertTrue(prop.programTypeId != 0);

        prop = ProgramType.safeGetByIdOrName("isha", "shoonya   InteNsive").toProp(client);
        assertEquals("name is populated correctly", "shoonyaintensive", prop.name);
        assertEquals("display name is populated correctly", "Shoonya Intensive", prop.displayName);

        prop = ProgramType.safeGetByIdOrName("isha", prop.programTypeId + "").toProp(client);
        assertEquals("get by id", "shoonyaintensive", prop.name);
        assertEquals("get by id", "Shoonya Intensive", prop.displayName);

        // practice should be sorted by name
        assertEquals("shakthichalanakriya", prop.practiceProps.get(0).name);
        assertEquals("shoonya", prop.practiceProps.get(1).name);
        assertEquals("suryanamaskar", prop.practiceProps.get(2).name);

        // exception for non existing
        try {
            ProgramType.safeGetByIdOrName("isha", "non existing"); // non-existing
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("exception thrown for non exisitng practise id", Status.ERROR_RESOURCE_NOT_FOUND,
                    ex.statusCode);
        }

        // exception for non existing
        try {
            ProgramType.safeGetByIdOrName("isha", 100 + ""); // non-existing
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("exception thrown for non exisitng practise id", Status.ERROR_RESOURCE_NOT_FOUND,
                    ex.statusCode);
        }
    }

    @Test
    public void ensureValidTest() {
        Set<Long> programTypeIds = new HashSet<>();
        programTypeIds.add((long) 100);
        programTypeIds.add((long) 101);

        try {
            ProgramType.ensureValid(client, programTypeIds);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // add some program types
        ProgramTypeProp pt1 = ProgramType.create(client, "PT1", null, User.SUPER_USER);
        ProgramTypeProp pt2 = ProgramType.create(client, "PT2", null, User.SUPER_USER);

        try {
            ProgramType.ensureValid(client, programTypeIds);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        programTypeIds.clear();
        programTypeIds.add(pt1.programTypeId);
        programTypeIds.add(pt2.programTypeId);

        ProgramType.ensureValid(client, programTypeIds); // no exception
    }

    @Test
    public void renameTest() {
        long shoonya = Practice.create(client, "Shoonya", User.SUPER_USER).practiceId;
        long shakthiChalana = Practice.create(client, "Shakthi Chalana Kriya", User.SUPER_USER).practiceId;
        long suryaNamaskar = Practice.create(client, "Surya Namaskar", User.SUPER_USER).practiceId;
        long shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER).practiceId;

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(suryaNamaskar);
        practiceIds.add(shoonya);
        practiceIds.add(shakthiChalana);

        ProgramTypeProp prop = ProgramType.create("isha", "Shoonya Intensive", practiceIds, userWithPermission);
        practiceIds.clear();
        practiceIds.add(shambhavi);
        ProgramType.create(client, "Shambhavi 2 day", practiceIds, userWithPermission);

        try {
            ProgramType.rename("isha", prop.programTypeId, "Shambhavi 2 day", userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("cannot rename to existing", Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        // can change case and rename
        ProgramType.rename("isha", prop.programTypeId, "shoonya intensive", userWithPermission);
        prop = ProgramType.safeGet("isha", prop.programTypeId).toProp(client);
        assertEquals("name correctly populated after rename", "shoonyaintensive", prop.name);
        assertEquals("name correctly populated after rename", "shoonya intensive", prop.displayName);

        ProgramType.rename("isha", prop.programTypeId, "Shoonya Meditation", userWithPermission);
        prop = ProgramType.safeGet("isha", prop.programTypeId).toProp(client);
        assertEquals("name populated correctly", "shoonyameditation", prop.name);
        assertEquals("display name populated correctly", "Shoonya Meditation", prop.displayName);

        try {
            ProgramType.rename("isha", prop.programTypeId, "Shoonya Training", validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("permission required to rename practice", Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        try {
            ProgramType.rename(invalidClient, prop.programTypeId, "Shoonya", userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test
    public void getAllTest() {

        try {
            ProgramType.getAll(invalidClient);
        } catch (APIException ex) {
            assertEquals("client should be valid", Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        assertEquals("getall returns empty list if no records", 0, ProgramType.getAll(client).size());

        long suryaNamaskar = Practice.create(client, "Surya Namaskar", User.SUPER_USER).practiceId;
        long shoonya = Practice.create(client, "Shoonya", User.SUPER_USER).practiceId;
        long shakthiChalana = Practice.create(client, "Shakthi Chalana Kriya", User.SUPER_USER).practiceId;
        long shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER).practiceId;

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(suryaNamaskar);
        practiceIds.add(shoonya);
        practiceIds.add(shakthiChalana);
        ProgramType.create(client, "Shoonya Intensive", practiceIds, userWithPermission);

        practiceIds.clear();
        practiceIds.add(shambhavi);
        ProgramType.create(client, "Shambhavi 2 day", practiceIds, userWithPermission);
        ObjectifyFilter.complete();

        List<ProgramTypeProp> props = ProgramType.getAll(client);
        assertEquals(2, props.size());
        // should be sorted
        assertEquals("shambhavi2day", props.get(0).name);
        assertEquals("Shambhavi 2 day", props.get(0).displayName);
        assertEquals(1, props.get(0).practiceProps.size());
        assertEquals("shambhavi", ((PracticeProp) props.get(0).practiceProps.toArray()[0]).name);

        assertEquals("shoonyaintensive", props.get(1).name);
        assertEquals("Shoonya Intensive", props.get(1).displayName);
        assertEquals(3, props.get(1).practiceProps.size());
        // practice should be sorted by name
        assertEquals("shakthichalanakriya", props.get(1).practiceProps.get(0).name);
        assertEquals("shoonya", props.get(1).practiceProps.get(1).name);
        assertEquals("suryanamaskar", props.get(1).practiceProps.get(2).name);
    }

    @Test
    public void getPracticeIdsTest() {
        PracticeProp p1 = Practice.create(client, "p1", User.SUPER_USER);
        PracticeProp p2 = Practice.create(client, "p2", User.SUPER_USER);
        PracticeProp p3 = Practice.create(client, "p3", User.SUPER_USER);

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(p1.practiceId);
        practiceIds.add(p2.practiceId);
        ProgramTypeProp pt1 = ProgramType.create(client, "pt1", practiceIds, User.SUPER_USER);

        practiceIds.clear();
        practiceIds.add(p2.practiceId);
        practiceIds.add(p3.practiceId);
        ProgramTypeProp pt2 = ProgramType.create(client, "pt2", practiceIds, User.SUPER_USER);

        Set<Long> programTypeIds = new HashSet<>();
        programTypeIds.add(pt1.programTypeId);
        programTypeIds.add((long) 100);

        practiceIds = ProgramType.getPracticeIds(client, programTypeIds);
        assertEquals(2, practiceIds.size());
        assertTrue(practiceIds.contains(p1.practiceId));
        assertTrue(practiceIds.contains(p2.practiceId));

        programTypeIds.add(pt2.programTypeId);
        practiceIds = ProgramType.getPracticeIds(client, programTypeIds);
        assertEquals(3, practiceIds.size());
        assertTrue(practiceIds.contains(p1.practiceId));
        assertTrue(practiceIds.contains(p2.practiceId));
        assertTrue(practiceIds.contains(p3.practiceId));
    }

    @Test
    public void toPropTest() {
        ProgramTypeEntity entity = new ProgramTypeEntity();
        entity.programTypeId = 123l;
        entity.displayName = "Shoonya Intensive";
        entity.name = entity.displayName.toLowerCase();

        ProgramTypeProp prop = entity.toProp(client);
        assertEquals("practise id correctly populated", 123, prop.programTypeId);
        assertEquals("display name correctly populated", "Shoonya Intensive", prop.displayName);
        assertEquals("name correctly populated", "shoonya intensive", prop.name);
    }

    @Test
    public void updatePracticeIdsTest() {
        PracticeProp upaYogaBasic = Practice.create(client, "Upa Yoga Basic", User.SUPER_USER);
        PracticeProp ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(upaYogaBasic.practiceId);
        practiceIds.add(ishaKriya.practiceId);

        ProgramTypeProp mysticEye = ProgramType.create(client, "Mystic Eye", practiceIds, User.SUPER_USER);

        VenueProp expo = Venue.create(client, "Expo", "expo", "expo", sgp.groupId, User.SUPER_USER);
        TeacherProp sadhguru = Teacher.create(client, "", "", "sadhguru@ishafoundation"
            + ".org", sgp.groupId, User.SUPER_USER);

        ProgramProp mysticEye12Jan = Program.create(client, sgp.groupId, mysticEye.programTypeId, expo.venueId,
                sadhguru.teacherId, 20140112, 20140112, 1, null, 0.0, Currency.SGD, User.SUPER_USER);

        ContactProp contact = new ContactProp();
        contact.email = "priya@gmail.com";
        contact.asOfyyyymmdd = 20140801;

        MemberProp priya = Member.create(client, sgp.groupId, contact, false, User.SUPER_USER);
        priya = Member.addOrDeleteProgram(client, priya.memberId, mysticEye12Jan.programId, true, userWithPermission);
        assertTrue(priya.practiceIds.contains(upaYogaBasic.practiceId));
        assertTrue(priya.practiceIds.contains(ishaKriya.practiceId));

        contact.email = "kalyan@gmail.com";

        MemberProp kalyan = Member.create(client, sgp.groupId, contact, false, User.SUPER_USER);
        kalyan = Member.addOrDeleteProgram(client, kalyan.memberId, mysticEye12Jan.programId, true, userWithPermission);
        assertTrue(kalyan.practiceIds.contains(upaYogaBasic.practiceId));
        assertTrue(kalyan.practiceIds.contains(ishaKriya.practiceId));

        PracticeProp mysticEyePractice = Practice.create(client, "Mystic Eye", User.SUPER_USER);
        practiceIds.clear();
        practiceIds.add(mysticEyePractice.practiceId);
        ProgramType.updatePracticeIds(client, mysticEye.programTypeId, practiceIds, User.SUPER_USER);

        ObjectifyFilter.complete();
        priya = MemberLoader.safeGet(client, priya.memberId, userWithPermission).toProp();
        assertTrue(priya.practiceIds.contains(mysticEyePractice.practiceId));
        assertTrue(!priya.practiceIds.contains(upaYogaBasic.practiceId));
        assertTrue(!priya.practiceIds.contains(ishaKriya.practiceId));

        kalyan = MemberLoader.safeGet(client, kalyan.memberId, userWithPermission).toProp();
        assertTrue(kalyan.practiceIds.contains(mysticEyePractice.practiceId));
        assertTrue(!kalyan.practiceIds.contains(upaYogaBasic.practiceId));
        assertTrue(!kalyan.practiceIds.contains(ishaKriya.practiceId));
    }

    @Test
    public void practiceIdSavedWhenCreating() {
        PracticeProp upaYogaBasic = Practice.create(client, "Upa Yoga Basic", User.SUPER_USER);
        PracticeProp ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);

        Set<Long> practiceIds = new HashSet<>();
        practiceIds.add(upaYogaBasic.practiceId);
        practiceIds.add(ishaKriya.practiceId);

        ProgramTypeProp mysticEye = ProgramType.create(client, "Mystic Eye", practiceIds, User.SUPER_USER);

        ProgramTypeEntity mysticEyeEntity = ProgramType.safeGet(client, mysticEye.programTypeId);
        assertTrue(mysticEyeEntity.practiceIds.contains(upaYogaBasic.practiceId));
        assertTrue(mysticEyeEntity.practiceIds.contains(ishaKriya.practiceId));
        assertEquals(2, mysticEyeEntity.practiceIds.size());
    }

    @Test
    public void practiceIdSavedWhenUpdating() {
        PracticeProp upaYogaBasic = Practice.create(client, "Upa Yoga Basic", User.SUPER_USER);

        ProgramTypeProp mysticEye = ProgramType.create(client, "Mystic Eye", Utils.getSet(upaYogaBasic.practiceId),
                User.SUPER_USER);

        ProgramTypeEntity mysticEyeEntity = ProgramType.safeGet(client, mysticEye.programTypeId);
        assertTrue(mysticEyeEntity.practiceIds.contains(upaYogaBasic.practiceId));
        assertEquals(1, mysticEyeEntity.practiceIds.size());

        PracticeProp ishaKriya = Practice.create(client, "Isha Kriya", User.SUPER_USER);

        ProgramType.updatePracticeIds(client, mysticEye.programTypeId,
                Utils.getSet(upaYogaBasic.practiceId, ishaKriya.practiceId), User.SUPER_USER);

        mysticEyeEntity = ProgramType.safeGet(client, mysticEye.programTypeId);
        assertTrue(mysticEyeEntity.practiceIds.contains(upaYogaBasic.practiceId));
        assertTrue(mysticEyeEntity.practiceIds.contains(ishaKriya.practiceId));
        assertEquals(2, mysticEyeEntity.practiceIds.size());
    }
}
