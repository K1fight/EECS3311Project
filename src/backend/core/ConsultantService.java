package backend.core;

import backend.user.Consultant;
import backend.booking.Booking;
import java.util.*;

public class ConsultantService {
    private Map<Consultant, List<Booking>> consultantBookings = new HashMap<>();
    private Map<Consultant, List<TimeSlot>> availability = new HashMap<>();

    public void manageAvailability(Consultant consultant, List<TimeSlot> slots) {
        availability.put(consultant, slots);
    }

    public void acceptBooking(Consultant consultant, Booking booking) {
        if (booking.getConsultant().equals(consultant)) {
            booking.confirm(); // State transition
            // Notify client
        }
    }

    public void rejectBooking(Consultant consultant, Booking booking) {
        if (booking.getConsultant().equals(consultant)) {
            booking.reject();
        }
    }

    public void completeBooking(Consultant consultant, Booking booking) {
        if (booking.getConsultant().equals(consultant)) {
            booking.complete();
        }
    }
}
