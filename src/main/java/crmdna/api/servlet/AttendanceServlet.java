package crmdna.api.servlet;

import crmdna.attendance.AttendanceFactory;
import crmdna.attendance.CheckInMemberProp;
import crmdna.attendance.IAttendance;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class AttendanceServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        // Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        String action = request.getParameter("action");
        if (action == null) {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else {

            String client = request.getParameter("client");
            if (client == null)
                client = "isha";

            String login = ServletUtils.getLogin(request);

            try {
                if (action.equals("checkin")) {

                    IAttendance impl = AttendanceFactory.getImpl(client);
                    int numCheckins =
                            impl.checkin(ServletUtils.getLongParam(request, "memberId"),
                                    ServletUtils.getLongParam(request, "programId"),
                                    ServletUtils.getIntParam(request, "sessionDateYYYYMMDD"),
                                    ServletUtils.getIntParam(request, "batchNo"), ServletUtils.getLogin(request));

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(numCheckins));

                } else if (action.equals("getNumCheckins")) {

                    IAttendance impl = AttendanceFactory.getImpl(client);
                    int numCheckins =
                            impl.getNumCheckins(ServletUtils.getLongParam(request, "programId"),
                                    ServletUtils.getIntParam(request, "sessionDateYYYYMMDD"),
                                    ServletUtils.getIntParam(request, "batchNo"));

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(numCheckins));

                } else if (action.equals("getMembersForCheckin")) {

                    IAttendance impl = AttendanceFactory.getImpl(client);

                    Integer maxResultSize = ServletUtils.getIntParam(request, "maxResultSize");
                    if (maxResultSize == null)
                        maxResultSize = 10;

                    List<CheckInMemberProp> checkInMemberProps =
                            impl.getMembersForCheckIn(request.getParameter("searchStr"),
                                    ServletUtils.getLongParam(request, "programId"),
                                    ServletUtils.getIntParam(request, "sessionDateYYYYMMDD"), maxResultSize, login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(checkInMemberProps));
                } else {
                    ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
                }

            } catch (Exception ex) {
                ServletUtils.setJson(response, APIUtils.toAPIResponse(ex, true, new RequestInfo().client(client).req(request).login(login)));
            }
        }
    }
}
