package crmdna.sessionpass;

import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.EmailConfig;
import crmdna.common.Utils;
import crmdna.common.Utils.Currency;
import crmdna.group.Group;
import crmdna.group.PaypalApiCredentialsProp;
import crmdna.mail2.*;
import crmdna.member.MemberEntity;
import crmdna.member.MemberLoader;
import crmdna.member.MemberProp;
import crmdna.payment.Payment;
import crmdna.payment.Payment.PaymentType;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;
import crmdna.user.User.ClientLevelPrivilege;

import java.util.*;
import java.util.logging.Logger;

import static crmdna.common.AssertUtils.*;
import static crmdna.common.OfyService.ofy;


public class SessionPass {

    public static final String MANUAL_TRANSACTION_ID = "MANUAL";

    public static String buySubscription(String client, long groupId, MemberProp memberProp,
        int numSessions, Date expiry, double amount, Currency currency,
        PaypalApiCredentialsProp paypalProp, String rootUrl, String successCallback,
        String errorCallback) {

        Client.ensureValid(client);

        ensure(numSessions > 0, "numSessions should be positive");
        ensure(numSessions < 1000, "numSessions should be less than 1000");

        ensureNotNull(expiry, "expiry is null");
        ensure(expiry.getTime() > new Date().getTime(), "Expiry should be in future");

        ensure(amount >= 0, "Invalid amount [" + amount + "]");
        ensureNotNull(currency, "Currency not specified");

        Utils.ensureValidEmail(memberProp.contact.email);

        SubscriptionEntity subscriptionEntity = new SubscriptionEntity();
        subscriptionEntity.subscriptionId =
                Sequence.getNext(client, SequenceType.SUBSCRIPTION, 1).get(0);
        subscriptionEntity.numSessions = numSessions;
        subscriptionEntity.expiryMS = expiry.getTime();
        subscriptionEntity.amount = amount;
        subscriptionEntity.currency = currency.toString();
        subscriptionEntity.memberId = memberProp.memberId;
        subscriptionEntity.groupId = groupId;

        String paymentName =
                "Subscription_" + memberProp.memberId + "_" + Integer.toString(numSessions) + "-passes";

        String paymentUrl = Payment
            .setExpressCheckoutAndGetPaymentUrl(client, PaymentType.SESSION_PASS,
                        memberProp.contact.email, paymentName, subscriptionEntity.subscriptionId, paypalProp,
                        amount, currency.toString(), rootUrl, successCallback, errorCallback);

        ofy(client).save().entity(subscriptionEntity).now();

        return paymentUrl;
    }

    public static List<SessionPassProp> allocatePasses(String client, long groupId, long memberId,
        int numSessions, Date expiry, double amount, Currency currency, String transactionId,
        String login) {

        Client.ensureValid(client);
        MemberEntity memberEntity = MemberLoader.safeGet(client, memberId, login);

        ensure(numSessions > 0, "numSessions should be positive");
        ensure(numSessions < 1000, "numSessions should be less than 1000");

        ensureNotNull(expiry, "expiry is null");
        ensure(expiry.getTime() > new Date().getTime(), "Expiry should be in future");

        ensure(amount >= 0, "Invalid amount [" + amount + "]");
        ensureNotNull(currency, "Currency not specified");

        ensureNotNull(transactionId, "transactionId is null");
        ensure(!transactionId.isEmpty(), "transactionId is empty");

        Utils.ensureValidEmail(memberEntity.toProp().contact.email);

        // TODO: allow only members with account
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_SESSION_PASS);

        return allocatePasses(client, groupId, numSessions, expiry.getTime(), memberId,
            login.toLowerCase(), amount, currency.toString(), transactionId);
    }

    public static List<SessionPassProp> allocatePasses(String client, long subscriptionId,
                                                       String transactionId) {

        Client.ensureValid(client);
        SubscriptionProp subscriptionProp =
                ofy(client).load().type(SubscriptionEntity.class).id(subscriptionId).now().toProp();

        MemberEntity memberEntity =
                MemberLoader.safeGet(client, subscriptionProp.memberId, User.SUPER_USER);

        ensureNotNull(transactionId, "transactionId is null");
        ensure(!transactionId.isEmpty(), "transactionId is empty");

        Utils.ensureValidEmail(memberEntity.toProp().contact.email);

        return allocatePasses(client, subscriptionProp.groupId, subscriptionProp.numSessions,
            subscriptionProp.expiryMS, subscriptionProp.memberId, "MEMBER",
            subscriptionProp.amount, subscriptionProp.currency, transactionId);
    }

    private static List<SessionPassProp> allocatePasses(String client, long groupId,
        int numSessions, long expiryMS, long memberId, String purchaseUpdatedBy, double amount,
        String currency, String transactionId) {

        List<SessionPassEntity> entities = new ArrayList<>(numSessions);
        List<Long> ids = Sequence.getNext(client, SequenceType.SESSIONPASS, numSessions);

        for (int i = 0; i < numSessions; i++) {
            SessionPassEntity entity = new SessionPassEntity();
            entity.sessionPassId = ids.get(i);
            entity.expiryMS = expiryMS;
            entity.memberId = memberId;
            entity.purchaseMS = new Date().getTime();

            entity.transactionId = transactionId;
            entity.used = false;
            entity.purchaseUpdatedBy = purchaseUpdatedBy;
            entity.amount = amount;
            entity.currency = currency;

            entities.add(entity);
        }

        ensureEqual(numSessions, entities.size(), "Incorrect no of entities");
        ofy(client).save().entities(entities).now();

        List<SessionPassProp> props = new ArrayList<>();
        for (SessionPassEntity entity : entities) {
            props.add(entity.toProp());
        }

        sendSubscriptionPurchaseNotification(client, groupId, numSessions, amount, currency,
            memberId, expiryMS);

        return props;
    }

    public static int getNumPasses(String client, String email, boolean unusedOnly) {

        Client.ensureValid(client);

        MemberEntity memberEntity = MemberLoader.getByEmail(client, email);
        ensureNotNull(memberEntity);
        Query<SessionPassEntity> q =
                ofy(client).load().type(SessionPassEntity.class).filter("memberId", memberEntity.getId())
                        .filter("expiryMS >", System.currentTimeMillis());

        if (unusedOnly) {
            q = q.filter("used", false);
        }

        return q.count();
    }

    public static int getNumRegistered(String client, String sessionDateTime) {

        Client.ensureValid(client);

        return ofy(client).load().type(SessionPassEntity.class).filter("tags", sessionDateTime).count();
    }

    public static List<SessionPassProp> getRegistrations(String client, String login,
                                                         String sessionDateTime) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_SESSION_PASS);

        List<SessionPassEntity> entities =
                ofy(client).load().type(SessionPassEntity.class).filter("tags", sessionDateTime).list();

        List<SessionPassProp> props = new ArrayList<>();
        for (SessionPassEntity entity : entities) {
            props.add(entity.toProp());
        }

        return props;
    }

    public static List<SessionPassProp> getValidPasses(String client, String login) {

        Client.ensureValid(client);
        User.ensureClientLevelPrivilege(client, login, ClientLevelPrivilege.UPDATE_SESSION_PASS);

        MemberEntity memberEntity = MemberLoader.getByEmail(client, login);
        ensureNotNull(memberEntity);
        List<SessionPassEntity> entities =
                ofy(client).load().type(SessionPassEntity.class).filter("memberId", memberEntity.getId())
                        .list();

        List<SessionPassProp> props = new ArrayList<>();
        for (SessionPassEntity entity : entities) {
            props.add(entity.toProp());
        }

        return props;
    }

    public static boolean isRegistered(String client, String email, String sessionDateTime) {

        Client.ensureValid(client);

        MemberEntity memberEntity = MemberLoader.getByEmail(client, email);
        ensureNotNull(memberEntity);

        return ofy(client).load().type(SessionPassEntity.class)
                .filter("memberId", memberEntity.getId()).filter("tags", sessionDateTime).count() > 0;
    }

    public static void bookSession(String client, String email, String practiceType,
                                   String sessionDateTime) {

        Client.ensureValid(client);

        MemberEntity memberEntity = MemberLoader.getByEmail(client, email);
        ensureNotNull(memberEntity);

        Query<SessionPassEntity> q = ofy(client).load().type(SessionPassEntity.class);
        SessionPassEntity sessionPassEntity =
                q.filter("memberId", memberEntity.getId()).filter("expiryMS >", System.currentTimeMillis())
                        .filter("used", false).order("expiryMS").first().safe();

        sessionPassEntity.used = true;
        sessionPassEntity.tags.add(practiceType);
        sessionPassEntity.tags.add(sessionDateTime);
        ofy(client).save().entity(sessionPassEntity).now();
    }

    public static void cancelBooking(String client, long sessionPassId) {

        Client.ensureValid(client);

        SessionPassEntity sessionPassEntity =
                ofy(client).load().type(SessionPassEntity.class).id(sessionPassId).safe();

        sessionPassEntity.used = false;
        sessionPassEntity.tags.clear();
        ofy(client).save().entity(sessionPassEntity).now();
    }

    public static void sendSubscriptionPurchaseNotification(String client, long groupId,
        int numSessions, double amount, String currency, long memberId, long expiryMS) {

        try {
            MailContentEntity mailContentEntity = MailContent.getByName(client,
                MailContent.ReservedMailContentName.RESERVED_SUBSCRIPTION_PURCHASE.toString(), 0);
            ensureNotNull(mailContentEntity, "RESERVED_SUBSCRIPTION_PURCHASE MailContent is not present");
            MailContentProp mailContentProp = mailContentEntity.toProp();

            MemberProp memberProp = MemberLoader.safeGet(client, memberId, User.SUPER_USER).toProp();
            EmailConfig emailConfig = Group.getEmailConfig(client, groupId, User.SUPER_USER);

            MailMap mailMap = new MailMap();
            mailMap.add(memberProp.contact.email, memberProp.contact.firstName,
                memberProp.contact.lastName);
            mailMap.add(memberProp.contact.email, MailMap.MergeVarID.SUBSCRIPTION_TYPE, numSessions + " Passes");
            mailMap.add(memberProp.contact.email, MailMap.MergeVarID.AMOUNT,
                currency + " " + String.valueOf(amount));
            mailMap.add(memberProp.contact.email, MailMap.MergeVarID.VALIDITY,
                (new Date(expiryMS)).toString());

            MailSendInput msi = new MailSendInput();
            msi.createMember = false;
            msi.groupId = groupId;
            msi.isTransactionEmail = true;
            msi.mailContentId = mailContentProp.mailContentId;
            msi.senderEmail = emailConfig.contactEmail;
            msi.suppressIfAlreadySent = false;

            Mail.send(client, msi, mailMap, User.SUPER_USER);
        } catch (Exception ex) {
            Logger LOGGER = Logger.getLogger(SessionPass.class.getName());
            LOGGER.severe(ex.toString());
        }
    }
}
