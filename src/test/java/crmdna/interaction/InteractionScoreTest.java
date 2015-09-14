package crmdna.interaction;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.member.Member;
import crmdna.member.MemberProp;
import crmdna.user.User;
import crmdna.user.UserProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class InteractionScoreTest {

    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());

    private final String client = "isha";
    MemberProp member1, member2;
    private UserProp user1, user2;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        // can call getAll without any groups
        List<GroupProp> centers = Group.getAll(client, false);
        assertEquals(0, centers.size());

        GroupProp prop = Group.create(client, "Chennai", User.SUPER_USER);
        assertEquals(1, prop.groupId);

        user1 = User.create(client, "user1@valid.com", prop.groupId,
                User.SUPER_USER);
        assertEquals(1, User.get(client, user1.email).toProp(client).userId);

        user2 = User.create(client, "user2@valid.com", prop.groupId,
                User.SUPER_USER);
        assertEquals(2,
                User.get(client, user2.email).toProp(client).userId);

        ContactProp contact = new ContactProp();
        contact.email = "member1@gmail.com";
        contact.asOfyyyymmdd = 20150801;
        member1 = Member.create(client, prop.groupId, contact, false,
                User.SUPER_USER);

        contact = new ContactProp();
        contact.email = "member2@gmail.com";
        contact.asOfyyyymmdd = 20150801;
        member2 = Member.create(client, prop.groupId, contact, false,
                User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test
    public void incrementBy1Test() {
        //when there is no previous interaction
        InteractionScoreProp prop = InteractionScore.incrementBy1(client, user1.email, member1.memberId);
        assertEquals(1, prop.interactionScore);
        assertEquals(user1.userId, prop.userId);
        assertEquals(member1.memberId, prop.memberId);

        UserMemberProp userMemberProp = new UserMemberProp();
        userMemberProp.memberId = member1.memberId;
        userMemberProp.userId = user1.userId;
        List<InteractionScoreProp> props = InteractionScore.get(client, Utils.getList(userMemberProp));
        assertEquals(1, props.size());
        assertEquals(1, props.get(0).interactionScore);
        assertEquals(member1.memberId, props.get(0).memberId);
        assertEquals(user1.userId, props.get(0).userId);

        //again increment
        prop = InteractionScore.incrementBy1(client, user1.email, member1.memberId);
        assertEquals(2, prop.interactionScore);
        assertEquals(user1.userId, prop.userId);
        assertEquals(member1.memberId, prop.memberId);

        props = InteractionScore.get(client, Utils.getList(userMemberProp));
        assertEquals(1, props.size());
        assertEquals(2, props.get(0).interactionScore);
        assertEquals(member1.memberId, props.get(0).memberId);
        assertEquals(user1.userId, props.get(0).userId);
    }

    @Test
    public void getTest() {
        InteractionScore.incrementBy1(client, user1.email, member1.memberId);
        InteractionScore.incrementBy1(client, user1.email, member1.memberId);

        List<UserMemberProp> userMemberProps = new ArrayList<>();
        UserMemberProp userMemberProp = new UserMemberProp();
        userMemberProp.memberId = member1.memberId;
        userMemberProp.userId = user1.userId;
        userMemberProps.add(userMemberProp);

        userMemberProp = new UserMemberProp();
        userMemberProp.memberId = member2.memberId;
        userMemberProp.userId = user2.userId;
        userMemberProps.add(userMemberProp);

        List<InteractionScoreProp> props = InteractionScore.get(client, userMemberProps);
        assertEquals(2, props.size());
        assertEquals(2, props.get(0).interactionScore);
        assertEquals(user1.userId, props.get(0).userId);
        assertEquals(member1.memberId, props.get(0).memberId);

        assertEquals(0, props.get(1).interactionScore);
        assertEquals(user2.userId, props.get(1).userId);
        assertEquals(member2.memberId, props.get(1).memberId);
    }

    @Test
    public void queryTest() {

        //when no interactions present
        InteractionScoreQueryCondition qc = new InteractionScoreQueryCondition();

        List<InteractionScoreProp> props = InteractionScore.query(client, qc, 100);
        assertTrue(props.isEmpty());

        qc.memberIds.add(member1.memberId);
        qc.memberIds.add(member2.memberId);
        qc.userIds.add(user1.userId);
        qc.userIds.add(user2.userId);

        props = InteractionScore.query(client, qc, 100);
        assertTrue(props.isEmpty());

        //set up scores like this
        //user1, member1: 0
        //user1, member2: 1
        //user2, member1: 2
        //user2, member2: 3

        //user1, member2
        InteractionScore.incrementBy1(client, user1.email, member2.memberId);

        //user2, member1
        InteractionScore.incrementBy1(client, user2.email, member1.memberId);
        InteractionScore.incrementBy1(client, user2.email, member1.memberId);

        //user2, member2
        InteractionScore.incrementBy1(client, user2.email, member2.memberId);
        InteractionScore.incrementBy1(client, user2.email, member2.memberId);
        InteractionScore.incrementBy1(client, user2.email, member2.memberId);

        props = InteractionScore.query(client, qc, 100);
        assertEquals(3, props.size());

        //should be sorted in desc by scores
        assertEquals(3, props.get(0).interactionScore);
        assertEquals(user2.userId, props.get(0).userId);
        assertEquals(member2.memberId, props.get(0).memberId);

        assertEquals(2, props.get(1).interactionScore);
        assertEquals(user2.userId, props.get(1).userId);
        assertEquals(member1.memberId, props.get(1).memberId);

        assertEquals(1, props.get(2).interactionScore);
        assertEquals(user1.userId, props.get(2).userId);
        assertEquals(member2.memberId, props.get(2).memberId);
    }
}
