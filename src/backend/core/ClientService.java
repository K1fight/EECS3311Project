package backend.core;

import backend.user.Client;
import backend.user.Consultant;
import backend.booking.Booking;
import backend.payment.PaymentMethod;
import backend.payment.PaymentMethodFactory;
import backend.service.ConsultingService;
import backend.notification.NotificationService;
import backend.policy.CancellationPolicy;
import backend.policy.DefaultCancellationPolicy;
import java.time.LocalDateTime;
import java.util.*;

// Client service layer, implementing client use cases
public class ClientService {
    private List<ConsultingService> services = new ArrayList<>();
    private Map<Client, List<Booking>> clientBookings = new HashMap<>();

    public List<ConsultingService> browseServices() {
        return services; // Return all services
    }

    public Booking requestBooking(Client client, ConsultingService service, Consultant consultant, LocalDateTime startTime) {
        // Check consultant availability (omitted)
        Booking booking = new Booking(client, consultant, service, startTime);
        clientBookings.computeIfAbsent(client, k -> new ArrayList<>()).add(booking);
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

    public PaymentMethod addPaymentMethod(Client client, String type, Map<String, String> details) {
        PaymentMethod method = PaymentMethodFactory.createPaymentMethod(type, details);
        // Save to client payment method list (omitted)
        return method;
    }

    // Other methods...
}
