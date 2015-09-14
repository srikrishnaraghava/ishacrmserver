package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.api.endpoint.ClientApi.ClientEnum;
import crmdna.api.endpoint.ProgramIshaApi.GroupEnum;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.user.User.App;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.User.GroupLevelPrivilege;
import crmdna.user.UserProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(name = "user")
public class UserApi {
    @ApiMethod(path = "create", httpMethod = HttpMethod.POST)
    public APIResponse create(@Named("client") String client, @Named("email") String email,
                              @Named("group_id") long groupId, @Nullable @Named("showStackTrace") Boolean showStackTrace,
                              HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);
            UserProp prop = crmdna.user.User.create(client, email, groupId, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "addClientLevelPrivilege", httpMethod = HttpMethod.POST)
    public APIResponse addClientLevelPrivilege(@Named("client") ClientEnum clientEnum,
                                               @Named("email") String email, @Named("privilege") ClientLevelPrivilege privilege,
                                               @Nullable @Named("clientIfOther") String clientOther,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            UserProp prop = crmdna.user.User.addClientLevelPrivilege(client, email, privilege, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "deleteClientLevelPrivilege", httpMethod = HttpMethod.POST)
    public APIResponse deleteClientLevelPrivilege(@Named("client") ClientEnum clientEnum,
                                                  @Named("email") String email, @Named("privilege") ClientLevelPrivilege privilege,
                                                  @Nullable @Named("clientIfOther") String clientOther,
                                                  @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            UserProp prop = crmdna.user.User.deleteClientLevelPrivilege(client, email, privilege, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "addGroupLevelPrivilege", httpMethod = HttpMethod.POST)
    public APIResponse addGroupLevelPrivilege(@Named("client") ClientEnum clientEnum,
                                              @Named("email") String email, @Named("group") GroupEnum groupEnum,
                                              @Nullable @Named("groupOtherIdOrName") String groupOtherIdOrName,
                                              @Named("privilege") GroupLevelPrivilege privilege,
                                              @Nullable @Named("clientIfOther") String clientOther,
                                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);
        long groupId = EndpointUtils.getGroupId(client, groupEnum, groupOtherIdOrName);

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            UserProp prop =
                    crmdna.user.User.addGroupLevelPrivilege(client, groupId, email, privilege, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "deleteGroupLevelPrivilege", httpMethod = HttpMethod.POST)
    public APIResponse deleteGroupLevelPrivilege(@Named("client") ClientEnum clientEnum,
                                                 @Named("email") String email, @Named("group") GroupEnum groupEnum,
                                                 @Nullable @Named("groupOtherIdOrName") String groupOtherIdOrName,
                                                 @Named("privilege") GroupLevelPrivilege privilege,
                                                 @Nullable @Named("clientIfOther") String clientOther,
                                                 @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);
        long groupId = EndpointUtils.getGroupId(client, groupEnum, groupOtherIdOrName);

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            UserProp prop =
                    crmdna.user.User.deleteGroupLevelPrivilege(client, groupId, email, privilege, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "addApp", httpMethod = HttpMethod.POST)
    public APIResponse addApp(@Named("client") ClientEnum clientEnum,
                              @Named("email") String email, @Named("app") App app,
                              @Nullable @Named("clientIfOther") String clientOther,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            UserProp prop =
                    crmdna.user.User.addApp(client, email, app, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "removeApp", httpMethod = HttpMethod.POST)
    public APIResponse removeApp(@Named("client") ClientEnum clientEnum,
                                 @Named("email") String email, @Named("app") App app,
                                 @Nullable @Named("clientIfOther") String clientOther,
                                 @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = null;
        String login = null;

        try {

            client = EndpointUtils.getClient(clientEnum, clientOther);
            login = Utils.getLoginEmail(user);

            UserProp prop =
                    crmdna.user.User.removeApp(client, email, app, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    // TODO: remove this and add query method
    @ApiMethod(path = "getAll", httpMethod = HttpMethod.GET)
    public APIResponse getAll(@Named("client") ClientEnum clientEnum,
                              @Nullable @Named("clientIfOther") String clientOther,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {


        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {
            login = Utils.getLoginEmail(user);
            List<UserProp> props = crmdna.user.User.getAll(client, login);

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getLoggedInUser", httpMethod = HttpMethod.GET)
    public APIResponse getLoggedInUser(@Nullable @Named("showStackTrace") Boolean showStackTrace,
                                       HttpServletRequest req, User user) {

        String login = null;

        try {
            login = Utils.getLoginEmail(user);
            Utils.getLoginEmail(user);

            if (null == login)
                login = "not logged in";

            return new APIResponse().status(Status.SUCCESS).object(login);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login));
        }
    }

    @ApiMethod(path = "clonePrivileges", httpMethod = HttpMethod.POST)
    public APIResponse clonePrivileges(@Named("client") ClientEnum clientEnum,
                                       @Nullable @Named("clientIfOther") String clientOther,
                                       @Named("sourceEmail") String sourceEmail, @Named("targetEmail") String targetEmail,
                                       @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = EndpointUtils.getClient(clientEnum, clientOther);

        String login = null;

        try {
            login = Utils.getLoginEmail(user);
            crmdna.user.User.clonePrivileges(client, sourceEmail, targetEmail, login);

            UserProp afterCloning = crmdna.user.User.get(client, targetEmail).toProp(client);

            return new APIResponse().status(Status.SUCCESS).object(afterCloning);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login));
        }
    }
}
