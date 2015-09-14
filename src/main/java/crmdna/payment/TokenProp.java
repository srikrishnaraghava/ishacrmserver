package crmdna.payment;

import crmdna.payment.Payment.PaymentType;

public class TokenProp {
    public String token;
    public String client;
    public PaymentType paymentType;
    public String successCallback;
    public String errorCallback;

    public long uniqueId;

    String paypalLogin;
    String paypalPwd;
    String paypalSecret;
    boolean paypalSandbox;
}
