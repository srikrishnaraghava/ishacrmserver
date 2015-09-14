package crmdna.payment;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class PaymentServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    public void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws IOException {

        // 1. get contactdetails like name email, program id

        // if already registered throw error message. and send email to support
        // contact for the group

        // get success url and failure url from request

        // create registrationentity with fields
        // registrationid, first name, last name, email, phoneno, gender,
        // timestamps, statuses
        // (timestamps and statuses are lists of date and string respectively)
        // status = REGN_REQUEST_RECEIVED
        // memberId should be populated. (add registeredProgramIds as a set in
        // memberEntity)

        // 2. call getpaymenturl. getpaymenturl will internally
        // call setexpresscheckout and get the token. from token url should be
        // created
        // status = PAYPAL_TOKEN_CREATED
        // add fields paypalToken, successUrl and cancelUrl in
        // registrationentity

        // 3. redirect to paypalpaymentUrl. this will let end user to input
        // credit card
        // details and click on pay now.

    }
}
