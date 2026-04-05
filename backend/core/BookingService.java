package backend.core;

import backend.booking.Booking;
import backend.booking.BookingStatus;
import backend.user.Client;
import backend.user.Consultant;
import backend.database.BookingDAO;
import java.util.*;

// Booking management service — DB-first, with local fallback
public class BookingService {
    private List<Booking> allBookings = new ArrayList<>();
    private BookingDAO bookingDAO;

    public void setBookingDAO(BookingDAO dao) {
        this.bookingDAO = dao;
    }

    /**
     * Get a booking by its UUID.
     * Tries DB first, then local in-memory list.
     */
    public Booking getBookingById(UUID id) {
        if (bookingDAO != null) {
            try {
                Booking b = bookingDAO.findById(id);
                if (b != null) return b;
            } catch (Exception e) {
                System.err.println("Error fetching booking from DB: " + e.getMessage());
            }
        }
        // Fallback to in-memory
        for (Booking b : allBookings) {
            if (b.getBookingId().equals(id)) return b;
        }
        return null;
    }

    /**
     * Update booking status in both DB and local list.
     */
    public void updateBooking(Booking booking) {
        if (booking == null) return;

        // Update local list
        boolean found = false;
        for (int i = 0; i < allBookings.size(); i++) {
            if (allBookings.get(i).getBookingId().equals(booking.getBookingId())) {
                allBookings.set(i, booking);
                found = true;
                break;
            }
        }
        if (!found) {
            allBookings.add(booking);
        }

        // Persist to DB
        if (bookingDAO != null) {
            try {
                bookingDAO.update(booking);
            } catch (Exception e) {
                System.err.println("Error updating booking in DB: " + e.getMessage());
            }
        }
    }

    /**
     * Add a booking to the local list (called when DB is unavailable).
     */
    public void addBookingLocally(Booking booking) {
        allBookings.add(booking);
    }

    /**
     * Get all local (in-memory) bookings.
     */
    public List<Booking> getLocalBookings() {
        return new ArrayList<>(allBookings);
    }

    /**
     * Get bookings for a client — DB first, then local.
     */
    public List<Booking> getBookingsForClient(String clientId) {
        List<Booking> results = new ArrayList<>();
        if (bookingDAO != null) {
            try {
                List<Booking> dbBookings = bookingDAO.findByClientId(clientId);
                if (dbBookings != null && !dbBookings.isEmpty()) {
                    return dbBookings;
                }
            } catch (Exception e) {
                System.err.println("Error fetching client bookings from DB: " + e.getMessage());
            }
        }
        for (Booking b : allBookings) {
            if (b.getClient().getUserID().toString().equals(clientId)) {
                results.add(b);
            }
        }
        return results;
    }

    /**
     * Get bookings for a consultant — DB first, then local.
     */
    public List<Booking> getBookingsForConsultant(String consultantId) {
        List<Booking> results = new ArrayList<>();
        if (bookingDAO != null) {
            try {
                List<Booking> dbBookings = bookingDAO.findByConsultantId(consultantId);
                if (dbBookings != null && !dbBookings.isEmpty()) {
                    return dbBookings;
                }
            } catch (Exception e) {
                System.err.println("Error fetching consultant bookings from DB: " + e.getMessage());
            }
        }
        for (Booking b : allBookings) {
            if (b.getConsultant().getUserID().toString().equals(consultantId)) {
                results.add(b);
            }
        }
        return results;
    }
}
