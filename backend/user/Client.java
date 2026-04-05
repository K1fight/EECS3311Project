package backend.user;

import backend.core.ClientService;
import backend.core.ConsultingService;
import backend.core.BookingService;
import backend.booking.Booking;
import backend.payment.PaymentMethod;
import backend.payment.PaymentTransaction;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Client: end-user who browses services, books consultations,
 * manages payment methods, and views booking/payment history.
 */
public class Client extends User {

    private ClientService clientService;

    /** Primary constructor. */
    public Client(String name, String email, String password) {
        super(name, email, password);
        this.clientService = new ClientService();
    }

    /** Constructor with explicit UUID (for DB rehydration). */
    public Client(java.util.UUID userID, String name, String email, String password) {
        super(userID, name, email, password);
        this.clientService = new ClientService();
    }

    // ======================== Service Layer Injection ========================

    public void setClientService(ClientService clientService) {
        this.clientService = clientService;
    }

    public ClientService getClientService() {
        return clientService;
    }

    // ======================== Use-Case Methods ========================

    /**
     * Browse all available consulting services.
     */
    public void browseServices() {
        List<ConsultingService> services = clientService.browseServices();
        if (services == null || services.isEmpty()) {
            System.out.println("No services available at the moment.");
            return;
        }
        System.out.println("\n=== Available Services ===");
        for (ConsultingService s : services) {
            System.out.println(s);
        }
    }

    /**
     * Request a booking for a service with a specific consultant.
     * @param service  the service to book
     * @param consultant the consultant to book with
     * @param startTime the desired start time
     * @return the created Booking, or null if failed
     */
    public Booking requestBooking(ConsultingService service, Consultant consultant, LocalDateTime startTime) {
        if (service == null) {
            System.out.println("Service is required.");
            return null;
        }
        if (consultant == null) {
            System.out.println("Consultant is required.");
            return null;
        }
        if (!consultant.isApproved()) {
            System.out.println("Consultant is not yet approved by an administrator.");
            return null;
        }
        if (startTime == null) {
            startTime = LocalDateTime.now().plusDays(1);
        }

        Booking booking = clientService.requestBooking(this, service, consultant, startTime);
        if (booking != null) {
            System.out.println("Booking requested: " + booking.getBookingId()
                + " | Service: " + service.getName()
                + " | Consultant: " + consultant.getName()
                + " | Time: " + booking.getStartTime());
        }
        return booking;
    }

    /**
     * Cancel a booking (subject to cancellation policy).
     * @param booking the booking to cancel
     */
    public void cancelBooking(Booking booking) {
        if (booking == null) {
            System.out.println("Booking is required.");
            return;
        }
        try {
            clientService.cancelBooking(this, booking);
            System.out.println("Booking cancelled: " + booking.getBookingId());
        } catch (IllegalStateException e) {
            System.out.println("Cannot cancel: " + e.getMessage());
        }
    }

    /**
     * View all bookings for this client.
     */
    public void viewBookingHistory() {
        List<Booking> history = clientService.viewBookingHistory(this);
        if (history.isEmpty()) {
            System.out.println("No booking history.");
            return;
        }
        System.out.println("\n=== Booking History ===");
        for (Booking b : history) {
            System.out.printf("  [%s] %s | Consultant: %s | Status: %s%n",
                b.getBookingId(), b.getService().getName(),
                b.getConsultant().getName(), b.getStatus());
        }
    }

    /**
     * Add a payment method for this client.
     * @param type   e.g. "CreditCard", "PayPal"
     * @param details e.g. {"maskedNumber": "****1234", "expiry": "12/27"}
     * @return the created PaymentMethod, or null
     */
    public PaymentMethod addPaymentMethod(String type, Map<String, String> details) {
        PaymentMethod method = clientService.addPaymentMethod(this, type, details);
        if (method != null) {
            System.out.println("Payment method added: " + method + " (" + details.getOrDefault("maskedNumber", "") + ")");
        }
        return method;
    }

    /**
     * List all saved payment methods for this client.
     */
    public void listPaymentMethods() {
        List<Map<String, Object>> methods = clientService.getClientPaymentMethods(this);
        if (methods.isEmpty()) {
            System.out.println("No payment methods saved.");
            return;
        }
        System.out.println("\n=== Saved Payment Methods ===");
        for (int i = 0; i < methods.size(); i++) {
            Map<String, Object> m = methods.get(i);
            PaymentMethod pm = (PaymentMethod) m.get("method");
            @SuppressWarnings("unchecked")
            Map<String, String> det = (Map<String, String>) m.get("details");
            System.out.printf("  [%d] %s — %s%n", i, pm, det.getOrDefault("maskedNumber", ""));
        }
    }

    /**
     * Remove a payment method by index.
     * @param index 0-based index from listPaymentMethods()
     */
    public void removePaymentMethod(int index) {
        boolean removed = clientService.removePaymentMethod(this, index);
        if (removed) {
            System.out.println("Payment method removed.");
        } else {
            System.out.println("Invalid payment method index.");
        }
    }

    /**
     * Add a completed payment transaction to this client's history.
     */
    public void addPaymentToHistory(PaymentTransaction transaction) {
        clientService.addPaymentToHistory(this, transaction);
    }

    /**
     * View this client's payment history.
     */
    public void viewPaymentHistory() {
        List<PaymentTransaction> history = clientService.viewPaymentHistory(this);
        if (history.isEmpty()) {
            System.out.println("No payment history.");
            return;
        }
        System.out.println("\n=== Payment History ===");
        for (PaymentTransaction t : history) {
            System.out.printf("  [%s] $%.2f | %s | %s%n",
                t.getTransactionId(), t.getAmount(),
                t.getPaymentMethod(), t.getStatus());
        }
    }

    // ======================== Account Type ========================

    @Override
    public AccountType getAccountType() {
        return AccountType.Client;
    }
}
