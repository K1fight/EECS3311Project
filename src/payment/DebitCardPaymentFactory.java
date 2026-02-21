package payment;

public class DebitCardPaymentFactory extends PaymentFactory {

    @Override
    public Payment createPayment(String cardNumber) {
        return new DebitCardPayment(cardNumber);
    }
}
