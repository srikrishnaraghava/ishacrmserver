package crmdna.api.endpoint;

import com.google.api.server.spi.config.Api;
import com.google.api.server.spi.config.ApiMethod;
import com.google.api.server.spi.config.ApiMethod.HttpMethod;
import com.google.appengine.api.users.User;
import crmdna.client.Client;
import crmdna.common.EmailConfig;
import crmdna.common.Utils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.group.Group;
import crmdna.group.Group.EmailType;
import crmdna.group.Group.GroupProp;
import crmdna.group.PaypalApiCredentialsProp;

import javax.annotation.Nullable;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Api(name = "group")
public class GroupApi {
    @ApiMethod(path = "create", httpMethod = HttpMethod.POST)
    public APIResponse create(@Named("client") String client,
                              @Named("displayName") String displayName,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            GroupProp prop = Group.create(client, displayName, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getAll", httpMethod = HttpMethod.GET)
    public APIResponse getAll(@Named("client") String client,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req) {

        if (null == client)
            client = "isha";

        try {

            Client.ensureValid(client);
            List<GroupProp> props = Group.getAll(client, false);

            return new APIResponse().status(Status.SUCCESS).object(props);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "rename", httpMethod = HttpMethod.GET)
    public APIResponse rename(@Named("client") String client,
                              @Named("groupId") long groupId, @Named("newDisplayName") String newDisplayName,
                              @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            GroupProp prop = Group.rename(client, groupId, newDisplayName, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "delete", httpMethod = HttpMethod.GET)
    public APIResponse delete(@Named("client") String client,
                              @Named("groupId") long groupId, @Nullable @Named("showStackTrace") Boolean showStackTrace,
                              HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            Group.delete(client, groupId, login);

            return new APIResponse().status(Status.SUCCESS).object("Center [" + groupId + "] deleted");
        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "setPaypalApiCredentials", httpMethod = HttpMethod.POST)
    public APIResponse setPaypalApiCredentials(@Named("client") String client,
                                               @Named("groupId") long groupId, @Named("paypalApiLogin") String payPalLogin,
                                               @Named("paypalApiPassword") String pwd, @Named("paypalApiSecret") String secret,
                                               @Named("sandbox") boolean sandbox, @Named("disable") boolean disable,
                                               @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;
        try {

            Group.safeGet(client, groupId);

            login = Utils.getLoginEmail(user);

            PaypalApiCredentialsProp prop =
                    Group.setPaypalApiCredentials(client, groupId, payPalLogin, pwd, secret, sandbox,
                            disable, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "setEmailTemplate", httpMethod = HttpMethod.POST)
    public APIResponse setEmailTemplate(@Named("client") String client,
                                        @Named("groupId") long groupId, @Named("emailType") EmailType emailType,
                                        @Named("html") List<String> htmls, @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                        HttpServletRequest req, User user) {

        if (client == null)
            client = "isha";

        String login = null;

        try {
            StringBuilder html = new StringBuilder();
            for (String s : htmls) {
                html.append(s);
            }

            login = Utils.getLoginEmail(user);
            Group.setEmailHtmlTemplate(client, groupId, emailType, html.toString(), login);

            return new APIResponse().status(Status.SUCCESS).object("success");

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getEmailTemplates", httpMethod = HttpMethod.GET)
    public APIResponse getEmailTemplates(@Named("client") String client,
                                         @Named("groupId") long groupId, @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                         HttpServletRequest req) {

        if (null == client)
            client = "isha";

        try {

            Map<EmailType, String> map = new HashMap<>();

            String prefix =
                    req.getScheme() + "://" + req.getServerName() + ":" + req.getServerPort()
                            + "/emailTemplate/get?client=" + client + "&groupId=" + groupId + "&emailType=";

            map.put(EmailType.REGISTRATION_CONFIRMATION, prefix + "REGISTRATION_CONFIRMATION");

            map.put(EmailType.REGISTRATION_REMINDER, prefix + "REGISTRATION_REMINDER");

            return new APIResponse().status(Status.SUCCESS).object(map);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req));
        }
    }

    @ApiMethod(path = "getPaypalApiCredentials", httpMethod = HttpMethod.GET)
    public APIResponse getPaypalApiCredentials(@Named("client") String client,
                                               @Named("groupId") long groupId, @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                               HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {

            Group.safeGet(client, groupId);

            login = Utils.getLoginEmail(user);
            PaypalApiCredentialsProp prop = Group.getPaypalApiCredentials(client, groupId, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "getEmailConfig", httpMethod = HttpMethod.GET)
    public APIResponse getEmailConfig(@Named("client") String client,
                                      @Named("groupId") long groupId, @Nullable @Named("showStackTrace") Boolean showStackTrace,
                                      HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;

        try {

            login = Utils.getLoginEmail(user);
            EmailConfig prop = Group.getEmailConfig(client, groupId, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "setMandrillApiKey", httpMethod = HttpMethod.POST)
    public APIResponse setMandrillApiKey(@Named("client") String client,
                                         @Named("groupId") long groupId, @Named("mandrillApiKey") String apiKey,
                                         @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = Utils.getLoginEmail(user);

        try {

            EmailConfig prop = Group.setMandrillApiKey(client, groupId, apiKey, login);

            return new APIResponse().status(Status.SUCCESS).object(prop);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "addOrDeleteAllowedEmailSender", httpMethod = HttpMethod.POST)
    public APIResponse addOrDeleteAllowedEmailSender(@Named("client") String client,
                                                     @Named("groupId") long groupId, @Named("fromEmail") String fromEmail,
                                                     @Named("fromName") String fromName, @Named("add") boolean add,
                                                     @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        if (null == client)
            client = "isha";

        String login = null;
        try {

            Group.safeGet(client, groupId);

            login = Utils.getLoginEmail(user);

            EmailConfig emailConfig =
                    Group.addOrDeleteAllowedEmailSender(client, groupId, fromEmail, fromName, add, login);

            return new APIResponse().status(Status.SUCCESS).object(emailConfig);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }

    @ApiMethod(path = "setContactInfo", httpMethod = HttpMethod.POST)
    public APIResponse setContactInfo(@Named("client") String client,
        @Named("groupId") long groupId, @Named("contactEmail") String contactEmail,
        @Named("contactName") String contactName,
        @Nullable @Named("showStackTrace") Boolean showStackTrace, HttpServletRequest req, User user) {

        String login = null;
        try {

            Group.safeGet(client, groupId);

            login = Utils.getLoginEmail(user);

            Group.setContactInfo(client, groupId, contactEmail, contactName, login);

            return new APIResponse().status(Status.SUCCESS);

        } catch (Exception ex) {
            return APIUtils.toAPIResponse(ex, showStackTrace, new RequestInfo().client(client).req(req)
                    .login(login));
        }
    }
}
