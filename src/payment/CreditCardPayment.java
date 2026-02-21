package payment;

public class CreditCardPayment implements Payment{
    private String cardNumber;

    public CreditCardPayment(String cardNumber){
        this.cardNumber = cardNumber;
    }

    @Override
    public boolean pay() {
        System.out.printf("Using debit card to pay: %s%n",cardNumber);
        return true;
    }
}
