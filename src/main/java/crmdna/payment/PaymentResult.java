package crmdna.payment;

public class PaymentResult {

    public boolean isPaymentAuthorized = true;
    public String invoiceNo;
    public String transactionId;
    public String ccy;
    public String amount;
    public boolean isPaymentPending = false;
    public String pendingReason;

    public long registrationId;
    public String client;
}
