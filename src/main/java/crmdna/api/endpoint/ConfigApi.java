package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.common.config.ConfigCRMDNA;
import crmdna.common.config.ConfigCRMDNAProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

@Api(name = "config")
public class ConfigApi {

    @ApiMethod(path = "getCRMDNAConfig", httpMethod = HttpMethod.GET)
    public APIResponse getCRMDNAConfig(@Nullable @Named("showStackTrace") Boolean showStackTrace,
                                       HttpServletRequest req) {
        try {

            ConfigCRMDNAProp prop = ConfigCRMDNA.get().toProp();

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req));
        }
    }

    @ApiMethod(path = "setCRMDNAConfig", httpMethod = HttpMethod.POST)
    public APIResponse setCRMDNAConfig(@Nullable @Named("fromEmailAddress") String fromEmail,
                                       @Nullable @Named("devMode") Boolean devMode,
                                       @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        try {

            login = Utils.getLoginEmail(user);
            ConfigCRMDNAProp prop = ConfigCRMDNA.set(fromEmail, devMode, Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client("CRMDNA").req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "addOrDeleteCRMDNADevTeamMember", httpMethod = HttpMethod.POST)
    public APIResponse addOrDeleteCRMDNADevTeamMember(
            @Nullable @Named("devTeamMemberEmail") String email, @Named("add") boolean add,
            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        try {

            ConfigCRMDNAProp prop =
                    ConfigCRMDNA.addOrDeleteDevTeamMember(email, add, Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req).login(login));
        }
    }

    @ApiMethod(path = "getCRMDNADevTeamMembers", httpMethod = HttpMethod.GET)
    public APIResponse getCRMDNADevTeamMembers(
            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        try {

            ConfigCRMDNAProp prop = ConfigCRMDNA.get().toProp();

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().req(req));
        }
    }
}
