package crmdna.api.servlet;

import crmdna.common.EmailConfig;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class GroupServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        String action = request.getParameter("action");
        String client = ServletUtils.getStrParam(request, "client");

        if (action == null) {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else if (action.equals("getAll")) {

            List<GroupProp> groupProps = Group.getAll(client, ServletUtils.getBoolParam(request, "populateLists"));
            ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(groupProps));

        } else if (action.equals("getEmailConfig")) {

            String group = ServletUtils.getStrParam(request, "group");
            long groupId = Group.safeGetByIdOrName(client, group).toProp().groupId;
            String login = request.getSession().getAttribute("login").toString();
            EmailConfig prop = Group.getEmailConfig(client, groupId, login);
            prop.mandrillApiKey = "x";
            ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(prop));
        } else {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
        }
    }
}
