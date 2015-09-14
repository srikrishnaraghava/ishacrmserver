package crmdna.list;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.member.Member;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.member.MemberQueryCondition;
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;
import crmdna.user.User;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserProp;
import org.junit.Before;
import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.*;

public class ListTest {
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
        c.firstName = "Sathya";
        c.lastName = "Thilakan";
        sathya = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);

        c.email = "sharmila@gmail.com";
        sharmila = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);
        assertEquals("sharmila@gmail.com", sharmila.contact.email);

        c.email = "murugavel@gmail.com";
        murugavel = Member.create(client, sgp.groupId, c, false, User.SUPER_USER);
        assertEquals("murugavel@gmail.com", murugavel.contact.email);
    }

    @Test
    public void userWithPermissionCanCreateList() {
        ListProp listProp =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        userWithListPermissionForSgp.email);

        listProp = List.safeGet(client, listProp.listId).toProp();
        assertEquals(1, listProp.listId);
        assertEquals(sgp.groupId, listProp.groupId);
        assertEquals("Shambhavi", listProp.displayName);
        assertTrue(listProp.restricted);
        assertTrue(listProp.practiceIds.contains(shambhavi.practiceId));
        assertEquals(1, listProp.practiceIds.size());
        assertTrue(listProp.enabled);
    }

    @Test(expected = APIException.class)
    public void userWOPermissionCannotCreateList() {
        List.createPublic(client, sgp.groupId, "Newsletter", validUser.email);
        assertTrue(false);
    }

    @Test(expected = APIException.class)
    public void cannotCreateListForInvalidClient() {
        List.createRestricted("invalid", sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                User.SUPER_USER);
        assertTrue(false);
    }

    @Test(expected = APIException.class)
    public void cannotCreateListForInvalidGroup() {
        long invalidGroupId = 10093;
        List.createPublic(client, invalidGroupId, "Newsletter", User.SUPER_USER);
        assertTrue(false);
    }

    @Test
    public void cannotCreateDuplicateWithinSameGroup() {
        List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                userWithListPermissionForSgp.email);

        try {
            List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                    userWithListPermissionForSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        try {
            List.createRestricted(client, sgp.groupId, "shambhavi", Utils.getSet(shambhavi.practiceId),
                    userWithListPermissionForSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        try {
            List.createRestricted(client, sgp.groupId, "sham bhavi", Utils.getSet(shambhavi.practiceId),
                    userWithListPermissionForSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }

        try {
            List.createRestricted(client, sgp.groupId, "sham_bhavi", Utils.getSet(shambhavi.practiceId),
                    userWithListPermissionForSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }
    }

    @Test
    public void canCreateDuplicateInADifferentGroup() {
        ListProp listProp =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        User.SUPER_USER);
        assertEquals(1, listProp.listId);

        listProp =
                List.createRestricted(client, kl.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        User.SUPER_USER);
        assertEquals(2, listProp.listId);
    }

    @Test
    public void userWithPermissionCanRename() {
        ListProp listProp =
                List.createPublic(client, sgp.groupId, "Newsletter", userWithListPermissionForSgp.email);
        listProp = List.safeGet(client, listProp.listId).toProp();
        assertEquals("Newsletter", listProp.displayName);

        List.rename(client, listProp.listId, "Isha Singapore Newsletter",
                userWithListPermissionForSgp.email);

        listProp = List.safeGet(client, listProp.listId).toProp();
        assertEquals("Isha Singapore Newsletter", listProp.displayName);
    }

    @Test
    public void cannotRenameToExistingName() {
        ListProp listProp =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        userWithListPermissionForSgp.email);
        assertEquals(1, listProp.listId);

        listProp =
                List.createRestricted(client, sgp.groupId, "Volunteers",
                        Utils.getSet(shambhavi.practiceId), userWithListPermissionForSgp.email);
        assertEquals(2, listProp.listId);

        try {
            List.rename(client, 2, "sham_bhavi", userWithListPermissionForSgp.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }
    }

    @Test
    public void userWOPermissionCannotRename() {
        ListProp listProp =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        User.SUPER_USER);

        try {
            List.rename(client, listProp.listId, "Shambhavi Mahamudra", validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }
    }

    @Test
    public void userWithPermissionCanDisable() {
        ListProp listProp =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        User.SUPER_USER);

        List.disable(client, listProp.listId, userWithListPermissionForSgp.email);

        listProp = List.safeGet(client, listProp.listId).toProp();
        assertEquals(false, listProp.enabled);
    }

    @Test
    public void cannotAddMemberToDisabledList() {
        ListProp newsletter =
                List.createPublic(client, sgp.groupId, "Newsletter", userWithListPermissionForSgp.email);

        newsletter = List.disable(client, newsletter.listId, userWithListPermissionForSgp.email);
        assertEquals(false, newsletter.enabled);

        ContactProp c = new ContactProp();
        c.email = "sathya.t@ishafoundation.org";
        c.asOfyyyymmdd = 20141026;

        try {
            Member.addOrDeleteList(client, sathya.memberId, newsletter.listId, true, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_PRECONDITION_FAILED, ex.statusCode);
        }
    }

    @Test
    public void memberCanSelfAddToAPublicList() {
        ListProp newsletter =
                List.createPublic(client, sgp.groupId, "Isha Singapore Newsletter",
                        userWithListPermissionForSgp.email);

        Member.addOrDeleteList(client, sathya.memberId, newsletter.listId, true, User.SUPER_USER);

        sathya =
                MemberLoader.safeGet(client, sathya.memberId, userWithListPermissionForSgp.email).toProp();
        assertTrue(sathya.listIds.contains(newsletter.listId));
    }

    @Test
    public void memberCannotSelfAddToARestrictedList() {
        ListProp shambhaviList =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        userWithListPermissionForSgp.email);
        assertEquals(true, shambhaviList.restricted);

        try {
            Member.addOrDeleteList(client, sathya.memberId, shambhaviList.listId,
                    true, sathya.contact.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
        }
    }

    @Test
    public void memberCannotSubscribeSomeOneElse() {

        try {
            Member.subscribeGroup(client, sathya.memberId, sgp.groupId, sharmila.contact.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
        }
    }

    @Test
    public void memberCannotUnsubscribeSomeOneElse() {
        ListProp newsletter =
                List.createPublic(client, sgp.groupId, "Isha Singapore Newsletter",
                        userWithListPermissionForSgp.email);

        Member.subscribeGroup(client, sathya.memberId, sgp.groupId, User.SUPER_USER);

        // let sharmila unsubscribe sathya - should throw an exception
        try {
            Member.unsubscribeGroup(client, sathya.memberId, sgp.groupId, sharmila.contact.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
        }
    }

    @Test
    public void userWithPermissionCanDeleteListWithNoMembers() {
        ListProp shambhaviList =
                List.createRestricted(client, sgp.groupId, "Isha Singapore Newsletter",
                        Utils.getSet(shambhavi.practiceId), userWithListPermissionForSgp.email);

        shambhaviList = List.disable(client, shambhaviList.listId, userWithListPermissionForSgp.email);
        assertEquals(false, shambhaviList.enabled);

        List.delete(client, shambhaviList.listId, userWithListPermissionForSgp.email);

        try {
            List.safeGet(client, shambhaviList.listId);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_NOT_FOUND, ex.statusCode);
        }
    }

    @Test(expected = APIException.class)
    public void userWOPermissionCannotDeleteListWithNoMembers() {
        ListProp shambhaviList =
                List.createRestricted(client, sgp.groupId, "Isha Singapore Newsletter",
                        Utils.getSet(shambhavi.practiceId), userWithListPermissionForSgp.email);

        List.delete(client, shambhaviList.listId, validUser.email);
        assertTrue(false);
    }

    @Test
    public void cannotDeleteListWithSubscribedMembersEvenIfDisabled() {
        ListProp newsletter =
                List.createPublic(client, sgp.groupId, "Isha Singapore Newsletter",
                        userWithListPermissionForSgp.email);

        Member.addOrDeleteList(client, sathya.memberId, newsletter.listId, true, User.SUPER_USER);

        // disable the list
        List.disable(client, newsletter.listId, User.SUPER_USER);
        assertEquals(false, List.safeGet(client, newsletter.listId).toProp().enabled);

        try {
            List.delete(client, newsletter.listId, User.SUPER_USER);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_PRECONDITION_FAILED, ex.statusCode);
        }
    }

    @Test(expected = APIException.class)
    public void cannotDeleteEnabledList() {
        ListProp newsletter =
                List.createPublic(client, sgp.groupId, "Isha Singapore Newsletter",
                        userWithListPermissionForSgp.email);

        List.delete(client, newsletter.listId, User.SUPER_USER);
        assertTrue(false);
    }

    @Test
    public void canQueryByListId() {
        ListProp shambhaviList =
                List.createRestricted(client, sgp.groupId, "Isha Singapore Newsletter",
                        Utils.getSet(shambhavi.practiceId), userWithListPermissionForSgp.email);

        Member.addOrDeleteList(client, sathya.memberId, shambhaviList.listId, true, User.SUPER_USER);
        Member.addOrDeleteList(client, sharmila.memberId, shambhaviList.listId, true, User.SUPER_USER);

        MemberQueryCondition qc = new MemberQueryCondition(client, 1000);
        qc.listIds = Utils.getSet(shambhaviList.listId);

        java.util.List<MemberProp> memberProps = MemberLoader.queryProps(qc, validUser.email);
        assertEquals(2, memberProps.size());
        Set<Long> memberIds = new HashSet<>();
        for (MemberProp memberProp : memberProps) {
            memberIds.add(memberProp.memberId);
        }
        assertTrue(memberIds.contains(sathya.memberId));
        assertTrue(memberIds.contains(sharmila.memberId));
    }

    @Test
    public void memberTaggedWithPracticesFromListAfterAdd() {
        ListProp shambhaviList =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        User.SUPER_USER);

        Member.addOrDeleteList(client, sathya.memberId, shambhaviList.listId, true, User.SUPER_USER);

        sathya = MemberLoader.safeGet(client, sathya.memberId, sathya.contact.email).toProp();
        assertTrue(sathya.practiceIds.contains(shambhavi.practiceId));
    }

    @Test
    public void memberUntaggedWithPracticesFromListAfterDelete() {
        ListProp shambhaviList =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        User.SUPER_USER);

        Member.addOrDeleteList(client, sathya.memberId, shambhaviList.listId, true,
                userWithListPermissionForSgp.email);

        sathya = MemberLoader.safeGet(client, sathya.memberId, sathya.contact.email).toProp();
        assertTrue(sathya.practiceIds.contains(shambhavi.practiceId));

        Member.addOrDeleteList(client, sathya.memberId, shambhaviList.listId, false,
                userWithListPermissionForSgp.email);
        sathya = MemberLoader.safeGet(client, sathya.memberId, sathya.contact.email).toProp();
        assertTrue(!sathya.practiceIds.contains(shambhavi.practiceId));
    }

    @Test
    public void canQueryByGroup() {
        ListProp shambhaviSgp =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        User.SUPER_USER);

        // just create another one for kl
        List.createRestricted(client, kl.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                User.SUPER_USER);

        ListProp newsletterSgp = List.createPublic(client, sgp.groupId, "Newsletter", User.SUPER_USER);

        java.util.List<ListProp> listProps = List.querySortedProps(client, sgp.groupId);
        assertEquals(2, listProps.size());
        assertEquals(newsletterSgp.listId, listProps.get(0).listId);
        assertEquals(shambhaviSgp.listId, listProps.get(1).listId);
    }

    @Test
    public void canSafeGetByGroupIdAndName() {
        ListProp shambhaviSgp =
                List.createRestricted(client, sgp.groupId, "Shambhavi", Utils.getSet(shambhavi.practiceId),
                        User.SUPER_USER);

        ListEntity listEntity = List.safeGetByGroupIdAndName(client, sgp.groupId, "SHAMbhaVi");
        assertEquals(shambhaviSgp.listId, listEntity.listId);
    }

    @Test(expected = APIException.class)
    public void canSafeGetByGroupIdAndNameThrowsIfGroupInvalid() {
        long invalidGroupId = 10230;
        List.safeGetByGroupIdAndName(client, invalidGroupId, "SHAMbhaVi");
    }

    @Test(expected = APIException.class)
    public void canSafeGetByGroupIdAndNameThrowsIfListDoesNotExist() {
        List.safeGetByGroupIdAndName(client, sgp.groupId, "SHAMbhaVi");
    }


    @Test
    public void callGetAllLists() {
        ListProp shambhaviSgp =
                List.createRestricted(client, sgp.groupId, "Shambhavi SGP",
                        Utils.getSet(shambhavi.practiceId), User.SUPER_USER);

        ListProp shambhaviKl =
                List.createRestricted(client, kl.groupId, "Shambhavi KL",
                        Utils.getSet(shambhavi.practiceId), User.SUPER_USER);

        ListProp newsletterSgp = List.createPublic(client, sgp.groupId, "Newsletter", User.SUPER_USER);

        java.util.List<ListProp> listProps = List.querySortedProps(client, null);
        assertEquals(3, listProps.size());
        assertEquals(newsletterSgp.listId, listProps.get(0).listId);
        assertEquals(shambhaviKl.listId, listProps.get(1).listId);
        assertEquals(shambhaviSgp.listId, listProps.get(2).listId);
    }

    @Test
    public void subscribeUnsubcribeGroupReturnValueTest() {

        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertTrue(! sathya.subscribedGroupIds.contains(sgp.groupId));
        //sathya not originally subscribed to sgp

        boolean result = Member.subscribeGroup(client, sathya.memberId, sgp.groupId, User.SUPER_USER);
        //returns true if changed
        assertTrue(result);
        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertTrue(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertFalse(sathya.unsubscribedGroupIds.contains(sgp.groupId));

        result = Member.subscribeGroup(client, sathya.memberId, sgp.groupId, User.SUPER_USER);
        assertFalse(result);
        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertTrue(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertFalse(sathya.unsubscribedGroupIds.contains(sgp.groupId));

        result = Member.unsubscribeGroup(client, sathya.memberId, sgp.groupId, User.SUPER_USER);
        assertTrue(result);
        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertFalse(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertTrue(sathya.unsubscribedGroupIds.contains(sgp.groupId));
    }

    @Test
    public void selfCanSubscibeAndUnsubscribe() {
        Member.subscribeGroup(client, sathya.memberId, sgp.groupId, sathya.contact.email);
        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertTrue(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertFalse(sathya.unsubscribedGroupIds.contains(sgp.groupId));

        Member.unsubscribeGroup(client, sathya.memberId, sgp.groupId, sathya.contact.email);
        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertFalse(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertTrue(sathya.unsubscribedGroupIds.contains(sgp.groupId));

        //self can undo unsubscription
        Member.subscribeGroup(client, sathya.memberId, sgp.groupId, sathya.contact.email);
        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertTrue(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertFalse(sathya.unsubscribedGroupIds.contains(sgp.groupId));

        //assert sathya is not a user
        try {
            User.ensureValidUser(client, sathya.contact.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INVALID_USER, ex.statusCode);
        }
    }

    @Test
    public void userCanSubscibeAndUnsubscribe() {
        Member.subscribeGroup(client, sathya.memberId, sgp.groupId, validUser.email);
        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertTrue(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertFalse(sathya.unsubscribedGroupIds.contains(sgp.groupId));

        Member.unsubscribeGroup(client, sathya.memberId, sgp.groupId, validUser.email);
        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertFalse(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertTrue(sathya.unsubscribedGroupIds.contains(sgp.groupId));
    }

    @Test
    public void userNeedsPrivilageToUndoUnsubscription() {
        Member.unsubscribeGroup(client, sathya.memberId, sgp.groupId, validUser.email);

        try {
            Member.subscribeGroup(client, sathya.memberId, sgp.groupId, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }

        //now add privilege
        User.addClientLevelPrivilege(client, validUser.email,
                User.ClientLevelPrivilege.SUBSCRIBE_GROUP, User.SUPER_USER);

        Member.subscribeGroup(client, sathya.memberId, sgp.groupId, validUser.email);
        //no exception
        sathya = MemberLoader.safeGet(client, sathya.memberId, User.SUPER_USER).toProp();
        assertTrue(sathya.subscribedGroupIds.contains(sgp.groupId));
        assertFalse(sathya.unsubscribedGroupIds.contains(sgp.groupId));
    }
}
