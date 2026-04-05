package backend.payment.bankTransfer;

import backend.payment.Payment;

public class BankTransfer implements Payment {
    private String accountNumber;

    public BankTransfer(String accountNumber){
        this.accountNumber = accountNumber;
    }



    @Override
    public boolean pay() {
        System.out.printf("Using bank transfer to pay: %s%n", accountNumber);
        return true;
    }
}
