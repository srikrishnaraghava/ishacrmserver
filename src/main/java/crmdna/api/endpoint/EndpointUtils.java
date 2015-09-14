package crmdna.api.endpoint;

import crmdna.api.endpoint.ClientApi.ClientEnum;
import crmdna.api.endpoint.ListApi.ListEnum;
import crmdna.api.endpoint.ProgramIshaApi.GroupEnum;
import crmdna.common.Utils;
import crmdna.group.Group;
import crmdna.list.List;

import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.AssertUtils.ensureNotNullNotEmpty;

public class EndpointUtils {
    static String getClient(ClientEnum clientEnum, String clientOther) {
        ensureNotNull(clientEnum, "clientEnum is null");

        if (clientEnum != ClientEnum.OTHER)
            return Utils.removeSpaceUnderscoreBracketAndHyphen(clientEnum.toString().toLowerCase());

        ensureNotNullNotEmpty(clientOther, "clientOther should be specified with client is 'OTHER'");
        return clientOther;
    }

    static long getGroupId(String client, GroupEnum groupEnum, String groupOtherIdOrName) {
        ensureNotNullNotEmpty(client, "client is null or empty");
        ensureNotNull(groupEnum, "groupEnum is null");

        String groupIdOrName = groupOtherIdOrName;
        if (groupEnum != GroupEnum.OTHER) {
            groupIdOrName = groupEnum.toString();
        }

        groupIdOrName = Utils.removeSpaceUnderscoreBracketAndHyphen(groupIdOrName.toLowerCase());

        long groupId = Group.safeGetByIdOrName(client, groupIdOrName).toProp().groupId;

        return groupId;
    }

    static long getListId(String client, long groupId, ListEnum listEnum, String listOther) {
        ensureNotNullNotEmpty(client, "client is null or empty");


        ensureNotNull(listEnum, "listEnum is null");

        String listName = listOther;
        if (listEnum != ListEnum.OTHER) {
            listName = listEnum.toString();
        }

        listName = Utils.removeSpaceUnderscoreBracketAndHyphen(listName.toLowerCase());

        long listId = List.safeGetByGroupIdAndName(client, groupId, listName).toProp().listId;

        return listId;
    }
}
