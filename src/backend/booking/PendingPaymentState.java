package backend.booking;

// Actually Confirmed implies PendingPayment, can be treated as the same state here
public class PendingPaymentState implements BookingState {
    @Override
    public void requested(Booking booking) { }

    @Override
    public void confirm(Booking booking) { }

    @Override
    public void cancel(Booking booking) { }

    @Override
    public void reject(Booking booking) { }

    @Override
    public void pending(Booking booking) { }

    @Override
    public void markPaid(Booking booking) { }

    @Override
    public void complete(Booking booking) { }
}
