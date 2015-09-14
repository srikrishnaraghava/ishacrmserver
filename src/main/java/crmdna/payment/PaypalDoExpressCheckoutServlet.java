package crmdna.payment;

import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.logging.Logger;

@SuppressWarnings("serial")
public class PaypalDoExpressCheckoutServlet extends HttpServlet {

    public void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        String token = req.getParameter("token");
        String payerId = req.getParameter("PayerID");
        String rootUrl = req.getServerName();
        TokenProp tokenProp = Token.safeGet(token);

        // TODO: handle exception in token prop

        try {

            String redirectUrl = Payment.doExpressCheckout(token, payerId, rootUrl);

            // Payment.doExpressCheckout should not throw any exception

            resp.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
            Utils.ensureValidUrl(redirectUrl);
            resp.setHeader("Location", redirectUrl);

        } catch (Exception ex) {
            // just in case
            APIResponse apiResponse =
                    APIUtils.toAPIResponse(ex, true, new RequestInfo().client("Not available").req(req));
            String errMessage =
                    "Error in Paypal DoExpressCheckout. Error code: " + apiResponse.statusCode
                            + "\nMessage: " + apiResponse.userFriendlyMessage + "\n\nStack trace: "
                            + apiResponse.object;
            Logger logger = Logger.getLogger(PaypalDoExpressCheckoutServlet.class.getName());
            logger.severe(errMessage);

            Utils.sendAlertEmailToDevTeam(tokenProp.client, ex, req, null);

            resp.setStatus(HttpServletResponse.SC_TEMPORARY_REDIRECT);
            resp.setHeader("Location", tokenProp.errorCallback);
        }
    }
}
