package crmdna.user;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.user.User.GroupLevelPrivilege;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class UserHelperTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());
    // local implementation / test harness implementation becomes HRD
    // only if setApplyAllHighRepJobPolicy is set. If the implementation is not
    // HRD then
    // cross group transactions would fail (as master slave does not support it)

    String client = "isha";
    GroupProp sgp;
    GroupProp kl;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        kl = Group.create(client, "KL", User.SUPER_USER);
        assertEquals(2, kl.groupId);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void getGroupIdsWithPrivilegeTest() {

        UserProp userProp = User.create(client, "email1@dummy.com",
                sgp.groupId, User.SUPER_USER);

        User.addGroupLevelPrivilege(client, sgp.groupId, userProp.email,
                GroupLevelPrivilege.CHECK_IN, User.SUPER_USER);

        User.addGroupLevelPrivilege(client, kl.groupId, userProp.email,
                GroupLevelPrivilege.CHECK_IN, User.SUPER_USER);

        Set<Long> groupIds = UserHelper.getGroupIdsWithPrivilage(client,
                userProp.email, GroupLevelPrivilege.CHECK_IN);
        assertEquals(2, groupIds.size());
        assertTrue(groupIds.contains(sgp.groupId));
        assertTrue(groupIds.contains(kl.groupId));
    }
}
