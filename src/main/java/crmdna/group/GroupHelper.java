package crmdna.group;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class GroupHelper {
    public static void populateName(String client, Iterable<? extends IHasGroupIdsAndNames> iterable) {

        // do a batch get using ids and populate names

        Set<Long> groupIds = new HashSet<>();
        for (IHasGroupIdsAndNames element : iterable) {
            groupIds.addAll(element.getGroupIds());
        }

        Map<Long, GroupEntity> map = Group.getEntities(client, groupIds);

        for (IHasGroupIdsAndNames element : iterable) {

            Set<String> names = new HashSet<>();
            for (Long id : element.getGroupIds()) {

                if (!map.containsKey(id))
                    continue;

                names.add(map.get(id).displayName);
            }
            element.setGroupNames(names);
        }
    }
}
