package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.client.Client;
import crmdna.client.ClientProp;
import crmdna.common.EmailConfig;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(name = "client")
public class ClientApi {

    @ApiMethod(path = "getall", httpMethod = HttpMethod.GET)
    public APIResponse getAll(@Nullable @Named("showStackTrace") Boolean showStackTrace,
                              HttpServletRequest req) {

        try {
            List<ClientProp> all = Client.getAll();

            return new APIResponse().status(Status.SUCCESS).object(all);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req));
        }
    }

    @ApiMethod(path = "create", httpMethod = HttpMethod.PUT)
    public APIResponse create(@Named("name") String name,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        try {
            ClientProp clientProp = Client.create(name);

            return new APIResponse().status(Status.SUCCESS).object(clientProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req));
        }
    }

    @ApiMethod(path = "updateDisplayName", httpMethod = HttpMethod.POST)
    public APIResponse updateDisplayName(@Named("name") String name,
                                         @Named("new_display_name") String newDisplayName,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        try {
            ClientProp clientProp = Client.updateDisplayName(name, newDisplayName);

            return new APIResponse().status(Status.SUCCESS).object(clientProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req));
        }
    }

    @ApiMethod(path = "setContactNameAndEmail", httpMethod = HttpMethod.POST)
    public APIResponse setContactNameAndEmail(@Named("client") String client,
                                              @Named("name") String name, @Named("email") String email,
                                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        try {
            login = Utils.getLoginEmail(user);
            ClientProp clientProp = Client.setContactNameAndEmail(client, email, name, login);

            return new APIResponse().status(Status.SUCCESS).object(clientProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req));
        }
    }

    @ApiMethod(path = "setMandrillApiKey", httpMethod = HttpMethod.POST)
    public APIResponse setMandrillApiKey(@Named("client") String client,
        @Named("mandrillApiKey") String apiKey,
        @Nullable @Named("showStackTrace") Boolean showStackTrace,
        HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = Utils.getLoginEmail(user);;

        try {

            EmailConfig prop = Client.setMandrillApiKey(client, apiKey, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                .login(login));
        }
    }

    public enum ClientEnum {
        ISHA, BHAIRAVI, OTHER
    }
}
