package backend.payment.debitCard;

import backend.payment.Payment;
import backend.payment.PaymentFactory;

public class DebitCardPaymentFactory extends PaymentFactory {

    @Override
    public Payment createPayment(String cardNumber) {
        return new DebitCardPayment(cardNumber);
    }
}
