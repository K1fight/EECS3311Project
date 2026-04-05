package backend.booking;

import static backend.booking.BookingStatus.*;

public class PendingPaymentState implements BookingState {
    @Override
    public void requested(Booking booking) {
        throw new IllegalStateException("Already requested.");
    }

    @Override
    public void confirm(Booking booking) {
        throw new IllegalStateException("Already confirmed.");
    }

    @Override
    public void cancel(Booking booking) {
        System.out.println("Booking cancelled.");
        booking.setState(new CancelledState());
        booking.setStatus(Cancelled);
        booking.notifyObservers();
    }

    @Override
    public void reject(Booking booking) {
        throw new IllegalStateException("Cannot reject a booking after it has been confirmed.");
    }

    @Override
    public void pending(Booking booking) {
        throw new IllegalStateException("Already pending payment.");
    }

    @Override
    public void markPaid(Booking booking) {
        System.out.println("Payment successful. Transitioning to Paid.");
        booking.setState(new PaidState());
        booking.setStatus(Paid);
        booking.notifyObservers();
    }

    @Override
    public void complete(Booking booking) {
        throw new IllegalStateException("Cannot complete booking before payment.");
    }
}
