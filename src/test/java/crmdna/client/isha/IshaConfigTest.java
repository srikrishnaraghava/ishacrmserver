package crmdna.client.isha;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.client.isha.IshaConfig.IshaConfigProp;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class IshaConfigTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    Set<Long> sathsangPracticeIds = new HashSet<>();
    String validUser = "valid@valid.com";
    String validUserWithPermission = "withperm@valid.com";

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create("isha");

        PracticeProp shambhavi = Practice.create("isha", "Shambhavi",
                User.SUPER_USER);
        PracticeProp mysticEye = Practice.create("isha", "MysticEye",
                User.SUPER_USER);

        sathsangPracticeIds.add(shambhavi.practiceId);
        sathsangPracticeIds.add(mysticEye.practiceId);

        GroupProp sgp = Group.create("isha", "Singapore", User.SUPER_USER);

        User.create("isha", validUser, sgp.groupId, User.SUPER_USER);

        User.create("isha", validUserWithPermission, sgp.groupId,
                User.SUPER_USER);
        User.addClientLevelPrivilege("isha", validUserWithPermission,
                ClientLevelPrivilege.UPDATE_CUSTOM_CONFIG, User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void safeGetTest() {
        try {
            IshaConfig.safeGet();
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        IshaConfig.setSathsangPractices(sathsangPracticeIds, User.SUPER_USER);

        IshaConfigProp customConfigProp = IshaConfig.safeGet();
        assertEquals(2, customConfigProp.sathsangPracticeIds.size());
        assertTrue(customConfigProp.sathsangPracticeIds
                .containsAll(sathsangPracticeIds));
    }

    @Test
    public void setSathsangPracticeIdsTest() {
        // check permission
        try {
            IshaConfig.setSathsangPractices(sathsangPracticeIds, validUser);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        // practices cannot be null
        try {
            IshaConfig.setSathsangPractices(null, validUserWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // cannot be empty set
        try {
            IshaConfig.setSathsangPractices(new HashSet<Long>(),
                    validUserWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // practices should be valid
        try {
            Set<Long> practiceIds = new HashSet<>();
            practiceIds.add(100l);
            IshaConfig.setSathsangPractices(practiceIds,
                    validUserWithPermission);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }

        // all ok
        IshaConfig.setSathsangPractices(sathsangPracticeIds,
                validUserWithPermission);
        Set<Long> practiceIds = IshaConfig.safeGet().sathsangPracticeIds;
        assertEquals(2, practiceIds.size());
        assertTrue(practiceIds.containsAll(sathsangPracticeIds));

    }
}
