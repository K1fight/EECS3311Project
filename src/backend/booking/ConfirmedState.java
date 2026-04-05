package backend.booking;

import static backend.booking.BookingStatus.*;

public class ConfirmedState implements BookingState {
    @Override
    public void requested(Booking booking) {throw new IllegalStateException("Already requested.");}

    @Override
    public void confirm(Booking booking) { throw new IllegalStateException("Already confirmed."); }

    @Override
    public void cancel(Booking booking) {
        System.out.println("Booking cancelled.");
        booking.setState(new CancelledState());
        booking.setStatus(Cancelled);
        booking.notifyObservers();
    }

    @Override
    public void reject(Booking booking) { throw new IllegalStateException("Cannot reject a confirmed booking."); }

    @Override
    public void pending(Booking booking) {
        System.out.println("Booking pending payment.");
        booking.setState(new PendingPaymentState());
        booking.setStatus(PendingPayment);
        booking.notifyObservers();
    }

    @Override
    public void markPaid(Booking booking) {
        throw new IllegalStateException("Booking must be in PendingPayment state before payment.");
    }

    @Override
    public void complete(Booking booking) { throw new IllegalStateException("Cannot complete booking before payment."); }
}
