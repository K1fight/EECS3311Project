package backend.payment.paypal;

import backend.payment.Payment;
import backend.payment.PaymentFactory;

public class PaypalPaymentFactory extends PaymentFactory {


    @Override
    public Payment createPayment(String account) {
        return new PaypalPayment(account);
    }
}
