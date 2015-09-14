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
import crmdna.teacher.Teacher;
import crmdna.teacher.Teacher.TeacherProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(name = "program") public class TeacherApi {
    @ApiMethod(path = "createTeacher", httpMethod = HttpMethod.POST)
    public APIResponse createTeacher(@Named("client") String client,
        @Named("firstName") String firstName, @Named("lastName") String lastName,
        @Named("email") String email, @Named("groupId") long groupId,
        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req,
        User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);
            TeacherProp prop = Teacher.create(client, firstName, lastName, email, groupId, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace,
                new RequestInfo().client(client).req(req).login(login));
        }
    }

    @ApiMethod(path = "getAllTeachers", httpMethod = HttpMethod.GET)
    public APIResponse getAllTeachers(@Named("client") String client,
        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        if (client == null)
            client = "isha";

        try {
            Client.ensureValid(client);
            List<TeacherProp> props = Teacher.getAll(client);

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils
                .toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "updateTeacher", httpMethod = HttpMethod.GET)
    public APIResponse updateTeacher(@Named("client") String client,
        @Named("teacherId") long teacherId, @Nullable @Named("newEmail") String newEmail,
        @Nullable @Named("newGroupId") Long newGroupId,
        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req,
        User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            TeacherProp prop = Teacher.update(client, teacherId, newEmail, newGroupId, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils
                .toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "deleteTeacher", httpMethod = HttpMethod.GET)
    public APIResponse deleteTeacher(@Named("client") String client,
        @Named("teacherId") long teacherId,
        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req,
        User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);
            Teacher.delete(client, teacherId, login);

            return new APIResponse().status(Status.SUCCESS)
                .object("Venue [" + teacherId + "] deleted");

        } catch (Exception ex) {
            return APIUtils
                .toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }
}
