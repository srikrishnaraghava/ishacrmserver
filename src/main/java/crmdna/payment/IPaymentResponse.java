package crmdna.payment;

import crmdna.common.Utils.PaypalErrorType;

import java.util.Map;

public interface IPaymentResponse {

    public String getInvoiceNo();

    public String handlePaypalError(PaypalErrorType error, Map<String, String> response);

    public String handlePaymentAuthorizationFailure(String invoiceNo);

    public String handleDoExpressCheckoutResponse(Map<String, String> response);

    public void handlePaymentAuthorization();
}
