package crmdna.payment;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Logger;

public class PaypalIpnServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {

        handleIpn(req);
    }

    private void handleIpn(HttpServletRequest req) {

        Logger logger = Logger.getLogger(Payment.class.getName());

        try {
            // 1. Read all posted req parameters
            String requestParams = this.getAllRequestParams(req);
            logger.info(requestParams);

            String charset = req.getParameter("charset");
            if (charset == null)
                charset = "UTF-8";

            String token = null;

            // 2. Prepare 'notify-validate' command with exactly the same parameters
            @SuppressWarnings("rawtypes")
            Enumeration en = req.getParameterNames();
            StringBuilder cmd = new StringBuilder("cmd=_notify-validate");
            String paramName;
            String paramValue;
            while (en.hasMoreElements()) {
                paramName = (String) en.nextElement();
                paramValue = req.getParameter(paramName);
                cmd.append("&").append(paramName).append("=")
                        .append(URLEncoder.encode(paramValue, charset));

                if (paramName == "token") {
                    token = paramValue;
                }
            }

            if (token != null) {
                TokenProp tokenProp = Token.safeGet(token);

                // 3. Post above command to Paypal IPN URL {@link IpnConfig#ipnUrl}
                String envPrefix = tokenProp.paypalSandbox ? "sandbox." : "";
                URL u = new URL("https://www." + envPrefix + "paypal.com/cgi-bin/webscr");
                HttpURLConnection uc = (HttpURLConnection) u.openConnection();
                uc.setDoOutput(true);
                uc.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
                uc.setRequestProperty("Host", "www." + envPrefix + "paypal.com:443");
                PrintWriter pw = new PrintWriter(uc.getOutputStream());
                pw.println(cmd.toString());
                pw.close();

                logger.info("cmd " + cmd.toString());

                // 4. Read response from Paypal
                BufferedReader in = new BufferedReader(new InputStreamReader(uc.getInputStream()));
                String res = in.readLine();
                in.close();

                logger.info("res " + res);

                // 6. Validate captured Paypal IPN Information
                if (res.equals("VERIFIED")) {
                    String paymentStatus = req.getParameter("payment_status");
                    if (paymentStatus.equals("Completed")) {
                        // Registration.handleRegistrationComplete(tokenProp.client, tokenProp.registrationId);
                    }
                } else {
                    logger.warning("Invalid response {" + res + "} expecting {VERIFIED}");
                }
            } else {
                logger.warning("Token is missing");
            }
        } catch (Exception ex) {
            logger.warning("Exception: " + ex.toString());
        }
    }

    private String getAllRequestParams(HttpServletRequest req) {
        @SuppressWarnings("rawtypes")
        Map map = req.getParameterMap();
        StringBuilder sb = new StringBuilder("\nREQUEST PARAMETERS\n");
        for (@SuppressWarnings("rawtypes")
             Iterator it = map.keySet().iterator(); it.hasNext(); ) {
            String pn = (String) it.next();
            sb.append(pn).append("\n");
            String[] pvs = (String[]) map.get(pn);
            for (int i = 0; i < pvs.length; i++) {
                String pv = pvs[i];
                sb.append("\t").append(pv).append("\n");
            }
        }
        return sb.toString();
    }
}
