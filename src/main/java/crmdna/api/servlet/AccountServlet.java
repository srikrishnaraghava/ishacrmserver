package crmdna.api.servlet;

import com.microtripit.mandrillapp.lutung.model.MandrillApiError;
import crmdna.common.DateUtils;
import crmdna.common.api.APIResponse;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.APIUtils;
import crmdna.common.api.RequestInfo;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.member.*;
import crmdna.member.Account.EmailVerificationResult;
import crmdna.sessionpass.SessionPass;
import crmdna.user.User;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

public class AccountServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws IOException {

        Logger LOGGER = Logger.getLogger(AccountServlet.class.getName());
        String action = request.getParameter("action");
        String client = ServletUtils.getStrParam(request, "client");

        if (action == null) {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_NOT_FOUND));
        } else if (action.equals("register")) {

            String group = ServletUtils.getStrParam(request, "group");
            String firstName = ServletUtils.getStrParam(request, "firstName");
            String lastName = ServletUtils.getStrParam(request, "lastName");
            String email = ServletUtils.getStrParam(request, "email");
            String password = ServletUtils.getStrParam(request, "password");
            String phoneNumber = ServletUtils.getStrParam(request, "phoneNumber");

            LOGGER.info(firstName + ":" + lastName + ":" + email + ":" + phoneNumber);

            try {
                MemberEntity memberEntity = MemberLoader.getByEmail(client, email);
                MemberProp memberProp;
                long groupId = Group.safeGetByIdOrName(client, group).toProp().groupId;
                if (memberEntity != null) {
                    Account.createAccount(client, groupId, memberEntity.getId(), password);
                } else {
                    ContactProp contact = new ContactProp();
                    contact.firstName = firstName;
                    contact.lastName = lastName;
                    contact.email = email;
                    contact.mobilePhone = phoneNumber;
                    contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());

                    memberProp = Member.create(client, groupId, contact, false, User.SUPER_USER);

                    Account.createAccount(client, groupId, memberProp.memberId, password);
                }
                ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));
            } catch (Exception ex) {
                ServletUtils.setJson(response, APIUtils.toAPIResponse(ex, true, new RequestInfo().client(client).req(request)));
            }
        } else if (action.equals("verify")) {

            long verificationCode = ServletUtils.getLongParam(request, "verificationCode");
            MemberProp memberProp = Account.getMemberWithAccount(client, ServletUtils.getStrParam(request, "email"));
            EmailVerificationResult verificationResult = Account.verifyEmail(client, memberProp.memberId, verificationCode);

            ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(verificationResult));

        } else if (action.equals("checkVerification")) {

            try {
                MemberProp memberProp = Account.getMemberWithAccount(client, ServletUtils.getStrParam(request, "email"));
                ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(
                        memberProp.isEmailVerified));
            } catch (Exception e) {
                ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_INTERNAL)
                    .message(e.getMessage()));
            }

        } else if (action.equals("resendVerification")) {

            MemberProp memberProp = Account.getMemberWithAccount(client, ServletUtils.getStrParam(request, "email"));
            try {
                long groupId = Group.safeGetByIdOrName(client,
                    ServletUtils.getStrParam(request, "group")).toProp().groupId;
                Account.sendVerificationEmail(client, groupId, memberProp.memberId,
                    User.SUPER_USER);
            } catch (MandrillApiError e) {
                ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_INTERNAL));
                return;
            }
            ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

        } else if (action.equals("changePassword")) {

            MemberProp memberProp = Account.getMemberWithAccount(client, ServletUtils.getStrParam(request, "email"));
            try {
                long groupId = Group.safeGetByIdOrName(
                    client, ServletUtils.getStrParam(request, "group")).toProp().groupId;
                Account.changePassword(client, groupId, memberProp.memberId,
                        ServletUtils.getStrParam(request, "existingPassword"),
                        ServletUtils.getStrParam(request, "newPassword"));

                ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS));

            } catch (Exception e) {
                ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT).message(e.getMessage()));
            }
        } else if (action.equals("getMembersWithAccounts")) {

            List<MemberProp> memberProps = Account.getMembersWithAccounts(client);
            List<AccountDetails> accounts = new ArrayList<>();

            for (MemberProp member : memberProps) {

                AccountDetails account = new AccountDetails();
                account.firstName = member.contact.firstName;
                account.lastName = member.contact.lastName;
                account.email = member.contact.email;
                account.phoneNos = member.contact.getPhoneNos();
                account.memberId = member.memberId;
                account.isEmailVerified = member.isEmailVerified;
                account.numTotalSessionPasses =
                        SessionPass.getNumPasses(client, member.contact.email, false);
                account.numUnusedSessionPasses =
                        SessionPass.getNumPasses(client, member.contact.email, true);
                account.accountCreatedMS = member.accountCreatedMS;

                accounts.add(account);
            }

            ServletUtils.setJson(response, new APIResponse().status(Status.SUCCESS).object(accounts));
        } else {
            ServletUtils.setJson(response, new APIResponse().status(Status.ERROR_RESOURCE_INCORRECT));
        }
    }

    public class AccountDetails {
        String firstName;
        String lastName;
        String email;
        String phoneNos;
        long memberId;
        boolean isEmailVerified;
        long accountCreatedMS;
        int numTotalSessionPasses;
        int numUnusedSessionPasses;
    }
}
