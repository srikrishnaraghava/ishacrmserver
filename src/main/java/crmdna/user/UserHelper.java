package crmdna.user;

import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.user.User.GroupLevelPrivilege;

import java.util.HashSet;
import java.util.Set;

import static crmdna.common.AssertUtils.ensureNotNull;

public class UserHelper {
    public static Set<Long> getGroupIdsWithPrivilage(String client, String email,
                                                     GroupLevelPrivilege privilege) {

        Client.ensureValid(client);
        UserEntity userEntity = User.safeGet(client, email);
        ensureNotNull(privilege, "privilege is null");

        Set<Long> groupIds = new HashSet<>();
        for (String rawPrivilege : userEntity.privileges) {
            String[] split = rawPrivilege.split("\\|\\|");

            if (split.length != 3)
                continue;

            String resourceType = split[0];
            String resource = split[1];
            String action = split[2];

            if (resourceType.equalsIgnoreCase("GROUP") && action.equalsIgnoreCase(privilege.toString())) {
                if (Utils.canParseAsLong(resource))
                    groupIds.add(Utils.safeParseAsLong(resource));
            }
        }

        return groupIds;
    }
}
