package crmdna.api.servlet;

import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.registration.*;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class RegistrationServlet extends HttpServlet {

    static void registerForProgram(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        String client = null;
        try {

            client = ServletUtils.getStrParam(req, "client");

            RegistrationProp registrationProp =
                    Registration.register(client, ServletUtils.getStrParam(req, "firstName"),
                            ServletUtils.getStrParam(req, "lastName"), ServletUtils.getStrParam(req, "nickName"),
                            ServletUtils.getStrParam(req, "gender"), ServletUtils.getStrParam(req, "email"),
                            ServletUtils.getStrParam(req, "mobilePhone", true),
                            ServletUtils.getStrParam(req, "homePhone", true),
                            ServletUtils.getStrParam(req, "officePhone", true),
                            ServletUtils.getStrParam(req, "country"),
                            ServletUtils.getStrParam(req, "postalCode"),
                            ServletUtils.getStrParam(req, "programId"), ServletUtils.getStrParam(req, "fee"),
                            ServletUtils.getStrParam(req, "batchNo"),
                            ServletUtils.getStrListParam(req, "marketingChannel"),
                            ServletUtils.getStrParam(req, "successUrl"),
                            ServletUtils.getStrParam(req, "errorUrl"), req.getRequestURL().toString());

            switch (registrationProp.getStatus()) {
                case REGISTRATION_COMPLETE:
                    Map<String, Object> map = new TreeMap<>();

                    map.put("email", registrationProp.email);
                    map.put("timestamp", registrationProp.getStatusTimestamp());
                    map.put("firstName", registrationProp.firstName);
                    map.put("registrationId", registrationProp.registrationId + "");

                    if (registrationProp.lastName != null)
                        map.put("lastName", registrationProp.lastName);

                    if (registrationProp.transactionId != null)
                        map.put("transactionId", registrationProp.transactionId);

                    map.put("status", registrationProp.getStatus());
                    map.put("timestamp", registrationProp.getStatusTimestamp());

                    ProgramProp progamProp =
                            Program.safeGet(client, registrationProp.programId).toProp(client);
                    map.put("programName", progamProp.programTypeProp.displayName);

                    String redirectUrl;
                    if (registrationProp.alreadyRegistered) {
                        map.put("alreadyRegistered", true);
                        redirectUrl = Utils.getUrl(registrationProp.errorCallbackUrl, map);
                    } else {
                        redirectUrl = Utils.getUrl(registrationProp.successCallbackUrl, map);
                    }

                    resp.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
                    Utils.ensureValidUrl(redirectUrl);
                    Logger logger = Logger.getLogger(RegistrationServlet.class.getName());
                    logger.info("redirect URL [" + redirectUrl + "]");
                    resp.setHeader("Location", redirectUrl);
                    break;

                case PAYMENT_AUTHORIZATION_PENDING:
                    resp.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
                    Utils.ensureValidUrl(registrationProp.paymentUrl);
                    resp.setHeader("Location", registrationProp.paymentUrl);
                    break;

                default:
                    // should never come here
                    throw new APIException().status(Status.ERROR_INTERNAL).message(
                            "Invalid registration status [" + registrationProp.getStatus()
                                    + "] in RegistrationServlet after call to register.");
            }
        } catch (Exception ex) {
            APIResponse apiResponse =
                    APIUtils.toAPIResponse(ex, true, new RequestInfo().client(client).req(req));
            String errMessage =
                    "An error occurred. Please try again.\n\n" + "Error code: " + apiResponse.statusCode
                            + "\nMessage: " + apiResponse.userFriendlyMessage + "\n\nStack trace: "
                            + apiResponse.object;
            Logger logger = Logger.getLogger(RegistrationServlet.class.getName());
            logger.warning(errMessage);
            resp.getWriter().println(errMessage);
        }
    }

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
                if (action.equals("getSummary")) {
                    RegistrationSummaryProp registrationSummaryProp =
                            Registration.getSummary(client, ServletUtils.getLongParam(request, "programId"),
                                    login);

                    ServletUtils.setJson(response,
                            new APIResponse().status(Status.SUCCESS).object(registrationSummaryProp));
                } else if (action.equals("query")) {
                    RegistrationQueryCondition qc = new RegistrationQueryCondition();
                    qc.programId = ServletUtils.getLongParam(request, "programId");
                    qc.searchStr = ServletUtils.getStrParam(request, "searchStr");
                    List<RegistrationProp> props = Registration.query(client, qc, login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(props));

                } else if (action.equals("transfer")) {

                    Registration.transfer(client, ServletUtils.getLongParam(request, "registrationId"),
                            ServletUtils.getLongParam(request, "programId"), login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

                } else if (action.equals("overrideAsCompleted")) {

                    Registration.overrideAsCompleted(client,
                            ServletUtils.getLongParam(request, "registrationId"), login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

                } else if (action.equals("invalidate")) {

                    Registration.invalidate(client, ServletUtils.getLongParam(request, "registrationId"),
                            login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

                } else if (action.equals("cancel")) {

                    Registration.cancel(client, ServletUtils.getLongParam(request, "registrationId"), login);

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

                } else if (action.equals("registerForProgram")) {

                    registerForProgram(request, response);

                } else if (action.equals("applyDiscount")) {

                    DiscountProp prop = Discount.applyDiscount(client,
                            ServletUtils.getStrParam(request, "discountCode"),
                            ServletUtils.getLongParam(request, "programId"));

                    ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(prop));
                } else {
                    ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
                }
            } catch (Exception ex) {
                ServletUtils.setJson(
                        response,
                        APIUtils.toAPIResponse(ex, true,
                                new RequestInfo().client(client).req(request).login(login)));
            }
        }
    }
}
