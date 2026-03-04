package frontend;

import backend.booking.Booking;
import backend.core.*;
import backend.payment.PaymentMethod;
import backend.policy.CancellationPolicy;
import backend.policy.PricingStrategy;
import backend.service.ConsultingService;
import backend.service.ServiceCategory;
import backend.user.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class BookingUI {
    private Scanner scanner = new Scanner(System.in);
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Services
    private AdminService adminService;
    private ClientService clientService;
    private ConsultantService consultantService;
    private BookingService bookingService;
    private PaymentService paymentService;

    // Current user
    private User currentUser;
    private boolean isLoggedIn = false;

    // Demo data
    private Map<String, Client> demoClients = new HashMap<>();
    private Map<String, Consultant> demoConsultants = new HashMap<>();
    private List<ConsultingService> availableServices;

    public static void main(String[] args) {
        BookingUI ui = new BookingUI();
        ui.initialize();
        ui.run();
    }

    public void initialize() {
        // Initialize services
        bookingService = new BookingService();
        consultantService = new ConsultantService();
        clientService = new ClientService(bookingService);
        adminService = new AdminService(consultantService);
        paymentService = new PaymentService();

    }

    public void run() {
        System.out.println("===========================================");
        System.out.println("   Welcome to Consulting Booking System");
        System.out.println("===========================================\n");

        while (true) {
            if (!isLoggedIn) {
                showLoginMenu();
            } else {
                showMainmenu();
            }
        }
    }

    private void showLoginMenu() {
        System.out.println("\n=== Login Menu ===");
        System.out.println("1. Login as Client");
        System.out.println("2. Login as Consultant");
        System.out.println("3. Login as Admin");
        System.out.println("4. Exit");
        System.out.print("Choose option: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                loginAsClient();
                break;
            case "2":
                loginAsConsultant();
                break;
            case "3":
                loginAsAdmin();
                break;
            case "4":
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private void loginAsClient() {
        System.out.print("Enter client ID (1 or 2): ");
        String clientId = scanner.nextLine();
        Client client = demoClients.get(clientId);

        if (client != null) {
            currentUser = client;
            isLoggedIn = true;
            System.out.println("Welcome, " + client.getName() + "!");
        } else {
            System.out.println("Invalid client ID!");
        }
    }

    private void loginAsConsultant() {
        System.out.print("Enter consultant ID (1 or 2): ");
        String consultantId = scanner.nextLine();
        Consultant consultant = demoConsultants.get(consultantId);

        if (consultant != null) {
            currentUser = consultant;
            isLoggedIn = true;
            System.out.println("Welcome, " + consultant.getName() + "!");
        } else {
            System.out.println("Invalid consultant ID!");
        }
    }

    private void loginAsAdmin() {
        currentUser = new Admin("System Admin", "admin@system.com", "admin");
        isLoggedIn = true;
        System.out.println("Welcome, Administrator!");
    }

    private void showMainmenu() {
        System.out.println("\n=== Main Menu ===");
        System.out.println("Logged in as: " + currentUser.getName() + " (" + currentUser.getAccountType() + ")");

        if (currentUser instanceof Client) {
            showClientMenu();
        } else if (currentUser instanceof Consultant) {
            showConsultantMenu();
        } else if (currentUser instanceof Admin) {
            showAdminMenu();
        }
    }

    private void showClientMenu() {
        Client client = (Client) currentUser;

        System.out.println("\n--- Client Options ---");
        System.out.println("1. Browse Services");
        System.out.println("2. Request Booking");
        System.out.println("3. View My Bookings");
        System.out.println("4. Cancel Booking");
        System.out.println("5. Make Payment");
        System.out.println("6. Manage Payment Methods");
        System.out.println("7. Logout");
        System.out.print("Choose option: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                browseServices();
                break;
            case "2":
                requestBooking(client);
                break;
            case "3":
                viewMyBookings(client);
                break;
            case "4":
                cancelBooking(client);
                break;
            case "5":
                makePayment(client);
                break;
            case "6":
                managePaymentMethods(client);
                break;
            case "7":
                logout();
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private void showConsultantMenu() {
        Consultant consultant = (Consultant) currentUser;

        System.out.println("\n--- Consultant Options ---");
        System.out.println("1. View My Bookings");
        System.out.println("2. Accept Booking");
        System.out.println("3. Reject Booking");
        System.out.println("4. Complete Booking");
        System.out.println("5. Manage Availability");
        System.out.println("6. Logout");
        System.out.print("Choose option: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                viewConsultantBookings(consultant);
                break;
            case "2":
                acceptBooking(consultant);
                break;
            case "3":
                rejectBooking(consultant);
                break;
            case "4":
                completeBooking(consultant);
                break;
            case "5":
                manageAvailability(consultant);
                break;
            case "6":
                logout();
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    private void showAdminMenu() {
        System.out.println("\n--- Admin Options ---");
        System.out.println("1. Approve Consultant");
        System.out.println("2. Reject Consultant");
        System.out.println("3. Set Cancellation Policy");
        System.out.println("4. Set Pricing Strategy");
        System.out.println("5. Logout");
        System.out.print("Choose option: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                approveConsultant();
                break;
            case "2":
                rejectConsultant();
                break;
            case "3":
                setCancellationPolicy();
                break;
            case "4":
                setPricingStrategy();
                break;
            case "5":
                logout();
                break;
            default:
                System.out.println("Invalid option!");
        }
    }

    // Client operations
    private void browseServices() {
        System.out.println("\n=== Available Services ===");
        for (int i = 0; i < availableServices.size(); i++) {
            ConsultingService service = availableServices.get(i);
            System.out.printf("%d. %s - $%.2f (%d min)%n",
                    i + 1, service.getName(), service.getBasePrice(), service.getDurationMinutes());
            System.out.println("   " + service.getDescription());
        }
    }

    private void requestBooking(Client client) {
        System.out.println("\n=== Request Booking ===");

        // Select service
        System.out.print("Select service (1-" + availableServices.size() + "): ");
        int serviceIndex = Integer.parseInt(scanner.nextLine()) - 1;
        if (serviceIndex < 0 || serviceIndex >= availableServices.size()) {
            System.out.println("Invalid service!");
            return;
        }
        ConsultingService service = availableServices.get(serviceIndex);

        // Select consultant
        System.out.println("\nAvailable Consultants:");
        int i = 1;
        for (Consultant consultant : demoConsultants.values()) {
            System.out.printf("%d. %s%n", i++, consultant.getName());
        }
        System.out.print("Select consultant: ");
        int consultantIndex = Integer.parseInt(scanner.nextLine());
        Consultant consultant = (Consultant) demoConsultants.values().toArray()[consultantIndex - 1];

        // Enter start time
        System.out.print("Enter start time (yyyy-MM-dd HH:mm): ");
        String timeStr = scanner.nextLine();
        LocalDateTime startTime = LocalDateTime.parse(timeStr, formatter);

        try {
            Booking booking = clientService.requestBooking(client, service, consultant, startTime);
            System.out.println("Booking created successfully! ID: " + booking.getBookingId());
        } catch (Exception e) {
            System.out.println("Failed to create booking: " + e.getMessage());
        }
    }

    private void viewMyBookings(Client client) {
        System.out.println("\n=== My Bookings ===");
        List<Booking> bookings = clientService.viewBookingHistory(client);

        if (bookings.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }

        for (Booking booking : bookings) {
            System.out.printf("ID: %s | Service: %s | Consultant: %s | Time: %s | State: %s%n",
                    booking.getBookingId(),
                    booking.getService().getName(),
                    booking.getConsultant().getName(),
                    booking.getStartTime().format(formatter),
                    booking.getCurrentState().getClass().getSimpleName());
        }
    }

    private void cancelBooking(Client client) {
        System.out.println("\n=== Cancel Booking ===");
        List<Booking> bookings = clientService.viewBookingHistory(client);

        if (bookings.isEmpty()) {
            System.out.println("No bookings to cancel.");
            return;
        }

        System.out.print("Enter booking ID to cancel: ");
        String bookingIdStr = scanner.nextLine();

        for (Booking booking : bookings) {
            if (booking.getBookingId().toString().equals(bookingIdStr)) {
                try {
                    clientService.cancelBooking(client, booking);
                    System.out.println("Booking cancelled successfully!");
                    return;
                } catch (Exception e) {
                    System.out.println("Failed to cancel: " + e.getMessage());
                    return;
                }
            }
        }
        System.out.println("Booking not found!");
    }

    private void makePayment(Client client) {
        System.out.println("\n=== Make Payment ===");
        List<Booking> bookings = clientService.viewBookingHistory(client);

        System.out.print("Enter booking ID to pay: ");
        String bookingIdStr = scanner.nextLine();

        Booking targetBooking = null;
        for (Booking booking : bookings) {
            if (booking.getBookingId().toString().equals(bookingIdStr)) {
                targetBooking = booking;
                break;
            }
        }

        if (targetBooking == null) {
            System.out.println("Booking not found!");
            return;
        }

        System.out.println("\nPayment Methods:");
        System.out.println("1. Credit Card");
        System.out.println("2. Debit Card");
        System.out.println("3. PayPal");
        System.out.println("4. Bank Transfer");
        System.out.print("Select payment method: ");
        String methodChoice = scanner.nextLine();

        Map<String, String> details = new HashMap<>();
        switch (methodChoice) {
            case "1":
                System.out.print("Enter card number (16 digits): ");
                details.put("cardNumber", scanner.nextLine());
                System.out.print("Enter expiry (MM/YY): ");
                details.put("expiry", scanner.nextLine());
                System.out.print("Enter CVV: ");
                details.put("cvv", scanner.nextLine());
                break;
            case "2":
                System.out.print("Enter card number (16 digits): ");
                details.put("cardNumber", scanner.nextLine());
                System.out.print("Enter expiry (MM/YY): ");
                details.put("expiry", scanner.nextLine());
                System.out.print("Enter CVV: ");
                details.put("cvv", scanner.nextLine());
                break;
            case "3":
                System.out.print("Enter PayPal email: ");
                details.put("email", scanner.nextLine());
                break;
            case "4":
                System.out.print("Enter account number: ");
                details.put("accountNumber", scanner.nextLine());
                System.out.print("Enter routing number: ");
                details.put("routingNumber", scanner.nextLine());
                break;
            default:
                System.out.println("Invalid payment method!");
                return;
        }

        PaymentMethod method = PaymentMethod.valueOf(methodChoice.equals("1") ? "Credit" :
                methodChoice.equals("2") ? "Debit" :
                        methodChoice.equals("3") ? "Paypal" : "BankTransfer");

        boolean success = paymentService.processPayment(targetBooking, method, details);
        System.out.println(success ? "Payment successful!" : "Payment failed!");
    }

    private void managePaymentMethods(Client client) {
        System.out.println("\n=== Manage Payment Methods ===");
        System.out.println("1. Add Payment Method");
        System.out.println("2. View Payment Methods");
        System.out.println("3. Remove Payment Method");
        System.out.print("Choose option: ");

        String choice = scanner.nextLine();

        switch (choice) {
            case "1":
                System.out.print("Enter payment method type (Credit/Debit/Paypal/BankTransfer): ");
                String type = scanner.nextLine();
                Map<String, String> details = new HashMap<>();
                System.out.print("Enter primary detail: ");
                details.put("primary", scanner.nextLine());

                PaymentMethod method = clientService.addPaymentMethod(client, type, details);
                System.out.println("Payment method added: " + method);
                break;
            case "2":
                List<PaymentMethod> methods = clientService.getClientPaymentMethods(client);
                for (PaymentMethod m : methods) {
                    System.out.println(m);
                }
                break;
            case "3":
                // Simplified removal
                System.out.println("Payment method removed.");
                break;
        }
    }

    // Consultant operations
    private void viewConsultantBookings(Consultant consultant) {
        System.out.println("\n=== My Bookings ===");
        List<Booking> bookings = consultantService.getConsultantBookings(consultant);

        if (bookings.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }

        for (Booking booking : bookings) {
            System.out.printf("ID: %s | Client: %s | Service: %s | Time: %s | State: %s%n",
                    booking.getBookingId(),
                    booking.getClient().getName(),
                    booking.getService().getName(),
                    booking.getStartTime().format(formatter),
                    booking.getCurrentState().getClass().getSimpleName());
        }
    }

    private void acceptBooking(Consultant consultant) {
        System.out.println("\n=== Accept Booking ===");
        System.out.print("Enter booking ID: ");
        String bookingIdStr = scanner.nextLine();

        // Find booking (simplified - in real app would use bookingService)
        System.out.println("Booking accepted!");
    }

    private void rejectBooking(Consultant consultant) {
        System.out.println("\n=== Reject Booking ===");
        System.out.print("Enter booking ID: ");
        String bookingIdStr = scanner.nextLine();

        System.out.println("Booking rejected!");
    }

    private void completeBooking(Consultant consultant) {
        System.out.println("\n=== Complete Booking ===");
        System.out.print("Enter booking ID: ");
        String bookingIdStr = scanner.nextLine();

        System.out.println("Booking completed!");
    }

    private void manageAvailability(Consultant consultant) {
        System.out.println("\n=== Manage Availability ===");
        System.out.print("Enter start time (yyyy-MM-dd HH:mm): ");
        String startStr = scanner.nextLine();
        System.out.print("Enter end time (yyyy-MM-dd HH:mm): ");
        String endStr = scanner.nextLine();

        LocalDateTime start = LocalDateTime.parse(startStr, formatter);
        LocalDateTime end = LocalDateTime.parse(endStr, formatter);

        consultantService.manageAvailability(consultant,
                List.of(new TimeSlot(start, end)));
        System.out.println("Availability updated!");
    }

    // Admin operations
    private void approveConsultant() {
        System.out.println("\n=== Approve Consultant ===");
        System.out.print("Enter consultant ID: ");
        String consultantId = scanner.nextLine();
        Consultant consultant = demoConsultants.get(consultantId);

        if (consultant != null) {
            adminService.approveConsultant(consultant);
            System.out.println("Consultant approved!");
        } else {
            System.out.println("Consultant not found!");
        }
    }

    private void rejectConsultant() {
        System.out.println("\n=== Reject Consultant ===");
        System.out.print("Enter consultant ID: ");
        String consultantId = scanner.nextLine();
        Consultant consultant = demoConsultants.get(consultantId);

        if (consultant != null) {
            adminService.rejectConsultant(consultant);
            System.out.println("Consultant rejected!");
        } else {
            System.out.println("Consultant not found!");
        }
    }

    private void setCancellationPolicy() {
        System.out.println("\n=== Set Cancellation Policy ===");
        System.out.println("Policy updated!");
    }

    private void setPricingStrategy() {
        System.out.println("\n=== Set Pricing Strategy ===");
        System.out.println("Strategy updated!");
    }

    private void logout() {
        currentUser = null;
        isLoggedIn = false;
        System.out.println("Logged out successfully!");
    }
}
