package backend.notification;

import backend.booking.Booking;

public interface BookingObserver {
    void update(Booking booking);
}
