package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.inventory.InventoryItemType;
import crmdna.inventory.InventoryItemTypeProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(name = "inventory")
public class InventoryItemTypeApi {
    @ApiMethod(path = "createInventoryItemType", httpMethod = HttpMethod.POST)
    public APIResponse createInventoryItemType(@Named("client") String client,
                                               @Named("displayName") String displayName,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);
            InventoryItemTypeProp prop = InventoryItemType.create(client, displayName, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getAllInventoryItemTypes", httpMethod = HttpMethod.GET)
    public APIResponse getAllInventoryItemTypes(@Named("client") String client,
                                                @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        if (client == null)
            client = "isha";

        try {
            Client.ensureValid(client);
            List<InventoryItemTypeProp> props = InventoryItemType.getAll(client);

            return new APIResponse().status(Status.SUCCESS).object(props);
        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(null));
        }
    }

    @ApiMethod(path = "renameInventoryItemType", httpMethod = HttpMethod.GET)
    public APIResponse renameInventoryItemType(@Named("client") String client,
                                               @Named("departmentId") long inventoryItemTypeId,
                                               @Named("newDisplayName") String newDisplayName,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        try {
            Client.ensureValid(client);
            InventoryItemTypeProp prop =
                    InventoryItemType.rename(client, inventoryItemTypeId, newDisplayName,
                            Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object(prop);
        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(null));
        }
    }

    @ApiMethod(path = "deleteInventoryItemType", httpMethod = HttpMethod.GET)
    public APIResponse deleteInventoryItemType(@Named("client") String client,
                                               @Named("departmentId") long inventoryItemTypeId,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);
            InventoryItemType.delete(client, inventoryItemTypeId, login);

            return new APIResponse().status(Status.SUCCESS).object(
                    "Department [" + inventoryItemTypeId + "] deleted");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
