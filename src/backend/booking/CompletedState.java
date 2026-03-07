package backend.booking;

public class CompletedState implements BookingState {
    @Override
    public void requested(Booking booking) { throw new IllegalStateException("Already completed."); }

    @Override
    public void confirm(Booking booking) { throw new IllegalStateException("Already completed."); }

    @Override
    public void cancel(Booking booking) { throw new IllegalStateException("Already completed."); }

    @Override
    public void reject(Booking booking) { throw new IllegalStateException("Already completed."); }

    @Override
    public void pending(Booking booking) { throw new IllegalStateException("Already completed."); }

    @Override
    public void markPaid(Booking booking) { throw new IllegalStateException("Already completed."); }

    @Override
    public void complete(Booking booking) { throw new IllegalStateException("Already completed."); }
}