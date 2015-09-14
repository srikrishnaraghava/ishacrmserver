package crmdna.api.servlet;

import com.google.api.client.auth.oauth2.AuthorizationCodeFlow;
import com.google.api.client.auth.oauth2.AuthorizationCodeResponseUrl;
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.auth.oauth2.TokenResponse;
import crmdna.user.User;
import crmdna.user.UserEntity;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class GoogleOAuth2CallbackServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private final Lock lock = new ReentrantLock();
    private AuthorizationCodeFlow flow;

    protected final void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {

        StringBuffer buf = req.getRequestURL();
        if (req.getQueryString() != null) {
            buf.append('?').append(req.getQueryString());
        }

        AuthorizationCodeResponseUrl responseUrl = new AuthorizationCodeResponseUrl(buf.toString());

        String code = responseUrl.getCode();
        if (responseUrl.getError() != null) {
            String state = URLDecoder.decode(req.getParameter("state"), "UTF-8");
            String[] params = state.split("\\|");
            resp.sendRedirect(params[1]);
        } else if (code == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            resp.getWriter().print("Missing authorization code");
        } else {
            String redirectUri = ServletUtils.getRedirectUri(req);
            lock.lock();
            try {
                if (flow == null) {
                    flow = ServletUtils.newFlow();
                }
                TokenResponse response = flow.newTokenRequest(code).setRedirectUri(redirectUri).execute();

                HttpSession sess = req.getSession(true);
                Credential credential = flow.createAndStoreCredential(response, sess.getId());
                String[] params = URLDecoder.decode(req.getParameter("state"), "UTF-8").split("\\|");
                String client = params[2];
                String email = ServletUtils.getEmail(credential);
                UserEntity user = User.get(client, email);
                if (user != null) {
                    sess.setAttribute("login", email);
                    sess.setAttribute("loginType", "google");
                }

                resp.sendRedirect(params[0]);
            } finally {
                lock.unlock();
            }
        }
    }
}
