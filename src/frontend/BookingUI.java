package frontend;

import backend.booking.Booking;
import backend.core.*;
import backend.payment.PaymentMethod;
import backend.core.ConsultingService;
import backend.service.ServiceCategory;
import backend.user.*;
import backend.core.TimeSlot;
import backend.policy.*;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class BookingUI {
    private Scanner scanner = new Scanner(System.in);
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Services
    private AdminService adminService;
    private ClientService clientService;
    private ConsultingService consultingService;
    private BookingService bookingService;
    private PaymentService paymentService;
    private UserService userService;

    // Current user
    private User currentUser;
    private boolean isLoggedIn = false;

    // Available services
    private List<ConsultingService> availableServices;

    public static void main(String[] args) {
        BookingUI ui = new BookingUI();
        ui.initialize();
        ui.run();
    }

    public void initialize() {
        // Initialize services
        bookingService = new BookingService();
        consultingService = new ConsultingService();
        userService = new UserService();
        clientService = new ClientService(bookingService, consultingService);
        adminService = new AdminService(consultingService, userService);
        paymentService = new PaymentService();

        // Initialize available services
        initializeServices();
    }

    private void initializeServices() {
        // Create available consulting services
        availableServices = new ArrayList<>();
        availableServices.add(new ConsultingService("Career Counseling", "Professional career guidance and advice", 100.0, 60, ServiceCategory.Career));
        availableServices.add(new ConsultingService("IT Consulting", "Technology and software development advice", 150.0, 90, ServiceCategory.Technology));
        availableServices.add(new ConsultingService("Financial Advisory", "Financial planning and investment advice", 200.0, 60, ServiceCategory.Finance));
        
        System.out.println("System initialized with " + availableServices.size() + " consulting services.");
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
        System.out.println("4. Register as Client");
        System.out.println("5. Register as Consultant");
        System.out.println("6. Exit");
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
                registerClient();
                break;
            case "5":
                registerConsultant();
                break;
            case "6":
                System.out.println("Goodbye!");
                System.exit(0);
                break;
            default:
                System.out.println("Invalid option! Please try again.");
        }
        
        // Continue showing login menu if not logged in
        if (!isLoggedIn) {
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void loginAsClient() {
        System.out.print("Enter client email: ");
        String email = scanner.nextLine();
        Client client = userService.getClientByEmail(email);

        if (client != null) {
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            User authenticatedUser = userService.authenticateUser(email, password);
            
            if (authenticatedUser instanceof Client) {
                // Use UserProxy to control permissions
                UserProxy userProxy = new UserProxy(authenticatedUser);
                currentUser = userProxy;
                userProxy.logIn();
                isLoggedIn = true;
                System.out.println("Welcome, " + client.getName() + "!");
            }
        } else {
            System.out.println("Client not found! Please register first.");
        }
    }

    private void loginAsConsultant() {
        System.out.print("Enter consultant email: ");
        String email = scanner.nextLine();
        Consultant consultant = userService.getConsultantByEmail(email);

        if (consultant != null) {
            System.out.print("Enter password: ");
            String password = scanner.nextLine();
            User authenticatedUser = userService.authenticateUser(email, password);
            
            if (authenticatedUser instanceof Consultant) {
                // Check if consultant is approved
                if (!consultant.isApproved()) {
                    System.out.println("Your account is pending approval. Please wait for admin approval.");
                    return;
                }
                // Use UserProxy to control permissions
                UserProxy userProxy = new UserProxy(authenticatedUser);
                currentUser = userProxy;
                userProxy.logIn();
                isLoggedIn = true;
                System.out.println("Welcome, " + consultant.getName() + "!");
            }
        } else {
            System.out.println("Consultant not found! Please register first.");
        }
    }

    private void loginAsAdmin() {
        Admin admin = new Admin("System Admin", "admin@system.com", "admin");
        // Use UserProxy to control permissions
        UserProxy userProxy = new UserProxy(admin);
        currentUser = userProxy;
        userProxy.logIn();
        isLoggedIn = true;
        System.out.println("Welcome, Administrator!");
    }

    private void registerClient() {
        System.out.println("\n=== Register as Client ===");
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        System.out.print("Enter your email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password (at least 6 characters): ");
        String password = scanner.nextLine();

        Client client = userService.registerClient(name, email, password);
        if (client != null) {
            System.out.println("Registration successful! You can now login.");
        } else {
            System.out.println("Registration failed. Please check your input and try again.");
        }
    }

    private void registerConsultant() {
        System.out.println("\n=== Register as Consultant ===");
        System.out.print("Enter your name: ");
        String name = scanner.nextLine();
        System.out.print("Enter your email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password (at least 6 characters): ");
        String password = scanner.nextLine();

        Consultant consultant = userService.registerConsultant(name, email, password);
        if (consultant != null) {
            System.out.println("Registration successful! You can now login.");
        } else {
            System.out.println("Registration failed. Please check your input and try again.");
        }
    }

    private void showMainmenu() {
        System.out.println("\n=== Main Menu ===");
        System.out.println("Logged in as: " + currentUser.getName() + " (" + currentUser.getAccountType() + ")");

        // Get the real user from proxy if needed
        User realUser = currentUser;
        if (currentUser instanceof UserProxy) {
            // Use reflection or casting to get the real user
            try {
                java.lang.reflect.Field field = UserProxy.class.getDeclaredField("realUser");
                field.setAccessible(true);
                realUser = (User) field.get(currentUser);
            } catch (Exception e) {
                System.out.println("Error accessing real user: " + e.getMessage());
            }
        }

        if (realUser instanceof Client) {
            showClientMenu();
        } else if (realUser instanceof Consultant) {
            showConsultantMenu();
        } else if (realUser instanceof Admin) {
            showAdminMenu();
        }
    }

    private void showClientMenu() {
        // Get the real client from proxy
        Client client = getRealUser(Client.class);
        if (client == null) {
            System.out.println("Error: Not a valid client session.");
            return;
        }

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
        
        // Pause after menu operation
        if (isLoggedIn) {
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void showConsultantMenu() {
        // Get the real consultant from proxy
        Consultant consultant = getRealUser(Consultant.class);
        if (consultant == null) {
            System.out.println("Error: Not a valid consultant session.");
            return;
        }

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
        
        // Pause after menu operation
        if (isLoggedIn) {
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
        }
    }

    private void showAdminMenu() {
        // Get the real admin from proxy
        Admin admin = getRealUser(Admin.class);
        if (admin == null) {
            System.out.println("Error: Not a valid admin session.");
            return;
        }

        System.out.println("\n--- Admin Options ---");
        System.out.println("1. Approve Consultant");
        System.out.println("2. Reject Consultant");
        System.out.println("3. View Pending Consultants");
        System.out.println("4. Set Cancellation Policy");
        System.out.println("5. Set Pricing Strategy");
        System.out.println("6. Logout");
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
                viewPendingConsultants();
                break;
            case "4":
                setCancellationPolicy();
                break;
            case "5":
                setPricingStrategy();
                break;
            case "6":
                logout();
                break;
            default:
                System.out.println("Invalid option!");
        }
        
        // Pause after menu operation
        if (isLoggedIn) {
            System.out.println("\nPress Enter to continue...");
            scanner.nextLine();
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

        // Select consultant from registered consultants
        Collection<Consultant> allConsultants = userService.getAllConsultants();
        if (allConsultants.isEmpty()) {
            System.out.println("No consultants available yet. Please wait for consultants to register.");
            return;
        }
        
        System.out.println("\nAvailable Consultants:");
        int i = 1;
        for (Consultant consultant : allConsultants) {
            System.out.printf("%d. %s%n", i++, consultant.getName());
        }
        System.out.print("Select consultant: ");
        int consultantIndex = Integer.parseInt(scanner.nextLine());
        
        if (consultantIndex < 1 || consultantIndex > allConsultants.size()) {
            System.out.println("Invalid consultant selection!");
            return;
        }
        
        Consultant consultant = (Consultant) allConsultants.toArray()[consultantIndex - 1];

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

        // Check if client has saved payment methods
        List<PaymentMethod> savedMethods = clientService.getClientPaymentMethods(client);
        
        PaymentMethod selectedMethod = null;
        Map<String, String> paymentDetails = null;
        
        if (!savedMethods.isEmpty()) {
            // Client has saved payment methods, let them choose
            System.out.println("\nYour Saved Payment Methods:");
            int i = 1;
            for (PaymentMethod method : savedMethods) {
                System.out.printf("%d. %s%n", i++, method);
            }
            System.out.printf("%d. Add New Payment Method%n", savedMethods.size() + 1);
            System.out.print("Select payment method (1-" + (savedMethods.size() + 1) + "): ");
            
            String methodChoice = scanner.nextLine();
            
            try {
                int choice = Integer.parseInt(methodChoice);
                
                if (choice >= 1 && choice <= savedMethods.size()) {
                    // Use existing payment method
                    selectedMethod = savedMethods.get(choice - 1);
                    // Need to collect payment details for validation
                    paymentDetails = collectPaymentDetails(selectedMethod);
                } else if (choice == savedMethods.size() + 1) {
                    // Add new payment method
                    selectedMethod = addNewPaymentMethodForPayment(client);
                    if (selectedMethod != null) {
                        // Collect details for the newly added method
                        paymentDetails = collectPaymentDetails(selectedMethod);
                    }
                } else {
                    System.out.println("Invalid selection!");
                    return;
                }
            } catch (NumberFormatException e) {
                System.out.println("Invalid input!");
                return;
            }
        } else {
            // No saved payment methods, must add a new one
            System.out.println("\nNo saved payment methods found. Please add a payment method.");
            selectedMethod = addNewPaymentMethodForPayment(client);
            if (selectedMethod != null) {
                paymentDetails = collectPaymentDetails(selectedMethod);
            }
        }
        
        if (selectedMethod == null || paymentDetails == null) {
            System.out.println("Payment method setup cancelled.");
            return;
        }
        
        // Process payment with selected method and details
        boolean success = paymentService.processPayment(targetBooking, selectedMethod, paymentDetails);
        System.out.println(success ? "Payment successful!" : "Payment failed!");
    }

    private void managePaymentMethods(Client client) {
        System.out.println("\n=== Manage Payment Methods ===");
        
        while (true) {
            System.out.println("\n1. Add Payment Method");
            System.out.println("2. View My Payment Methods");
            System.out.println("3. Remove Payment Method");
            System.out.println("4. Back to Main Menu");
            System.out.print("Choose option: ");

            String choice = scanner.nextLine();

            switch (choice) {
                case "1":
                    addPaymentMethod(client);
                    break;
                case "2":
                    viewPaymentMethods(client);
                    break;
                case "3":
                    removePaymentMethod(client);
                    break;
                case "4":
                    return;
                default:
                    System.out.println("Invalid option! Please try again.");
            }
        }
    }
    
    private void addPaymentMethod(Client client) {
        System.out.println("\n--- Add Payment Method ---");
        System.out.println("Available Payment Types:");
        System.out.println("1. Credit Card");
        System.out.println("2. Debit Card");
        System.out.println("3. PayPal");
        System.out.println("4. Bank Transfer");
        System.out.print("Select payment type (1-4): ");
        
        String typeChoice = scanner.nextLine();
        String type;
        Map<String, String> details = new HashMap<>();
        
        switch (typeChoice) {
            case "1":
                type = "Credit";
                System.out.print("Enter card number (16 digits): ");
                details.put("cardNumber", scanner.nextLine());
                System.out.print("Enter expiry (MM/YY): ");
                details.put("expiry", scanner.nextLine());
                System.out.print("Enter CVV: ");
                details.put("cvv", scanner.nextLine());
                System.out.print("Enter cardholder name: ");
                details.put("cardholderName", scanner.nextLine());
                break;
                
            case "2":
                type = "Debit";
                System.out.print("Enter card number (16 digits): ");
                details.put("cardNumber", scanner.nextLine());
                System.out.print("Enter expiry (MM/YY): ");
                details.put("expiry", scanner.nextLine());
                System.out.print("Enter CVV: ");
                details.put("cvv", scanner.nextLine());
                System.out.print("Enter cardholder name: ");
                details.put("cardholderName", scanner.nextLine());
                break;
                
            case "3":
                type = "Paypal";
                System.out.print("Enter PayPal email: ");
                details.put("email", scanner.nextLine());
                break;
                
            case "4":
                type = "BankTransfer";
                System.out.print("Enter account number: ");
                details.put("accountNumber", scanner.nextLine());
                System.out.print("Enter routing number: ");
                details.put("routingNumber", scanner.nextLine());
                System.out.print("Enter bank name: ");
                details.put("bankName", scanner.nextLine());
                System.out.print("Enter account holder name: ");
                details.put("accountHolderName", scanner.nextLine());
                break;
                
            default:
                System.out.println("Invalid payment type!");
                return;
        }
        
        try {
            PaymentMethod method = clientService.addPaymentMethod(client, type, details);
            if (method != null) {
                System.out.println("\n✓ Payment method added successfully!");
                System.out.println("Type: " + method);
            } else {
                System.out.println("\n✗ Failed to add payment method.");
            }
        } catch (Exception e) {
            System.out.println("\n✗ Error: " + e.getMessage());
        }
    }
    
    private void viewPaymentMethods(Client client) {
        System.out.println("\n--- Your Payment Methods ---");
        List<PaymentMethod> methods = clientService.getClientPaymentMethods(client);
        
        if (methods.isEmpty()) {
            System.out.println("No payment methods found. Please add a payment method first.");
            return;
        }
        
        System.out.println("You have " + methods.size() + " payment method(s):");
        int i = 1;
        for (PaymentMethod method : methods) {
            System.out.printf("%d. %s%n", i++, method);
        }
    }
    
    private void removePaymentMethod(Client client) {
        System.out.println("\n--- Remove Payment Method ---");
        List<PaymentMethod> methods = clientService.getClientPaymentMethods(client);
        
        if (methods.isEmpty()) {
            System.out.println("No payment methods to remove.");
            return;
        }
        
        System.out.println("Your payment methods:");
        int i = 1;
        for (PaymentMethod method : methods) {
            System.out.printf("%d. %s%n", i++, method);
        }
        
        System.out.print("\nEnter the number of the payment method to remove (1-" + methods.size() + ", or 0 to cancel): ");
        String input = scanner.nextLine();
        
        try {
            int index = Integer.parseInt(input) - 1;
            
            if (index == -1) {
                System.out.println("Removal cancelled.");
                return;
            }
            
            if (index >= 0 && index < methods.size()) {
                boolean success = clientService.removePaymentMethod(client, index);
                if (success) {
                    System.out.println("✓ Payment method removed successfully!");
                } else {
                    System.out.println("✗ Failed to remove payment method.");
                }
            } else {
                System.out.println("Invalid selection! Please enter a number between 1 and " + methods.size() + ".");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input! Please enter a number.");
        }
    }

    // Consultant operations
    private void viewConsultantBookings(Consultant consultant) {
        System.out.println("\n=== My Bookings ===");
        List<Booking> bookings = consultingService.getConsultantBookings(consultant);

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

        List<Booking> bookings = consultingService.getConsultantBookings(consultant);
        for (Booking booking : bookings) {
            if (booking.getBookingId().toString().equals(bookingIdStr)) {
                try {
                    consultingService.acceptBooking(consultant, booking);
                    System.out.println("Booking accepted successfully!");
                    return;
                } catch (Exception e) {
                    System.out.println("Failed to accept booking: " + e.getMessage());
                    return;
                }
            }
        }
        System.out.println("Booking not found!");
    }

    private void rejectBooking(Consultant consultant) {
        System.out.println("\n=== Reject Booking ===");
        System.out.print("Enter booking ID: ");
        String bookingIdStr = scanner.nextLine();

        List<Booking> bookings = consultingService.getConsultantBookings(consultant);
        for (Booking booking : bookings) {
            if (booking.getBookingId().toString().equals(bookingIdStr)) {
                try {
                    consultingService.rejectBooking(consultant, booking);
                    System.out.println("Booking rejected successfully!");
                    return;
                } catch (Exception e) {
                    System.out.println("Failed to reject booking: " + e.getMessage());
                    return;
                }
            }
        }
        System.out.println("Booking not found!");
    }

    private void completeBooking(Consultant consultant) {
        System.out.println("\n=== Complete Booking ===");
        System.out.print("Enter booking ID: ");
        String bookingIdStr = scanner.nextLine();

        List<Booking> bookings = consultingService.getConsultantBookings(consultant);
        for (Booking booking : bookings) {
            if (booking.getBookingId().toString().equals(bookingIdStr)) {
                try {
                    consultingService.completeBooking(consultant, booking);
                    System.out.println("Booking completed successfully!");
                    return;
                } catch (Exception e) {
                    System.out.println("Failed to complete booking: " + e.getMessage());
                    return;
                }
            }
        }
        System.out.println("Booking not found!");
    }

    private void manageAvailability(Consultant consultant) {
        System.out.println("\n=== Manage Availability ===");
        System.out.print("Enter start time (yyyy-MM-dd HH:mm): ");
        String startStr = scanner.nextLine();
        System.out.print("Enter end time (yyyy-MM-dd HH:mm): ");
        String endStr = scanner.nextLine();

        LocalDateTime start = LocalDateTime.parse(startStr, formatter);
        LocalDateTime end = LocalDateTime.parse(endStr, formatter);

        consultingService.manageAvailability(consultant,
                List.of(new TimeSlot(start, end)));
        System.out.println("Availability updated!");
    }

    // Admin operations
    private void approveConsultant() {
        System.out.println("\n=== Approve Consultant ===");
        System.out.print("Enter consultant email: ");
        String email = scanner.nextLine();
        Consultant consultant = userService.getConsultantByEmail(email);

        if (consultant != null) {
            boolean success = userService.approveConsultant(email);
            if (success) {
                System.out.println("Consultant " + consultant.getName() + " has been approved and can now login!");
            } else {
                System.out.println("Failed to approve consultant.");
            }
        } else {
            System.out.println("Consultant not found!");
        }
    }

    private void rejectConsultant() {
        System.out.println("\n=== Reject Consultant ===");
        System.out.print("Enter consultant email: ");
        String email = scanner.nextLine();
        Consultant consultant = userService.getConsultantByEmail(email);

        if (consultant != null) {
            boolean success = userService.rejectConsultant(email);
            if (success) {
                System.out.println("Consultant " + consultant.getName() + " has been rejected.");
            } else {
                System.out.println("Failed to reject consultant.");
            }
        } else {
            System.out.println("Consultant not found!");
        }
    }
    
    private void viewPendingConsultants() {
        System.out.println("\n=== Pending Consultants ===");
        
        // Debug output
        System.out.println("Total registered consultants: " + userService.getAllConsultants().size());
        Collection<Consultant> allConsultants = userService.getAllConsultants();
        for (Consultant c : allConsultants) {
            System.out.println("  - " + c.getName() + " (" + c.getEmail() + ") - Approved: " + c.isApproved());
        }
        
        Collection<Consultant> pendingConsultants = userService.getPendingConsultants();
        
        if (pendingConsultants.isEmpty()) {
            System.out.println("No pending consultants waiting for approval.");
            return;
        }
        
        System.out.println("\nConsultants waiting for approval:");
        int i = 1;
        for (Consultant consultant : pendingConsultants) {
            System.out.printf("%d. %s - %s (Registered: %s)%n", 
                i++, 
                consultant.getName(), 
                consultant.getEmail(),
                consultant.getUserID());
        }
        
        System.out.println("\nTo approve or reject, use options 1 or 2 from the admin menu.");
    }

    private void setCancellationPolicy() {
        System.out.println("\n=== Set Cancellation Policy ===");
        System.out.println("Available Cancellation Policies:");
        System.out.println("1. Default Policy (100% refund 48h before, 50% 24h before)");
        System.out.println("2. Flexible Policy (Customizable)");
        System.out.print("Select policy type (1-2): ");
        
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1":
                adminService.setCancellationPolicy(new DefaultCancellationPolicy());
                System.out.println("✓ Default cancellation policy has been set.");
                break;
                
            case "2":
                try {
                    System.out.print("Enter full refund hours before booking (default 48): ");
                    int fullRefundHours = Integer.parseInt(scanner.nextLine());
                    
                    System.out.print("Enter partial refund hours before booking (default 24): ");
                    int partialRefundHours = Integer.parseInt(scanner.nextLine());
                    
                    System.out.print("Enter partial refund percentage (0.0-1.0, default 0.5): ");
                    double partialPercentage = Double.parseDouble(scanner.nextLine());
                    
                    FlexibleCancellationPolicy policy = new FlexibleCancellationPolicy(
                        fullRefundHours, partialRefundHours, partialPercentage
                    );
                    adminService.setCancellationPolicy(policy);
                    System.out.println("✓ Flexible cancellation policy has been set.");
                    System.out.printf("  - Full refund: %d hours before%n", fullRefundHours);
                    System.out.printf("  - Partial refund (%.0f%%): %d hours before%n", 
                        partialPercentage * 100, partialRefundHours);
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input! Using default flexible policy.");
                    adminService.setCancellationPolicy(new FlexibleCancellationPolicy());
                }
                break;
                
            default:
                System.out.println("Invalid option!");
        }
    }

    private void setPricingStrategy() {
        System.out.println("\n=== Set Pricing Strategy ===");
        System.out.println("Available Pricing Strategies:");
        System.out.println("1. Fixed Pricing (Base price only)");
        System.out.println("2. Dynamic Pricing (Varies by time and day)");
        System.out.print("Select strategy type (1-2): ");
        
        String choice = scanner.nextLine();
        
        switch (choice) {
            case "1":
                adminService.setPricingStrategy(new FixedPricingStrategy());
                System.out.println("✓ Fixed pricing strategy has been set.");
                break;
                
            case "2":
                try {
                    DynamicPricingStrategy strategy = new DynamicPricingStrategy();
                    
                    System.out.print("Enter peak hours multiplier (default 1.5): ");
                    String peakInput = scanner.nextLine();
                    if (!peakInput.isEmpty()) {
                        strategy.setPeakMultiplier(Double.parseDouble(peakInput));
                    }
                    
                    System.out.print("Enter off-peak hours multiplier (default 0.8): ");
                    String offPeakInput = scanner.nextLine();
                    if (!offPeakInput.isEmpty()) {
                        strategy.setOffPeakMultiplier(Double.parseDouble(offPeakInput));
                    }
                    
                    adminService.setPricingStrategy(strategy);
                    System.out.println("✓ Dynamic pricing strategy has been set.");
                    System.out.println("  - Peak hours (9AM-5PM weekdays): Higher rates");
                    System.out.println("  - Weekends: 20% surcharge");
                    System.out.println("  - Off-peak: Discounted rates");
                } catch (NumberFormatException e) {
                    System.out.println("Invalid input! Using default dynamic pricing.");
                    adminService.setPricingStrategy(new DynamicPricingStrategy());
                }
                break;
                
            default:
                System.out.println("Invalid option!");
        }
    }

    private void logout() {
        currentUser = null;
        isLoggedIn = false;
        System.out.println("Logged out successfully!");
    }

    /**
     * Helper method to collect payment details based on payment method type
     * @param method The payment method type
     * @return Map containing payment details, or null if cancelled
     */
    private Map<String, String> collectPaymentDetails(PaymentMethod method) {
        Map<String, String> details = new HashMap<>();
        
        System.out.println("\n--- Enter Payment Details for " + method + " ---");
        
        switch (method) {
            case Credit:
            case Debit:
                System.out.print("Enter card number (16 digits): ");
                details.put("cardNumber", scanner.nextLine());
                System.out.print("Enter expiry (MM/YY): ");
                details.put("expiry", scanner.nextLine());
                System.out.print("Enter CVV: ");
                details.put("cvv", scanner.nextLine());
                break;
                
            case Paypal:
                System.out.print("Enter PayPal email: ");
                details.put("email", scanner.nextLine());
                break;
                
            case BankTransfer:
                System.out.print("Enter account number: ");
                details.put("accountNumber", scanner.nextLine());
                System.out.print("Enter routing number: ");
                details.put("routingNumber", scanner.nextLine());
                break;
                
            default:
                System.out.println("Unsupported payment method!");
                return null;
        }
        
        return details;
    }

    /**
     * Helper method to add a new payment method during payment process
     * @param client The client adding the payment method
     * @return The newly created payment method, or null if cancelled
     */
    private PaymentMethod addNewPaymentMethodForPayment(Client client) {
        System.out.println("\n--- Add New Payment Method ---");
        System.out.println("Available Payment Types:");
        System.out.println("1. Credit Card");
        System.out.println("2. Debit Card");
        System.out.println("3. PayPal");
        System.out.println("4. Bank Transfer");
        System.out.print("Select payment type (1-4): ");
        
        String typeChoice = scanner.nextLine();
        String type;
        Map<String, String> details = new HashMap<>();
        
        switch (typeChoice) {
            case "1":
                type = "Credit";
                System.out.print("Enter card number (16 digits): ");
                details.put("cardNumber", scanner.nextLine());
                System.out.print("Enter expiry (MM/YY): ");
                details.put("expiry", scanner.nextLine());
                System.out.print("Enter CVV: ");
                details.put("cvv", scanner.nextLine());
                System.out.print("Enter cardholder name: ");
                details.put("cardholderName", scanner.nextLine());
                break;
                
            case "2":
                type = "Debit";
                System.out.print("Enter card number (16 digits): ");
                details.put("cardNumber", scanner.nextLine());
                System.out.print("Enter expiry (MM/YY): ");
                details.put("expiry", scanner.nextLine());
                System.out.print("Enter CVV: ");
                details.put("cvv", scanner.nextLine());
                System.out.print("Enter cardholder name: ");
                details.put("cardholderName", scanner.nextLine());
                break;
                
            case "3":
                type = "Paypal";
                System.out.print("Enter PayPal email: ");
                details.put("email", scanner.nextLine());
                break;
                
            case "4":
                type = "BankTransfer";
                System.out.print("Enter account number: ");
                details.put("accountNumber", scanner.nextLine());
                System.out.print("Enter routing number: ");
                details.put("routingNumber", scanner.nextLine());
                System.out.print("Enter bank name: ");
                details.put("bankName", scanner.nextLine());
                System.out.print("Enter account holder name: ");
                details.put("accountHolderName", scanner.nextLine());
                break;
                
            default:
                System.out.println("Invalid payment type!");
                return null;
        }
        
        try {
            PaymentMethod method = clientService.addPaymentMethod(client, type, details);
            if (method != null) {
                System.out.println("\n✓ Payment method added successfully!");
                System.out.println("Type: " + method);
                return method;
            } else {
                System.out.println("\n✗ Failed to add payment method.");
                return null;
            }
        } catch (Exception e) {
            System.out.println("\n✗ Error: " + e.getMessage());
            return null;
        }
    }

    /**
     * Helper method to get the real user from UserProxy
     * @param <T> The expected user type
     * @param userType The class of the expected user type
     * @return The real user object, or null if not matching
     */
    private <T extends User> T getRealUser(Class<T> userType) {
        if (currentUser instanceof UserProxy) {
            try {
                java.lang.reflect.Field field = UserProxy.class.getDeclaredField("realUser");
                field.setAccessible(true);
                User realUser = (User) field.get(currentUser);
                if (userType.isInstance(realUser)) {
                    return userType.cast(realUser);
                }
            } catch (Exception e) {
                System.out.println("Error accessing real user: " + e.getMessage());
            }
        } else if (userType.isInstance(currentUser)) {
            return userType.cast(currentUser);
        }
        return null;
    }
}
