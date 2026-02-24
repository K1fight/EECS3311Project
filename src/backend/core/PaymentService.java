package backend.core;

import backend.booking.Booking;
import backend.payment.*;
import backend.payment.bankTransfer.BankTransferFactory;
import backend.payment.creditCard.CreditCardPaymentFactory;
import backend.payment.debitCard.DebitCardPaymentFactory;
import backend.payment.paypal.PaypalPaymentFactory;

import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Payment Service: Handles payment requests, validates payment information, simulates payment delays, updates booking status, and records transactions.
 */
public class PaymentService {
    private PaymentHistory globalPaymentHistory = new PaymentHistory(); // Or independent for each client

    public boolean processPayment(Booking booking, PaymentMethod method, Map<String, String> details) {
        if (!validatePaymentDetails(method, details)) {
            System.out.println("Payment validation failed.");
            return false;
        }

        Payment payment = createPayment(method, details);
        if (payment == null) {
            return false;
        }

        // 3. Simulate payment processing delay (2-3 seconds)
        try {
            System.out.println("Processing payment...");
            TimeUnit.SECONDS.sleep(2); // Simulate delay
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return false;
        }

        // 4. Execute payment
        boolean success = payment.pay();

        // 5. Generate masked information for recording
        String maskedDetail = maskDetail(method, details);

        // 6. Create transaction record
        PaymentTransaction transaction = new PaymentTransaction(
                booking.getService().getBasePrice(),
                method,
                maskedDetail
        );

        if (success) {
            // Payment successful: Update booking status, record successful transaction
            booking.markPaid();  // Assuming Booking class has a markPaid() method to transition state to Paid
            transaction.succeed();
            System.out.println("Payment successful. Transaction ID: " + transaction.getTransactionId());
        } else {
            // Payment failed
            transaction.fail("Payment declined by processor");
            System.out.println("Payment failed.");
        }

        // 7. Save transaction record (e.g., store in global history, or associate with client)
        globalPaymentHistory.addTransaction(transaction);

        return success;
    }

    /**
     * Validate detailed information based on payment method
     */
    private boolean validatePaymentDetails(PaymentMethod method, Map<String, String> details) {
        switch (method) {
            case Credit:
            case Debit:
                String cardNum = details.get("cardNumber");
                String expiry = details.get("expiry");
                String cvv = details.get("cvv");
                return cardNum != null && cardNum.matches("\\d{16}") &&
                        expiry != null && expiry.matches("(0[1-9]|1[0-2])/\\d{2}") &&
                        cvv != null && cvv.matches("\\d{3,4}");
            case Paypal:
                String email = details.get("email");
                return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
            case BankTransfer:
                String account = details.get("accountNumber");
                String routing = details.get("routingNumber");
                return account != null && account.matches("\\d{10,12}") &&
                        routing != null && routing.matches("\\d{9}");
            default:
                return false;
        }
    }

    /**
     * Create payment object using the corresponding factory (pass only primary identifier, ignore secondary fields to match existing constructor)
     */
    private Payment createPayment(PaymentMethod method, Map<String, String> details) {
        String primaryDetail = null;
        PaymentFactory factory = null;

        switch (method) {
            case Credit:
                primaryDetail = details.get("cardNumber");
                factory = new CreditCardPaymentFactory();
                break;
            case Debit:
                primaryDetail = details.get("cardNumber");
                factory = new DebitCardPaymentFactory();
                break;
            case Paypal:
                primaryDetail = details.get("email");
                factory = new PaypalPaymentFactory();
                break;
            case BankTransfer:
                primaryDetail = details.get("accountNumber");
                factory = new BankTransferFactory();
                break;
        }

        if (factory == null || primaryDetail == null) {
            return null;
        }
        return factory.createPayment(primaryDetail);
    }

    /**
     * Generate masked payment details (for display and recording)
     */
    private String maskDetail(PaymentMethod method, Map<String, String> details) {
        switch (method) {
            case Credit:
            case Debit:
                String card = details.get("cardNumber");
                return "**** **** **** " + card.substring(card.length() - 4);
            case Paypal:
                String email = details.get("email");
                return email.replaceAll("(?<=.{3}).(?=[^@]*@)", "*");
            case BankTransfer:
                String account = details.get("accountNumber");
                return "Account: ****" + account.substring(account.length() - 4);
            default:
                return "";
        }
    }
}
