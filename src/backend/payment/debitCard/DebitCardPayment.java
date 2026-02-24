package backend.payment.debitCard;

import backend.payment.Payment;

public class DebitCardPayment implements Payment {
    private String cardNumber;

    public DebitCardPayment(String cardNumber){
        this.cardNumber = cardNumber;
    }
    @Override
    public boolean pay(){
        System.out.printf("Using debit card to pay: %s%n",cardNumber);
        return true;
    }
}
