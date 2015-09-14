package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.api.endpoint.ProgramIshaApi.IshaProgramType;
import crmdna.common.DateUtils;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.member.Member;
import crmdna.member.UnverifiedProgramProp;
import crmdna.programtype.ProgramType;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

import static crmdna.common.AssertUtils.ensure;

@Api(name = "member")
public class MemberIshaApi {
    @ApiMethod(path = "addUnverifiedProgram", httpMethod = HttpMethod.POST)
    public APIResponse addUnverifiedProgram(@Named("memberId") long memberId,
                                            @Nullable @Named("programTypeDropDown") IshaProgramType programTypeEnum,
                                            @Nullable @Named("programTypeIdOrName") String programTypeIdOrName,
                                            @Named("month") DateUtils.Month month, @Named("year") int year, @Named("city") String city,
                                            @Nullable @Named("country") String country, @Nullable @Named("teacher") String teacher,
                                            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            ensure((programTypeEnum != null) ^ (programTypeIdOrName != null),
                    "Either programTypeDropDown or programTypeIdOrName (Exactly 1) should be specified");

            if (programTypeEnum != null)
                programTypeIdOrName = programTypeEnum.toString();

            long programTypeId =
                    ProgramType.safeGetByIdOrName(client, programTypeIdOrName).toProp(client).programTypeId;

            List<UnverifiedProgramProp> list =
                    Member.addUnverifiedProgram(client, memberId, programTypeId, month, year, city, country,
                            teacher, login);

            return new APIResponse().status(Status.SUCCESS).object(list);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "deleteUnverifiedProgram", httpMethod = HttpMethod.POST)
    public APIResponse deleteUnverifiedProgram(@Named("memberId") long memberId,
                                               @Named("unverifiedProgramId") int unverifiedProgramId,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String client = "isha";

        String login = null;

        try {
            login = Utils.getLoginEmail(user);

            List<UnverifiedProgramProp> list =
                    Member.deleteUnverifiedProgram(client, memberId, unverifiedProgramId, login);

            return new APIResponse().status(Status.SUCCESS).object(list);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
