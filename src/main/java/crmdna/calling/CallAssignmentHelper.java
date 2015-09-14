package crmdna.calling;

import crmdna.interaction.InteractionScoreProp;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by sathya on 24/7/15.
 */
public class CallAssignmentHelper {

    public static CallAssignmentResult assignMembersBasedOnScore(List<InteractionScoreProp> scores,
                                                                 Map<Long, Integer> userIdVsMaxMembers) {

        if (userIdVsMaxMembers == null) {
            userIdVsMaxMembers = new HashMap<>();
        }

        //sort list in descending order of scores
        Collections.sort(scores, new Comparator<InteractionScoreProp>() {
            @Override
            public int compare(InteractionScoreProp o1, InteractionScoreProp o2) {
                return new Integer(o2.interactionScore).compareTo(new Integer(o1.interactionScore));
            }
        });

        //iterate through scores and get set of memberIds and userIds
        Set<Long> memberIds = new HashSet<>();
        Set<Long> userIds = new HashSet<>();

        for (InteractionScoreProp p : scores) {
            memberIds.add(p.memberId);
            userIds.add(p.userId);
        }

        Map<Long, Set<Long>> userIdVsMemberIds = new HashMap<>();

        for (InteractionScoreProp p : scores) {
            if (! memberIds.contains(p.memberId)) {
                //member is already assigned
                continue;
            }

            //member is not assigned. if user has space then assign
            if (userHasSpace(userIdVsMemberIds, p.userId, userIdVsMaxMembers)) {
                assignMemberToUser(userIdVsMemberIds, p.memberId, p.userId);
                memberIds.remove(p.memberId);
                if (memberIds.isEmpty()) {
                    break;
                }
            }
        }

        CallAssignmentResult result = new CallAssignmentResult();
        result.userIdVsMemberIds = userIdVsMemberIds;
        result.unassignedMembers = memberIds;

        return result;
    }

    static int getNumMembers(Map<Long, Set<Long>> userIdVsMembers, long userId) {
        Set<Long> set = userIdVsMembers.get(userId);
        return (set == null) ? 0 : set.size();
    }

    static void assignMemberToUser(Map<Long, Set<Long>> userIdVsMemberIds, long memberId, long userId) {
        Set<Long> existing = userIdVsMemberIds.get(userId);
        if (existing == null) {
            existing = new HashSet<>();
            userIdVsMemberIds.put(userId, existing);
        }

        existing.add(memberId);
    }

    static boolean userHasSpace(Map<Long, Set<Long>> userIdVsMembers, long userId, Map<Long, Integer> userIdVsMaxMembers) {
        Integer maxMembers = userIdVsMaxMembers.get(userId);
        return (maxMembers == null)
                || (getNumMembers(userIdVsMembers, userId) < maxMembers);
    }
}
