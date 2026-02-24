package backend.policy;

import backend.booking.Booking;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

public class DefaultCancellationPolicy implements CancellationPolicy {
    @Override
    public boolean canCancel(Booking booking, LocalDateTime cancellationTime) {
        // Allow cancellation 24 hours before start time
        return cancellationTime.isBefore(booking.getStartTime().minusHours(24));
    }

    @Override
    public double getRefundPercentage(Booking booking, LocalDateTime cancellationTime) {
        long hoursBefore = ChronoUnit.HOURS.between(cancellationTime, booking.getStartTime());
        if (hoursBefore >= 48) return 1.0;
        else if (hoursBefore >= 24) return 0.5;
        else return 0.0;
    }
}
