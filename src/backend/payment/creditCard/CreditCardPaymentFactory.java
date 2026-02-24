package backend.payment.creditCard;

import backend.payment.Payment;
import backend.payment.PaymentFactory;

public class CreditCardPaymentFactory extends PaymentFactory {
    @Override
    public Payment createPayment(String cardNumber) {
       return new CreditCardPayment(cardNumber);
    }

}
