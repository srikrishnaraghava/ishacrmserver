package crmdna.api.servlet;

import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.group.Group;
import crmdna.member.MemberEntity;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.sessionpass.SessionPass;
import crmdna.sessionpass.SessionPassProp;
import crmdna.user.User;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import static crmdna.common.AssertUtils.ensureNotNull;

public class SessionPassServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String action = request.getParameter("action");
        if (action == null) {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else {

            String client = request.getParameter("client");
            if (client == null)
                client = "isha";

            String login = ServletUtils.getLogin(request);

            try {
                if (action.equals("buySubscription")) {

                    Group.GroupProp groupProp =
                        Group.safeGetByIdOrName(client, ServletUtils.getStrParam(request,
                            "group")).toProp();

                    Calendar expiry = Calendar.getInstance();
                    expiry.add(Calendar.MONTH, ServletUtils.getIntParam(request, "expiryInMonths"));

                    MemberEntity memberEntity = MemberLoader.getByEmail(client, login);
                    ensureNotNull(memberEntity);

                    String paymentUrl = SessionPass.buySubscription(client, groupProp.groupId,
                        memberEntity.toProp(),
                                    ServletUtils.getIntParam(request, "numSessions"), expiry.getTime(),
                                    ServletUtils.getDoubleParam(request, "amount"), Currency.SGD,
                        Group.getPaypalApiCredentials(client, groupProp.groupId, User.SUPER_USER),
                                    request.getRequestURL().toString(),
                                    ServletUtils.getStrParam(request, "successUrl"),
                                    ServletUtils.getStrParam(request, "errorUrl"));

                    response.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
                    Utils.ensureValidUrl(paymentUrl);
                    response.setHeader("Location", paymentUrl);

                } else if (action.equals("getNumPasses")) {

                    MemberEntity memberEntity = MemberLoader.getByEmail(client, login);
                    ensureNotNull(memberEntity);

                    SessionPasses passes = new SessionPasses();
                    passes.email = login;
                    passes.id = memberEntity.getId();
                    passes.credits = SessionPass.getNumPasses(client, login, true);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(passes));

                } else if (action.equals("allocatePasses")) {

                    Calendar expiry = Calendar.getInstance();
                    expiry.add(Calendar.MONTH, ServletUtils.getIntParam(request, "expiryInMonths"));

                    long groupId = Group.safeGetByIdOrName(
                        client, ServletUtils.getStrParam(request, "group")).toProp().groupId;
                    SessionPass
                        .allocatePasses(client, groupId,
                            ServletUtils.getLongParam(request, "memberId"),
                            ServletUtils.getIntParam(request, "numSessions"), expiry.getTime(),
                            ServletUtils.getDoubleParam(request, "amount"), Currency.SGD,
                            ServletUtils.getStrParam(request, "transactionId"), login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

                } else if (action.equals("getValidPasses")) {

                    ServletUtils.setJson(response,
                            new APIResponse().status(Status.SUCCESS)
                                    .object(SessionPass.getValidPasses(client, login)));

                } else if (action.equals("bookSession")) {

                    int numPasses = SessionPass.getNumPasses(client, login, true);
                    SessionPass.bookSession(client, login, ServletUtils.getStrParam(request, "practiceType"),
                            ServletUtils.getStrParam(request, "sessionDateTime"));

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(numPasses - 1));

                } else if (action.equals("cancelBooking")) {

                    SessionPass.cancelBooking(client, ServletUtils.getLongParam(request, "sessionPassId"));

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

                } else if (action.equals("getSessionDetails")) {

                    String sessionDateTime = ServletUtils.getStrParam(request, "sessionDateTime");

                    SessionDetails sessionDetails = new SessionDetails();
                    sessionDetails.numRegistered = SessionPass.getNumRegistered(client, sessionDateTime);
                    sessionDetails.isRegistered = SessionPass.isRegistered(client, login, sessionDateTime);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(sessionDetails));

                } else if (action.equals("getRegistrations")) {

                    String sessionDateTime = ServletUtils.getStrParam(request, "sessionDateTime");

                    SessionDetails sessionDetails = new SessionDetails();
                    sessionDetails.numRegistered = SessionPass.getNumRegistered(client, sessionDateTime);
                    sessionDetails.isRegistered = SessionPass.isRegistered(client, login, sessionDateTime);

                    List<SessionPassProp> props =
                            SessionPass.getRegistrations(client, login, sessionDateTime);
                    List<SessionRegistration> registrations = new ArrayList<>();

                    for (SessionPassProp prop : props) {
                        MemberProp memberProp = MemberLoader.safeGet(client, prop.memberId, login).toProp();
                        SessionRegistration reg = new SessionRegistration();

                        reg.firstName = memberProp.contact.firstName;
                        reg.lastName = memberProp.contact.lastName;
                        reg.phoneNos = memberProp.contact.getPhoneNos();
                        reg.sessionPassId = prop.sessionPassId;
                        registrations.add(reg);
                    }

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(registrations));
                } else {
                    ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
                }

            } catch (Exception ex) {
                ServletUtils.setJson(response, APIUtils.toAPIResponse(ex, true, new RequestInfo().client(client).req(request).login(login)));
            }
        }
    }

    public class SessionPasses {
        String email;
        long id;
        long credits;
    }

    public class SessionDetails {
        boolean isRegistered;
        long numRegistered;
    }

    public class SessionRegistration {
        String firstName;
        String lastName;
        String phoneNos;
        long sessionPassId;
    }
}
