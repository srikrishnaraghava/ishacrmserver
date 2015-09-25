package crmdna.api.servlet;

import crmdna.common.AssertUtils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.mail2.Mail;
import crmdna.mail2.MailContent;
import crmdna.mail2.MailContentProp;
import crmdna.mail2.MailStatsProp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

@SuppressWarnings("serial")
public class MailContentServlet extends HttpServlet {

    private void viewContent(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        Long mailContentId = ServletUtils.getLongParam(request, "mailContentId");
        AssertUtils.ensureNotNull(mailContentId, "MailContentId is invalid");
        MailContentProp mailContentProp = MailContent.safeGet(client, mailContentId).toProp();
        response.getWriter().println(mailContentProp.body);
    }

    private void viewStats(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        Long mailContentId = ServletUtils.getLongParam(request, "mailContentId");
        AssertUtils.ensureNotNull(mailContentId, "MailContentId is invalid");

        MailStatsProp mailStatsProp = Mail.getStatsByMailContent(client, mailContentId, login);

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(mailStatsProp));
    }

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
        throws IOException {

        String action = request.getParameter("action");
        if (action == null) {
            ServletUtils
                .setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else {

            String client = request.getParameter("client");
            if (client == null)
                client = "isha";

            String login = ServletUtils.getLogin(request);

            try {
                switch (action.toLowerCase()) {
                    case "viewcontent":
                        viewContent(client, login, request, response);
                        break;
                    case "viewstats":
                        viewStats(client, login, request, response);
                        break;
                    default:
                        ServletUtils.setJson(response,
                            new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
                }
            } catch (Exception ex) {
                ServletUtils.setJson(response, APIUtils.toAPIResponse(ex, true,
                    new RequestInfo().client(client).req(request).login(login)));
            }
        }
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response)
        throws IOException {
        doPost(request, response);
    }
}
