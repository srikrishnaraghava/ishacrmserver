package crmdna.member;

import com.googlecode.objectify.Key;
import com.microtripit.mandrillapp.lutung.model.MandrillApiError;
import crmdna.client.Client;
import crmdna.common.EmailConfig;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.api.RequestInfo;
import crmdna.encryption.Encryption;
import crmdna.group.Group;
import crmdna.mail2.*;
import crmdna.mail2.MailContent.ReservedMailContentName;
import crmdna.member.Member.AccountType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static crmdna.common.AssertUtils.*;
import static crmdna.common.OfyService.ofy;

public class Account {
    public static final int MAX_PASSWORD_LENGTH = 50;
    public static final int MIN_PASSWORD_LENGTH = 3;

    public static MemberProp createAccount(String client, long groupId, long memberId, String
        password)
            throws NoSuchAlgorithmException, InvalidKeySpecException, MandrillApiError, IOException {

        Client.ensureValid(client);
        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, User.SUPER_USER);

        Utils.ensureValidEmail(memberEntity.email);
        ensureValidPassword(password);

        String email = memberEntity.email.toLowerCase();
        List<Key<MemberEntity>> memberKeys =
                ofy(client).load().type(MemberEntity.class).filter("email", memberEntity.email)
                        .filter("hasAccount", true).keys().list();
        if (!memberKeys.isEmpty())
            throw new APIException("There is already an account for email [" + email + "]")
                    .status(Status.ERROR_RESOURCE_ALREADY_EXISTS);
        // TODO: memcache lock

        ensureVerificationEmailIsSetUp(client, groupId);

        byte[] salt = Encryption.generateRandomSalt();
        ensure(salt != null, "salt is null");
        ensure(salt.length > 0, "salt has length 0");
        memberEntity.salt = salt;

        byte[] encryptedPassword = Encryption.getEncryptedPassword(password, salt);
        ensure(encryptedPassword != null, "encryptedPassword is null");
        ensure(encryptedPassword.length > 0, "encryptedPassword has length 0");
        memberEntity.encryptedPwd = encryptedPassword;

        memberEntity.isEmailVerified = false;
        final int ONE_MILLION = 1000000;
        memberEntity.verificationCode = new Random().nextInt(ONE_MILLION);
        memberEntity.hasAccount = true;
        memberEntity.accountType = AccountType.FEDERATED;

        ofy(client).save().entity(memberEntity).now();

        sendVerificationEmail(client, groupId, memberId, User.SUPER_USER);

        return memberEntity.toProp();
    }

    public static void sendVerificationEmail(String client, long groupId, long memberId,
        String login)
            throws MandrillApiError, IOException {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, User.SUPER_USER);
        MemberProp memberProp = memberEntity.toProp();
        Utils.ensureValidEmail(memberProp.contact.email);

        if (memberEntity.isEmailVerified)
            throw new APIException("Email is already verified").status(Status.ERROR_PRECONDITION_FAILED);

        ensure(memberEntity.verificationCode != 0, "Verification code not set in memberEntity ["
                + memberId + "]");

        String email = memberProp.contact.email;
        MailMap mailMap = new MailMap();
        String firstName = "Member";
        if (memberProp.contact.firstName != null)
            firstName = memberProp.contact.firstName;
        mailMap.add(email, firstName, "N.A");
        mailMap
            .add(email, MailMap.MergeVarID.VERIFICATION_CODE, memberEntity.verificationCode + "");
        long mailContentId = MailContent
            .getByName(client, ReservedMailContentName.RESERVED_EMAIL_VERIFICATION.toString(), 0)
            .toProp().mailContentId;

        EmailConfig emailConfig = Group.getEmailConfig(client, groupId, login);

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.groupId = groupId;
        msi.isTransactionEmail = true;
        msi.mailContentId = mailContentId;
        msi.senderEmail = emailConfig.contactEmail;
        msi.suppressIfAlreadySent = false;

        Mail.send(client, msi, mailMap, login);
    }

    public static void sendPasswordChangeNotificationEmail(String client, long groupId,
        long memberId)
            throws MandrillApiError, IOException {

        Client.ensureValid(client);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, User.SUPER_USER);
        MemberProp memberProp = memberEntity.toProp();
        Utils.ensureValidEmail(memberProp.contact.email);

        String email = memberProp.contact.email;
        MailMap mailMap = new MailMap();
        String firstName = memberEntity.firstName != null ? memberEntity.firstName : "Member";
        mailMap.add(email, firstName, "N.A");

        MailContentEntity mailContentEntity = MailContent
            .getByName(client, ReservedMailContentName.RESERVED_PASSWORD_CHANGE.toString(), 0);
        if (mailContentEntity == null) {
            String errMessage =
                    "There is no mail content for name [" + ReservedMailContentName.RESERVED_PASSWORD_CHANGE
                            + "] for client [" + client + "] for group id [0]";

            throw new APIException(errMessage).status(Status.ERROR_INVALID_SETUP);
        }

        ensureNotNull(mailContentEntity.toProp().body, "Body for verification email is null");

        EmailConfig emailConfig = Group.getEmailConfig(client, groupId, User.SUPER_USER);

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.groupId = groupId;
        msi.isTransactionEmail = true;
        msi.mailContentId = mailContentEntity.toProp().mailContentId;
        msi.senderEmail = emailConfig.contactEmail;
        msi.suppressIfAlreadySent = false;

        Mail.send(client, msi, mailMap, User.SUPER_USER);
    }

    public static void sendPasswordResetEmail(String client, long groupId, long memberId, String
        password)
            throws MandrillApiError, IOException {

        Client.ensureValid(client);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, User.SUPER_USER);
        MemberProp memberProp = memberEntity.toProp();
        Utils.ensureValidEmail(memberProp.contact.email);

        String email = memberProp.contact.email;
        MailMap mailMap = new MailMap();
        String firstName = memberEntity.firstName != null ? memberEntity.firstName : "Member";
        mailMap.add(email, firstName, "N.A");
        mailMap.add(email, MailMap.MergeVarID.PASSWORD, password);

        MailContentEntity mailContentEntity =
                MailContent
                        .getByName(client, ReservedMailContentName.RESERVED_PASSWORD_RESET.toString(), 0);
        if (mailContentEntity == null) {
            String errMessage =
                    "There is no mail content for name [" + ReservedMailContentName.RESERVED_PASSWORD_RESET
                            + "] for client [" + client + "], group id [0]";

            throw new APIException(errMessage).status(Status.ERROR_INVALID_SETUP);
        }

        EmailConfig emailConfig = Group.getEmailConfig(client, groupId, User.SUPER_USER);

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.groupId = groupId;
        msi.isTransactionEmail = true;
        msi.mailContentId = mailContentEntity.toProp().mailContentId;
        msi.senderEmail = emailConfig.contactEmail;
        msi.suppressIfAlreadySent = false;

        Mail.send(client, msi, mailMap, User.SUPER_USER);
    }

    private static void ensureVerificationEmailIsSetUp(String client, long groupId) {
        MailContentEntity mailContentEntity =                MailContent.getByName(client,
                        ReservedMailContentName.RESERVED_EMAIL_VERIFICATION.toString(), 0);
        if (mailContentEntity == null) {
            String errMessage =
                    "There is no mail content for name ["
                            + ReservedMailContentName.RESERVED_EMAIL_VERIFICATION + "] for client [" + client
                            + "]";
            RuntimeException ex = new RuntimeException(errMessage);

            Utils.sendAlertEmailToDevTeam(ex, new RequestInfo().client(client));

            throw new APIException(errMessage).status(Status.ERROR_RESOURCE_NOT_FOUND);
        }

        ensureNotNull(mailContentEntity.toProp().body, "Body for verification email is null");

        EmailConfig emailConfig = Group.getEmailConfig(client, groupId, User.SUPER_USER);

        ensureNotNull(emailConfig.contactEmail, "contactEmail is null for group [" + groupId + "]");
        ensureNotNull(emailConfig.contactName, "contactName is null for group [" + groupId + "]");

        Utils.ensureValidEmail(emailConfig.contactEmail);
    }

    private static void ensureValidPassword(String password) {
        ensureNotNull(password, "Password is null");

        ensure(!password.isEmpty(), "Password is empty");
        ensure(password.length() > MIN_PASSWORD_LENGTH, "Password should be greater than ["
                + MIN_PASSWORD_LENGTH + "] characters");

        ensure(password.length() < MAX_PASSWORD_LENGTH, "Password should be lesser than ["
                + MAX_PASSWORD_LENGTH + "] characters");
    }

    public static MemberProp changePassword(String client, long groupId, long memberId, String
        existingPassword,
                                            String newPassword) throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        Client.ensureValid(client);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, User.SUPER_USER);

        if (!memberEntity.hasAccount)
            throw new APIException("There is no account for member [" + memberId + "]")
                    .status(Status.ERROR_OPERATION_NOT_ALLOWED);

        if (memberEntity.accountDisabled)
            throw new APIException("Account is disabled for member [" + memberId + "]")
                    .status(Status.ERROR_OPERATION_NOT_ALLOWED);

        LoginResult loginResult = getLoginResult(client, memberEntity.email, existingPassword);
        if (loginResult != LoginResult.SUCCESS)
            throw new APIException("Unable to change password - " + loginResult)
                    .status(Status.ERROR_AUTH_FAILURE);

        ensure(!existingPassword.equals(newPassword), "Password cannot be the same");
        ensureValidPassword(newPassword);

        ensureNotNull(memberEntity.salt, "salt is null");
        memberEntity.encryptedPwd = Encryption.getEncryptedPassword(newPassword, memberEntity.salt);

        ofy(client).save().entity(memberEntity).now();
        sendPasswordChangeNotificationEmail(client, groupId, memberId);

        return memberEntity.toProp();
    }

    public static String resetPassword(String client, long groupId, long memberId)
        throws NoSuchAlgorithmException, InvalidKeySpecException, MandrillApiError, IOException {

        Client.ensureValid(client);
        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, User.SUPER_USER);

        if (!memberEntity.hasAccount)
            throw new APIException("There is no account for member [" + memberId + "]")
                    .status(Status.ERROR_OPERATION_NOT_ALLOWED);

        if (memberEntity.accountDisabled)
            throw new APIException("Account is disabled for member [" + memberId + "]")
                    .status(Status.ERROR_OPERATION_NOT_ALLOWED);

        String password = Utils.getRandomAlphaNumericString(6);
        memberEntity.encryptedPwd = Encryption.getEncryptedPassword(password, memberEntity.salt);

        ofy(client).save().entity(memberEntity).now();
        sendPasswordResetEmail(client, groupId, memberId, password);
        return password;
    }

    public static LoginResult getLoginResult(String client, String email, String password)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        Client.ensureValid(client);
        Utils.ensureValidEmail(email);

        email = email.toLowerCase();

        MemberQueryCondition queryCondition = new MemberQueryCondition(client, 100);
        queryCondition.email = email;

        int count = MemberLoader.getCount(queryCondition, User.SUPER_USER);

        if (count == 0)
            return LoginResult.EMAIL_DOES_NOT_EXIST;

        queryCondition.hasAccount = true;

        List<MemberEntity> memberEntities = MemberLoader.queryEntities(queryCondition, User.SUPER_USER);

        if (memberEntities.isEmpty())
            return LoginResult.EMAIL_NOT_A_VALID_ACCOUNT;

        if (memberEntities.size() > 1) {
            String errMessage =
                    "Email [" + email + "] has [" + memberEntities.size() + "] accounts for client ["
                            + client + "]";
            // should never happen
            Utils.sendAlertEmailToDevTeam(new RuntimeException(errMessage),
                    new RequestInfo().client(client));
            throw new APIException(errMessage).status(Status.ERROR_INTERNAL);
        }

        MemberEntity memberEntity = memberEntities.get(0);

        if (memberEntity.accountDisabled)
            return LoginResult.ACCOUNT_DISABLED;

        if (!memberEntity.isEmailVerified)
            return LoginResult.EMAIL_NOT_VERIFIED;

        ensureNotNullNotEmpty(password, "Supplied password is null or empty");
        ensureNotNull(memberEntity.encryptedPwd, "No password stored in Member Entity");
        ensureNotNull(memberEntity.salt, "No salt stored in Member Entity");

        boolean result =
                Encryption.authenticate(password, memberEntity.encryptedPwd, memberEntity.salt);

        LoginResult loginResult = LoginResult.WRONG_CREDENTIAL;
        if (result)
            loginResult = LoginResult.SUCCESS;

        return loginResult;
    }

    public static EmailVerificationResult verifyEmail(String client, long memberId,
                                                      long verificationCode) {

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, User.SUPER_USER);

        if (!memberEntity.hasAccount)
            return EmailVerificationResult.EMAIL_NOT_A_VALID_ACCOUNT;

        if (memberEntity.isEmailVerified)
            return EmailVerificationResult.ALREADY_VERIFIED;

        if (memberEntity.verificationCode == verificationCode) {
            memberEntity.isEmailVerified = true;
            memberEntity.accountCreatedMS = System.currentTimeMillis();
            ofy(client).save().entity(memberEntity).now();

            return EmailVerificationResult.SUCCESS;
        } else
            return EmailVerificationResult.WRONG_VERIFICATION_CODE;
    }

    public static MemberProp setEmailAsVerified(String client, long memberId, String login) {
        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.VERIFY_EMAIL);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);
        memberEntity.isEmailVerified = true;

        ofy(client).save().entity(memberEntity).now();

        return memberEntity.toProp();
    }

    public static MemberProp setEmailAsUnverified(String client, long memberId, String login) {
        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.VERIFY_EMAIL);

        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);
        memberEntity.isEmailVerified = false;

        ofy(client).save().entity(memberEntity).now();

        return memberEntity.toProp();
    }

    public static MemberProp disableOrEnableAccount(String client, long memberId, boolean disable,
                                                    String login) {
        Client.ensureValid(client);

        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT);
        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);

        if (!memberEntity.hasAccount)
            throw new APIException("There is no account for member [" + memberId + "]")
                    .status(Status.ERROR_OPERATION_NOT_ALLOWED);

        memberEntity.accountDisabled = disable;
        ofy(client).save().entity(memberEntity).now();
        return memberEntity.toProp();
    }

    public static List<MemberProp> getMembersWithAccounts(String client) {

        List<MemberEntity> entities =
                ofy(client).load().type(MemberEntity.class).filter("hasAccount", true)
                        .filter("isEmailVerified", true).list();

        List<MemberProp> props = new ArrayList<>();
        for (MemberEntity entity : entities) {
            props.add(entity.toProp());
        }

        return props;
    }

    public static MemberProp getMemberWithAccount(String client, String email) {

        Utils.ensureValidEmail(email);

        List<MemberEntity> entities =
                ofy(client).load().type(MemberEntity.class).filter("hasAccount", true)
                        .filter("email", email).list();

        ensure(entities.size() == 1, "There are [" + entities.size() + "] members with email ["
                + email + "]");

        return entities.get(0).toProp();
    }


    public enum LoginResult {
        SUCCESS, EMAIL_DOES_NOT_EXIST, EMAIL_NOT_A_VALID_ACCOUNT, EMAIL_NOT_VERIFIED, WRONG_CREDENTIAL, ACCOUNT_DISABLED
    }

    public enum EmailVerificationResult {
        SUCCESS, EMAIL_NOT_A_VALID_ACCOUNT, WRONG_VERIFICATION_CODE, ALREADY_VERIFIED
    }
}
