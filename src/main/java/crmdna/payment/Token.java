package crmdna.payment;

import crmdna.client.Client;
import crmdna.common.Utils;
import crmdna.common.api.APIException;
import crmdna.common.api.APIResponse.Status;

import static crmdna.common.OfyService.ofyCrmDna;

public class Token {

    static void save(TokenProp tokenProp) {
        Client.ensureValid(tokenProp.client);
        ensureValidToken(tokenProp.token);

        Utils.ensureNotNullOrEmpty(tokenProp.paypalLogin, "Paypal login is null or empty");
        Utils.ensureNotNullOrEmpty(tokenProp.paypalPwd, "Paypal password is null or empty");
        Utils.ensureNotNullOrEmpty(tokenProp.paypalSecret, "Paypal secret is null or empty");
        Utils.ensureNonZero(tokenProp.uniqueId, "uniqueId is zero");

        TokenEntity tokenEntity = new TokenEntity();
        tokenEntity.token = tokenProp.token;
        tokenEntity.paymentType = tokenProp.paymentType;
        tokenEntity.uniqueId = tokenProp.uniqueId;
        tokenEntity.client = tokenProp.client;
        tokenEntity.paypalLogin = tokenProp.paypalLogin;
        tokenEntity.paypalPwd = tokenProp.paypalPwd;
        tokenEntity.paypalSecret = tokenProp.paypalSecret;
        tokenEntity.paypalSandbox = tokenProp.paypalSandbox;
        tokenEntity.successCallback = tokenProp.successCallback;
        tokenEntity.errorCallback = tokenProp.errorCallback;

        ofyCrmDna().save().entity(tokenEntity).now();
    }

    static TokenProp safeGet(String token) {

        ensureValidToken(token);
        TokenEntity tokenEntity = ofyCrmDna().load().type(TokenEntity.class).id(token).now();

        if (token == null)
            throw new APIException().status(Status.ERROR_RESOURCE_NOT_FOUND).message(
                    "Cannot find paypal token [" + token + "] in CRMDNA datastore");

        return tokenEntity.toProp();
    }

    static void ensureValidToken(String token) {
        if ((token == null) || token.equals("")) {
            Utils.throwIncorrectSpecException("Invalid token [" + token + "]");
        }
    }
}
