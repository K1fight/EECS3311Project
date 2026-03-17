package backend.policy;

import backend.booking.Booking;
import java.time.LocalDateTime;

// Strategy pattern: Cancellation policy interface
public interface CancellationPolicy {
    boolean canCancel(Booking booking, LocalDateTime cancellationTime);
    double getRefundPercentage(Booking booking, LocalDateTime cancellationTime);
}
