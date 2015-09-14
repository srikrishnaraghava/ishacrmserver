package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.attendance.AttendanceFactory;
import crmdna.attendance.CheckInMemberProp;
import crmdna.attendance.IAttendance;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.List;

@Api(name = "attendance", clientIds = {"220866004543.apps.googleusercontent.com",
        com.google.api.server.spi.Constant.API_EXPLORER_CLIENT_ID})
public class AttendanceApi {
    @ApiMethod(path = "checkin", httpMethod = HttpMethod.POST)
    public APIResponse checkin(@Named("client") String client, @Named("memberId") long memberId,
                               @Named("programId") long programId, @Named("sessionDateYYYYMMDD") int sessionDateYYYYMMDD,
                               @Named("batchNo") int batchNo, @Nullable @Named("showStackTrace") Boolean showStackTrace,
                               HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);

            IAttendance impl = AttendanceFactory.getImpl(client);
            int numCheckins = impl.checkin(memberId, programId, sessionDateYYYYMMDD, batchNo, login);

            APIResponse apiResponse = new APIResponse();
            apiResponse.statusCode = Status.SUCCESS;
            apiResponse.object = numCheckins;
            apiResponse.userFriendlyMessage = "Number of checkins: " + numCheckins;

            return apiResponse;
        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "checkout", httpMethod = HttpMethod.POST)
    public APIResponse checkout(@Named("client") String client,
                                @Named("memberId") long memberId, @Named("programId") long programId,
                                @Named("sessionDateYYYYMMDD") int sessionDateYYYYMMDD,
                                @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            IAttendance impl = AttendanceFactory.getImpl(client);
            int numCheckins = impl.checkout(memberId, programId, sessionDateYYYYMMDD, login);

            APIResponse apiResponse = new APIResponse();
            apiResponse.statusCode = Status.SUCCESS;
            apiResponse.object = numCheckins;
            apiResponse.userFriendlyMessage = "Number of checkins: " + numCheckins;

            return apiResponse;
        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }

    }

    @ApiMethod(path = "getNumCheckins", httpMethod = HttpMethod.GET)
    public APIResponse getNumCheckins(@Named("client") String client,
                                      @Named("programId") long programId, @Named("sessionDateYYYYMMDD") int sessionDateYYYYMMDD,
                                      @Named("batchNo") int batchNo, @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                      HttpServletRequest req) {

        if (client == null)
            client = "isha";

        try {

            IAttendance impl = AttendanceFactory.getImpl(client);

            int numCheckins = impl.getNumCheckins(programId, sessionDateYYYYMMDD, batchNo);

            APIResponse apiResponse = new APIResponse();
            apiResponse.statusCode = Status.SUCCESS;
            apiResponse.object = numCheckins;
            apiResponse.userFriendlyMessage = "Number of checkins: " + numCheckins;
            return apiResponse;

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }

    }

    @ApiMethod(path = "getMembersForCheckIn", httpMethod = HttpMethod.GET)
    public APIResponse getMembersForCheckIn(@Named("client") String client,
                                            @Named("searchStr") String searchStr, @Named("programId") long programId,
                                            @Named("sessionDateYYYYMMDD") int sessionDateYYYYMMDD,
                                            @Nullable @Named("maxResultsSizeDefault10") Integer maxResultSize,
                                            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        if (maxResultSize == null)
            maxResultSize = 10;

        try {

            login = Utils.getLoginEmail(user);
            IAttendance impl = AttendanceFactory.getImpl(client);

            List<CheckInMemberProp> checkInMemberProps =
                    impl.getMembersForCheckIn(searchStr, programId, sessionDateYYYYMMDD, maxResultSize, login);

            return new APIResponse().status(Status.SUCCESS).object(checkInMemberProps);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
