package backend.payment;

import java.util.ArrayList;
import java.util.List;

public class PaymentHistory {
    private List<PaymentTransaction> transactions = new ArrayList<>();

    public void addTransaction(PaymentTransaction transaction) {
        transactions.add(transaction);
    }
}
