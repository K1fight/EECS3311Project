package backend.booking;

import static backend.booking.BookingStatus.*;

public class RequestedState implements BookingState {
    @Override
    public void requested(Booking booking) {
        throw new IllegalStateException("Already requested.");
    }

    @Override
    public void confirm(Booking booking) {
        System.out.println("Booking confirmed, pending payment.");
        booking.setState(new ConfirmedState());
        booking.setStatus(Confirmed);
        booking.notifyObservers();
    }

    @Override
    public void cancel(Booking booking) {
        throw new IllegalStateException("Cannot cancel before confirmation");
    }

    @Override
    public void reject(Booking booking) {
        System.out.println("Booking rejected by consultant.");
        booking.setState(new RejectedState());
        booking.setStatus(Rejected);
        booking.notifyObservers();
    }

    @Override
    public void pending(Booking booking) { }

    @Override
    public void markPaid(Booking booking) {
        throw new IllegalStateException("Cannot pay before confirmation.");
    }

    @Override
    public void complete(Booking booking) {
        throw new IllegalStateException("Cannot complete before payment.");
    }
}