package backend.payment;

import java.time.LocalDateTime;
import java.util.UUID;

public class PaymentTransaction {
    private UUID transactionId;
    private double amount;
    private PaymentMethod paymentMethod;
    private String paymentDetailMasked;
    private LocalDateTime timestamp;
    private PaymentStatus status;
    private String failureReason;

    public PaymentTransaction(double amount, PaymentMethod method, String detailMasked) {
        this.transactionId = UUID.randomUUID();
        this.amount = amount;
        this.paymentMethod = method;
        this.paymentDetailMasked = detailMasked;
        this.timestamp = LocalDateTime.now();
        this.status = PaymentStatus.PENDING;
    }


    public void succeed() {
        this.status = PaymentStatus.SUCCESS;
    }


    public void fail(String reason) {
        this.status = PaymentStatus.FAILED;
        this.failureReason = reason;
    }


    public UUID getTransactionId() { return transactionId; }
    public PaymentStatus getStatus() { return status; }
}