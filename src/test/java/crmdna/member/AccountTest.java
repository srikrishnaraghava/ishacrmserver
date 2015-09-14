package crmdna.member;

import com.google.appengine.tools.development.testing.LocalDatastoreServiceTestConfig;
import com.google.appengine.tools.development.testing.LocalServiceTestHelper;
import com.googlecode.objectify.ObjectifyFilter;
import com.microtripit.mandrillapp.lutung.model.MandrillApiError;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.EmailConfig;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.Group.GroupProp;
import crmdna.mail2.*;
import crmdna.mail2.MailContent.ReservedMailContentName;
import crmdna.member.Account.EmailVerificationResult;
import crmdna.member.Account.LoginResult;
import crmdna.member.Member.AccountType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;
import crmdna.user.UserProp;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AccountTest {
    private final LocalServiceTestHelper datastoreHelper = new LocalServiceTestHelper(
            new LocalDatastoreServiceTestConfig().setApplyAllHighRepJobPolicy());
    // local implementation / test harness implementation becomes HRD
    // only if setApplyAllHighRepJobPolicy is set. If the implementation is not
    // HRD then
    // cross group transactions would fail (as master slave does not support it)

    String client = "isha";
    GroupProp sgp;
    UserProp validUser;
    UserProp userWithVerifyEmailPermission;
    UserProp userWithAccountEnablePermission;

    MemberProp memberWithEmail;

    @Before
    public void setUp() {
        datastoreHelper.setUp();
        ObjectifyFilter.complete();

        Client.create(client);

        sgp = Group.create(client, "Singapore", User.SUPER_USER);
        assertEquals(1, sgp.groupId);

        validUser = User.create(client, "validuser@dummy.com", sgp.groupId,
                User.SUPER_USER);
        userWithVerifyEmailPermission = User.create(client,
                "userwithverifyemailpermission@dummy.com", sgp.groupId,
                User.SUPER_USER);
        User.addClientLevelPrivilege(client,
                userWithVerifyEmailPermission.email,
                ClientLevelPrivilege.VERIFY_EMAIL, User.SUPER_USER);

        userWithAccountEnablePermission = User.create(client,
                "userwithaccountenablepermission@dummy.com", sgp.groupId,
                User.SUPER_USER);
        User.addClientLevelPrivilege(client,
                userWithAccountEnablePermission.email,
                ClientLevelPrivilege.ENABLE_DISABLE_ACCOUNT, User.SUPER_USER);

        MailContent.create(client,
                ReservedMailContentName.RESERVED_EMAIL_VERIFICATION.toString(),
                0, "Bhairavi Yoga: Email Verification",
                "Hello, Your email verification code is: *|VERIFICATIONCODE|*",
                User.SUPER_USER);

        MailContent.create(client,
                ReservedMailContentName.RESERVED_PASSWORD_CHANGE.toString(), 0,
                "Bhairavi Yoga: Password Change",
                "Hello, Your password has been changed", User.SUPER_USER);

        MailContent
                .create(client,
                        ReservedMailContentName.RESERVED_PASSWORD_RESET
                                .toString(),
                        0,
                        "Bhairavi Yoga: Password Reset",
                        "Hello, Your password has been reset. Your new password is: *|PASSWORD|*",
                        User.SUPER_USER);

        Group.setContactInfo(client, sgp.groupId, "verify@verify.com", "Isha Yoga", User.SUPER_USER);
        EmailConfig emailConfig = Group.getEmailConfig(client, sgp.groupId, User.SUPER_USER);
        assertEquals("verify@verify.com", emailConfig.contactEmail);

        ContactProp contact = new ContactProp();
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        contact.firstName = "Sathya";
        contact.email = "sathya.t@ishafoundation.org";
        contact.mobilePhone = "+6593232152";

        memberWithEmail = Member.create(client, sgp.groupId, contact, false,
                User.SUPER_USER);
    }

    @After
    public void tearDown() {
        ObjectifyFilter.complete();
        datastoreHelper.tearDown();
    }

    @Test(expected = APIException.class)
    public void cannotCreateAccountForInvalidClient()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {
        Account.createAccount("invalidclient", 1, 1, "pass123");
    }

    @Test
    public void cannotCreateAccountWhenMemberHasNoEmail()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        ContactProp contact = new ContactProp();
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        contact.firstName = "Sathya";
        contact.mobilePhone = "+6593232152";

        MemberProp memberProp = Member.create(client, sgp.groupId, contact,
                false, User.SUPER_USER);

        try {
            Account.createAccount(client, 1, memberProp.memberId, "pass123");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test(expected = APIException.class)
    public void cannotCreateAccountWithPasswordLessThan4Char()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        ContactProp contact = new ContactProp();
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        contact.firstName = "Sathya";
        contact.email = "sathya.t@ishafoundation.org";
        contact.mobilePhone = "+6593232152";
        MemberProp memberProp = Member.create(client, sgp.groupId, contact,
                false, User.SUPER_USER);

        Account.createAccount(client, 1, memberProp.memberId, "123");
    }

    @Test(expected = APIException.class)
    public void cannotCreateAccountWithPasswordMoreThan50Char()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        ContactProp contact = new ContactProp();
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        contact.firstName = "Sathya";
        contact.email = "sathya.t@ishafoundation.org";
        contact.mobilePhone = "+6593232152";
        MemberProp memberProp = Member.create(client, sgp.groupId, contact,
                false, User.SUPER_USER);

        Account.createAccount(
            client, 1, memberProp.memberId,
                "123456789123456789123456789123456789123456789123456789123456789123456789123456789");
    }

    @Test
    public void cannotCreateAccountForSameMemberTwice()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "123456");

        try {
            Account.createAccount(client, 1, memberWithEmail.memberId, "123456");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }
    }

    @Test
    public void cannotCreateMoreThanOneAccountWithOneEmail()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        ContactProp contact = new ContactProp();
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        contact.firstName = "Sathya";
        contact.email = "sathya.isha@gmail.com";
        contact.mobilePhone = "+6593232152";
        MemberProp sathya1 = Member.create(client, sgp.groupId, contact, false,
                User.SUPER_USER);

        contact = new ContactProp();
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());
        contact.firstName = "Sathyanarayanan";
        contact.email = "sathya.isha@gmail.com";
        MemberProp sathya2 = Member.create(client, sgp.groupId, contact, true,
                User.SUPER_USER);

        Account.createAccount(client, 1, sathya1.memberId, "123456");

        try {
            Account.createAccount(client, 1, sathya2.memberId, "pass123");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_ALREADY_EXISTS, ex.statusCode);
        }
    }

    @Test
    public void canCreateAccountWithValidEmailAndPassword()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        MemberProp memberProp = Account.createAccount(client, 1, memberWithEmail.memberId,
            "123456");
        assertTrue(memberProp.hasAccount);
        assertEquals(false, memberProp.accountDisabled);
        assertEquals(AccountType.FEDERATED, memberProp.accountType);
        assertEquals(false, memberProp.isEmailVerified);

        MemberEntity memberEntity = MemberLoader.safeGet(client,
                memberProp.memberId, User.SUPER_USER);
        assertTrue(memberEntity.verificationCode != 0);
    }

    @Test
    public void verficationEmailSentWhenNewAccountCreated()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();

        MemberProp memberProp = Account.createAccount(client, 1,
                memberWithEmail.memberId, "pass123");

        ObjectifyFilter.complete();
        SentMailQueryCondition qc = new SentMailQueryCondition();
        qc.email = memberProp.contact.email;
        List<SentMailEntity> sentMailEntities = Mail
                .queryEntitiesSortedByTimeDesc(client, qc, User.SUPER_USER);
        assertEquals(1, sentMailEntities.size());

        long mailContentId = sentMailEntities.get(0).toProp().mailContentId;
        String mailContentName = MailContent.safeGet(client, mailContentId)
                .toProp().name;

        String expectedMailContentName = Utils
                .removeSpaceUnderscoreBracketAndHyphen(ReservedMailContentName.RESERVED_EMAIL_VERIFICATION
                        .toString().toLowerCase());
        assertEquals(expectedMailContentName, mailContentName);
    }

    @Test
    public void cannotLoginWithNonexistingEmail()
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        LoginResult loginResult = Account.getLoginResult(client,
                "dummy@invalid.com", "pass123");
        assertEquals(LoginResult.EMAIL_DOES_NOT_EXIST, loginResult);
    }

    @Test
    public void cannotLoginIfNoAccountCreatedForEmail()
            throws NoSuchAlgorithmException, InvalidKeySpecException {

        LoginResult loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass123");
        assertEquals(LoginResult.EMAIL_NOT_A_VALID_ACCOUNT, loginResult);
    }

    @Test
    public void cannotLoginIfEmailIsUnverified()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {
        MailTest.suppressEmailInTestEnv();

        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        LoginResult loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass123");
        assertEquals(LoginResult.EMAIL_NOT_VERIFIED, loginResult);
    }

    @Test
    public void canVerifyEmailWithCorrectVerificationCode()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        long verificationCode = MemberLoader.safeGet(client,
                memberWithEmail.memberId, User.SUPER_USER).verificationCode;

        EmailVerificationResult result = Account.verifyEmail(client,
                memberWithEmail.memberId, verificationCode);
        assertEquals(EmailVerificationResult.SUCCESS, result);
    }

    @Test
    public void memberHasCorrectStatusOnceEmailVerified()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        long verificationCode = MemberLoader.safeGet(client,
                memberWithEmail.memberId, User.SUPER_USER).verificationCode;

        EmailVerificationResult result = Account.verifyEmail(client,
                memberWithEmail.memberId, verificationCode);
        assertEquals(EmailVerificationResult.SUCCESS, result);

        MemberEntity memberEntity = MemberLoader.safeGet(client,
                memberWithEmail.memberId, validUser.email);
        assertTrue(memberEntity.isEmailVerified);
    }

    @Test
    public void cannotVerifyEmailWithIncorrectVerificationCode()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        long verificationCode = MemberLoader.safeGet(client,
                memberWithEmail.memberId, User.SUPER_USER).verificationCode;

        EmailVerificationResult result = Account.verifyEmail(client,
                memberWithEmail.memberId, verificationCode + 100);
        assertEquals(EmailVerificationResult.WRONG_VERIFICATION_CODE, result);
    }

    @Test
    public void cannotVerifyEmailWithNoAccount() {

        MailTest.suppressEmailInTestEnv();

        EmailVerificationResult result = Account.verifyEmail(client,
                memberWithEmail.memberId, 100);
        assertEquals(EmailVerificationResult.EMAIL_NOT_A_VALID_ACCOUNT, result);
    }

    @Test
    public void wrongVerificationCodeForAnAlreadyVerifiedAccountDoesNotChangeAccountStatus()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {
        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        long verificationCode = MemberLoader.safeGet(client,
                memberWithEmail.memberId, User.SUPER_USER).verificationCode;

        EmailVerificationResult result = Account.verifyEmail(client,
                memberWithEmail.memberId, verificationCode);
        assertEquals(EmailVerificationResult.SUCCESS, result);

        result = Account.verifyEmail(client, memberWithEmail.memberId,
                verificationCode + 100);
        assertEquals(EmailVerificationResult.ALREADY_VERIFIED, result);

        MemberEntity memberEntity = MemberLoader.safeGet(client,
                memberWithEmail.memberId, User.SUPER_USER);
        assertTrue(memberEntity.isEmailVerified);
    }

    @Test
    public void cannotLoginWithIncorrectCredential()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();

        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                User.SUPER_USER);

        LoginResult loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "invalid password");
        assertEquals(LoginResult.WRONG_CREDENTIAL, loginResult);
    }

    @Test
    public void userWOPermissionCannotSetAccountToVerified()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        try {
            Account.setEmailAsVerified(client, memberWithEmail.memberId,
                    validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }
    }

    @Test
    public void userWithPermissionCanSetAccountToVerified()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        boolean isEmailVerified = MemberLoader.safeGet(client,
                memberWithEmail.memberId, User.SUPER_USER).isEmailVerified;
        assertEquals(false, isEmailVerified);

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        isEmailVerified = MemberLoader.safeGet(client,
                memberWithEmail.memberId, User.SUPER_USER).isEmailVerified;
        assertEquals(true, isEmailVerified);
    }

    @Test
    public void canLoginWithCorrectCredential()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        MemberEntity memberEntity = MemberLoader.safeGet(client,
                memberWithEmail.memberId, validUser.email);
        assertEquals(memberEntity.hasAccount, true);
        assertEquals(true, memberEntity.isEmailVerified);

        LoginResult loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass123");
        assertEquals(LoginResult.SUCCESS, loginResult);
    }

    @Test
    public void emailSentForPasswordResetWithNewPassword()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        Account.resetPassword(client, 1, memberWithEmail.memberId);

        SentMailQueryCondition qc = new SentMailQueryCondition();
        qc.email = memberWithEmail.contact.email;

        SentMailEntity sentMailEntity = Mail.queryEntitiesSortedByTimeDesc(
                client, qc, User.SUPER_USER).get(0);

        long mailContentId = sentMailEntity.toProp().mailContentId;
        String mailContentName = MailContent.safeGet(client, mailContentId)
                .toProp().name;
        String expected = Utils
                .removeSpaceUnderscoreBracketAndHyphen(MailContent.ReservedMailContentName.RESERVED_PASSWORD_RESET
                        .toString().toLowerCase());
        assertEquals(expected, mailContentName);
    }

    @Test
    public void canLoginWithNewPasswordAfterReset()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        String newPassword = Account.resetPassword(client, 1,
                memberWithEmail.memberId);

        LoginResult loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, newPassword);
        assertEquals(LoginResult.SUCCESS, loginResult);
    }

    @Test
    public void cannotChangeToInvalidPassword()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        // too short
        try {
            Account.changePassword(client, 1, memberWithEmail.memberId, "pass123",
                    "123");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }

        // too long
        try {
            Account.changePassword(
                    client, 1,
                    memberWithEmail.memberId,
                    "pass123",
                    "123456789123456789123456789123456789123456789123456789123456789123456789123456789123456789123456789123456789");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void cannotChangePasswordWithoutExistingPassword()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        try {
            Account.changePassword(client, 1, memberWithEmail.memberId,
                    "wrongpassword", "newpassword");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_AUTH_FAILURE, ex.statusCode);
        }
    }

    @Test
    public void cannotChangeToSamePassword() throws NoSuchAlgorithmException,
            InvalidKeySpecException, MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        try {
            Account.changePassword(client, 1, memberWithEmail.memberId, "pass123",
                    "pass123");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_RESOURCE_INCORRECT, ex.statusCode);
        }
    }

    @Test
    public void canChangeToValidPassword() throws NoSuchAlgorithmException,
            InvalidKeySpecException, MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        Account.changePassword(client, 1, memberWithEmail.memberId, "pass123",
                "pass456");

        LoginResult loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass456");
        assertEquals(LoginResult.SUCCESS, loginResult);
    }

    @Test
    public void cannotLoginWithOldPassword() throws NoSuchAlgorithmException,
            InvalidKeySpecException, MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        Account.changePassword(client, 1, memberWithEmail.memberId, "pass123",
                "pass456");

        LoginResult loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass123");
        assertEquals(LoginResult.WRONG_CREDENTIAL, loginResult);
    }

    @Test
    public void emailNotificationSentForPasswordChange()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        MailTest.suppressEmailInTestEnv();
        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        ObjectifyFilter.complete();
        Account.changePassword(client, 1, memberWithEmail.memberId, "pass123",
                "pass456");

        SentMailQueryCondition qc = new SentMailQueryCondition();
        qc.email = memberWithEmail.contact.email;
        List<SentMailEntity> sentMailEntities = Mail
                .queryEntitiesSortedByTimeDesc(client, qc, User.SUPER_USER);
        assertEquals(2, sentMailEntities.size());

        long mailContentId = sentMailEntities.get(0).toProp().mailContentId;
        String mailContentName = MailContent.safeGet(client, mailContentId)
                .toProp().name;
        String expected = Utils
                .removeSpaceUnderscoreBracketAndHyphen(MailContent.ReservedMailContentName.RESERVED_PASSWORD_CHANGE
                        .toString().toLowerCase());
        assertEquals(expected, mailContentName);
    }

    @Test
    public void permissionedUserCanDisableAccount()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {
        MailTest.suppressEmailInTestEnv();

        MemberProp memberProp = Account.createAccount(client, 1,
                memberWithEmail.memberId, "pass123");

        long verificationCode = MemberLoader.safeGet(client,
                memberProp.memberId, validUser.email).verificationCode;

        Account.verifyEmail(client, memberProp.memberId, verificationCode);

        memberProp = MemberLoader.safeGet(client, memberProp.memberId,
                validUser.email).toProp();
        assertTrue(memberProp.isEmailVerified);

        LoginResult loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass123");
        assertEquals(LoginResult.SUCCESS, loginResult);

        Account.disableOrEnableAccount(client, memberWithEmail.memberId, true,
                userWithAccountEnablePermission.email);

        loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass123");
        assertEquals(LoginResult.ACCOUNT_DISABLED, loginResult);
    }

    @Test
    public void userWOpermissionedUserCannotDisableAccount()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        try {
            Account.disableOrEnableAccount(client, memberWithEmail.memberId,
                    true, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }
    }

    @Test
    public void permissionedUserCanEnableAccount()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {
        MailTest.suppressEmailInTestEnv();

        MemberProp memberProp = Account.createAccount(client, 1,
                memberWithEmail.memberId, "pass123");

        long verificationCode = MemberLoader.safeGet(client,
                memberProp.memberId, validUser.email).verificationCode;

        Account.verifyEmail(client, memberProp.memberId, verificationCode);

        memberProp = MemberLoader.safeGet(client, memberProp.memberId,
                validUser.email).toProp();
        assertTrue(memberProp.isEmailVerified);

        LoginResult loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass123");
        assertEquals(LoginResult.SUCCESS, loginResult);

        Account.disableOrEnableAccount(client, memberWithEmail.memberId, true,
                User.SUPER_USER);

        loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass123");
        assertEquals(LoginResult.ACCOUNT_DISABLED, loginResult);

        // account is disabled

        // try to enable it again
        Account.disableOrEnableAccount(client, memberWithEmail.memberId, false,
                userWithAccountEnablePermission.email);
        memberProp = MemberLoader.safeGet(client, memberProp.memberId,
                validUser.email).toProp();
        assertTrue(!memberProp.accountDisabled);
        loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass123");
        assertEquals(LoginResult.SUCCESS, loginResult);
    }

    @Test
    public void userWOpermissionCannotEnableAccount()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {

        Account.createAccount(client, 1, memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        try {
            Account.disableOrEnableAccount(client, memberWithEmail.memberId,
                    false, validUser.email);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_INSUFFICIENT_PERMISSION, ex.statusCode);
        }
    }

    @Test
    public void cannotLoginWhenAccountDisabled()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {
        MemberProp memberProp = Account.createAccount(client, 1,
                memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        Account.disableOrEnableAccount(client, memberProp.memberId, true,
                User.SUPER_USER);

        LoginResult loginResult = Account.getLoginResult(client,
                memberWithEmail.contact.email, "pass123");
        assertEquals(LoginResult.ACCOUNT_DISABLED, loginResult);
    }

    @Test
    public void cannotResetPasswordWhenAccountDisabled()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {
        MemberProp memberProp = Account.createAccount(client, 1,
                memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        Account.disableOrEnableAccount(client, memberProp.memberId, true,
                User.SUPER_USER);

        try {
            Account.resetPassword(client, 1, memberWithEmail.memberId);
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_OPERATION_NOT_ALLOWED, ex.statusCode);
        }
    }

    @Test
    public void cannotChangePasswordWhenAccountDisabled()
            throws NoSuchAlgorithmException, InvalidKeySpecException,
            MandrillApiError, IOException {
        MemberProp memberProp = Account.createAccount(client, 1,
                memberWithEmail.memberId, "pass123");

        Account.setEmailAsVerified(client, memberWithEmail.memberId,
                userWithVerifyEmailPermission.email);

        Account.disableOrEnableAccount(client, memberProp.memberId, true,
                User.SUPER_USER);

        try {
            Account.changePassword(client, 1, memberWithEmail.memberId, "pass123",
                    "pass456");
            assertTrue(false);
        } catch (APIException ex) {
            assertEquals(Status.ERROR_OPERATION_NOT_ALLOWED, ex.statusCode);
        }
    }
}
