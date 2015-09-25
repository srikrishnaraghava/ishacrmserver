package crmdna.registration;

import com.googlecode.objectify.VoidWork;
import com.googlecode.objectify.cmd.Query;
import crmdna.client.Client;
import crmdna.common.DateUtils;
import crmdna.common.EmailConfig;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.common.contact.Contact;
import crmdna.common.contact.ContactProp;
import crmdna.group.Group;
import crmdna.group.PaypalApiCredentialsProp;
import crmdna.mail2.*;
import crmdna.member.Member;
import crmdna.member.MemberProp;
import crmdna.payment.Payment;
import crmdna.payment.Payment.PaymentType;
import crmdna.program.Program;
import crmdna.program.ProgramProp;
import crmdna.sequence.Sequence;
import crmdna.sequence.Sequence.SequenceType;
import crmdna.user.User;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.logging.Logger;

import static crmdna.common.AssertUtils.ensureNotNull;
import static crmdna.common.OfyService.ofy;

public class Registration {

    public static RegistrationProp register(String client, String firstName, String lastName,
                                            String nickName, String gender, String email, String mobilePhone, String homePhone,
                                            String officePhone, String country, String postalCode, String programIdStr, String feeStr,
                                            String batchNoStr, String marketingChannel, String successCallbackUrl,
                                            String errorCallbackUrl, String rootUrl) {

        ContactProp contact = new ContactProp();
        contact.firstName = firstName;
        contact.lastName = lastName;
        contact.nickName = nickName;
        contact.email = email;
        contact.mobilePhone = mobilePhone;
        contact.homePhone = homePhone;
        contact.officePhone = officePhone;
        contact.gender = Contact.getGender(gender);
        contact.asOfyyyymmdd = DateUtils.toYYYYMMDD(new Date());

        contact.homeAddress.country = country;
        contact.homeAddress.pincode = postalCode;

        if (null == programIdStr)
            Utils.throwIncorrectSpecException("programId should be specified");
        long programId = Utils.safeParseAsLong(programIdStr);

        Double fee = null;
        if (null != feeStr)
            fee = Utils.safeParseAsDouble(feeStr);

        int batchNo = 1;
        if (batchNoStr != null)
            batchNo = Utils.safeParseAsInt(batchNoStr);

        if ((null == successCallbackUrl) || successCallbackUrl.equals(""))
            Utils.throwIncorrectSpecException("successCallbackUrl is not specified");
        Utils.ensureValidUrl(successCallbackUrl);

        if (null == errorCallbackUrl)
            errorCallbackUrl = successCallbackUrl;
        Utils.ensureValidUrl(errorCallbackUrl);

        return register(client, contact, programId, batchNo, fee, null, marketingChannel,
            successCallbackUrl, errorCallbackUrl, rootUrl);
    }

    public static RegistrationEntity safeGet(String client, long registrationId) {
        Client.ensureValid(client);

        RegistrationEntity entity =
                ofy(client).load().type(RegistrationEntity.class).id(registrationId).now();

        if (null == entity)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Cannot find Registration details for registration id [" + registrationId + "]");

        return entity;
    }

    public static RegistrationProp register(String client, ContactProp contact, long programId,
                                            int batchNo, Double fee, String paymentReference,
                                            String marketingChannel, String successCallbackUrl,
                                            String errorCallbackUrl, String rootUrl) {

        Client.ensureValid(client);

        ProgramProp programProp = Program.safeGet(client, programId).toProp(client);
        if ((batchNo < 1) || (batchNo > programProp.numBatches))
            Utils.throwIncorrectSpecException("Batch number is invalid");

        // cannot register 30 days after the end of program
        // 30 days is just so that there is enough buffer
        int currentYYYYMMDD = DateUtils.toYYYYMMDD(new Date()); // current date
        Date current = new Date();
        Date end = DateUtils.toDate(programProp.endYYYYMMDD);

        if (Utils.getNumDays(end, current) > 30)
            throw new APIException().status(Status.ERROR_OPERATION_NOT_ALLOWED).message(
                    "Registration not allowed as current date [" + currentYYYYMMDD
                            + "] is more than 30 days after the program end date [" + programProp.endYYYYMMDD
                            + "]");

        Contact.ensureFirstNameAndValidEmailSpecified(contact);
        contact.email = contact.email.toLowerCase();

        Long registrationId = getMatchingRegistrationId(client, programId, contact);

        RegistrationEntity registrationEntity;
        if (null == registrationId) {
            registrationEntity = new RegistrationEntity();
            registrationEntity.registrationId = Sequence.getNext(client, SequenceType.REGISTRANT);
        } else {
            registrationEntity = safeGet(client, registrationId);
            RegistrationProp registrationProp = registrationEntity.toProp();

            if (registrationProp.getStatus() == RegistrationStatus.REGISTRATION_COMPLETE) {
                registrationProp.alreadyRegistered = true;
                return registrationProp;
            }

            if (registrationProp.getStatus() == RegistrationStatus.PAYMENT_PENDING) {
                Logger logger = Logger.getLogger(Registration.class.getName());
                String errMessage =
                        "Registration for email [" + contact.email + "] and Name [" + contact.getName()
                                + "] is already with status [PAYMENT_PENDING] and registration Id ["
                                + registrationProp.registrationId
                                + "]. Now a new registration for the same name and "
                                + "email is being attempted. This could lead to duplicate registration and "
                                + "duplicate payments.";
                logger.warning(errMessage);
            }

            // clear off the paypal error/payment fields
            registrationEntity.amount = null;
            registrationEntity.ccy = null;
            registrationEntity.isPaymentPending = false;
            registrationEntity.L_ERRORCODE0 = null;
            registrationEntity.L_LONGMESSAGE0 = null;
            registrationEntity.L_SEVERITYCODE0 = null;
            registrationEntity.L_SHORTMESSAGE0 = null;
            registrationEntity.redirectUrl = null;
        }

        Long memberId =
                Member.getMatchingMemberId(client, contact.email, contact.firstName, contact.lastName);
        if (memberId == null) {
            MemberProp memberProp = Member
                .create(client, programProp.groupProp.groupId, contact, true, User.SUPER_USER);
            memberId = memberProp.memberId;
        }

        populateContactDetailsAndQSTags(registrationEntity, contact);
        registrationEntity.memberId = memberId;
        registrationEntity.programId = programId;
        registrationEntity.marketingChannel = marketingChannel;
        registrationEntity.successCallbackUrl = successCallbackUrl;
        registrationEntity.errorCallbackUrl = errorCallbackUrl;

        Logger logger = Logger.getLogger(Registration.class.getName());
        logger.info("registrationId = [" + registrationEntity.registrationId + "], memberId = ["
                + registrationEntity.memberId + "], programId = [" + registrationEntity.programId + "]");

        registrationEntity.recordStateChange(RegistrationStatus.REQUEST_RECEIVED);
        ofy(client).save().entity(registrationEntity).now();

        fee = getFee(fee, programProp.fee);

        registrationEntity.amount = fee.toString();
        registrationEntity.ccy = programProp.ccy.toString();

        if ((programProp.fee == 0) || (paymentReference != null)) {
            registrationEntity.transactionId = paymentReference;
            registrationEntity.recordStateChange(RegistrationStatus.REGISTRATION_COMPLETE);
            ofy(client).save().entity(registrationEntity).now();
            return registrationEntity.toProp();
        }

        // payment is involved - ensure group is set to receive payment
        PaypalApiCredentialsProp prop =
                Group.getPaypalApiCredentials(client, programProp.groupProp.groupId, User.SUPER_USER);

        if ((null == prop) || prop.disable)
            throw new APIException().status(Status.ERROR_OPERATION_NOT_ALLOWED).message(
                    "Paypal info is either not available or payments are disabled for group ["
                            + programProp.groupProp.displayName + "]");

        String paymentName = programProp.programTypeProp.displayName + DateUtils
            .getDurationAsString(programProp.startYYYYMMDD, programProp.endYYYYMMDD);

        // do a paypal set express checkout and get the payment url
        try {

            registrationEntity.paymentUrl = Payment
                .setExpressCheckoutAndGetPaymentUrl(client, PaymentType.PROGRAM_REGISTRATION,
                            contact.email, paymentName, registrationEntity.registrationId, prop, fee,
                            programProp.ccy.toString(), rootUrl, successCallbackUrl, errorCallbackUrl);

            registrationEntity.recordStateChange(RegistrationStatus.PAYMENT_AUTHORIZATION_PENDING);
            ofy(client).save().entity(registrationEntity).now();
            return registrationEntity.toProp();

        } catch (APIException ex) {
            registrationEntity.recordStateChange(RegistrationStatus.ERROR_CREATING_PAYMENT_URL);
            ofy(client).save().entity(registrationEntity).now();
            throw ex;
        }
    }

    static double getFee(Double override, double programFee) {
        double fee = programFee;

        if (override != null)
            fee = override;

        return fee;
    }

    static void populateContactDetailsAndQSTags(RegistrationEntity registrationEntity,
                                                ContactProp contact) {
        if (contact.email != null) {
            Utils.ensureValidEmail(contact.email);
            registrationEntity.email = contact.email.toLowerCase();
        }

        if (contact.firstName != null)
            registrationEntity.firstName = contact.firstName;

        if (contact.lastName != null)
            registrationEntity.lastName = contact.lastName;

        if (contact.nickName != null)
            registrationEntity.nickName = contact.nickName;

        if (contact.homeAddress.address != null)
            registrationEntity.homeAddress = contact.homeAddress.address;

        if (contact.officeAddress.address != null)
            registrationEntity.officeAddress = contact.officeAddress.address;

        if (contact.homePhone != null) {
            Utils.ensureValidPhoneNumber(contact.homePhone);
            registrationEntity.homePhone = contact.homePhone;
        }

        if (contact.officePhone != null) {
            Utils.ensureValidPhoneNumber(contact.officePhone);
            registrationEntity.officePhone = contact.officePhone;
        }

        if (contact.mobilePhone != null) {
            Utils.ensureValidPhoneNumber(contact.mobilePhone);
            registrationEntity.mobilePhone = contact.mobilePhone;
        }

        registrationEntity.qsTags = Utils
            .getQSTags(contact.firstName, contact.lastName, contact.nickName, contact.email,
                contact.mobilePhone, contact.homePhone, contact.officePhone);
    }

    static Long getMatchingRegistrationIdEnsuringNotDuplicate_notused(String client, long programId,
                                                                      ContactProp contact) {

        Contact.ensureFirstNameAndValidEmailSpecified(contact);
        contact.email = contact.email.toLowerCase();

        RegistrationQueryCondition queryCondition = new RegistrationQueryCondition();
        queryCondition.programId = programId;
        queryCondition.searchStr = contact.email;
        // queryCondition.status = RegistrationStatus.REGISTRATION_COMPLETE;

        List<RegistrationProp> props = query(client, queryCondition, User.SUPER_USER);

        for (RegistrationProp prop : props) {
            if (Utils.closeEnough(prop.getName().toLowerCase(), contact.getName().toLowerCase())) {

                if (prop.getStatus() == RegistrationStatus.REGISTRATION_COMPLETE)
                    throw new APIException().status(Status.ERROR_RESOURCE_ALREADY_EXISTS).message(
                            contact.getName() + " with email [" + contact.email
                                    + "] is already registered for this program");

                // if existing entity status is PAYMENT_PENDING, we could
                // potentially get payment (through ipn) in the future.
                // For such cases do not return the matching registration ID;
                // allow the system to create a new registration ID
                // TODO: verify this with Thulasi

                if (prop.getStatus() == RegistrationStatus.PAYMENT_PENDING) {
                    Logger logger = Logger.getLogger(Registration.class.getName());
                    String errMessage =
                            "Registration for email [" + contact.email + "] and Name [" + contact.getName()
                                    + "] is already with status [PAYMENT_PENDING] and registration Id ["
                                    + prop.registrationId + "]. Now a new registration for the same name and " + ""
                                    + "email is being attempted. This could lead to duplicate registration and "
                                    + "duplicate payments.";
                    logger.warning(errMessage);
                } else {
                    return prop.registrationId;
                }
            }
        }

        // no matching registration id, doing it the first time
        return null;
    }

    static Long getMatchingRegistrationId(String client, long programId, ContactProp contact) {

        Contact.ensureFirstNameAndValidEmailSpecified(contact);
        contact.email = contact.email.toLowerCase();

        RegistrationQueryCondition queryCondition = new RegistrationQueryCondition();
        queryCondition.programId = programId;
        queryCondition.searchStr = contact.email;

        List<RegistrationProp> props = query(client, queryCondition, User.SUPER_USER);

        for (RegistrationProp prop : props) {
            if (Utils.closeEnough(prop.getName().toLowerCase(), contact.getName().toLowerCase())) {
                return prop.registrationId;
            }
        }

        // no matching registration id. return null
        return null;
    }

    public static boolean isAlreadyRegistered(String client, long programId, MemberProp memberProp) {

        RegistrationQueryCondition queryCondition = new RegistrationQueryCondition();
        queryCondition.programId = programId;
        queryCondition.searchStr = memberProp.contact.email;

        List<RegistrationProp> props = query(client, queryCondition, User.SUPER_USER);

        for (RegistrationProp prop : props) {
            if (prop.memberId == memberProp.memberId) {
                return true;
            }
        }

        return false;
    }

    public static List<RegistrationProp> query(String client, RegistrationQueryCondition qc,
                                               String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);

        Query<RegistrationEntity> q = ofy(client).load().type(RegistrationEntity.class);

        if (null != qc.programId)
            q = q.filter("programId", qc.programId);

        if (null != qc.status) {
            q = q.filter("status", qc.status);
        }

        if (null != qc.searchStr) {
            if (qc.searchStr.length() < 3)
                Utils.throwIncorrectSpecException("Search string when specified should be atleast 3 char");
            qc.searchStr = qc.searchStr.toLowerCase();
            Set<String> searchTags = Utils.getQSTags(qc.searchStr);
            for (String tag : searchTags) {
                q = q.filter("qsTags", tag);
            }
        }

        List<RegistrationEntity> entities = q.list();

        List<RegistrationProp> props = new ArrayList<>();
        for (RegistrationEntity entity : entities)
            props.add(entity.toProp());

        if (qc.sortByFirstName)
            sortByFirstName(props);

        return props;
    }

    public static RegistrationProp queryByTransactionId(String client, String transactionId) {
        Client.ensureValid(client);

        Query<RegistrationEntity> q =
                ofy(client).load().type(RegistrationEntity.class).filter("transactionId", transactionId);

        List<RegistrationEntity> entities = q.list();

        if (entities.size() == 0)
            return null;

        if (entities.size() > 1) {
            // should never happen
            Logger logger = Logger.getLogger(Registration.class.getName());
            logger.severe("Found [" + entities.size() + "] registration entities "
                    + "with transaction id [" + transactionId + "]");
        }

        return entities.get(0).toProp();
    }

    public static RegistrationProp getByTransactionId(String client, String transactionId) {
        Client.ensureValid(client);

        TransactionEntity transactionEntity =
                ofy(client).load().type(TransactionEntity.class).id(transactionId).now();

        if (transactionEntity == null)
            return null;

        long registrationId = transactionEntity.registrationId;

        return safeGet(client, registrationId).toProp();
    }

    public static RegistrationStatusProp queryRegistrationStatus(String client, long programId,
                                                                 String email, String firstName, String lastName) {

        Client.ensureValid(client);

        ContactProp contactDetailProp = new ContactProp();
        contactDetailProp.firstName = firstName;
        contactDetailProp.lastName = lastName;
        contactDetailProp.email = email;

        Contact.ensureFirstNameAndValidEmailSpecified(contactDetailProp);

        Long registrationId = getMatchingRegistrationId(client, programId, contactDetailProp);

        if (registrationId == null) {
            RegistrationStatusProp rsp = new RegistrationStatusProp();
            rsp.alreadyRegistered = false;
            rsp.hasAttemptedRegistrationBefore = false;
            return rsp;
        }

        RegistrationStatusProp rsp = new RegistrationStatusProp();
        rsp.hasAttemptedRegistrationBefore = true;

        RegistrationProp rp = Registration.safeGet(client, registrationId).toProp();
        rsp.lastRegistrationStatus = rp.getStatus();
        rsp.registrationStatusTimestamp = rp.getStatusTimestamp();

        if (rsp.lastRegistrationStatus == RegistrationStatus.REGISTRATION_COMPLETE)
            rsp.alreadyRegistered = true;

        return rsp;

    }

    public static RegistrationSummaryProp getSummary(String client, long programId, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);
        Program.safeGet(client, programId);

        RegistrationSummaryProp rsp = new RegistrationSummaryProp();

        RegistrationQueryCondition qc = new RegistrationQueryCondition();
        qc.programId = programId;

        List<RegistrationProp> registrationProps = query(client, qc, login);

        for (RegistrationProp registrationProp : registrationProps) {
            RegistrationStatus status = registrationProp.getStatus();

            if (status == RegistrationStatus.REGISTRATION_COMPLETE) {
                rsp.numCompleted++;
            } else {
                if ((status != RegistrationStatus.CANCELLED) && (status != RegistrationStatus.INVALID)) {
                    rsp.numStartedButNotCompleted++;
                }
            }

            if (!rsp.regStatusVsNum.containsKey(status))
                rsp.regStatusVsNum.put(status, 1);
            else {
                int current = rsp.regStatusVsNum.get(status);
                rsp.regStatusVsNum.put(status, current + 1);
            }

            int yyyymmdd = DateUtils.toYYYYMMDD(registrationProp.getStatusTimestamp());

            if (status == RegistrationStatus.REGISTRATION_COMPLETE) {

                if (!rsp.yyyymmddVsNumCompleted.containsKey(yyyymmdd))
                    rsp.yyyymmddVsNumCompleted.put(yyyymmdd, 1);
                else {
                    int current = rsp.yyyymmddVsNumCompleted.get(yyyymmdd);
                    rsp.yyyymmddVsNumCompleted.put(yyyymmdd, current + 1);
                }

            } else {
                if (!rsp.yyyymmddVsNumStartedButNotCompleted.containsKey(yyyymmdd))
                    rsp.yyyymmddVsNumStartedButNotCompleted.put(yyyymmdd, 1);
                else {
                    int current = rsp.yyyymmddVsNumStartedButNotCompleted.get(yyyymmdd);
                    rsp.yyyymmddVsNumStartedButNotCompleted.put(yyyymmdd, current + 1);
                }
            }
        }

        return rsp;
    }

    public static void transfer(String client, long registrationId, long programId, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);
        Program.safeGet(client, programId);
        RegistrationEntity entity = safeGet(client, registrationId);

        long oldProgramId = entity.programId;
        entity.programId = programId;
        ofy(client).save().entity(entity);

        Logger logger = Logger.getLogger(Registration.class.getName());
        logger.info(
            "Registration ID [" + registrationId + "] transferred from Program ID " + oldProgramId
                + "to Program ID " + programId);

    }

    public static void overrideAsCompleted(String client, long registrationId, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);
        RegistrationEntity entity = safeGet(client, registrationId);

        handleDoExpressCheckoutResponse(client, registrationId, entity.amount, entity.ccy,
            "REFER2PAYPALTXNID", false, "", "");

        Logger logger = Logger.getLogger(Registration.class.getName());
        logger.info("Registration ID [" + registrationId + "] overriden as COMPLETE");
    }

    public static void invalidate(String client, long registrationId, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);
        RegistrationEntity entity = safeGet(client, registrationId);

        entity.recordStateChange(RegistrationStatus.INVALID);
        ofy(client).save().entity(entity);

        Logger logger = Logger.getLogger(Registration.class.getName());
        logger.info("Registration ID [" + registrationId + "] INVALIDATED");
    }

    public static void cancel(String client, long registrationId, String login) {

        Client.ensureValid(client);
        User.ensureValidUser(client, login);
        RegistrationEntity entity = safeGet(client, registrationId);

        entity.recordStateChange(RegistrationStatus.CANCELLED);
        ofy(client).save().entity(entity);

        Logger logger = Logger.getLogger(Registration.class.getName());
        logger.info("Registration ID [" + registrationId + "] CANCELLED");
    }

    public static void handlePaymentAuthorizationFailure(String client, long registrationId,
                                                         String redirectUrl) {
        Client.ensureValid(client);

        RegistrationEntity entity = safeGet(client, registrationId);
        entity.recordStateChange(RegistrationStatus.PAYMENT_NOT_AUTHORIZED);
        entity.redirectUrl = redirectUrl;
        ofy(client).save().entity(entity);
    }

    public static void handleDoExpressCheckoutResponse(final String client,
                                                       final long registrationId, final String amount, final String ccy, final String transactionId,
                                                       final boolean isPaymentPending, final String pendingReason, final String redirectUrl) {

        Client.ensureValid(client);

        final Logger logger = Logger.getLogger(Registration.class.getName());

        ofy(client).transact(new VoidWork() {

            @Override
            public void vrun() {
                RegistrationEntity registrationEntity = safeGet(client, registrationId);

                registrationEntity.amount = amount;
                registrationEntity.ccy = ccy;
                registrationEntity.pendingReason = pendingReason;
                registrationEntity.redirectUrl = redirectUrl;

                RegistrationStatus status;
                if (isPaymentPending) {
                    status = RegistrationStatus.PAYMENT_PENDING;
                } else {
                    status = RegistrationStatus.REGISTRATION_COMPLETE;
                }

                registrationEntity.recordStateChange(status);
                registrationEntity.transactionId = transactionId;

                // transaction entity
                TransactionEntity transactionEntity = new TransactionEntity();
                transactionEntity.transactionId = transactionId;
                transactionEntity.registrationId = registrationId;

                ofy(client).save().entities(registrationEntity, transactionEntity).now();

                logger.info("Registration ID [" + registrationId + "]" + ", Status [" + status
                        + "], transactionId [" + transactionId + "], Redirect URL [" + redirectUrl + "]");
            }
        });

        RegistrationEntity registrationEntity = safeGet(client, registrationId);
        try {
            if (registrationEntity.getStatus() == RegistrationStatus.REGISTRATION_COMPLETE) {
                sendConfirmationEmail(client, registrationId);
            }
        } catch (Exception ex) {
            logger.severe(ex.toString());
        }
    }

    public static void handlePaymentAuthorization(String client, long registrationId) {
        Client.ensureValid(client);

        safeGet(client, registrationId);

        RegistrationEntity entity = safeGet(client, registrationId);
        entity.recordStateChange(RegistrationStatus.PAYMENT_AUTHORIZED);
        ofy(client).save().entity(entity).now();
    }

    public static void sendConfirmationEmail(String client, long registrationId) throws Exception {

        RegistrationProp registrationProp = Registration.safeGet(client, registrationId).toProp();
        ProgramProp programProp =
            Program.safeGet(client, registrationProp.programId).toProp(client);

        MailContentEntity mailContentEntity = MailContent.getByName(client,
            MailContent.ReservedMailContentName.RESERVED_REGISTRATION_CONFIRMATION.toString(), 0);
        ensureNotNull(mailContentEntity,
            "RESERVED_REGISTRATION_CONFIRMATION MailContent is not present");
        MailContentProp mailContentProp = mailContentEntity.toProp();

        EmailConfig emailConfig =
            Group.getEmailConfig(client, programProp.groupProp.groupId, User.SUPER_USER);

        SimpleDateFormat ftSrc = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat ftDst = new SimpleDateFormat("MMM d");

        String startDate = ftDst.format(ftSrc.parse(String.valueOf(programProp.startYYYYMMDD)));
        String endDate = ftDst.format(ftSrc.parse(String.valueOf(programProp.endYYYYMMDD)));

        String dates = startDate;
        if (!startDate.equalsIgnoreCase(endDate))
            dates = dates + " - " + endDate;

        String subject = "Registration - " + programProp.programTypeProp.displayName + ": " + dates;

        MailMap mailMap = new MailMap();
        mailMap.add(registrationProp.email, registrationProp.firstName, registrationProp.lastName);
        mailMap.add(MailMap.MergeVarID.PROGRAM_NAME, programProp.programTypeProp.displayName);
        mailMap.add(MailMap.MergeVarID.REGISTRATION_ID, registrationProp.registrationId + "");
        mailMap.add(MailMap.MergeVarID.EMAIL, registrationProp.email);
        mailMap.add(MailMap.MergeVarID.MOBILE_PHONE, registrationProp.mobilePhone);
        mailMap.add(MailMap.MergeVarID.HOME_PHONE, registrationProp.homePhone);
        mailMap.add(MailMap.MergeVarID.OFFICE_PHONE, registrationProp.officePhone);
        mailMap.add(MailMap.MergeVarID.DATES, dates);
        mailMap.add(MailMap.MergeVarID.VENUE,
            programProp.venueProp.displayName + " - " + programProp.venueProp.address);
        mailMap.add(MailMap.MergeVarID.TRANSACTION_ID,
            registrationProp.transactionId.equals("REFER2PAYPALTXNID") ?
                "" : registrationProp.transactionId);
        mailMap.add(MailMap.MergeVarID.INVOICE_NUMBER,
            Invoice.getInvoiceNo(programProp.programTypeProp.displayName,
                programProp.groupProp.displayName, registrationId));
        mailMap.add(MailMap.MergeVarID.AMOUNT, programProp.ccy + " " + registrationProp.amount);

        List<ProgramSession> programSessions = new ArrayList<>();

        for (String sessionTimings : programProp.batch1SessionTimings) {

            ProgramSession programSession = new ProgramSession();
            String session = sessionTimings.replaceAll("Z", "");
            programSession.mandatory = session.indexOf('M') >= 0;

            session = session.replaceAll("M", "");

            String[] timings = session.split(">");

            ftSrc = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm");
            SimpleDateFormat ftDay = new SimpleDateFormat("EEE d MMM");
            SimpleDateFormat ftTime = new SimpleDateFormat("h:mm a");

            Date dtStart = ftSrc.parse(String.valueOf(timings[0]));
            programSession.day = ftDay.format(dtStart);
            programSession.start_time = ftTime.format(dtStart);
            programSession.end_time = ftTime.format(ftSrc.parse(String.valueOf(timings[1])));

            programSessions.add(programSession);
        }

        mailMap.add(MailMap.MergeVarID.SESSIONS, programSessions);

        MailSendInput msi = new MailSendInput();
        msi.createMember = false;
        msi.groupId = programProp.groupProp.groupId;
        msi.isTransactionEmail = true;
        msi.mailContentId = mailContentProp.mailContentId;
        msi.senderEmail = emailConfig.contactEmail;
        msi.suppressIfAlreadySent = false;
        msi.overrideSubject = subject;

        Mail.send(client, msi, mailMap, User.SUPER_USER);
    }

    static void sortByFirstName(List<RegistrationProp> props) {
        Collections.sort(props, new Comparator<RegistrationProp>() {

            @Override
            public int compare(RegistrationProp o1, RegistrationProp o2) {
                if ((o1.firstName == null) || (o2.firstName == null))
                    return 0;

                return o1.firstName.compareTo(o2.firstName);
            }
        });
    }

    public enum RegistrationStatus {
        REQUEST_RECEIVED, PAYMENT_AUTHORIZATION_PENDING, ERROR_CREATING_PAYMENT_URL, PAYMENT_NOT_AUTHORIZED, PAYMENT_AUTHORIZED, PAYMENT_PENDING, REGISTRATION_COMPLETE, PAYPAL_ERROR, INVALID, CANCELLED
    }


    public static class ProgramSession {
        String day;
        String start_time;
        String end_time;
        boolean mandatory;
    }
}
