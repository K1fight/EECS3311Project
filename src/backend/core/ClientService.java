package backend.core;

import backend.user.Client;
import backend.user.Consultant;
import backend.booking.Booking;
import backend.payment.PaymentMethod;
import backend.payment.PaymentMethodFactory;
import backend.notification.NotificationService;
import backend.policy.CancellationPolicy;
import backend.policy.DefaultCancellationPolicy;
import java.time.LocalDateTime;
import java.util.*;

// Client service layer, implementing client use cases
public class ClientService {
    private List<ConsultingService> services = new ArrayList<>();
    private Map<Client, List<Booking>> clientBookings = new HashMap<>();
    private Map<Client, List<PaymentMethod>> clientPaymentMethods = new HashMap<>();
    private BookingService bookingService;
    private ConsultingService consultingService;

    public ClientService() {
        this.bookingService = new BookingService();
        this.consultingService = new ConsultingService();
    }

    public ClientService(BookingService bookingService) {
        this.bookingService = bookingService;
        this.consultingService = new ConsultingService();
    }

    public ClientService(BookingService bookingService, ConsultingService consultingService) {
        this.bookingService = bookingService;
        this.consultingService = consultingService;
    }

    public List<ConsultingService> browseServices() {
        return services; // Return all services
    }

    public Booking requestBooking(Client client, ConsultingService service, Consultant consultant, LocalDateTime startTime) {
        // Check consultant availability (omitted)
        Booking booking = new Booking(client, consultant, service, startTime);
        clientBookings.computeIfAbsent(client, k -> new ArrayList<>()).add(booking);
        
        // Also add to consultant's bookings
        if (consultingService != null) {
            consultingService.addBookingToConsultant(booking);
        }
        
        // Notify consultant
        NotificationService.sendEmail(consultant, "New booking request from " + client.getName());
        return booking;
    }

    public void cancelBooking(Client client, Booking booking) {
        // Apply cancellation policy
        CancellationPolicy policy = new DefaultCancellationPolicy();
        if (policy.canCancel(booking, LocalDateTime.now())) {
            booking.cancel();
        } else {
            throw new IllegalStateException("Cannot cancel due to policy");
        }
    }

    public List<Booking> viewBookingHistory(Client client) {
        return clientBookings.getOrDefault(client, Collections.emptyList());
    }

    public List<PaymentMethod> getClientPaymentMethods(Client client) {
        return clientPaymentMethods.getOrDefault(client, Collections.emptyList());
    }

    public PaymentMethod addPaymentMethod(Client client, String type, Map<String, String> details) {
        PaymentMethod method = PaymentMethodFactory.createPaymentMethod(type, details);
        clientPaymentMethods.computeIfAbsent(client, k -> new ArrayList<>()).add(method);
        return method;
    }
    
    /**
     * Remove a specific payment method for a client
     * @param client The client
     * @param methodIndex Index of the payment method to remove (0-based)
     * @return true if removed successfully, false otherwise
     */
    public boolean removePaymentMethod(Client client, int methodIndex) {
        List<PaymentMethod> methods = clientPaymentMethods.get(client);
        if (methods == null || methods.isEmpty()) {
            return false;
        }
        
        if (methodIndex >= 0 && methodIndex < methods.size()) {
            methods.remove(methodIndex);
            System.out.println("Payment method removed successfully.");
            return true;
        }
        
        System.out.println("Invalid payment method index.");
        return false;
    }
    
    /**
     * Remove all payment methods for a client
     * @param client The client
     */
    public void removeAllPaymentMethods(Client client) {
        clientPaymentMethods.remove(client);
        System.out.println("All payment methods removed.");
    }

    // Other methods...
}
