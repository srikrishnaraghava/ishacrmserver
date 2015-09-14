package crmdna.list;

import crmdna.client.Client;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import static crmdna.common.AssertUtils.ensureNotNull;

public class ListHelper {

    public static void populateGroupName(String client, java.util.List<ListProp> listProps) {
        Client.ensureValid(client);

        ensureNotNull(listProps, "listProps is null");

        Set<Long> groupIds = new HashSet<>();
        for (ListProp listProp : listProps) {
            groupIds.add(listProp.groupId);
        }

        // remove 0 just in case
        groupIds.remove(0);

        Map<Long, GroupProp> map = Group.get(client, groupIds);

        for (ListProp listProp : listProps) {
            long groupId = listProp.groupId;

            if (map.containsKey(groupId))
                listProp.groupName = map.get(groupId).displayName;
        }
    }
}
