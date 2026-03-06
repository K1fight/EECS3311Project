package backend.policy;

import backend.booking.Booking;
import java.time.Duration;
import java.time.LocalDateTime;

// Flexible cancellation policy with customizable refund rules
public class FlexibleCancellationPolicy implements CancellationPolicy {
    private int fullRefundHoursBefore = 48;
    private int partialRefundHoursBefore = 24;
    private double partialRefundPercentage = 0.5;

    public FlexibleCancellationPolicy() {}

    public FlexibleCancellationPolicy(int fullRefundHours, int partialRefundHours, double partialPercentage) {
        this.fullRefundHoursBefore = fullRefundHours;
        this.partialRefundHoursBefore = partialRefundHours;
        this.partialRefundPercentage = partialPercentage;
    }

    @Override
    public boolean canCancel(Booking booking, LocalDateTime cancellationTime) {
        return cancellationTime.isBefore(booking.getStartTime());
    }

    @Override
    public double getRefundPercentage(Booking booking, LocalDateTime cancellationTime) {
        long hoursBefore = Duration.between(cancellationTime, booking.getStartTime()).toHours();
        
        if (hoursBefore >= fullRefundHoursBefore) {
            return 1.0; // Full refund
        } else if (hoursBefore >= partialRefundHoursBefore) {
            return partialRefundPercentage; // Partial refund
        } else {
            return 0.0; // No refund
        }
    }

    public void setFullRefundHoursBefore(int hours) {
        this.fullRefundHoursBefore = hours;
    }

    public void setPartialRefundHoursBefore(int hours) {
        this.partialRefundHoursBefore = hours;
    }

    public void setPartialRefundPercentage(double percentage) {
        this.partialRefundPercentage = percentage;
    }
}
