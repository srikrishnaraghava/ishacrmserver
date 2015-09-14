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
import crmdna.hr.Department;
import crmdna.hr.DepartmentProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(name = "department")
public class DepartmentApi {
    @ApiMethod(path = "create", httpMethod = HttpMethod.POST)
    public APIResponse create(@Named("client") String client,
                              @Named("displayName") String displayName,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);
            DepartmentProp prop = Department.create(client, displayName, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getAll", httpMethod = HttpMethod.GET)
    public APIResponse getAll(@Named("client") String client,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        if (client == null)
            client = "isha";

        try {
            Client.ensureValid(client);
            List<DepartmentProp> props = Department.getAll(client);

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(null));
        }
    }

    @ApiMethod(path = "update", httpMethod = HttpMethod.GET)
    public APIResponse update(@Named("client") String client,
                              @Named("departmentId") long departmentId, @Named("newDisplayName") String newDisplayName,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        try {
            Client.ensureValid(client);
            DepartmentProp prop =
                    Department.rename(client, departmentId, newDisplayName, Utils.getLoginEmail(user));

            return new APIResponse().status(Status.SUCCESS).object(prop);
        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(null));
        }
    }

    @ApiMethod(path = "delete", httpMethod = HttpMethod.GET)
    public APIResponse delete(@Named("client") String client,
                              @Named("departmentId") long departmentId,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);
            Department.delete(client, departmentId, login);

            return new APIResponse().status(Status.SUCCESS).object(
                    "Department [" + departmentId + "] deleted");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
