package backend.core;

import backend.service.ServiceCategory;
import backend.user.Consultant;
import backend.booking.Booking;

import java.util.*;

public class ConsultingService {
    // Service attributes (consulting service data)
    private UUID serviceId;
    private String name;
    private String description;
    private double basePrice;
    private int durationMinutes; // e.g., 60 minutes
    private ServiceCategory category;
    
    // Consultant management attributes
    private Map<Consultant, List<Booking>> consultantBookings = new HashMap<>();
    private Map<Consultant, List<TimeSlot>> availability = new HashMap<>();

    public ConsultingService(String name, String description, double basePrice, int durationMinutes, ServiceCategory category) {
        this.serviceId = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.durationMinutes = durationMinutes;
        this.category = category;
    }
    
    // Default constructor for service management
    public ConsultingService() {
    }

    // ========== Consulting Service Getters ==========
    public double getBasePrice() { return basePrice; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ServiceCategory getCategory() { return category; }
    public UUID getServiceId() { return serviceId; }
    
    // ========== Consultant Management Methods ==========
    
    /**
     * Add a booking to a consultant
     * @param booking The booking to add
     */
    public void addBookingToConsultant(Booking booking) {
        Consultant consultant = booking.getConsultant();
        consultantBookings.computeIfAbsent(consultant, k -> new ArrayList<>()).add(booking);
    }

    /**
     * Manage consultant availability
     * @param consultant The consultant
     * @param slots Available time slots
     */
    public void manageAvailability(Consultant consultant, List<TimeSlot> slots) {
        availability.put(consultant, slots);
    }

    /**
     * Get all bookings for a consultant
     * @param consultant The consultant
     * @return List of bookings
     */
    public List<Booking> getConsultantBookings(Consultant consultant) {
        return consultantBookings.getOrDefault(consultant, Collections.emptyList());
    }

    /**
     * Accept a booking request
     * @param consultant The consultant
     * @param booking The booking to accept
     */
    public void acceptBooking(Consultant consultant, Booking booking) {
        if (booking.getConsultant().equals(consultant)) {
            booking.confirm(); // State transition
            // Notify client
        }
    }

    /**
     * Reject a booking request
     * @param consultant The consultant
     * @param booking The booking to reject
     */
    public void rejectBooking(Consultant consultant, Booking booking) {
        if (booking.getConsultant().equals(consultant)) {
            booking.reject();
        }
    }

    /**
     * Complete a booking
     * @param consultant The consultant
     * @param booking The booking to complete
     */
    public void completeBooking(Consultant consultant, Booking booking) {
        if (booking.getConsultant().equals(consultant)) {
            booking.complete();
        }
    }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - $%.2f (%d min): %s", category, name, basePrice, durationMinutes, description);
    }
}
