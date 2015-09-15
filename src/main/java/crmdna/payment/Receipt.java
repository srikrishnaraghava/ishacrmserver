package crmdna.payment;

import com.googlecode.objectify.cmd.Query;
import crmdna.common.DateUtils;
import crmdna.common.EmailConfig;
import crmdna.common.NumberUtils;
import crmdna.common.Utils.Currency;
import crmdna.group.Group;
import crmdna.mail2.*;
import crmdna.mail2.MailContent.ReservedMailContentName;
import crmdna.user.User;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;
import static crmdna.mail2.MailContent.ensureReservedMailContentExists;

public class Receipt {

    enum Purpose {
        PROGRAM_REGISTRATION,
        VOLUNTEER_CONTRIBUTION,
        ADHOC_DONATION
    }

    public static class ReceiptQueryCondition {
        public Long registrationId;
        public Long groupId;
        public Integer startYYYYMMDD;
        public Integer endYYYYMMDD;
        public String firstName;
        public String lastName;
        public String email;
        public Purpose purpose;
        public Double amount;
    }

    public static ReceiptProp generateForRegistration(String client, long groupId,
        long registrationId, boolean sendEmail) {

        ensureReservedMailContentExists(client, groupId, ReservedMailContentName.RESERVED_RECEIPT);

        ReceiptProp receiptProp = getByRegistrationId(client, registrationId);
        if (receiptProp == null) {
            ReceiptEntity receiptEntity = new ReceiptEntity(client, groupId);
            receiptEntity.registrationId = registrationId;
            receiptEntity.purpose = Purpose.PROGRAM_REGISTRATION;

            ofy(client).save().entity(receiptEntity).now();

            receiptProp = receiptEntity.toProp(client);
            sendEmail = true;
        }

        if (sendEmail) {
            sendEmail(client, receiptProp);
        }

        return receiptProp;
    }

    public static ReceiptProp generateForProgram(String client, long groupId, String firstName,
        String lastName, String email, long programId, Purpose purpose, Currency ccy, double
        amount) {

        ensureReservedMailContentExists(client, groupId, ReservedMailContentName.RESERVED_RECEIPT);

        ReceiptEntity receiptEntity = new ReceiptEntity(client, groupId);
        receiptEntity.firstName = firstName;
        receiptEntity.lastName = lastName;
        receiptEntity.email = email;
        receiptEntity.programId = programId;
        receiptEntity.purpose = purpose;
        receiptEntity.ccy = ccy;
        receiptEntity.amount = amount;

        ofy(client).save().entity(receiptEntity).now();

        ReceiptProp receiptProp = receiptEntity.toProp(client);
        sendEmail(client, receiptProp);

        return receiptProp;
    }

    public static ReceiptProp generateForAdhocDonation(String client, long groupId,
        String firstName, String lastName, String email, String adhocReference, Purpose purpose,
        Currency ccy, double amount) {

        ensureReservedMailContentExists(client, groupId, ReservedMailContentName.RESERVED_RECEIPT);

        ReceiptEntity receiptEntity = new ReceiptEntity(client, groupId);
        receiptEntity.firstName = firstName;
        receiptEntity.lastName = lastName;
        receiptEntity.email = email;
        receiptEntity.adhocReference = adhocReference;
        receiptEntity.purpose = purpose;
        receiptEntity.ccy = ccy;
        receiptEntity.amount = amount;

        ofy(client).save().entity(receiptEntity).now();

        ReceiptProp receiptProp = receiptEntity.toProp(client);
        sendEmail(client, receiptProp);

        return receiptProp;
    }

    public static ReceiptProp get(String client, String receiptId) {

        ensureNotNull(client, "client should not be null");
        ensureNotNull(receiptId, "receiptId should not be null");

        ReceiptEntity entity = ofy(client).load().type(ReceiptEntity.class).id(receiptId).now();

        return (entity == null) ? null : entity.toProp(client);
    }

    public static List<ReceiptProp> query(String client, ReceiptQueryCondition qc) {
        Query<ReceiptEntity> query = ofy(client).load().type(ReceiptEntity.class);

        if (qc.registrationId != null) {
            query.filter("registrationId", qc.registrationId);
        }

        if (qc.groupId != null) {
            query.filter("groupId", qc.groupId);
        }

        if (qc.startYYYYMMDD != null) {
            query.filter("ms >=", DateUtils.toDate(qc.startYYYYMMDD).getTime());
        }

        if (qc.endYYYYMMDD != null) {
            query.filter("ms <=", DateUtils.toDate(qc.endYYYYMMDD).getTime());
        }

        if (qc.firstName != null) {
            query.filter("firstName", qc.firstName);
        }

        if (qc.lastName != null) {
            query.filter("lastName", qc.lastName);
        }

        if (qc.email != null) {
            query.filter("email", qc.email);
        }

        if (qc.purpose != null) {
            query.filter("purpose", qc.purpose);
        }

        if (qc.amount != null) {
            query.filter("amount", qc.amount);
        }

        List<ReceiptProp> props = new ArrayList<>();
        List<ReceiptEntity> entities = query.list();
        for (ReceiptEntity entity : entities) {
            props.add(entity.toProp(client));
        }

        return props;
    }

    public static ReceiptProp getByRegistrationId(String client, long registrationId) {
        List<ReceiptEntity> entities = ofy(client).load().type(ReceiptEntity.class)
            .filter("registrationId", registrationId)
            .list();

        if (entities.size() == 0) {
            return null;
        }

        return entities.get(0).toProp(client);
    }

    static String getReceiptId(long groupId, long id) {
        return "E"+ String.format("%03d", groupId) + "-" + String.format("%04d", id);
    }

    static void sendEmail(String client, ReceiptProp receiptProp) {
        MailMap mailMap = new MailMap();
        mailMap.add(receiptProp.email, receiptProp.firstName, receiptProp.lastName);
        mailMap.add(MailMap.MergeVarID.DATE, DateUtils.toLongDateString(new Date(receiptProp.ms)));
        mailMap.add(MailMap.MergeVarID.TRANSACTION_ID, receiptProp.receiptId);
        mailMap.add(MailMap.MergeVarID.PURPOSE, receiptProp.purpose);
        mailMap.add(MailMap.MergeVarID.AMOUNT, String.format("%.2f", receiptProp.amount));

        NumberUtils.DecimalNumberString amountInWords = NumberUtils.toWords(receiptProp.amount);
        mailMap.add(MailMap.MergeVarID.AMOUNT_IN_WORDS,
            "DOLLARS " + amountInWords.whole.toUpperCase() +
                ((amountInWords.fraction != null) ?
                    " AND CENTS " + amountInWords.fraction.toUpperCase() : ""));

        MailContentEntity mailContentEntity = MailContent.getByName(client, ReservedMailContentName.RESERVED_RECEIPT.toString(), receiptProp.groupId);
        ensureNotNull(mailContentEntity);
        MailContentProp mailContentProp = mailContentEntity.toProp();

        EmailConfig emailConfig = Group.getEmailConfig(client, receiptProp.groupId, User.SUPER_USER);
        try {
            MailSendInput msi = new MailSendInput();
            msi.createMember = false;
            msi.groupId = receiptProp.groupId;
            msi.isTransactionEmail = true;
            msi.mailContentId = mailContentProp.mailContentId;
            msi.senderEmail = emailConfig.contactEmail;
            msi.suppressIfAlreadySent = false;

            Mail.send(client, msi, mailMap, User.SUPER_USER);
        } catch (Exception ex) {
            Logger LOGGER = Logger.getLogger(Receipt.class.getName());
            LOGGER.severe("Mail Send Error: " + ex.toString());
        }
    }
}
