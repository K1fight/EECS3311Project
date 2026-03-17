package backend.booking;

public class CancelledState implements BookingState {
    @Override
    public void requested(Booking booking) { throw new IllegalStateException("Cannot request a cancelled booking."); }

    @Override
    public void confirm(Booking booking) {
        throw new IllegalStateException("Cannot confirm a cancelled booking.");
    }

    @Override
    public void cancel(Booking booking) { throw new IllegalStateException("Already cancelled."); }

    @Override
    public void reject(Booking booking) { throw new IllegalStateException("Cannot reject a cancelled booking."); }

    @Override
    public void pending(Booking booking) { }

    @Override
    public void markPaid(Booking booking) {
        throw new IllegalStateException("Cannot pay a cancelled booking.");
    }

    @Override
    public void complete(Booking booking) {
        throw new IllegalStateException("Cannot complete a cancelled booking.");
    }
}