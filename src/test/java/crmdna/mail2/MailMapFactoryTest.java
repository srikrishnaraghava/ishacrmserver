package crmdna.mail2;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.google.common.collect.Lists;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.list.List;
import crmdna.list.ListHelper;
import crmdna.list.ListProp;
import crmdna.member.Member;
import crmdna.member.MemberProp;
import crmdna.member.MemberQueryCondition;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserProp;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class MailMapFactoryTest {
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
    public void emailMapDoesNotIncludeMembersWOEmail() {

        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        mqc.listIds.add(sgpTeachers.listId);

        MailMap mailMap = MailMapFactory.getFromMemberQueryCondition(mqc, sgp.groupId,
                "Isha", "Isha", userWithSendMailPermissionForSgp.email);

        /*MailMap mailMap =
            ListHelper.getMailMap(client, sgpTeachers.listId, "Isha", "Isha",
                    userWithSendMailPermissionForSgp.email);*/

        assertEquals(1, mailMap.size());
        Set<String> emails = mailMap.getEmails();
        assertEquals(1, emails.size());
        assertTrue(emails.contains("sathya.t@ishafoundation.org"));

        // sharmila is missing

        assertEquals("Sathya", mailMap.get(MailMap.MergeVarID.FIRST_NAME,
            "sathya.t@ishafoundation.org"));
        assertEquals("Isha", mailMap.get(MailMap.MergeVarID.LAST_NAME,
            "sathya.t@ishafoundation.org"));
    }

    @Test
    public void emailMapDoesNotIncludeUnsubscribedMembers() {
        Member.unsubscribeGroup(client, sathya.memberId, sgp.groupId, User.SUPER_USER);

        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        mqc.listIds.add(sgpTeachers.listId);
        MailMap mailMap = MailMapFactory.getFromMemberQueryCondition(mqc, sgp.groupId,
                "Isha", "Isha", userWithSendMailPermissionForSgp.email);

        //sathya is unsubscribed, sharmila does not have a valid email
        assertTrue(mailMap.isEmpty());
    }

    @Test
    public void defaultAppliedForMembersWOLastName() {

        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        mqc.listIds.add(sgpTeachers.listId);
        MailMap mailMap = MailMapFactory.getFromMemberQueryCondition(mqc, sgp.groupId,
                "Isha", "Isha", userWithSendMailPermissionForSgp.email);

        /*MailMap mailMap =
                ListHelper.getMailMap(client, sgpTeachers.listId, "Isha", "Isha",
                        userWithSendMailPermissionForSgp.email);*/

        assertEquals(1, mailMap.size());
        Set<String> emails = mailMap.getEmails();
        assertEquals(1, emails.size());
        assertTrue(emails.contains("sathya.t@ishafoundation.org"));

        assertEquals("Sathya", mailMap.get(MailMap.MergeVarID.FIRST_NAME,
            "sathya.t@ishafoundation.org"));
        assertEquals("Isha", mailMap.get(MailMap.MergeVarID.LAST_NAME,
            "sathya.t@ishafoundation.org"));
    }

    @Test
    public void userWOPermissionCannotGetMailMap() {
        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        mqc.listIds.add(sgpTeachers.listId);

        try {
            MailMapFactory.getFromMemberQueryCondition(mqc, sgp.groupId,
                    "Isha", "Isha", validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }
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

    @Test
    public void getEmailMapFromListOfMapTest() {

        //Both firstname and last name specified
        Map<String, String> map = new HashMap<>();
        map.put("firstname", "Sathya ");
        map.put("lastname", "Thilakan ");
        final String email = "sathya.t@ishafoundation.org";
        map.put("email", email);

        MailMap mailMap = MailMapFactory.getFromListOfMap(Lists.newArrayList(map), "Isha",
                "Isha");
        assertEquals("Sathya", mailMap.get(MailMap.MergeVarID.FIRST_NAME, email));
        assertEquals("Thilakan", mailMap.get(MailMap.MergeVarID.LAST_NAME, email));
        assertEquals(1, mailMap.size());

        //full name specified as last name
        map.put("firstname", "Sathya Thilakan");
        map.put("lastname", null);

        mailMap = MailMapFactory.getFromListOfMap(Lists.newArrayList(map), "Isha", "Isha");
        assertEquals("Sathya", mailMap.get(MailMap.MergeVarID.FIRST_NAME, email));
        assertEquals("Thilakan", mailMap.get(MailMap.MergeVarID.LAST_NAME, email));
        assertEquals(1, mailMap.size());

        //full name specified with initial
        map.put("firstname", "T. Sathya");
        map.put("lastname", null);
        mailMap = MailMapFactory.getFromListOfMap(Lists.newArrayList(map), "Isha", "Isha");
        assertEquals("T. Sathya", mailMap.get(MailMap.MergeVarID.FIRST_NAME, email));
        assertEquals("Isha", mailMap.get(MailMap.MergeVarID.LAST_NAME, email));
        assertEquals(1, mailMap.size());

        //test space trimmed in email field
        map.clear();
        map.put("firstname", "Sathya");
        map.put("lastname", null);
        map.put("email", " sathya.t@ishafoundation.org ");
        mailMap = MailMapFactory.getFromListOfMap(Lists.newArrayList(map), "Isha", "Isha");
        assertTrue(mailMap.getEmails().contains("sathya.t@ishafoundation.org"));
    }
}
