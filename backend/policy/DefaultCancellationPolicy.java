package backend.policy;

import backend.booking.Booking;
import java.time.Duration;
import java.time.LocalDateTime;

public class DefaultCancellationPolicy implements CancellationPolicy {
    @Override
    public boolean canCancel(Booking booking, LocalDateTime cancellationTime) {
        // Allow cancellation any time before the booking start time.
        // Refund amount is determined separately by getRefundPercentage.
        return cancellationTime.isBefore(booking.getStartTime());
    }

    @Override
    public double getRefundPercentage(Booking booking, LocalDateTime cancellationTime) {
        long hoursBefore = Duration.between(cancellationTime, booking.getStartTime()).toHours();
        if (hoursBefore >= 48) return 1.0;
        else if (hoursBefore >= 24) return 0.5;
        else return 0.0;
    }
}
