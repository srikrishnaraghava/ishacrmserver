package crmdna.teacher;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group.GroupProp;
import crmdna.teacher.Teacher.TeacherProp;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class TeacherTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    private final String validUser = "valid@login.com";
    private final String userWithPermission = "withpermission@login.com";

    private GroupProp sgp, kl, iyc;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        sgp = crmdna.group.Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        kl = crmdna.group.Group.create(client, "KL", User.SUPER_USER);
        assertEquals(2, kl.groupId);

        iyc = crmdna.group.Group.create(client, "Isha Yoga Center",
                User.SUPER_USER);
        assertEquals(3, iyc.groupId);

        User.create(client, validUser, sgp.groupId, User.SUPER_USER);
        assertEquals(1, User.get(client, validUser).toProp(client).userId);

        User.create(client, userWithPermission, sgp.groupId, User.SUPER_USER);
        assertEquals(2,
                User.get(client, userWithPermission).toProp(client).userId);

        User.addClientLevelPrivilege(client, userWithPermission,
                ClientLevelPrivilege.UPDATE_TEACHER, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void createTest() {
        try {
            Teacher.create(invalidClient, "", "", "ramesh.c@ishafoundation.org",
                    sgp.groupId, userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("error for invalid client",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Teacher.create(client, "", "", "ramesh.c@ishafoundation.org", sgp.groupId,
                    validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("user needs permission to create",
                    Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        TeacherProp teacherProp = Teacher.create(client, "", "",
            "ramesh.c@IshaFoundation.org", sgp.groupId, userWithPermission);
        teacherProp = Teacher.safeGet(client, teacherProp.teacherId).toProp();
        assertEquals("first id is 1", 1, teacherProp.teacherId);
        assertEquals("email saved correctly in lower case",
                "ramesh.c@ishafoundation.org", teacherProp.email);
        assertEquals("group id saved correctly", sgp.groupId,
                teacherProp.groupId);

        // cannot create duplicate
        try {
            Teacher.create(client, "", "", "ramesh.c@ishaFoundation.ORG", sgp.groupId,
                    userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("cannot create duplicate",
                    Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        teacherProp = Teacher.create(client, "", "", "rams@ishafoundation.org",
                sgp.groupId, userWithPermission);
        teacherProp = Teacher.safeGet(client, teacherProp.teacherId).toProp();
        assertEquals("id assigned in sequence", 2, teacherProp.teacherId);

        try {
            Teacher.create(client, "", "", "bademail", sgp.groupId, userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("email should be valid",
                    Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void safeGetTest() {
        try {
            Teacher.safeGet(invalidClient, 1);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Teacher.safeGet(client, 1);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("teacher id should be valid",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        TeacherProp teacherProp = Teacher.create(client, "", "",
            "ramS@ishafoundation.org", sgp.groupId, userWithPermission);
        teacherProp = Teacher.safeGet(client, teacherProp.teacherId).toProp();
        assertEquals("first id is 1", 1, teacherProp.teacherId);
        assertEquals("email saved correctly in lower case",
                "rams@ishafoundation.org", teacherProp.email);
    }

    @Test
    public void safeGetByIdOrEmailTest() {
        try {
            Teacher.safeGetByIdOrEmail(invalidClient, "rams@ishafoundation.org");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should exist",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Teacher.safeGetByIdOrEmail(client, "rams@ishafoundation.org");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("teacher email should exist",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        TeacherProp teacherProp = Teacher.create(client, "", "",
            "ramS@ishafoundation.org", sgp.groupId, userWithPermission);
        teacherProp = Teacher.safeGetByIdOrEmail(client,
                "rams@ishafoundation.org").toProp();
        assertEquals("first id is 1", 1, teacherProp.teacherId);
        assertEquals("email saved correctly in lower case",
                "rams@ishafoundation.org", teacherProp.email);

        teacherProp = Teacher.safeGetByIdOrEmail(client,
                teacherProp.teacherId + "").toProp();
        assertEquals("get by id", 1, teacherProp.teacherId);
        assertEquals("get by id", "rams@ishafoundation.org", teacherProp.email);
    }

    @Test
    public void updateTest() {
        Teacher.create(client, "", "", "ramS@ishafoundation.org", sgp.groupId,
                userWithPermission);
        Teacher.create(client, "", "", "ramesh.c@ishayoga.org", sgp.groupId,
                userWithPermission);

        try {
            Teacher.update(invalidClient, 1, null, null, userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        try {
            Teacher.update(client, 1, null, null, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("user should have permission",
                    Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        try {
            Teacher.update(client, 100, null, null, userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("teacher id should be valid",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // can update only group id
        Teacher.update(client, 1, null, iyc.groupId, userWithPermission);
        TeacherProp teacherProp = Teacher.safeGet(client, 1).toProp();
        assertEquals(1, teacherProp.teacherId);
        assertEquals("email unaffected", "rams@ishafoundation.org",
                teacherProp.email);
        assertEquals("group id changed", iyc.groupId, teacherProp.groupId);

        // can update only email
        Teacher.update(client, 1, "ramasamy.puLLappan@ishafoundation.org",
                null, userWithPermission);
        teacherProp = Teacher.safeGet(client, 1).toProp();
        assertEquals(1, teacherProp.teacherId);
        assertEquals("email changed", "ramasamy.pullappan@ishafoundation.org",
                teacherProp.email);
        assertEquals("group id not changed", iyc.groupId, teacherProp.groupId);

        // can update both email and group id
        Teacher.update(client, 2, "ramesh.c@ishafoundatIOn.org", iyc.groupId,
                userWithPermission);
        teacherProp = Teacher.safeGet(client, 2).toProp();
        assertEquals(2, teacherProp.teacherId);
        assertEquals("email changed", "ramesh.c@ishafoundation.org",
                teacherProp.email);
        assertEquals("group id changed", iyc.groupId, teacherProp.groupId);

        try {
            Teacher.update(client, 2, "bademail", sgp.groupId,
                    userWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("email should be valid",
                    Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void getAllTest() {
        try {
            Teacher.getAll(invalidClient);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals("client should be valid",
                    Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        List<TeacherProp> teacherProps = Teacher.getAll(client);
        assertEquals("getAll works when no teachers are present", 0,
                teacherProps.size());

        Teacher.create(client, "", "", "ramasamy.pullappan@ishafoundation.org",
                sgp.groupId, userWithPermission);
        Teacher.create(client, "", "", "ramesh.c@ishafoundation.org", sgp.groupId,
                userWithPermission);

        teacherProps = Teacher.getAll(client);
        assertEquals("all teachers returned", 2, teacherProps.size());
        // should be sorted
        assertEquals("email is correct",
                "ramasamy.pullappan@ishafoundation.org",
                teacherProps.get(0).email);
        assertEquals("group id is correct", sgp.groupId,
                teacherProps.get(0).groupId);

        assertEquals("email is correct", "ramesh.c@ishafoundation.org",
                teacherProps.get(1).email);
        assertEquals("group id is correct", sgp.groupId,
                teacherProps.get(1).groupId);
    }
}
