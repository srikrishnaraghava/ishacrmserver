package crmdna.calling;


import java.util.Map;
import java.util.Set;

/**
 * Created by sathya on 24/7/15.
 */
public class CallAssignmentResult {
    Map<Long, Set<Long>> userIdVsMemberIds;
    Set<Long> unassignedMembers;

    boolean allMembersAssigned() {
       return (unassignedMembers == null) || unassignedMembers.isEmpty();
    }
}
