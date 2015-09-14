package crmdna.payment;

import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.Utils.PaypalErrorType;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;
import crmdna.group.PaypalApiCredentialsProp;
import paypalnvp.core.PayPal;
import paypalnvp.core.PayPal.Environment;
import paypalnvp.fields.PaymentAction;
import paypalnvp.fields.PaymentItem;
import paypalnvp.profile.BaseProfile;
import paypalnvp.profile.Profile;
import paypalnvp.request.DoExpressCheckoutPayment;
import paypalnvp.request.GetExpressCheckoutDetails;
import paypalnvp.request.SetExpressCheckout;

import java.util.Map;
import java.util.logging.Logger;

public class Payment {

    public static String setExpressCheckoutAndGetPaymentUrl(String client, PaymentType paymentType,
                                                            String email, String paypalPaymentName, long uniqueId, PaypalApiCredentialsProp paypalProp,
                                                            double cost, String currency, String rootUrl, String successCallback, String errorCallback) {

        Client.ensureValid(client);
        Utils.ensureValidEmail(email);

        ensureValidPaypalApiCredentials(paypalProp.login, paypalProp.pwd, paypalProp.secret);

        if ((null == paypalPaymentName) || paypalPaymentName.equals(""))
            Utils.throwIncorrectSpecException("Paypal payment name not specified");

        TokenProp tokenProp = new TokenProp();
        tokenProp.client = client;
        tokenProp.paypalLogin = paypalProp.login;
        tokenProp.paypalPwd = paypalProp.pwd;
        tokenProp.paypalSecret = paypalProp.secret;
        tokenProp.paypalSandbox = paypalProp.sandbox;
        tokenProp.paymentType = paymentType;
        tokenProp.successCallback = successCallback;
        tokenProp.errorCallback = errorCallback;
        tokenProp.uniqueId = uniqueId;
        tokenProp.token = null;

        IPaymentResponse paymentResponse = PaymentResponseFactory.getImpl(tokenProp);
        PaymentItem paymentItem = new PaymentItem();

        paymentItem.setAmount(Utils.asCurrencyString(cost));
        paymentItem.setName(paypalPaymentName);

        PaymentItem[] paymentItems = {paymentItem};
        paypalnvp.fields.Payment payment = new paypalnvp.fields.Payment(paymentItems);
        payment.setInvoiceNumber(paymentResponse.getInvoiceNo());
        // set client as custom field
        payment.setCustomField(client);
        payment.setCurrency(getPaypalCurrencyEnum(currency));

        String returnUrl = rootUrl + "/doExpressCheckout";
        String cancelUrl = returnUrl;
        SetExpressCheckout setExpressCheckout = new SetExpressCheckout(payment, returnUrl, cancelUrl);
        setExpressCheckout.setEmail(email);

        PayPal payPal = getPayPalConnection(tokenProp);
        payPal.setResponse(setExpressCheckout);

        Map<String, String> response = setExpressCheckout.getNVPResponse();

        Logger logger = Logger.getLogger(Payment.class.getName());
        logger.info("SetExpressCheckout response: " + response.toString());

        if (!"Success".equals(response.get("ACK"))) {

            paymentResponse.handlePaypalError(PaypalErrorType.PAYPAL_SET_EXPRESS_CHECKOUT_FAILURE,
                    response);

            String errorMessage =
                    getErrMessageFromNVPResponse(PaypalErrorType.PAYPAL_SET_EXPRESS_CHECKOUT_FAILURE,
                            response);
            logger.warning(errorMessage);
            throw new APIException().status(Status.PAYPAL_ERROR).message(errorMessage);
        }

        // set express checkout is successful
        tokenProp.token = response.get("TOKEN");
        if ((tokenProp.token == null) || tokenProp.token.equals("")) {
            throw new APIException().status(Status.PAYPAL_ERROR).message(
                    "Invalid token [" + tokenProp.token + "] after call to setExpressCheckout");
        }

        Token.save(tokenProp);

        String paymentUrl = getPaypalPaymentUrlFromToken(tokenProp.token, tokenProp.paypalSandbox);
        Utils.ensureValidUrl(paymentUrl);
        return paymentUrl;
    }

    static String doExpressCheckout(String token, String payerId, String rootUrl) {

        if ((token == null) || token.equals(""))
            throw new APIException().status(Status.PAYPAL_ERROR).message("Paypal token in null");

        TokenProp tokenProp = Token.safeGet(token);

        String client = tokenProp.client;

        IPaymentResponse paymentResponse = PaymentResponseFactory.getImpl(tokenProp);

        PayPal payPal = getPayPalConnection(tokenProp);

        GetExpressCheckoutDetails checkoutDetails = new GetExpressCheckoutDetails(token);
        payPal.setResponse(checkoutDetails);
        Map<String, String> response = checkoutDetails.getNVPResponse();

        Logger logger = Logger.getLogger(Payment.class.getName());
        logger.info("GetExpressCheckoutDetails response: " + response.toString());

        if (!"Success".equals(response.get("ACK"))) {
            logger.severe(getErrMessageFromNVPResponse(
                    PaypalErrorType.PAYPAL_GET_EXPRESS_CHECKOUT_FAILURE, response));

            return paymentResponse.handlePaypalError(PaypalErrorType.PAYPAL_GET_EXPRESS_CHECKOUT_FAILURE,
                    response);
        }

        logger.info("GetExpressCheckoutDetails success for token [" + token + "]");

        String invoiceNo = response.get("INVNUM");
        Client.safeGet(client);

        if ((payerId == null) || payerId.equals("")) {
            return paymentResponse.handlePaymentAuthorizationFailure(invoiceNo);
        }

        logger.info(
            "GetExpressCheckoutDetails success for token [" + token + "], INVNUM = [" + invoiceNo
                + "], payerId = [" + payerId + "]");

        paymentResponse.handlePaymentAuthorization();

        PaymentItem item = new PaymentItem();
        item.setAmount(response.get("AMT"));

        PaymentItem[] items = {item};
        paypalnvp.fields.Payment payment = new paypalnvp.fields.Payment(items);
        payment.setCurrency(getPaypalCurrencyEnum(response.get("CURRENCYCODE")));

        payment.setNotifyUrl(rootUrl + "/payment/ipn");

        DoExpressCheckoutPayment doExpressCheckout =
                new DoExpressCheckoutPayment(payment, token, PaymentAction.SALE, payerId);
        payPal.setResponse(doExpressCheckout);
        response = doExpressCheckout.getNVPResponse();
        logger.info("doExpressCheckout: " + response.toString());

        if (!"Success".equals(response.get("ACK"))) {
            logger.severe(getErrMessageFromNVPResponse(
                    PaypalErrorType.PAYPAL_DO_EXPRESS_CHECKOUT_FAILURE, response));

            return paymentResponse.handlePaypalError(PaypalErrorType.PAYPAL_DO_EXPRESS_CHECKOUT_FAILURE,
                    response);
        }

        return paymentResponse.handleDoExpressCheckoutResponse(response);
    }

    public static String getTransactionId(String client, long registrationId, String login) {

    /*
     * RegistrationEntity r = Registration.safeGet(client, registrationId);
     *
     * String token = "EC-8NC7633742189682L"; // TokenProp tokenProp = Token.safeGet(token);
     * ProgramProp p = Program.safeGet(client, r.toProp().programId).toProp();
     * GetExpressCheckoutDetails checkoutDetails = new GetExpressCheckoutDetails(token);
     * PaypalApiCredentialsProp api = Group.getPaypalApiCredentials(client, p.groupProp.groupId,
     * login);
     *
     * PayPal payPal = getPayPalConnection(api.login, api.pwd, api.secret, false);
     * payPal.setResponse(checkoutDetails); Map<String, String> response =
     * checkoutDetails.getNVPResponse();
     *
     * Logger logger = Logger.getLogger(Payment.class.getName());
     * logger.info("Get express checkout response: " + response.toString());
     *
     * return response.get("TRANSACTIONID");
     */
        return "";
    }

    static String getPaypalPaymentUrlFromToken(String token, boolean sandbox) {

        StringBuilder builder = new StringBuilder();
        builder.append("https://www.");
        if (sandbox)
            builder.append("sandbox.");

        builder.append("paypal.com/cgi-bin/webscr?cmd=_express-checkout&token=");
        builder.append(token);

        return builder.toString();
    }

    static PayPal getPayPalConnection(TokenProp tokenProp) {

        ensureValidPaypalApiCredentials(tokenProp.paypalLogin, tokenProp.paypalPwd,
                tokenProp.paypalSecret);

        Environment env = Environment.SANDBOX;
        if (!tokenProp.paypalSandbox)
            env = Environment.LIVE;

        Profile user =
                new BaseProfile.Builder(tokenProp.paypalLogin, tokenProp.paypalPwd).signature(
                        tokenProp.paypalSecret).build();

        PayPal paypal = new PayPal(user, env);

        System.out.println(
            "getPayPalConnection: " + tokenProp.paypalLogin + "|" + tokenProp.paypalPwd + "|"
                + tokenProp.paypalSecret + "|" + env.toString());

        return paypal;
    }

    static paypalnvp.fields.Currency getPaypalCurrencyEnum(String currency) {
        if (currency == null)
            Utils.throwIncorrectSpecException("Currency is null");

        currency = currency.toUpperCase();

        if (currency.equals("SGD"))
            return paypalnvp.fields.Currency.SGD;

        if (currency.equals("AUD"))
            return paypalnvp.fields.Currency.AUD;

        if (currency.equals("USD"))
            return paypalnvp.fields.Currency.USD;

        if (currency.equals("GBP"))
            return paypalnvp.fields.Currency.GBP;

        if (currency.equals("MYR"))
            return paypalnvp.fields.Currency.MYR;

        Utils.throwIncorrectSpecException("Payment not supported for currency [" + currency + "]");

        return null; // should never reach here
    }

    static void ensureValidPaypalApiCredentials(String login, String pwd, String secret) {

        if ((null == login) || login.equals(""))
            Utils.throwIncorrectSpecException("Paypal login is not specified");

        if ((null == pwd) || pwd.equals(""))
            Utils.throwIncorrectSpecException("Paypal password is not specified");

        if ((null == secret) || secret.equals(""))
            Utils.throwIncorrectSpecException("Paypal secret is not specified");
    }

    private static String getErrMessageFromNVPResponse(PaypalErrorType paypalErrorType,
                                                       Map<String, String> response) {
        return paypalErrorType + " - " + "L_SEVERITYCODE0: " + response.get("L_SEVERITYCODE0")
                        + ", L_ERRORCODE0: " + response.get("L_ERRORCODE0") + ", L_SHORTMESSAGE0: "
                        + response.get("L_SHORTMESSAGE0") + ", L_LONGMESSAGE0: "
                        + response.get("L_LONGMESSAGE0");
    }

    public enum PaymentType {
        PROGRAM_REGISTRATION, SESSION_PASS
    }

}
