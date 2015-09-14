package crmdna.list;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.mail2.MailMap;
import crmdna.member.*;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserProp;
import org.junit.Before;
import org.junit.Test;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.*;

public class BulkSubscriptionTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    private GroupProp sgp;
    private GroupProp kl;
    private UserProp userWithListPermissionForSgp;
    private UserProp validUser;
    private PracticeProp shambhavi;

    private MemberProp sathya;
    private MemberProp sharmila;
    private MemberProp murugavel;

    private ListProp shambhaviSgpList;

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

        userWithListPermissionForSgp =
                User.create(client, "userwithcreatelist@dummy.com", sgp.groupId, User.SUPER_USER);
        User.addGroupLevelPrivilege(client, sgp.groupId, userWithListPermissionForSgp.email,
                GroupLevelPrivilege.UPDATE_LIST, User.SUPER_USER);

        shambhavi = Practice.create(client, "Shambhavi", User.SUPER_USER);

        ContactProp c = new ContactProp();
        c.email = "sathya.t@ishafoundation.org";
        c.asOfyyyymmdd = 20141026;
        sathya = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);

        c.email = "sharmila@gmail.com";
        sharmila = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);
        assertEquals("sharmila@gmail.com", sharmila.contact.email);

        c.email = "murugavel@gmail.com";
        murugavel = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);
        assertEquals("murugavel@gmail.com", murugavel.contact.email);

        shambhaviSgpList =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        User.SUPER_USER);
        assertEquals(1, shambhaviSgpList.listId);
    }

    @Test
    public void permissionRequiredToBulkSubscribe() {
        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        try {
            Member.bulkSubscribeList(client, shambhaviSgpList.listId, mailMap, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }
    }

    @Test
    public void newMemberCreatedForNonExistingEmail() {

        MailMap mailMap = new MailMap();
        mailMap.add("Syamala@gMail.com", "Syamala", "Thilakan");

        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        mqc.email = "syamala@gmail.com";
        int count = MemberLoader.getCount(mqc, validUser.email);
        assertEquals(0, count);

        Member.bulkSubscribeList(client, shambhaviSgpList.listId, mailMap,
                userWithListPermissionForSgp.email);
        ObjectifyFilter.complete();

        mqc = new MemberQueryCondition(client, 10000);
        mqc.listIds.add(shambhaviSgpList.listId);
        java.util.List<MemberProp> memberProps = MemberLoader.querySortedProps(mqc, validUser.email);
        assertEquals(1, memberProps.size());
        MemberProp memberProp = memberProps.get(0);
        assertEquals("syamala@gmail.com", memberProp.contact.email);
        assertEquals("Syamala", memberProp.contact.firstName);
        assertEquals("Thilakan", memberProp.contact.lastName);
        assertTrue(memberProp.groupIds.contains(shambhaviSgpList.groupId));
        assertEquals(1, memberProp.groupIds.size());
        assertEquals(DateUtils.toYYYYMMDD(new Date()), memberProp.contact.asOfyyyymmdd);
        assertTrue(memberProp.practiceIds.contains(shambhavi.practiceId));
    }

    @Test
    public void nameNotUpdatedForExistingMember() {

        assertEquals("sathya.t@ishafoundation.org", sathya.contact.email);
        assertNull(sathya.contact.firstName);
        assertNull(sathya.contact.lastName);

        MailMap mailMap = new MailMap();
        mailMap.add("sathya.t@ishafoundation.org", "Sathya", "Thilakan");

        Member.bulkSubscribeList(client, shambhaviSgpList.listId, mailMap, User.SUPER_USER);
        ObjectifyFilter.complete();

        MemberQueryCondition mqc = new MemberQueryCondition(client, 10000);
        mqc.listIds.add(shambhaviSgpList.listId);

        java.util.List<MemberProp> memberProps = MemberLoader.querySortedProps(mqc, User.SUPER_USER);
        assertEquals(1, memberProps.size());
        assertEquals(sathya.memberId, memberProps.get(0).memberId);

        sathya = memberProps.get(0);
        assertEquals("sathya.t@ishafoundation.org", sathya.contact.email);
        assertNull(sathya.contact.firstName);
        assertNull(sathya.contact.lastName);
        assertTrue(sathya.listIds.contains(shambhaviSgpList.listId));
        assertTrue(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertTrue(sathya.practiceIds.contains(shambhavi.practiceId));
    }

    @Test
    public void fullTestBulkSubscription() {

        // test a slightly complex case by adding 6 emails of which 3 are
        // existing members (sathya, sharmila, murugavel),
        // 1 already subscribed to list (sathya)
        // 1 already unsubscribed to group (murugavel)

        Member.addOrDeleteList(client, sathya.memberId, shambhaviSgpList.listId, true, User.SUPER_USER);

        Member.subscribeGroup(client, murugavel.memberId, sgp.groupId, User.SUPER_USER);
        Member.unsubscribeGroup(client, murugavel.memberId, sgp.groupId, User.SUPER_USER);

        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertTrue(sathya.listIds.contains(shambhaviSgpList.listId));
        assertTrue(sathya.practiceIds.contains(shambhavi.practiceId));

        murugavel = MemberLoader.safeGet(client, murugavel.memberId, User.SUPER_USER).toProp();
        assertTrue(murugavel.unsubscribedGroupIds.contains(sgp.groupId));

        MailMap mailMap = new MailMap();
        mailMap.add(sathya.contact.email, "Sathya", "Thilakan");
        mailMap.add(sharmila.contact.email, "Sharmila", "Napa");
        mailMap.add(murugavel.contact.email, "Murugavel", "Gnanasekaran");
        mailMap.add("sowmya.ram@gmail.com", "Sowmya", "Ramakrishnan");
        mailMap.add("rthilakan@gmail.com", "Thilakan", "Ramamurthy");
        mailMap.add("syamala@gmail.com", "Syamala", "Thilakan");

        assertEquals(6, mailMap.size());

        BulkSubscriptionResultProp result =
                Member.bulkSubscribeList(client, shambhaviSgpList.listId, mailMap,
                        userWithListPermissionForSgp.email);
        ObjectifyFilter.complete();

        int numTotal = result.existingMemberEmails.size() + result.newMemberEmails.size();
        assertEquals(6, numTotal);

        assertEquals(3, result.existingMemberEmails.size());
        assertTrue(result.existingMemberEmails.contains(sathya.contact.email));
        assertTrue(result.existingMemberEmails.contains(sharmila.contact.email));
        assertTrue(result.existingMemberEmails.contains(murugavel.contact.email));

        assertEquals(3, result.newMemberEmails.size());
        assertTrue(result.newMemberEmails.contains("sowmya.ram@gmail.com"));
        assertTrue(result.newMemberEmails.contains("rthilakan@gmail.com"));
        assertTrue(result.newMemberEmails.contains("syamala@gmail.com"));

        assertEquals(5, result.addedToListEmails.size());
        //except sathya all others should be added to list. sathya is already present in list
        assertTrue(! result.addedToListEmails.contains(sathya.contact.email));

        assertEquals(1, result.alreadyUnsubscribedToGroupEmails.size());
        assertTrue(result.alreadyUnsubscribedToGroupEmails.contains(murugavel.contact.email));

        MemberQueryCondition mqc = new MemberQueryCondition(client, 1000);
        mqc.subscribedGroupIds.add(sgp.groupId);

        java.util.List<MemberProp> memberProps = MemberLoader.queryProps(mqc, validUser.email);
        assertEquals(5, memberProps.size());

        Map<String, MemberProp> emailVsMemberProp = new HashMap<>();
        for (MemberProp memberProp : memberProps) {
            emailVsMemberProp.put(memberProp.contact.email, memberProp);
            assertTrue(memberProp.listIds.contains(shambhaviSgpList.listId));
            assertTrue(memberProp.practiceIds.contains(shambhavi.practiceId));
        }
        assertEquals(5, emailVsMemberProp.size());

        assertTrue(emailVsMemberProp.containsKey(sathya.contact.email));
        assertTrue(emailVsMemberProp.containsKey(sharmila.contact.email));
        assertTrue(emailVsMemberProp.containsKey("sowmya.ram@gmail.com"));
        assertTrue(emailVsMemberProp.containsKey("rthilakan@gmail.com"));
        assertTrue(emailVsMemberProp.containsKey("syamala@gmail.com"));

        mqc = new MemberQueryCondition(client, 1000);
        mqc.unsubscribedGroupIds.add(sgp.groupId);
        memberProps = MemberLoader.queryProps(mqc, validUser.email);
        assertEquals(1, memberProps.size());
    }

    @Test
    public void worksWithDuplicateEmails() {
        // 4 existing members m1, m2, m3, m4. m1, m2 and m3 have the same email.
        // add emails m1 list

        // m1
        ContactProp c = new ContactProp();
        c.firstName = "first_m1";
        c.lastName = "last_m1";
        c.email = "m1@dummytest.com";
        c.asOfyyyymmdd = 20150117;

        Member.create(client, sgp.groupId, c, true, User.SUPER_USER);

        // m2
        c.firstName = "first_m2";
        c.lastName = "last_m2";
        Member.create(client, sgp.groupId, c, true, User.SUPER_USER);

        // m3
        c.firstName = "first_m3";
        c.lastName = "last_m3";
        Member.create(client, sgp.groupId, c, true, User.SUPER_USER);

        // m4
        c.firstName = "first_m4";
        c.lastName = "last_m4";
        c.email = "m4@dummytest.com";
        Member.create(client, sgp.groupId, c, true, User.SUPER_USER);

        MailMap mailMap = new MailMap();
        mailMap.add("m1@dummytest.com", "first_m1", "last_m1");

        BulkSubscriptionResultProp result =
                Member.bulkSubscribeList(client, shambhaviSgpList.listId, mailMap, User.SUPER_USER);

        assertEquals(1, result.existingMemberEmails.size());
        assertTrue(result.existingMemberEmails.contains("m1@dummytest.com"));

        assertTrue(result.newMemberEmails.isEmpty());

        assertEquals(1, result.addedToListEmails.size());
        assertTrue(result.addedToListEmails.contains("m1@dummytest.com"));

        assertEquals(1, result.addedToGroupSubscriptionEmails.size());
        assertTrue(result.addedToGroupSubscriptionEmails.contains("m1@dummytest.com"));

        assertTrue(result.alreadySubscribedToGroupEmails.isEmpty());
        assertTrue(result.alreadyUnsubscribedToGroupEmails.isEmpty());
    }

    @Test
    public void cannotBulkSubscribeToDisabledList() {
        List.disable(client, shambhaviSgpList.listId, User.SUPER_USER);

        MailMap mailMap = new MailMap();
        mailMap.add("Syamala@gMail.com", "Syamala", "Thilakan");

        try {
            Member.bulkSubscribeList(client, shambhaviSgpList.listId, mailMap, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_PRECONDITION_FAILED, ex.statusCode);
        }
    }

    @Test
    public void unsubscribedEmailsNotAddedBack() {
        // m1
        ContactProp c = new ContactProp();
        c.firstName = "first_m1";
        c.lastName = "last_m1";
        c.email = "m1@dummytest.com";
        c.asOfyyyymmdd = 20150117;

        MemberProp m1 = Member.create(client, sgp.groupId, c, true, User.SUPER_USER);
        //subscribe first and then unsubscribe
        Member.subscribeGroup(client, m1.memberId, sgp.groupId, User.SUPER_USER);
        Member.unsubscribeGroup(client, m1.memberId, sgp.groupId, User.SUPER_USER);

        m1 = MemberLoader.safeGet(client, m1.memberId, User.SUPER_USER).toProp();
        assertTrue(m1.unsubscribedGroupIds.contains(sgp.groupId));

        MailMap mailMap = new MailMap();
        mailMap.add("m1@dummytest.com", "first_m1", "last_m1");
        BulkSubscriptionResultProp result =
                Member.bulkSubscribeList(client, shambhaviSgpList.listId, mailMap, User.SUPER_USER);

        assertEquals(1, result.alreadyUnsubscribedToGroupEmails.size());
        assertTrue(result.alreadyUnsubscribedToGroupEmails.contains("m1@dummytest.com"));

        m1 = MemberLoader.safeGet(client, m1.memberId, User.SUPER_USER).toProp();
        assertTrue(m1.listIds.contains(shambhaviSgpList.listId));
        assertTrue(m1.unsubscribedGroupIds.contains(sgp.groupId));
    }
}
