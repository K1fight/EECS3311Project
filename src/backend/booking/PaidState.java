package backend.booking;

public class PaidState implements BookingState {
    @Override
    public void confirm(Booking booking) { }

    @Override
    public void cancel(Booking booking) {
        // Cancellation may be allowed based on refund policy
        booking.setState(new CancelledState());
    }

    @Override
    public void reject(Booking booking) {
        // Usually cannot reject paid bookings
    }

    @Override
    public void markPaid(Booking booking) { }

    @Override
    public void complete(Booking booking) {
        booking.setState(new CompletedState());
        System.out.println("Booking completed.");
    }
}
