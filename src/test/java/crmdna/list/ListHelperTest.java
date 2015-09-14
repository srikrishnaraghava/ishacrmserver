package crmdna.list;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.member.Member;
import crmdna.member.MemberProp;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserProp;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.assertEquals;

public class ListHelperTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private GroupProp sgp, kl;
    private ListProp sgpTeachers;
    private UserProp userWithSendMailPermissionForSgp;
    private UserProp validUser;

    private MemberProp sathya;
    private MemberProp sharmila;
    private MemberProp murugavel;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);
        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        kl = Group.create(client, "KL", User.SUPER_USER);
        assertEquals(2, kl.groupId);

        validUser = User.create(client, "validuser@dummy.com", sgp.groupId, User.SUPER_USER);

        userWithSendMailPermissionForSgp =
                User.create(client, "userwithsendmail@dummy.com", sgp.groupId, User.SUPER_USER);
        User.addGroupLevelPrivilege(client, sgp.groupId, userWithSendMailPermissionForSgp.email,
                GroupLevelPrivilege.SEND_EMAIL, User.SUPER_USER);

        sgpTeachers = List.createRestricted(client, sgp.groupId, "Sgp Teachers", null, User.SUPER_USER);

        ContactProp c = new ContactProp();
        c.email = "sathya.t@ishafoundation.org";
        c.asOfyyyymmdd = 20141026;
        c.firstName = "Sathya";
        sathya = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);

        c.firstName = "Sharmila";
        c.mobilePhone = "+6593849384";
        c.email = null;
        sharmila = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);
        assertEquals("Sharmila", sharmila.contact.firstName);

        c.email = "murugavel@gmail.com";
        murugavel = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);
        assertEquals("murugavel@gmail.com", murugavel.contact.email);

        // add sathya and sharmila to sgpTeachers
        Member.addOrDeleteList(client, sathya.memberId, sgpTeachers.listId,
                true, User.SUPER_USER);
        Member.addOrDeleteList(client, sharmila.memberId, sgpTeachers.listId,
                true, User.SUPER_USER);
        Member.subscribeGroup(client, sathya.memberId, sgp.groupId, User.SUPER_USER);
        Member.subscribeGroup(client, sharmila.memberId, sgp.groupId, User.SUPER_USER);

        ObjectifyFilter.complete();
    }

    @Test
    public void populateGroupNameTest() {
        ListProp listProp1 = List.createPublic(client, sgp.groupId, "SGP List", User.SUPER_USER);
        ListProp listProp2 = List.createPublic(client, kl.groupId, "KL List", User.SUPER_USER);

        java.util.List<ListProp> listProps = new ArrayList<>();
        listProps.add(listProp1);
        listProps.add(listProp2);

        ListHelper.populateGroupName(client, listProps);

        assertEquals("Singapore", listProp1.groupName);
        assertEquals("KL", listProp2.groupName);
    }
}
