package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import crmdna.api.endpoint.ClientApi.ClientEnum;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.member.Account;
import crmdna.member.Account.EmailVerificationResult;
import crmdna.member.Account.LoginResult;
import crmdna.member.MemberEntity;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.user.User;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

@Api(name = "member")
public class AccountApi {
    @ApiMethod(path = "createAccount", httpMethod = HttpMethod.POST)
    public APIResponse createAccount(@Named("client") ClientEnum clientEnum,
                                     @Nullable @Named("clientOther") String clientOther,
                                     @Named("groupId") long groupId,
                                     @Named("memberIdOrEmail") String memberIdOrEmail, @Named("password") String password,
                                     @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        String client = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);

            MemberEntity memberEntity =
                    MemberLoader.safeGetByIdOrEmail(client, memberIdOrEmail, User.SUPER_USER);

            MemberProp memberProp = Account.createAccount(client, groupId, memberEntity.getId(),
                password);

            return new APIResponse().status(Status.SUCCESS).object(memberProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "verifyAccount", httpMethod = HttpMethod.POST)
    public APIResponse verifyAccount(@Named("client") ClientEnum clientEnum,
                                     @Nullable @Named("clientOther") String clientOther,
                                     @Named("memberIdOrEmail") String memberIdOrEmail,
                                     @Named("verificationCode") long verificationCode,
                                     @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        String client = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);

            MemberEntity memberEntity =
                    MemberLoader.safeGetByIdOrEmail(client, memberIdOrEmail, User.SUPER_USER);

            EmailVerificationResult verificationResult =
                    Account.verifyEmail(client, memberEntity.getId(), verificationCode);

            return new APIResponse().status(Status.SUCCESS).object(verificationResult);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "resendVerificationEmail", httpMethod = HttpMethod.POST)
    public APIResponse resendVerificationEmail(@Named("client") ClientEnum clientEnum,
                                               @Nullable @Named("clientOther") String clientOther,
                                               @Named("groupId") long groupId,
                                               @Named("memberIdOrEmail") String memberIdOrEmail,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req,
                                               com.google.appengine.api.users.User user) {

        String client = null;
        String login = null;

        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);
            MemberEntity memberEntity = MemberLoader.safeGetByIdOrEmail(client, memberIdOrEmail,
                login);

            login = Utils.getLoginEmail(user);

            Account.sendVerificationEmail(client, groupId, memberEntity.getId(), login);

            return new APIResponse().status(Status.SUCCESS).object(
                    "An email with verification code has been sent to ["
                            + memberEntity.toProp().contact.email);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "getLoginResult", httpMethod = HttpMethod.POST)
    public APIResponse getLoginResult(@Named("client") ClientEnum clientEnum,
                                      @Nullable @Named("clientOther") String clientOther, @Named("email") String email,
                                      @Named("password") String password,
                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        String client = null;
        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);

            LoginResult loginResult = Account.getLoginResult(client, email, password);

            return new APIResponse().status(Status.SUCCESS).object(loginResult);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "changePassword", httpMethod = HttpMethod.POST)
    public APIResponse changePassword(@Named("client") ClientEnum clientEnum,
                                      @Nullable @Named("clientOther") String clientOther,
                                      @Named("groupId") long groupId,
                                      @Named("memberIdOrEmail") String memberIdOrEmail,
                                      @Named("existingPassword") String existingPassword, @Named("newPassword") String newPassword,
                                      @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        String client = null;
        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);

            MemberEntity memberEntity =
                    MemberLoader.safeGetByIdOrEmail(client, memberIdOrEmail, User.SUPER_USER);

            MemberProp memberProp =
                    Account.changePassword(client, groupId, memberEntity.getId(),
                        existingPassword, newPassword);

            return new APIResponse().status(Status.SUCCESS).object(memberProp);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "resetPassword", httpMethod = HttpMethod.POST)
    public APIResponse resetPassword(@Named("client") ClientEnum clientEnum,
                                     @Nullable @Named("clientOther") String clientOther,
                                     @Named("groupId") long groupId,
                                     @Named("memberIdOrEmail") String memberIdOrEmail,
                                     @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        String client = null;
        try {
            client = EndpointUtils.getClient(clientEnum, clientOther);

            MemberEntity memberEntity =
                    MemberLoader.safeGetByIdOrEmail(client, memberIdOrEmail, User.SUPER_USER);

            Account.resetPassword(client, groupId, memberEntity.getId());

            return new APIResponse().status(Status.SUCCESS).object(
                    "Password has been reset and an email with new password has been sent to ["
                            + memberEntity.toProp().contact.email + "]");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "disableOrEnableAccount", httpMethod = HttpMethod.POST)
    public APIResponse disableOrEnableAccount(@Named("client") ClientEnum clientEnum,
                                              @Nullable @Named("clientOther") String clientOther,
                                              @Named("memberIdOrEmail") String memberIdOrEmail, @Named("disable") boolean disable,
                                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req,
                                              com.google.appengine.api.users.User user) {

        String client = null;
        String login = null;
        try {
            login = Utils.getLoginEmail(user);

            client = EndpointUtils.getClient(clientEnum, clientOther);

            MemberEntity memberEntity =
                    MemberLoader.safeGetByIdOrEmail(client, memberIdOrEmail, User.SUPER_USER);

            Account.disableOrEnableAccount(client, memberEntity.getId(), disable, login);

            String message =
                    "Account for [" + memberEntity.toProp().contact.email + "] has been "
                            + (disable ? "disabled" : "enabled");
            return new APIResponse().status(Status.SUCCESS).object(message);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "updateVerificationStatus", httpMethod = HttpMethod.POST)
    public APIResponse updateVerificationStatus(@Named("client") ClientEnum clientEnum,
        @Nullable @Named("clientOther") String clientOther,
        @Named("memberIdOrEmail") String memberIdOrEmail, @Named("verified") boolean verified,
        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req,
        com.google.appengine.api.users.User user) {

        String client = null;
        String login = null;
        try {
            login = Utils.getLoginEmail(user);

            client = EndpointUtils.getClient(clientEnum, clientOther);


            MemberEntity memberEntity =
                MemberLoader.safeGetByIdOrEmail(client, memberIdOrEmail, User.SUPER_USER);
            if (verified) {
                Account.setEmailAsVerified(client, memberEntity.getId(), login);
            } else {
                Account.setEmailAsUnverified(client, memberEntity.getId(), login);
            }

            String message =
                "Account Verification for [" + memberEntity.toProp().contact.email + "] : "
                    + (verified ? "verified" : "unverified");
            return new APIResponse().status(Status.SUCCESS).object(message);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }
}
