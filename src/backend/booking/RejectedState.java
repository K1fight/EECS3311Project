package backend.booking;

public class RejectedState implements BookingState {
    @Override
    public void requested(Booking booking) { throw new IllegalStateException("Cannot request a rejected booking."); }

    @Override
    public void confirm(Booking booking) {
        throw new IllegalStateException("Cannot confirm a rejected booking.");
    }

    @Override
    public void cancel(Booking booking) {
        throw new IllegalStateException("Cannot cancel a rejected booking.");
    }

    @Override
    public void reject(Booking booking) { throw new IllegalStateException("Booking already rejected."); }

    @Override
    public void pending(Booking booking) { }

    @Override
    public void markPaid(Booking booking) { throw new IllegalStateException("Cannot pay a rejected booking."); }

    @Override
    public void complete(Booking booking) { throw new IllegalStateException("Cannot complete a rejected booking."); }
}
