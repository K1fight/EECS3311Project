package backend.core;

import backend.user.Consultant;
import backend.booking.Booking;
import java.util.*;

public class ConsultantService {
    private Map<Consultant, List<Booking>> consultantBookings = new HashMap<>();
    private Map<Consultant, List<TimeSlot>> availability = new HashMap<>();

    public void addBookingToConsultant(Booking booking) {
        Consultant consultant = booking.getConsultant();
        consultantBookings.computeIfAbsent(consultant, k -> new ArrayList<>()).add(booking);
    }

    public void manageAvailability(Consultant consultant, List<TimeSlot> slots) {
        availability.put(consultant, slots);
    }

    public List<Booking> getConsultantBookings(Consultant consultant) {
        return consultantBookings.getOrDefault(consultant, Collections.emptyList());
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
