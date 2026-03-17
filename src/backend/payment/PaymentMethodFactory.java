package backend.payment;

import java.util.Map;

public class PaymentMethodFactory {
    
    public static PaymentMethod createPaymentMethod(String type, Map<String, String> details) {
        switch (type.toLowerCase()) {
            case "credit":
                return PaymentMethod.Credit;
            case "debit":
                return PaymentMethod.Debit;
            case "paypal":
                return PaymentMethod.Paypal;
            case "banktransfer":
                return PaymentMethod.BankTransfer;
            default:
                throw new IllegalArgumentException("Unknown payment method type: " + type);
        }
    }
}
