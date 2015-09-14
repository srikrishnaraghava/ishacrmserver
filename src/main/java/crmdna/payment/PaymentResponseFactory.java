package crmdna.payment;

import crmdna.payment.Payment.PaymentType;
import crmdna.registration.PaymentResponseRegistrationImpl;
import crmdna.sessionpass.PaymentResponseSessionPassImpl;

public class PaymentResponseFactory {
    public static IPaymentResponse getImpl(TokenProp tokenProp) {

        if (tokenProp.paymentType == PaymentType.PROGRAM_REGISTRATION)
            return new PaymentResponseRegistrationImpl(tokenProp);
        else if (tokenProp.paymentType == PaymentType.SESSION_PASS)
            return new PaymentResponseSessionPassImpl(tokenProp);

        return new PaymentResponseDefaultImpl(tokenProp);
    }
}
