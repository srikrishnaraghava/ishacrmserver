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
import crmdna.programtype.ProgramType;
import crmdna.programtype.ProgramTypeProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Set;

import static crmdna.common.AssertUtils.ensure;

@Api(name = "program")
public class ProgramTypeApi {
    @ApiMethod(path = "createProgramType", httpMethod = HttpMethod.POST)
    public APIResponse createProgramType(@Named("client") String client,
                                         @Named("displayName") String displayName,
                                         @Nullable @Named("practiseIds") Set<Long> practiseIds,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            ProgramTypeProp prop = ProgramType.create(client, displayName, practiseIds, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getAllProgramTypes", httpMethod = HttpMethod.GET)
    public APIResponse getAllProgramTypes(@Named("client") String client,
                                          @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        if (client == null)
            client = "isha";

        String login = null;

        try {

            List<ProgramTypeProp> props = ProgramType.getAll(client);

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "renameProgramType", httpMethod = HttpMethod.GET)
    public APIResponse renameProgramType(@Named("client") String client,
                                         @Named("programTypeId") long programTypeId, @Named("newDisplayName") String newDisplayName,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);
            ProgramTypeProp prop = ProgramType.rename(client, programTypeId, newDisplayName, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "updatePracticeIds", httpMethod = HttpMethod.GET)
    public APIResponse updatePracticeIds(@Named("client") String client,
                                         @Named("programTypeIdOrName") String programTypeIdOrName,
                                         @Nullable @Named("practiceIdsToBeAdded") List<Long> toAdd,
                                         @Nullable @Named("practiceIdsToBeDeleted") List<Long> toDelete,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            ensure((toAdd != null) && (toDelete != null), "Both toAdd and toDelete cannot be null");

            login = Utils.getLoginEmail(user);

            long programTypeId;
            if (Utils.canParseAsLong(programTypeIdOrName))
                programTypeId = Utils.safeParseAsLong(programTypeIdOrName);
            else
                programTypeId =
                        ProgramType.safeGetByIdOrName(client, programTypeIdOrName).toProp(client).programTypeId;

            Set<Long> practiceIds = ProgramType.getPracticeIds(client, programTypeId);
            if (toAdd != null)
                practiceIds.addAll(toAdd);

            if (toDelete != null)
                practiceIds.removeAll(toDelete);

            ProgramType.updatePracticeIds(client, programTypeId, practiceIds, login);

            return new APIResponse().status(Status.SUCCESS).object("All the affected members rebuilt");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "deleteProgramType", httpMethod = HttpMethod.GET)
    public APIResponse deleteProgramType(@Named("client") String client,
                                         @Named("programTypeId") long programTypeId,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            Client.ensureValid(client);

            login = Utils.getLoginEmail(user);
            ProgramType.delete(client, programTypeId, login);

            return new APIResponse().status(Status.SUCCESS).object(
                    "Program type [" + programTypeId + "] deleted");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
