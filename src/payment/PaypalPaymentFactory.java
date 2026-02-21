package payment;

public class PaypalPaymentFactory extends PaymentFactory{


    @Override
    public Payment createPayment(String account) {
        return new PaypalPayment(account);
    }
}
