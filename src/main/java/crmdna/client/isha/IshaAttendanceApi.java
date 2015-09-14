package crmdna.client.isha;

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

@Api(name = "isha")
public class IshaAttendanceApi {

    @ApiMethod(path = "getMembersForCheckIn", httpMethod = HttpMethod.GET)
    public APIResponse getMembersForCheckIn(@Named("searchStr") String searchStr,
                                            @Named("programId") long programId, @Named("sessionDateYYYYMMDD") int sessionDateYYYYMMDD,
                                            @Named("maxResultsSize") Integer maxResultsSize,
                                            @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;

        try {

            if (maxResultsSize == null)
                maxResultsSize = 25; // default value

            IAttendance impl = AttendanceFactory.getImpl("isha");
            login = Utils.getLoginEmail(user);

            List<CheckInMemberProp> checkInMemberProps =
                    impl.getMembersForCheckIn(searchStr, programId, sessionDateYYYYMMDD, 25, login);

            return new APIResponse().status(Status.SUCCESS).object(checkInMemberProps);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace,
                    new RequestInfo().client("isha").login(login).req(req));
        }
    }
}
