package backend.user;

import backend.core.ConsultingService;
import backend.booking.Booking;
import backend.core.TimeSlot;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Consultant: subject-matter expert who provides consulting sessions.
 * Must be approved by an Admin before they can receive bookings.
 */
public class Consultant extends User {

    private boolean isApproved;
    private String specialty;
    private String bio;

    // In-memory booking and availability tracking (augments DB persistence)
    private List<Booking> myBookings;
    private List<TimeSlot> myAvailability;

    // ======================== Constructors ========================

    /** Primary constructor — default not approved until Admin approves. */
    public Consultant(String name, String email, String password) {
        super(name, email, password);
        this.isApproved = false;
    }

    /** Rehydration constructor (for DB loading). */
    public Consultant(java.util.UUID userID, String name, String email, String password, boolean isApproved) {
        super(userID, name, email, password);
        this.isApproved = isApproved;
    }

    /** Full constructor with specialty and bio. */
    public Consultant(String name, String email, String password, String specialty, String bio) {
        super(name, email, password);
        this.isApproved = false;
        this.specialty = specialty;
        this.bio = bio;
    }

    // ======================== Approval ========================

    /**
     * Whether this consultant has been approved by an Admin.
     * Unapproved consultants cannot receive bookings.
     */
    public boolean isApproved() {
        return isApproved;
    }

    /**
     * Set the approval status. Only Admin should call this.
     */
    public void setApproved(boolean approved) {
        this.isApproved = approved;
        System.out.println("[Consultant] " + getName()
            + " is now " + (approved ? "APPROVED" : "pending approval"));
    }

    // ======================== Profile ========================

    public String getSpecialty() {
        return specialty;
    }

    public void setSpecialty(String specialty) {
        this.specialty = specialty;
    }

    public String getBio() {
        return bio;
    }

    public void setBio(String bio) {
        this.bio = bio;
    }

    // ======================== Use-Case Methods ========================

    /**
     * Provide a consulting session for a confirmed booking.
     * @param booking a Confirmed or Paid booking
     */
    public void provideConsultation(Booking booking) {
        if (booking == null) {
            System.out.println("Booking is required.");
            return;
        }
        if (!booking.getConsultant().equals(this)) {
            System.out.println("This booking is not assigned to you.");
            return;
        }
        if (!isApproved()) {
            System.out.println("You must be approved before providing consultations.");
            return;
        }

        switch (booking.getStatus()) {
            case Confirmed, Paid -> {
                System.out.println("Starting consultation: " + booking.getBookingId());
                System.out.println("  Client : " + booking.getClient().getName());
                System.out.println("  Service: " + booking.getService().getName());
                System.out.println("  Time   : " + booking.getStartTime());
                System.out.println("  Duration: " + booking.getService().getDurationMinutes() + " minutes");
            }
            case Completed -> System.out.println("This booking has already been completed.");
            default -> System.out.println("Booking must be Confirmed or Paid before the consultation. Current status: " + booking.getStatus());
        }
    }

    /**
     * Review and respond to a pending booking request.
     * @param booking the booking to review
     * @param accept true to accept, false to reject
     */
    public void reviewBookingRequest(Booking booking, boolean accept) {
        if (booking == null) {
            System.out.println("Booking is required.");
            return;
        }
        if (!booking.getConsultant().equals(this)) {
            System.out.println("This booking is not assigned to you.");
            return;
        }
        if (!isApproved()) {
            System.out.println("You must be approved by an Admin first.");
            return;
        }

        if (accept) {
            booking.confirm();
            System.out.println("Booking confirmed: " + booking.getBookingId());
        } else {
            booking.reject();
            System.out.println("Booking rejected: " + booking.getBookingId());
        }
    }

    /**
     * Complete a booking (mark consultation as done).
     * @param booking the booking to complete
     */
    public void completeConsultation(Booking booking) {
        if (booking == null) {
            System.out.println("Booking is required.");
            return;
        }
        if (!booking.getConsultant().equals(this)) {
            System.out.println("This booking is not assigned to you.");
            return;
        }
        booking.complete();
        System.out.println("Consultation completed: " + booking.getBookingId());
    }

    // ======================== Account Type ========================

    @Override
    public AccountType getAccountType() {
        return AccountType.Consultant;
    }

    // ======================== String ========================

    @Override
    public String toString() {
        return String.format("Consultant[id=%s, name=%s, email=%s, approved=%s, specialty=%s]",
            getUserID(), getName(), getEmail(), isApproved, specialty);
    }
}
