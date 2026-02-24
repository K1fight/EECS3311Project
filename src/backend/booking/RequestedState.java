package backend.booking;

public class RequestedState implements BookingState {
    @Override
    public void confirm(Booking booking) {
        booking.setState(new ConfirmedState());
        System.out.println("Booking confirmed, pending payment.");
    }

    @Override
    public void cancel(Booking booking) {
        booking.setState(new CancelledState());
        System.out.println("Booking cancelled by client.");
    }

    @Override
    public void reject(Booking booking) {
        booking.setState(new RejectedState());
        System.out.println("Booking rejected by consultant.");
    }

    @Override
    public void markPaid(Booking booking) {
        throw new IllegalStateException("Cannot pay before confirmation.");
    }

    @Override
    public void complete(Booking booking) {
        throw new IllegalStateException("Cannot complete before payment.");
    }
}