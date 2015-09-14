package crmdna.group;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.ICode;
import crmdna.group.Group.GroupProp;
import crmdna.member.MemberProp;
import crmdna.user.User;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static crmdna.common.TestUtil.ensureResourceNotFoundException;
import static org.junit.Assert.assertEquals;

public class GroupHelperTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private final String invalidClient = "invalid";
    GroupProp chennai;
    GroupProp sgp;
    GroupProp kl;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        chennai = Group.create(client, "Chennai", User.SUPER_USER);
        assertEquals(1, chennai.groupId);

        sgp = Group.create(client, "Singpaore", User.SUPER_USER);
        assertEquals(2, sgp.groupId);

        kl = Group.create(client, "KL", User.SUPER_USER);
        assertEquals(3, kl.groupId);
    }

    @Test
    public void populateTest() {
        MemberProp memberProp1 = new MemberProp();
        memberProp1.groupIds.add(sgp.groupId);
        memberProp1.groupIds.add(chennai.groupId);
        memberProp1.groupIds.add((long) 100); // non existing group id

        MemberProp memberProp2 = new MemberProp();
        memberProp2.groupIds.add(sgp.groupId);
        memberProp2.groupIds.add(chennai.groupId);
        memberProp2.groupIds.add(kl.groupId);

        final List<MemberProp> memberProps = new ArrayList<>();
        memberProps.add(memberProp1);
        memberProps.add(memberProp2);

        GroupHelper.populateName(client, memberProps);
        assertEquals(2, memberProp1.groups.size());
        assertEquals(3, memberProp2.groups.size());

        // client should be valid
        ensureResourceNotFoundException(new ICode() {

            @Override
            public void run() {
                GroupHelper.populateName(invalidClient, memberProps);
            }
        });
    }
}
