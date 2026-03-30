package backend.payment;

import java.util.Map;

public class PaymentMethodFactory {
    
    public static PaymentMethod createPaymentMethod(String type, Map<String, String> details) {
        switch (type.toLowerCase()) {
            case "creditcard":
                return PaymentMethod.CreditCard;
            case "debitcard":
                return PaymentMethod.DebitCard;
            case "paypal":
                return PaymentMethod.PayPal;
            case "banktransfer":
                return PaymentMethod.BankTransfer;
            default:
                throw new IllegalArgumentException("Unknown payment method type: " + type);
        }
    }
}
