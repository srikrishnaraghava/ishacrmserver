package crmdna.api.servlet;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.Credential;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class AuthFilter implements Filter {

    private final static Logger LOGGER = Logger.getLogger(AuthFilter.class.getName());
    private FilterConfig filterConfig = null;
    private Credential googleCredential;
    private AuthorizationCodeFlow googleAuthflow;

    public void init(FilterConfig filterConfig) throws ServletException {

        LOGGER.setLevel(Level.INFO);
        LOGGER.info("Initializing Authorization Filter");
        this.filterConfig = filterConfig;

        try {
            this.googleAuthflow = ServletUtils.newFlow();
        } catch (IOException e) {
            // FIXME
        }
    }

    public void destroy() {
        this.filterConfig = null;
    }

    @Override
    public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
            throws IOException, ServletException {

        if (filterConfig == null)
            return;

        LOGGER.info("Invoking Authorization Filter");

        HttpServletRequest request = (HttpServletRequest) req;
        HttpServletResponse response = (HttpServletResponse) res;

        String origin = request.getHeader("Origin");
        String[] allowedOrigins =
                {"https://ishacrmdev.appspot.com", "https://ishacrmdev-t.appspot.com",
                        "https://ishacrm.appspot.com", "http://localhost:54031", "http://localhost:56022",
                        "http://www.bhairaviyoga.sg", "http://bhairaviyoga.sg", "http://admin.bhairaviyoga.sg",
                        "http://test.bhairaviyoga.sg", "http://members.bhairaviyoga.sg",
                        "http://admin.bhairavinaturals.sg", "http://test.bhairavinaturals.sg"};

        response.setHeader("Access-Control-Allow-Methods", "GET,POST");
        response.setHeader("Access-Control-Allow-Headers", "Content-Type");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Max-Age", "86400");

        if (!isAuthenticated(request.getRequestURI(), request.getParameter("action"))) {
            response.setHeader("Access-Control-Allow-Origin", origin);
            chain.doFilter(request, response);
            return;
        }

        if (Arrays.asList(allowedOrigins).contains(origin)) {
            response.setHeader("Access-Control-Allow-Origin", origin);
        }

        HttpSession session = request.getSession(true);
        Object loginObj = session.getAttribute("login");
        if (loginObj == null) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_LOGIN_REQUIRED));
            LOGGER.info("login missing in session");
            return;
        }

        String login = loginObj.toString();
        String loginType = session.getAttribute("loginType").toString();
        if (loginType.equalsIgnoreCase("google")) {

            googleCredential = googleAuthflow.loadCredential(session.getId());
            if (googleCredential == null || googleCredential.getAccessToken() == null) {
                response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_LOGIN_REQUIRED));
                LOGGER.info("google auth expired for " + login);
                return;
            }
        }

        chain.doFilter(request, response);
    }

    private boolean isAuthenticated(String uri, String action) {

        LOGGER.info(uri + " -> " + action);

        if (uri.startsWith("/login")) {
            if (action == null) {
                return false;
            } else if (action.equals("google-login")) {
                return false;
            } else if (action.equals("login")) {
                return false;
            } else if (action.equals("register")) {
                return false;
            }
        } else if (uri.startsWith("/account")) {
            if (action == null) {
                return false;
            } else if (action.equals("register")) {
                return false;
            } else if (action.equals("verify")) {
                return false;
            } else if (action.equals("checkVerification")) {
                return false;
            }
        } else if (uri.startsWith("/register")) {
            if (action == null) {
                return false;
            } else if (action.equals("registerForProgram")) {
                return false;
            } else if (action.equals("applyDiscount")) {
                return false;
            }
        } else if (uri.startsWith("/program")) {
            if (action == null) {
                return false;
            }
            if (action.equals("query") || action.equals("get")) {
                return false;
            }
        } else if (uri.startsWith("/sessionPass")) {
            if (action == null) {
                return false;
            }
            if (action.equals("purchaseSubscriptionNoAuth")) {
                return false;
            }
        } else if (uri.startsWith("/member")) {
            if (action == null) {
                return false;
            }
            if (action.equals("sendReportAsEmail")) {
                return false;
            }
        }

        return true;
    }
}
