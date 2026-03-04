package backend.booking;

// Actually Confirmed implies PendingPayment, can be treated as the same state here
public class PendingPaymentState implements BookingState {
    @Override
    public void confirm(Booking booking) { }

    @Override
    public void cancel(Booking booking) {
        booking.setState(new CancelledState());
    }

    @Override
    public void reject(Booking booking) {
        booking.setState(new RejectedState());
    }

    @Override
    public void markPaid(Booking booking) {
        booking.setState(new PaidState());
    }

    @Override
    public void complete(Booking booking) {
        throw new IllegalStateException("Cannot complete before payment.");
    }
}
