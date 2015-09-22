package crmdna.api.servlet;

import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.encryption.Encryption;
import crmdna.member.MemberEntity;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.user.User;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

public class LoginServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Logger LOGGER = Logger.getLogger(LoginServlet.class.getName());
        String action = request.getParameter("action");
        String client = ServletUtils.getStrParam(request, "client");
        if (client == null) {
            client = "isha";
        }

        if (action == null) {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else if (action.equals("get")) {

            String login = ServletUtils.getLogin(request);

            User.ensureValidUser(ServletUtils.getStrParam(request, "client"), login);

            ServletUtils.setJson(response, new APIResponse()
                .status(Status.SUCCESS)
                .object(User.get(client, login).toProp(client)));

        } else if (action.equals("login")) {

            String email = request.getParameter("email");
            String password = request.getParameter("password");

            // LOGGER.info(email + ":" + password);

            APIResponse apiResponse;

            try {
                MemberEntity memberEntity = MemberLoader.getByEmail(client, email);
                if (memberEntity == null) {
                    throw new APIException().status(Status.ERROR_INVALID_USER).message("Invalid User");
                }

                MemberProp memberProp = memberEntity.toProp();
                if (memberProp.hasAccount == false) {
                    throw new APIException().status(Status.ERROR_INVALID_USER).message("Invalid User");
                }

                byte[] encryptedPwd = Encryption.getEncryptedPassword(password, memberProp.getSalt());
                if (Arrays.equals(memberProp.getEncryptedPwd(), encryptedPwd) == false) {
                    throw new APIException().status(Status.ERROR_AUTH_FAILURE).message("Invalid Password");
                }

                if (memberProp.isEmailVerified == false) {
                    throw new APIException().status(Status.ERROR_PRECONDITION_FAILED).message(
                            "User pending verification");
                }

                HttpSession sess = request.getSession(true);
                sess.setAttribute("login", email);
                sess.setAttribute("loginType", "normal");

                apiResponse = new APIResponse().status(Status.SUCCESS).object(email);

            } catch (Exception e) {
                apiResponse =
                        APIUtils.toAPIResponse(e, true, new RequestInfo().client(client).req(request));
            }

            String successUrl = request.getParameter("successUrl");

            if (successUrl != null) {
                String errorUrl = request.getParameter("errorUrl");
                response.sendRedirect((apiResponse.statusCode == Status.SUCCESS ? successUrl : errorUrl)
                        + "?status=" + apiResponse.statusCode + "&message=" + apiResponse.userFriendlyMessage);
            } else {
                ServletUtils.setJson(response, apiResponse);
            }

        } else if (action.equals("google-login")) {

            String redirectUri = ServletUtils.getRedirectUri(request);
            String onSuccessUrl = request.getParameter("onSuccessUrl");
            String onErrorUrl = request.getParameter("onErrorUrl");

            response.sendRedirect(ServletUtils.newFlow().newAuthorizationUrl()
                    .setState(onSuccessUrl + "|" + onErrorUrl + "|" + client).setRedirectUri
                    (redirectUri).build());

        } else if (action.equals("logout")) {
            LOGGER.info(ServletUtils.getLogin(request));
            request.getSession(true).invalidate();
            ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));
        } else {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
        }
    }
}
