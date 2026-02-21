package payment;

public class CreditCardPaymentFactory extends PaymentFactory{
    @Override
    public Payment createPayment(String cardNumber) {
       return new CreditCardPayment(cardNumber);
    }

}
