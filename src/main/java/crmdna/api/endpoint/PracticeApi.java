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
import crmdna.practice.Practice;
import crmdna.practice.Practice.PracticeProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(name = "program")
public class PracticeApi {
    @ApiMethod(path = "createPractice", httpMethod = HttpMethod.POST)
    public APIResponse createPractice(@Named("client") String client,
                                      @Named("displayName") String displayName,
                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            PracticeProp prop = Practice.create(client, displayName, Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getAllPractices", httpMethod = HttpMethod.GET)
    public APIResponse getAllPractices(@Named("client") String client,
                                       @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        if (client == null)
            client = "isha";

        try {

            List<PracticeProp> props = Practice.getAll(client);

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "renamePractice", httpMethod = HttpMethod.GET)
    public APIResponse renamePractice(@Named("client") String client,
                                      @Named("practiceId") long practiceId, @Named("newDisplayName") String newDisplayName,
                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            PracticeProp prop = Practice.rename(client, practiceId, newDisplayName, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "deletePractice", httpMethod = HttpMethod.GET)
    public APIResponse deletePractice(@Named("client") String client,
                                      @Named("practiceId") long practiceId,
                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);
            Practice.delete(client, practiceId, login);

            return new APIResponse().status(Status.SUCCESS).object(
                    "Practice [" + practiceId + "] deleted");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
