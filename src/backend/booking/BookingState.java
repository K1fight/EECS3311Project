package backend.booking;

// State interface
public interface BookingState {
    void requested(Booking booking);
    void confirm(Booking booking);
    void cancel(Booking booking);
    void reject(Booking booking);
    void pending(Booking booking);
    void markPaid(Booking booking);
    void complete(Booking booking);
}
