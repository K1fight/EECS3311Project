package backend.payment.bankTransfer;

import backend.payment.Payment;
import backend.payment.PaymentFactory;

public class BankTransferFactory extends PaymentFactory {
    @Override
    public Payment createPayment(String cardNumber) {
        return new BankTransfer(cardNumber);
    }

}

