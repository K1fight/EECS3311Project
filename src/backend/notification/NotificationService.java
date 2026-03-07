package backend.notification;

import backend.booking.Booking;

public class NotificationService implements BookingObserver {
    @Override
    public void update(Booking booking) {
        switch (booking.getStatus()) {
            case Requested:
                System.out.println("NOTIF: Booking created. Waiting for consultant confirmation.");
                break;
            case Confirmed:
                System.out.println("NOTIF: Booking " + booking.getBookingId() +
                        " confirmed! Notifying client and consultant.");
                break;
            case Rejected:
                System.out.println("NOTIF: Booking " + booking.getBookingId() +
                        " rejected.");
                break;
            case Cancelled:
                System.out.println("NOTIF: Booking " + booking.getBookingId() +
                        " cancelled. Notifying client and consultant.");
                break;
            case Completed:
                System.out.println("NOTIF: Booking " + booking.getBookingId() +
                        " completed. Sending completion notification.");
                break;
        }
    }
}
