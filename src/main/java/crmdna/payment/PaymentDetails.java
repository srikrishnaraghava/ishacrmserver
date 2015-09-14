package crmdna.payment;

public class PaymentDetails {
    public Gateway gateway;
    public String paymentUrl;
    public double cost;
    public String currency;
    public enum Gateway {
        PAYPAL
    }
}
