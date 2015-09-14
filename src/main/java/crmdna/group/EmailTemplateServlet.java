package crmdna.group;

import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.group.Group.EmailType;
import crmdna.group.Group.GroupProp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class EmailTemplateServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");

        String client = null;
        try {
            client = req.getParameter("client");

            String groupIdStr = req.getParameter("groupId");
            if (groupIdStr == null)
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "group Id not specified in query param");

            long groupId = Utils.safeParseAsLong(groupIdStr);

            String emailTypeStr = req.getParameter("emailType");
            if (emailTypeStr == null)
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "group Id not specified in query param");
            emailTypeStr = emailTypeStr.toUpperCase();

            EmailType emailType;
            if (emailTypeStr.equals("REGISTRATION_CONFIRMATION"))
                emailType = EmailType.REGISTRATION_CONFIRMATION;
            else if (emailTypeStr.equals("REGISTRATION_REMINDER"))
                emailType = EmailType.REGISTRATION_REMINDER;
            else {
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "Invalid emailType [" + emailTypeStr + "]");
            }

            String template = Group.getEmailTemplate(client, groupId, emailType);

            if (template == null) {
                GroupProp groupProp = Group.safeGet(client, groupId).toProp();
                resp.getWriter().println(
                        " No template is set for group [" + groupProp.displayName + "], emailType ["
                                + emailType + "]");
            } else
                resp.getWriter().println(template);

        } catch (Exception ex) {
            APIResponse apiResponse =
                    APIUtils.toAPIResponse(ex, true, new RequestInfo().client(client).req(req));
            String errMessage =
                    "An error occurred. Please try again.\n\n" + "Error code: " + apiResponse.statusCode
                            + "\nMessage: " + apiResponse.userFriendlyMessage + "\n\nStack trace: "
                            + apiResponse.object;
            Logger logger = Logger.getLogger(EmailTemplateServlet.class.getName());
            logger.warning(errMessage);
            resp.getWriter().println(errMessage);
        }
    }
}
