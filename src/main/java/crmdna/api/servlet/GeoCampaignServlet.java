package crmdna.api.servlet;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Enumeration;

public class GeoCampaignServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    static String getRedirect(String country) {

        String url;
        switch (country) {
            case "NZ":
            case "AU":
                url = "http://www.ishayoga.org.au";
                break;

            case "MY":
                url = "http://www.malaysia.ishayoga.org";
                break;

            default:
                url = "http://www.ishayoga.sg";
                break;
        }

        return url;
    }

    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException {

        @SuppressWarnings("unchecked")
        Enumeration<String> countryList = request.getHeaders("X-AppEngine-Country");
        String country = "SG";
        if ((countryList != null) && countryList.hasMoreElements()) {
            country = countryList.nextElement();
        }

        response.sendRedirect(getRedirect(country));
    }
}
