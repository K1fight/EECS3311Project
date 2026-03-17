package backend.payment.paypal;

import backend.payment.Payment;

public class PaypalPayment implements Payment {
    private String account;

    public PaypalPayment(String account){
        this.account = account;
    }
    @Override
    public boolean pay(){
        System.out.printf("Using PayPal to pay: %s%n", account);
        return true;
    }
}
