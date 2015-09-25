package crmdna.user;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import com.googlecode.objectify.annotation.Index;
import crmdna.common.Utils;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TreeSet;

@Entity
@Cache
public class UserEntity {
    @Id
    public String email;

    @Index
    long userId;
    @Index
    long groupId;

    TreeSet<String> privileges = new TreeSet<>();

    // format is: <resource type>||<resourceid>||<action>

    // eg: group||1||read, group||2||write

    public UserProp toProp(String client) {

        UserProp userProp = new UserProp();
        userProp.email = email;
        userProp.userId = userId;
        userProp.groupId = groupId;

        Map<Long, TreeSet<String>> groupIdVsActions = new HashMap<>();

        TreeSet<String> apps = new TreeSet<>();
        for (String privilege : privileges) {
            String[] split = privilege.split("\\|\\|");

            if (split.length != 3)
                continue;

            String resourceType = split[0];
            String resource = split[1];
            String action = split[2];

            if (resourceType.equalsIgnoreCase("CLIENT"))
                userProp.clientLevelPrivileges.add(action);
            else if (resourceType.equalsIgnoreCase("GROUP")) {
                if (Utils.canParseAsLong(resource)) {
                    long groupId = Utils.safeParseAsLong(resource);
                    if (!groupIdVsActions.containsKey(groupId))
                        groupIdVsActions.put(groupId, new TreeSet<String>());

                    Set<String> set = groupIdVsActions.get(groupId);
                    set.add(action);
                }
            } else if (resourceType.equalsIgnoreCase("APP")) {
                apps.add(resource);
            }
        }

        userProp.apps = apps;

        Map<Long, GroupProp> groupIdVsProp = Group.get(client, groupIdVsActions.keySet());

        for (Entry<Long, TreeSet<String>> entry : groupIdVsActions.entrySet()) {
            long groupId = entry.getKey();

            if (groupIdVsProp.containsKey(groupId)) {
                String groupName = groupIdVsProp.get(groupId).displayName;

                userProp.groupLevelPrivileges.put(groupName, entry.getValue());
            }
        }

        userProp.groupName = Group.safeGet(client, userProp.groupId).toProp().displayName;

        return userProp;
    }
}
