package crmdna.registration;

import com.googlecode.objectify.VoidWork;
import crmdna.common.Utils;
import crmdna.common.Utils.PaypalErrorType;
import crmdna.payment.IPaymentResponse;
import crmdna.payment.Payment;
import crmdna.payment.TokenProp;
import crmdna.program.Program;
import crmdna.program.ProgramEntity;
import crmdna.registration.Registration.RegistrationStatus;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static crmdna.common.OfyService.ofy;

public class PaymentResponseRegistrationImpl implements IPaymentResponse {

    protected TokenProp tokenProp;

    public PaymentResponseRegistrationImpl(TokenProp tokenProp) {
        this.tokenProp = tokenProp;
    }

    private static Map<String, Object> getProgramDetailsAsMap(String client, long programId) {

        ProgramEntity programEntity = Program.get(client, programId);
        if (null == programEntity) {
            Logger logger = Logger.getLogger(Payment.class.getName());
            logger.severe("ProgramId [" + programId + "] not found");
            return new HashMap<>();
        }

        return programEntity.toProp(client).asMap();
    }

    public String getInvoiceNo() {
        RegistrationEntity entity = Registration.safeGet(tokenProp.client, tokenProp.uniqueId);
        ProgramEntity programEntity = Program.safeGet(tokenProp.client, entity.programId);

        return Invoice.getInvoiceNo(programEntity.toProp(tokenProp.client).programTypeProp.displayName,
                programEntity.toProp(tokenProp.client).groupProp.displayName, tokenProp.uniqueId);
    }

    public String handlePaypalError(PaypalErrorType error, Map<String, String> response) {

        RegistrationEntity entity = Registration.safeGet(tokenProp.client, tokenProp.uniqueId);

        entity.paypalErrorType = error;
        entity.L_ERRORCODE0 = response.get("L_ERRORCODE0");
        entity.L_LONGMESSAGE0 = response.get("L_LONGMESSAGE0");
        entity.L_SEVERITYCODE0 = response.get("L_SEVERITYCODE0");
        entity.L_SHORTMESSAGE0 = response.get("L_SHORTMESSAGE0");

        Map<String, Object> map = new HashMap<>();
        map.put("status", PaypalErrorType.PAYPAL_GET_EXPRESS_CHECKOUT_FAILURE);

        // Token may not be created fully in some error cases
        if (tokenProp.token != null)
            map.put("token", tokenProp.token);

        map.put("L_ERRORCODE0", entity.L_ERRORCODE0);
        map.put("L_SEVERITYCODE0", entity.L_SEVERITYCODE0);
        map.put("L_SHORTMESSAGE0", entity.L_SHORTMESSAGE0);

        String invoiceNo = response.get("INVNUM");
        if (invoiceNo != null) {
            map.put("invoiceNo", invoiceNo);
        }

        map.putAll(getProgramDetailsAsMap(tokenProp.client, entity.programId));

        entity.redirectUrl = Utils.getUrl(tokenProp.errorCallback, map);
        entity.recordStateChange(RegistrationStatus.PAYPAL_ERROR);
        ofy(tokenProp.client).save().entity(entity).now();

        return entity.redirectUrl;
    }

    public String handlePaymentAuthorizationFailure(String invoiceNo) {

        RegistrationEntity entity = Registration.safeGet(tokenProp.client, tokenProp.uniqueId);
        Map<String, Object> map = new HashMap<>();

        map.put("status", RegistrationStatus.PAYMENT_NOT_AUTHORIZED);
        map.put("invoiceNo", invoiceNo);
        map.put("registrationId", tokenProp.uniqueId);
        map.putAll(getProgramDetailsAsMap(tokenProp.client, entity.programId));

        entity.recordStateChange(RegistrationStatus.PAYMENT_NOT_AUTHORIZED);
        entity.redirectUrl = Utils.getUrl(tokenProp.errorCallback, map);
        ofy(tokenProp.client).save().entity(entity);

        return entity.redirectUrl;
    }

    public void handlePaymentAuthorization() {
        RegistrationEntity entity = Registration.safeGet(tokenProp.client, tokenProp.uniqueId);
        entity.recordStateChange(RegistrationStatus.PAYMENT_AUTHORIZED);
        ofy(tokenProp.client).save().entity(entity).now();
    }

    public String handleDoExpressCheckoutResponse(Map<String, String> response) {

        RegistrationEntity entity = Registration.safeGet(tokenProp.client, tokenProp.uniqueId);

        String transactionId = response.get("TRANSACTIONID");
        String amount = response.get("AMT");
        String ccy = response.get("CURRENCYCODE");
        String pendingReason = response.get("PENDINGREASON");

        // do express checkout is successful, payment could still be pending
        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", transactionId);
        map.put("amount", amount);
        map.put("ccy", ccy);
        map.put("email", entity.email);
        map.putAll(getProgramDetailsAsMap(tokenProp.client, entity.programId));

        boolean isPaymentPending = false;
        if (response.get("PAYMENTSTATUS").equals("Pending")) {
            isPaymentPending = true;
            map.put("status", RegistrationStatus.PAYMENT_PENDING.toString());
            map.put("pendingReason", response.get("PENDINGREASON"));
        } else
            map.put("status", RegistrationStatus.REGISTRATION_COMPLETE.toString());

        String redirectUrl = Utils.getUrl(tokenProp.successCallback, map);
        Utils.ensureValidUrl(redirectUrl);

        handleRegistrationSuccess(amount, ccy, transactionId, isPaymentPending, pendingReason,
                redirectUrl);

        RegistrationEntity registrationEntity =
                Registration.safeGet(tokenProp.client, tokenProp.uniqueId);
        try {
            if (registrationEntity.getStatus() == RegistrationStatus.REGISTRATION_COMPLETE) {
                Registration.sendConfirmationEmail(tokenProp.client, tokenProp.uniqueId);
            }
        } catch (Exception ex) {
            Logger logger = Logger.getLogger(Registration.class.getName());
            logger.severe(ex.toString());
        }

        return redirectUrl;
    }

    private void handleRegistrationSuccess(final String amount, final String ccy,
                                           final String transactionId, final boolean isPaymentPending, final String pendingReason,
                                           final String redirectUrl) {

        final Logger logger = Logger.getLogger(Registration.class.getName());

        ofy(tokenProp.client).transact(new VoidWork() {

            @Override
            public void vrun() {
                RegistrationEntity registrationEntity =
                        Registration.safeGet(tokenProp.client, tokenProp.uniqueId);

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
                transactionEntity.registrationId = tokenProp.uniqueId;

                ofy(tokenProp.client).save().entities(registrationEntity, transactionEntity).now();

                logger.info("Registration ID [" + tokenProp.uniqueId + "]" + ", Status [" + status
                        + "], transactionId [" + transactionId + "], Redirect URL [" + redirectUrl + "]");
            }
        });
    }

}
