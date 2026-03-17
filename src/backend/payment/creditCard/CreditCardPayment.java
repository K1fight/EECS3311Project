package backend.payment.creditCard;

import backend.payment.Payment;

public class CreditCardPayment implements Payment {
    private String cardNumber;

    public CreditCardPayment(String cardNumber){
        this.cardNumber = cardNumber;
    }

    @Override
    public boolean pay() {
        System.out.printf("Using credit card to pay: %s%n",cardNumber);
        return true;
    }
}
