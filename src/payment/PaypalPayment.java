package payment;

public class PaypalPayment implements Payment{
    private String account;

    public PaypalPayment(String account){
        this.account = account;
    }
    @Override
    public boolean pay(){
        System.out.printf("Using debit card to pay: %s%n",account);
        return true;
    }
}
