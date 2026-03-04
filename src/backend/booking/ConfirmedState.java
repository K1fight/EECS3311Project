package backend.booking;

public class ConfirmedState implements BookingState {
    @Override
    public void confirm(Booking booking) {
        // Already confirmed
    }

    @Override
    public void cancel(Booking booking) {
        System.out.println("Booking cancelled by user.");
        booking.setState(new CancelledState());
    }

    @Override
    public void reject(Booking booking) {
        // Cannot reject a confirmed booking
    }

    @Override
    public void markPaid(Booking booking) {
        System.out.println("Payment successful. Transitioning to Paid.");
        booking.setState(new PaidState());
    }

    @Override
    public void complete(Booking booking) {
        // Cannot complete before payment (usually)
    }
}
