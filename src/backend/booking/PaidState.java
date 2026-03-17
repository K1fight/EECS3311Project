package backend.booking;

import static backend.booking.BookingStatus.*;

public class PaidState implements BookingState {
    @Override
    public void requested(Booking booking) { throw new IllegalStateException("Already requested."); }

    @Override
    public void confirm(Booking booking) { throw new IllegalStateException("Already confirmed."); }

    @Override
    public void cancel(Booking booking) {
        // Cancellation may be allowed based on refund policy
        System.out.println("Booking cancelled.");
        booking.setState(new CancelledState());
        booking.setStatus(Cancelled);
        booking.notifyObservers();
    }

    @Override
    public void reject(Booking booking) { throw new IllegalStateException("Cannot reject after confirmation."); }

    @Override
    public void pending(Booking booking) { }

    @Override
    public void markPaid(Booking booking) { throw new IllegalStateException("Already Paid."); }

    @Override
    public void complete(Booking booking) {
        System.out.println("Booking complete.");
        booking.setState(new CompletedState());
        booking.setStatus(Completed);
        booking.notifyObservers();
    }
}
