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
import crmdna.inventory.InventoryLocation;
import crmdna.inventory.InventoryLocationProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

@Api(name = "inventory")
public class InventoryLocationApi {

    @ApiMethod(path = "createInventoryLocation", httpMethod = HttpMethod.POST)
    public APIResponse createInventoryLocation(@Named("client") String client,
                                               @Named("displayName") String displayName, @Named("address") String address,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);

            InventoryLocationProp prop = InventoryLocation.create(client, displayName, address, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

}
