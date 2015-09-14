package crmdna.sessionpass;

import com.google.appengine.api.utils.SystemProperty;
import crmdna.common.Utils;
import crmdna.common.Utils.PaypalErrorType;
import crmdna.payment.IPaymentResponse;
import crmdna.payment.TokenProp;
import crmdna.registration.Registration;
import crmdna.registration.Registration.RegistrationStatus;
import crmdna.registration.RegistrationEntity;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import static crmdna.common.OfyService.ofy;

public class PaymentResponseSessionPassImpl implements IPaymentResponse {

    protected TokenProp tokenProp;
    protected Logger LOGGER;

    public PaymentResponseSessionPassImpl(TokenProp tokenProp) {
        this.tokenProp = tokenProp;
        LOGGER = Logger.getLogger(PaymentResponseSessionPassImpl.class.getName());
    }

    public String getInvoiceNo() {
        String invoiceNo = "Subscription";
        String appSfx = SystemProperty.applicationId.get().replace("ishacrmserver", "") + "-";

        if (appSfx.equals("-") ) {
        invoiceNo += appSfx;
        } else {
            invoiceNo += "-" + (new Date()).getTime() + "-" + appSfx;
        }

        invoiceNo += tokenProp.uniqueId;

        LOGGER.info(invoiceNo);
        return invoiceNo;
    }

    public String handlePaypalError(PaypalErrorType error, Map<String, String> response) {

        Map<String, Object> map = new HashMap<>();
        map.put("status", PaypalErrorType.PAYPAL_GET_EXPRESS_CHECKOUT_FAILURE);

        // Token may not be created fully in some error cases
        if (tokenProp.token != null)
            map.put("token", tokenProp.token);

        map.put("L_ERRORCODE0", response.get("L_ERRORCODE0"));
        map.put("L_SEVERITYCODE0", response.get("L_SEVERITYCODE0"));
        map.put("L_SHORTMESSAGE0", response.get("L_SHORTMESSAGE0"));

        String invoiceNo = response.get("INVNUM");
        if (invoiceNo != null) {
            map.put("invoiceNo", invoiceNo);
        }

        return Utils.getUrl(tokenProp.errorCallback, map);
    }

    public String handlePaymentAuthorizationFailure(String invoiceNo) {

        RegistrationEntity entity = Registration.safeGet(tokenProp.client, tokenProp.uniqueId);
        Map<String, Object> map = new HashMap<>();

        map.put("status", RegistrationStatus.PAYMENT_NOT_AUTHORIZED);
        map.put("invoiceNo", invoiceNo);
        map.put("sessionPassId", new Long(tokenProp.uniqueId));

        String redirectUrl = Utils.getUrl(tokenProp.errorCallback, map);
        ofy(tokenProp.client).save().entity(entity);

        return redirectUrl;
    }

    public void handlePaymentAuthorization() {

    }

    public String handleDoExpressCheckoutResponse(Map<String, String> response) {

        String transactionId = response.get("TRANSACTIONID");
        String amount = response.get("AMT");
        String ccy = response.get("CURRENCYCODE");
        String pendingReason = response.get("PENDINGREASON");

        Map<String, Object> map = new HashMap<>();
        map.put("transactionId", transactionId);
        map.put("amount", amount);
        map.put("ccy", ccy);

        boolean isPaymentPending = false;
        if (response.get("PAYMENTSTATUS").equals("Pending")) {
            isPaymentPending = true;
            map.put("status", RegistrationStatus.PAYMENT_PENDING.toString());
            map.put("pendingReason", pendingReason);
        } else
            map.put("status", RegistrationStatus.REGISTRATION_COMPLETE.toString());

        map.put("programType", "Subscription_" + tokenProp.uniqueId);

        String redirectUrl = Utils.getUrl(tokenProp.successCallback, map);
        Utils.ensureValidUrl(redirectUrl);

        try {
            if (!isPaymentPending) {
                SessionPass.allocatePasses(tokenProp.client, tokenProp.uniqueId, transactionId);
            }
        } catch(Exception ex) {
            Logger LOGGER = Logger.getLogger(SessionPass.class.getName());
            LOGGER.severe(ex.toString());
        }

        return redirectUrl;
    }
}
