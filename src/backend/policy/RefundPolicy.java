package backend.policy;

import backend.booking.Booking;
import java.time.LocalDateTime;

public interface RefundPolicy {
    public void calculate();
    
    /**
     * Calculate refund amount based on cancellation policy
     * @param booking The booking to calculate refund for
     * @param cancellationTime The time of cancellation
     * @param originalAmount Original payment amount
     * @return Refund amount
     */
    default double calculateRefund(Booking booking, LocalDateTime cancellationTime, double originalAmount) {
        CancellationPolicy policy = SystemPolicy.getCancellationPolicy();
        if (policy.canCancel(booking, cancellationTime)) {
            double refundPercentage = policy.getRefundPercentage(booking, cancellationTime);
            return originalAmount * refundPercentage;
        }
        return 0.0;
    }
}
