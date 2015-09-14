package crmdna.calling;

import crmdna.interaction.InteractionScoreProp;
import org.junit.Test;

import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Created by sathya on 25/7/15.
 */
public class CallAssignmentHelperTest {

    @Test
    public void withoutMaxLimit() {

        //members: 1, 2, 3, 4, ..., 100
        //users: 1, 2, 3, 4

        //random score: member id + user id

        final int NUM_MEMBERS = 100;
        Set<Long> memberIds = new HashSet<>();
        for (long i = 1; i <= NUM_MEMBERS; i++) {
            memberIds.add(i);
        }

        final int NUM_USERS = 4;
        Set<Long> userIds = new HashSet<>();
        for (long i = 1; i <= NUM_USERS; i++) {
            userIds.add(i);
        }

        List<InteractionScoreProp> scores = getUserIdPlusMemberIdScores(memberIds, userIds);

        CallAssignmentResult result = CallAssignmentHelper.assignMembersBasedOnScore(scores, null);
        assertTrue(result.allMembersAssigned());

        //all members should be assigned to user4
        assertEquals(1, result.userIdVsMemberIds.size());

        assertEquals(100, result.userIdVsMemberIds.get(4l).size());


        assertTrue(result.userIdVsMemberIds.get(4l).containsAll(memberIds));
    }

    @Test
    public void withMaxLimit() {

        //members: 1, 2, 3, 4, ..., 100
        //users: 1, 2, 3, 4

        //random score: member id + user id

        final int NUM_MEMBERS = 100;
        Set<Long> memberIds = new HashSet<>();
        for (long i = 1; i <= NUM_MEMBERS; i++) {
            memberIds.add(i);
        }

        final int NUM_USERS = 4;
        Set<Long> userIds = new HashSet<>();
        for (long i = 1; i <= NUM_USERS; i++) {
            userIds.add(i);
        }

        List<InteractionScoreProp> scores = getUserIdPlusMemberIdScores(memberIds, userIds);

        Map<Long, Integer> userIdVsMaxMembers = new HashMap<>();
        //set limit for each user as 30
        userIdVsMaxMembers.put(1l, 30);
        userIdVsMaxMembers.put(2l, 30);
        userIdVsMaxMembers.put(3l, 30);
        userIdVsMaxMembers.put(4l, 30);

        CallAssignmentResult result = CallAssignmentHelper.assignMembersBasedOnScore(scores, userIdVsMaxMembers);
        assertTrue(result.allMembersAssigned());

        //user4 should get members 71 to 100
        //user3 should get members 41 to 70
        //user2 should get members 11 to 40
        //user1 should get members 1 to 10

        assertEquals(4, result.userIdVsMemberIds.size());

        //user4
        memberIds = result.userIdVsMemberIds.get(4l);
        assertEquals(30, memberIds.size());
        for (long memberId = 71; memberId <= 100; memberId++) {
            assertTrue(memberIds.contains(memberId));
        }

        //user3
        memberIds = result.userIdVsMemberIds.get(3l);
        assertEquals(30, memberIds.size());
        for (long memberId = 41; memberId <= 70; memberId++) {
            assertTrue(memberIds.contains(memberId));
        }

        //user2
        memberIds = result.userIdVsMemberIds.get(2l);
        assertEquals(30, memberIds.size());
        for (long memberId = 11; memberId <= 40; memberId++) {
            assertTrue(memberIds.contains(memberId));
        }

        //user1
        memberIds = result.userIdVsMemberIds.get(1l);
        assertEquals(10, memberIds.size());
        for (long memberId = 1; memberId <= 10; memberId++) {
            assertTrue(memberIds.contains(memberId));
        }
    }

    @Test
    public void moreMembersThanSpaceAvailable() {

        //members: 1, 2, 3, 4, ..., 100
        //users: 1, 2, 3, 4

        //random score: member id + user id

        final int NUM_MEMBERS = 100;
        Set<Long> memberIds = new HashSet<>();
        for (long i = 1; i <= NUM_MEMBERS; i++) {
            memberIds.add(i);
        }

        final int NUM_USERS = 4;
        Set<Long> userIds = new HashSet<>();
        for (long i = 1; i <= NUM_USERS; i++) {
            userIds.add(i);
        }

        List<InteractionScoreProp> scores = getUserIdPlusMemberIdScores(memberIds, userIds);

        Map<Long, Integer> userIdVsMaxMembers = new HashMap<>();
        //set limit for each user as 10
        userIdVsMaxMembers.put(1l, 10);
        userIdVsMaxMembers.put(2l, 10);
        userIdVsMaxMembers.put(3l, 10);
        userIdVsMaxMembers.put(4l, 10);

        CallAssignmentResult result = CallAssignmentHelper.assignMembersBasedOnScore(scores, userIdVsMaxMembers);

        //user4 should get members 91 to 100
        //user3 should get members 81 to 90
        //user2 should get members 71 to 80
        //user1 should get members 61 to 70

        //members 1 to 60 will be unassigned

        assertTrue(! result.allMembersAssigned());

        assertEquals(4, result.userIdVsMemberIds.size());

        //user4
        memberIds = result.userIdVsMemberIds.get(4l);
        assertEquals(10, memberIds.size());
        for (long memberId = 91; memberId <= 100; memberId++) {
            assertTrue(memberIds.contains(memberId));
        }

        //user3
        memberIds = result.userIdVsMemberIds.get(3l);
        assertEquals(10, memberIds.size());
        for (long memberId = 81; memberId <= 90; memberId++) {
            assertTrue(memberIds.contains(memberId));
        }

        //user2
        memberIds = result.userIdVsMemberIds.get(2l);
        assertEquals(10, memberIds.size());
        for (long memberId = 71; memberId <= 80; memberId++) {
            assertTrue(memberIds.contains(memberId));
        }

        //user1
        memberIds = result.userIdVsMemberIds.get(1l);
        assertEquals(10, memberIds.size());
        for (long memberId = 61; memberId <= 70; memberId++) {
            assertTrue(memberIds.contains(memberId));
        }

        for (long memberId = 1; memberId <= 60; memberId++) {
            assertTrue(result.unassignedMembers.contains(memberId));
        }
        assertEquals(60, result.unassignedMembers.size());
    }

    private List<InteractionScoreProp> getUserIdPlusMemberIdScores(Set<Long> memberIds, Set<Long> userIds) {
        List<InteractionScoreProp> scores = new ArrayList<>();

        for (long memberId : memberIds) {
            for (long userId : userIds) {
                InteractionScoreProp score = new InteractionScoreProp();
                score.memberId = memberId;
                score.userId = userId;
                score.interactionScore = (int)(memberId + userId);
                scores.add(score);
            }
        }

        return scores;
    }
}
