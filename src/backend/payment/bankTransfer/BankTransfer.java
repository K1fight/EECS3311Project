package backend.payment.bankTransfer;

import backend.payment.Payment;

public class BankTransfer implements Payment {
    private String cardNumber;

    public BankTransfer(String cardNumber){
        this.cardNumber = cardNumber;
    }



    @Override
    public boolean pay() {
        System.out.printf("Using bank transfer to pay: %s%n",cardNumber);
        return true;
    }
}
