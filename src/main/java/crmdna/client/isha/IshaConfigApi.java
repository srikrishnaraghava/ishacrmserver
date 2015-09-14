package crmdna.client.isha;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.client.isha.IshaConfig.IshaConfigProp;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.Set;

@Api(name = "isha")
public class IshaConfigApi {

    @ApiMethod(path = "get", httpMethod = HttpMethod.GET)
    public APIResponse get(@Nullable @Named("showStackTrace") Boolean showStackTrace,
                           HttpServletRequest req) {
        try {

            IshaConfigProp prop = IshaConfig.safeGet();
            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client("isha").req(req));
        }
    }

    @ApiMethod(path = "setSathsangPracticeIds", httpMethod = HttpMethod.POST)
    public APIResponse setSathsangPracticeIds(@Named("sathsang_practice_ids") Set<Long> practiceIds,
                                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        try {
            login = Utils.getLoginEmail(user);
            IshaConfigProp prop = IshaConfig.setSathsangPractices(practiceIds, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client("isha").req(req)
                    .login(login));
        }
    }
}
