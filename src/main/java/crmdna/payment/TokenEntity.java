package crmdna.payment;

import com.googlecode.objectify.annotation.Cache;
import com.googlecode.objectify.annotation.Entity;
import com.googlecode.objectify.annotation.Id;
import crmdna.payment.Payment.PaymentType;

@Entity
@Cache
public class TokenEntity {
    @Id
    String token;
    String client;
    PaymentType paymentType;
    String successCallback;
    String errorCallback;

    long uniqueId;

    String paypalLogin;
    String paypalPwd;
    String paypalSecret;
    boolean paypalSandbox;

    TokenProp toProp() {
        TokenProp tokenProp = new TokenProp();
        tokenProp.token = token;
        tokenProp.client = client;
        tokenProp.paymentType = paymentType;
        tokenProp.successCallback = successCallback;
        tokenProp.errorCallback = errorCallback;

        tokenProp.uniqueId = uniqueId;

        tokenProp.paypalLogin = paypalLogin;
        tokenProp.paypalPwd = paypalPwd;
        tokenProp.paypalSecret = paypalSecret;
        tokenProp.paypalSandbox = paypalSandbox;

        return tokenProp;
    }
}
