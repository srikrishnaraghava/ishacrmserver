package crmdna.payment;

import crmdna.common.Utils.PaypalErrorType;

import java.util.Map;
import java.util.logging.Logger;

public class PaymentResponseDefaultImpl implements IPaymentResponse {

    protected TokenProp tokenProp;

    PaymentResponseDefaultImpl(TokenProp tokenProp) {
        this.tokenProp = tokenProp;
    }

    public String getInvoiceNo() {
        Logger LOGGER = Logger.getLogger(PaymentResponseDefaultImpl.class.getName());
        LOGGER.info("");
        return "";
    }

    public String handlePaypalError(PaypalErrorType error, Map<String, String> response) {
        Logger LOGGER = Logger.getLogger(PaymentResponseDefaultImpl.class.getName());
        LOGGER.info("");
        return "";
    }

    public String handlePaymentAuthorizationFailure(String invoiceNo) {
        Logger LOGGER = Logger.getLogger(PaymentResponseDefaultImpl.class.getName());
        LOGGER.info("");
        return "";
    }

    public String handleDoExpressCheckoutResponse(Map<String, String> response) {
        Logger LOGGER = Logger.getLogger(PaymentResponseDefaultImpl.class.getName());
        LOGGER.info("");
        return "";
    }

    public void handlePaymentAuthorization() {
        Logger LOGGER = Logger.getLogger(PaymentResponseDefaultImpl.class.getName());
        LOGGER.info("");

    }
}
