package crmdna.registration;

import crmdna.common.api.APIResponse;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class RegistrationResult extends HttpServlet {

    public void doOptions(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        resp.setHeader("Access-Control-Allow-Origin", "*");
        resp.setHeader("Access-Control-Allow-Methods", "POST");
        resp.setHeader("Access-Control-Allow-Headers", "Content-Type");
        resp.setHeader("Access-Control-Max-Age", "86400");
    }

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        try {

            RegistrationResultProp prop = new RegistrationResultProp();
            prop.amount = req.getParameter("amount");
            prop.ccy = req.getParameter("ccy");
            prop.transactionId = req.getParameter("transactionId");
            prop.status = req.getParameter("status");

            String html = getHtml(prop);
            resp.getWriter().println(html);

        } catch (Exception ex) {
            APIResponse apiResponse =
                    APIUtils.toAPIResponse(ex, true, new RequestInfo().req(req).client("not available"));
            String errMessage =
                    "An error occurred. Please try again.\n\n" + "Error code: " + apiResponse.statusCode
                            + "\nMessage: " + apiResponse.userFriendlyMessage + "\n\nStack trace: "
                            + apiResponse.object;
            Logger logger = Logger.getLogger(RegistrationResult.class.getName());
            logger.warning(errMessage);
            resp.getWriter().println(errMessage);
        }
    }

    private String getHtml(RegistrationResultProp prop) {
        StringBuilder builder = new StringBuilder();

        builder.append("<h1>Registration Result</h1>");
        builder.append("<br><b>Status:</b> ");
        builder.append(prop.status);
        builder.append("<br><b>Transaction Id:</b> ");
        builder.append(prop.transactionId);
        builder.append("<br><b>Amount Paid:</b> ");
        builder.append(prop.amount + " " + prop.ccy);

        return builder.toString();
    }

    private static class RegistrationResultProp {
        String amount;
        String ccy;
        String transactionId;
        String status;
    }
}
