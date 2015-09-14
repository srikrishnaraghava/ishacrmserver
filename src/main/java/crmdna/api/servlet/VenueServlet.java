package crmdna.api.servlet;

import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.venue.Venue;
import crmdna.venue.Venue.VenueProp;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

public class VenueServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    private void create(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        VenueProp venueProp = Venue.create(client, ServletUtils.getStrParam(request, "displayName"),
            ServletUtils.getStrParam(request, "shortName"),
            ServletUtils.getStrParam(request, "address"),
            ServletUtils.getLongParam(request, "groupId"), login);

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(venueProp));
    }

    private void update(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws IOException {

        VenueProp venueProp = Venue.update(client, ServletUtils.getLongParam(request, "venueId"),
            ServletUtils.getStrParam(request, "displayName"),
            ServletUtils.getStrParam(request, "shortName"),
            ServletUtils.getStrParam(request, "address"),
            ServletUtils.getLongParam(request, "groupId"), login);

        ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(venueProp));
    }

    private void getAll(String client, String login, HttpServletRequest request,
        HttpServletResponse response) throws Exception {

        List<VenueProp> venueProps = Venue.getAllForGroup(client,
            ServletUtils.getLongParam(request, "groupId"));

        ServletUtils.setJson(response, new APIResponse().object(venueProps).status(Status.SUCCESS));
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
                switch (action) {
                    case "create":
                        create(client, login, request, response);
                        break;
                    case "update":
                        update(client, login, request, response);
                        break;
                    case "getAll":
                        getAll(client, login, request, response);
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
}
