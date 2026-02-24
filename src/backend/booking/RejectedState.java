package backend.booking;

public class RejectedState implements BookingState {
    @Override
    public void confirm(Booking booking) {
        throw new IllegalStateException("Cannot confirm a rejected booking.");
    }

    @Override
    public void cancel(Booking booking) {
        // Already rejected, no operation needed
    }

    @Override
    public void reject(Booking booking) { }

    @Override
    public void markPaid(Booking booking) {
        throw new IllegalStateException("Cannot pay a rejected booking.");
    }

    @Override
    public void complete(Booking booking) {
        throw new IllegalStateException("Cannot complete a rejected booking.");
    }
}
