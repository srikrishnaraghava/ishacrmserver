package crmdna.mail2;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.user.User;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class MailContentServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setContentType("text/html");

        String client = null;
        try {
            client = req.getParameter("client");

            String mailContentIdStr = req.getParameter("mailContentId");
            if (mailContentIdStr == null)
                throw new APIException().status(Status.ERROR_RESOURCE_INCORRECT).message(
                        "mail Content Id not specified in query param");

            long mailContentId = Utils.safeParseAsLong(mailContentIdStr);

            String action = req.getParameter("action");
            if (action == null)
                action = Action.VIEWCONTENT.toString();

            if (action.equalsIgnoreCase(Action.VIEWCONTENT.toString())) {
                String body = MailContent.safeGet(client, mailContentId).body;
                resp.getWriter().println(body);
                return;

            } else if (action.equalsIgnoreCase(Action.VIEWSTATS.toString())) {
                MailStatsProp mailStatsProp =
                        Mail.getStatsByMailContent(client, mailContentId, User.SUPER_USER);

                Gson gson = new GsonBuilder().setPrettyPrinting().create();
                resp.getWriter().println(gson.toJson(mailStatsProp));
                return;
            } else {
                String message =
                        "Invalid action [" + action
                                + "]. Action (case insensitive) should be either VIEWCONTENT or VIEWSTATS";
                resp.getWriter().println(message);
                return;
            }

        } catch (Exception ex) {
            APIResponse apiResponse =
                    APIUtils.toAPIResponse(ex, true, new RequestInfo().client(client).req(req));
            String errMessage =
                    "An error occurred. Please try again.\n\n" + "Error code: " + apiResponse.statusCode
                            + "\nMessage: " + apiResponse.userFriendlyMessage + "\n\nStack trace: "
                            + apiResponse.object;
            Logger logger = Logger.getLogger(MailContentServlet.class.getName());
            logger.warning(errMessage);
            resp.getWriter().println(errMessage);
        }
    }

    private enum Action {
        VIEWCONTENT, VIEWSTATS
    }
}
