package backend.booking;

public class CompletedState implements BookingState {
    @Override
    public void confirm(Booking booking) { }
    @Override
    public void cancel(Booking booking) { }
    @Override
    public void reject(Booking booking) { }
    @Override
    public void markPaid(Booking booking) { }
    @Override
    public void complete(Booking booking) { }
}