package crmdna.client.isha;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.program.Program;
import crmdna.program.SessionProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(name = "isha")
public class IshaProgramApi {

    @ApiMethod(path = "getOngoingSessions", httpMethod = HttpMethod.GET)
    public APIResponse getOngoingSessions(@Named("dateYYYYMMDD") int dateYYYYMMDD,
                                          @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        try {

            login = Utils.getLoginEmail(user);
            List<SessionProp> sessionProps = Program.getOngoingSessions("isha", dateYYYYMMDD, login);

            return new APIResponse().status(Status.SUCCESS).object(sessionProps);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client("isha").req(req)
                    .login(login));
        }
    }
}
