package frontend;

import backend.booking.Booking;
import backend.core.*;
import backend.database.*;
import backend.payment.PaymentMethod;
import backend.core.ConsultingService;
import backend.service.ServiceCategory;
import backend.user.*;
import backend.core.TimeSlot;
import backend.policy.*;

import java.net.http.*;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

public class BookingUI {
    private Scanner scanner = new Scanner(System.in);
    private DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private HttpClient httpClient = HttpClient.newHttpClient();
    private String apiBaseUrl = System.getenv("API_BASE_URL") != null
            ? System.getenv("API_BASE_URL")
            : "http://localhost:8080/api";
    private String currentUserId = null;
    private String currentUserEmail = null;

    // Map to store consultant IDs from API (email -> userId)
    private Map<String, String> consultantIdMap = new HashMap<>();
    // Map to store service IDs from API (name -> serviceId)
    private Map<String, String> serviceIdMap = new HashMap<>();

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
        System.out.println("Connecting to API server at " + apiBaseUrl + "...");

        // Test API connection
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/health"))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                System.out.println("✓ Connected to API server successfully!\n");
            } else {
                System.out.println("✗ Failed to connect to API server. Please ensure the server is running.");
                System.out.println("Running in offline mode...\n");
            }
        } catch (Exception e) {
            System.out.println("✗ API server not available: " + e.getMessage());
            System.out.println("Running in offline mode...\n");
        }

        // Initialize services (for offline mode fallback)
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
        // Try to load services from API first
        availableServices = new ArrayList<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/services"))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                // Parse services from JSON response
                if (responseBody.contains("\"services\":")) {
                    int start = responseBody.indexOf("\"services\":[");
                    int arrayStart = responseBody.indexOf("[", start);
                    int arrayEnd = responseBody.indexOf("]", arrayStart);
                    String servicesJson = responseBody.substring(arrayStart, arrayEnd + 1);

                    int pos = 0;
                    while (pos < servicesJson.length()) {
                        int objStart = servicesJson.indexOf("{", pos);
                        if (objStart == -1) break;
                        int objEnd = servicesJson.indexOf("}", objStart);
                        if (objEnd == -1) break;

                        String obj = servicesJson.substring(objStart, objEnd + 1);

                        String serviceId = extractJsonValue(obj, "serviceId");
                        String name = extractJsonValue(obj, "name");
                        String description = extractJsonValue(obj, "description");
                        String priceStr = extractJsonValue(obj, "basePrice");
                        String durationStr = extractJsonValue(obj, "durationMinutes");
                        String category = extractJsonValue(obj, "category");

                        if (serviceId != null && name != null && priceStr != null && durationStr != null) {
                            double price = Double.parseDouble(priceStr);
                            int duration = Integer.parseInt(durationStr);
                            ServiceCategory cat = ServiceCategory.valueOf(category != null ? category : "Career");

                            ConsultingService service = new ConsultingService(name, description, price, duration, cat);
                            availableServices.add(service);
                            // Store service ID mapping
                            serviceIdMap.put(name, serviceId);
                        }

                        pos = objEnd + 1;
                    }

                    if (!availableServices.isEmpty()) {
                        System.out.println("Loaded " + availableServices.size() + " services from database.");
                        return;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error loading services from API: " + e.getMessage());
        }

        // Fallback to hardcoded services
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
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try {
            // Call API login endpoint
            String jsonData = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/login"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                // Parse response to get user info
                if (responseBody.contains("\"success\":true")) {
                    // Extract user ID and info from response
                    currentUserId = extractJsonValue(responseBody, "userId");
                    currentUserEmail = email;
                    isLoggedIn = true;
                    String userName = extractJsonValue(responseBody, "name");
                    System.out.println("Welcome, " + userName + "!");

                    // Create a temporary user object for API login (since we don't have the full User object)
                    // This allows main menu to work even without local user object
                    try {
                        Client tempClient = new Client(userName, email, "dummy");
                        UserProxy userProxy = new UserProxy(tempClient);
                        userProxy.logIn();
                        currentUser = userProxy;
                    } catch (Exception e) {
                        // Ignore - we'll handle null currentUser in showMainmenu
                    }
                    return;
                }
            }

            System.out.println("Login failed! Invalid credentials.");
        } catch (Exception e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            System.out.println("Falling back to local authentication...");

            // Fallback to local authentication
            Client client = userService.getClientByEmail(email);
            if (client != null) {
                System.out.print("Enter password: ");
                String inputPassword = scanner.nextLine();
                User authenticatedUser = userService.authenticateUser(email, inputPassword);

                if (authenticatedUser instanceof Client) {
                    UserProxy userProxy = new UserProxy(authenticatedUser);
                    currentUser = userProxy;
                    userProxy.logIn();
                    isLoggedIn = true;
                    currentUserId = client.getUserID().toString();
                    currentUserEmail = email;
                    System.out.println("Welcome, " + client.getName() + "!");
                }
            } else {
                System.out.println("Client not found! Please register first.");
            }
        }
    }

    private void loginAsConsultant() {
        System.out.print("Enter consultant email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try {
            String jsonData = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/login"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                if (responseBody.contains("\"success\":true")) {
                    currentUserId = extractJsonValue(responseBody, "userId");
                    currentUserEmail = email;
                    isLoggedIn = true;
                    String userName = extractJsonValue(responseBody, "name");
                    System.out.println("Welcome, " + userName + "!");

                    // Create a temporary user object for API login
                    try {
                        Consultant tempConsultant = new Consultant(userName, email, "dummy");
                        UserProxy userProxy = new UserProxy(tempConsultant);
                        userProxy.logIn();
                        currentUser = userProxy;
                    } catch (Exception ex) {
                        // Ignore
                    }
                    return;
                }
            }

            System.out.println("Login failed! Invalid credentials or account pending approval.");
        } catch (Exception e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            System.out.println("Falling back to local authentication...");

            Consultant consultant = userService.getConsultantByEmail(email);
            if (consultant != null) {
                System.out.print("Enter password: ");
                String inputPassword = scanner.nextLine();
                User authenticatedUser = userService.authenticateUser(email, inputPassword);

                if (authenticatedUser instanceof Consultant) {
                    if (!consultant.isApproved()) {
                        System.out.println("Your account is pending approval. Please wait for admin approval.");
                        return;
                    }
                    UserProxy userProxy = new UserProxy(authenticatedUser);
                    currentUser = userProxy;
                    userProxy.logIn();
                    isLoggedIn = true;
                    currentUserId = consultant.getUserID().toString();
                    currentUserEmail = email;
                    System.out.println("Welcome, " + consultant.getName() + "!");
                }
            } else {
                System.out.println("Consultant not found! Please register first.");
            }
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

        try {
            String jsonData = "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"accountType\":\"Client\"}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/register"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Registration successful! You can now login.");
                return;
            }
        } catch (Exception e) {
            System.out.println("Server registration failed, trying local registration...");
        }

        // Fallback to local registration
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

        try {
            String jsonData = "{\"name\":\"" + name + "\",\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"accountType\":\"Consultant\"}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/register"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Registration successful! You can now login after admin approval.");
                return;
            }
        } catch (Exception e) {
            System.out.println("Server registration failed, trying local registration...");
        }

        // Fallback to local registration
        Consultant consultant = userService.registerConsultant(name, email, password);
        if (consultant != null) {
            System.out.println("Registration successful! You can now login after admin approval.");
        } else {
            System.out.println("Registration failed. Please check your input and try again.");
        }
    }

    private void showMainmenu() {
        // Guard against null currentUser (can happen if API login succeeds but local user not set)
        if (currentUser == null) {
            // Try to recover from API login (which only sets currentUserId and currentUserEmail)
            if (currentUserEmail != null && currentUserId != null) {
                // Create a temporary user object for API login case
                System.out.println("\n=== Main Menu ===");
                System.out.println("Logged in as: " + currentUserEmail + " (Client)");
            } else {
                System.out.println("Session error. Please login again.");
                isLoggedIn = false;
            }
            return;
        }

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
        System.out.println("7. View Payment History");
        System.out.println("8. AI Customer Assistant");
        System.out.println("9. Logout");
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
                viewPaymentHistory(client);
                break;
            case "8":
                launchAIChatbot(client);
                break;
            case "9":
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
        System.out.println("4. View System Status");
        System.out.println("5. Set Cancellation Policy");
        System.out.println("6. Set Pricing Strategy");
        System.out.println("7. Logout");
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
                viewSystemStatus();
                break;
            case "5":
                setCancellationPolicy();
                break;
            case "6":
                setPricingStrategy();
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

    // Client operations
    private void browseServices() {
        System.out.println("\n=== Available Services ===");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/services"))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                // Parse and display services from API
                System.out.println("Loading services from server...");
                // Simple parsing - in production use JSON library
                if (responseBody.contains("\"services\"")) {
                    // Services returned from API
                    for (int i = 0; i < availableServices.size(); i++) {
                        ConsultingService service = availableServices.get(i);
                        System.out.printf("%d. %s - $%.2f (%d min)%n",
                                i + 1, service.getName(), service.getBasePrice(), service.getDurationMinutes());
                        System.out.println("   " + service.getDescription());
                    }
                    return;
                }
            }
        } catch (Exception e) {
            // Fall through to local services
        }

        // Display local services as fallback
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

        // Get consultants from API
        List<Consultant> consultants = getConsultantsFromAPI();
        if (consultants.isEmpty()) {
            System.out.println("No consultants available yet. Please wait for consultants to register.");
            return;
        }

        System.out.println("\nAvailable Consultants:");
        int i = 1;
        for (Consultant consultant : consultants) {
            System.out.printf("%d. %s%n", i++, consultant.getName());
        }
        System.out.print("Select consultant: ");
        int consultantIndex = Integer.parseInt(scanner.nextLine());

        if (consultantIndex < 1 || consultantIndex > consultants.size()) {
            System.out.println("Invalid consultant selection!");
            return;
        }

        Consultant consultant = consultants.get(consultantIndex - 1);

        // Enter start time
        System.out.print("Enter start time (yyyy-MM-dd HH:mm): ");
        String timeStr = scanner.nextLine();
        LocalDateTime startTime = LocalDateTime.parse(timeStr, formatter);

        try {
            // Try API first
            // Get consultant ID from the map (API returns userId, Consultant object has random UUID)
            String consultantId = consultantIdMap.containsKey(consultant.getEmail())
                ? consultantIdMap.get(consultant.getEmail())
                : consultant.getUserID().toString();

            // Get service ID from the map (API returns UUID, local object has random UUID)
            String serviceId = serviceIdMap.containsKey(service.getName())
                ? serviceIdMap.get(service.getName())
                : service.getServiceId().toString();

            String jsonData = "{\"clientId\":\"" + currentUserId + "\",\"consultantId\":\"" +
                             consultantId + "\",\"serviceId\":\"" +
                             serviceId + "\",\"startTime\":\"" +
                             startTime.toString().replace("T", " ") + "\"}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/bookings/create"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Booking created successfully via API!");
                return;
            }
        } catch (Exception e) {
            System.out.println("API booking failed, using local booking...");
        }

        // Fallback to local booking
        try {
            Booking booking = clientService.requestBooking(client, service, consultant, startTime);
            System.out.println("Booking created successfully! ID: " + booking.getBookingId());
        } catch (Exception e) {
            System.out.println("Failed to create booking: " + e.getMessage());
        }
    }

    private List<Consultant> getConsultantsFromAPI() {
        List<Consultant> consultants = new ArrayList<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/consultants"))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                // Parse consultants from JSON response
                if (responseBody.contains("\"consultants\":")) {
                    // Extract consultants array
                    int start = responseBody.indexOf("\"consultants\":[");
                    int arrayStart = responseBody.indexOf("[", start);
                    int arrayEnd = responseBody.indexOf("]", arrayStart);
                    String consultantsJson = responseBody.substring(arrayStart, arrayEnd + 1);

                    // Parse each consultant object
                    int pos = 0;
                    while (pos < consultantsJson.length()) {
                        int objStart = consultantsJson.indexOf("{", pos);
                        if (objStart == -1) break;
                        int objEnd = consultantsJson.indexOf("}", objStart);
                        if (objEnd == -1) break;

                        String obj = consultantsJson.substring(objStart, objEnd + 1);

                        // Extract fields
                        String userId = extractJsonValue(obj, "userId");
                        String name = extractJsonValue(obj, "name");
                        String email = extractJsonValue(obj, "email");
                        String isApproved = extractJsonValue(obj, "isApproved");

                        if (userId != null && name != null && email != null) {
                            // Only add approved consultants
                            if ("true".equalsIgnoreCase(isApproved)) {
                                Consultant c = new Consultant(name, email, "dummy");
                                // We'll use email as key for now
                                consultants.add(c);
                                // Store the ID mapping for booking
                                consultantIdMap.put(email, userId);
                            }
                        }

                        pos = objEnd + 1;
                    }

                    if (!consultants.isEmpty()) {
                        System.out.println("Loaded " + consultants.size() + " consultants from server.");
                        return consultants;
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching consultants from API: " + e.getMessage());
        }

        // Fallback to local consultants - ONLY approved ones
        try {
            Collection<Consultant> allConsultants = userService.getAllConsultants();
            for (Consultant c : allConsultants) {
                if (c.isApproved()) {
                    consultants.add(c);
                }
            }
        } catch (Exception e) {
            // Ignore - local service might not be initialized
        }

        return consultants;
    }

    private void viewMyBookings(Client client) {
        System.out.println("\n=== My Bookings ===");

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/bookings/client?clientId=" + currentUserId))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                if (responseBody.contains("\"bookings\":")) {
                    int arrayStart = responseBody.indexOf("[", responseBody.indexOf("\"bookings\":"));
                    int arrayEnd = responseBody.lastIndexOf("]");
                    String bookingsJson = responseBody.substring(arrayStart, arrayEnd + 1);

                    if (bookingsJson.equals("[]")) {
                        System.out.println("No bookings found.");
                        return;
                    }

                    int pos = 0;
                    int count = 0;
                    while (pos < bookingsJson.length()) {
                        int objStart = bookingsJson.indexOf("{", pos);
                        if (objStart == -1) break;
                        int objEnd = bookingsJson.indexOf("}", objStart);
                        if (objEnd == -1) break;
                        String obj = bookingsJson.substring(objStart, objEnd + 1);

                        count++;
                        System.out.printf("%d. ID: %s%n   Service: %s | Consultant: %s%n   Time: %s | Status: %s%n%n",
                            count,
                            extractJsonValue(obj, "bookingId"),
                            extractJsonValue(obj, "serviceName"),
                            extractJsonValue(obj, "consultantName"),
                            extractJsonValue(obj, "startTime"),
                            extractJsonValue(obj, "status"));

                        pos = objEnd + 1;
                    }
                    if (count == 0) System.out.println("No bookings found.");
                    return;
                }
            }
        } catch (Exception e) {
            System.err.println("Error fetching bookings: " + e.getMessage());
        }

        System.out.println("Unable to load bookings from server.");
    }

    private void cancelBooking(Client client) {
        System.out.println("\n=== Cancel Booking ===");

        // Show current bookings first
        viewMyBookings(client);

        System.out.print("Enter booking ID to cancel: ");
        String bookingIdStr = scanner.nextLine();

        try {
            String jsonData = "{\"bookingId\":\"" + bookingIdStr + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/bookings/cancel"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Booking cancelled successfully!");
            } else {
                String error = extractJsonValue(response.body(), "error");
                System.out.println("Failed to cancel: " + (error != null ? error : response.body()));
            }
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void makePayment(Client client) {
        System.out.println("\n=== Make Payment ===");

        // Show current bookings first
        viewMyBookings(client);

        System.out.print("Enter booking ID to pay: ");
        String bookingIdStr = scanner.nextLine();

        // Select payment method
        System.out.println("\nSelect Payment Method:");
        System.out.println("1. CreditCard");
        System.out.println("2. DebitCard");
        System.out.println("3. PayPal");
        System.out.println("4. BankTransfer");
        System.out.print("Choose (1-4): ");
        String methodChoice = scanner.nextLine();

        String paymentMethod;
        switch (methodChoice) {
            case "1": paymentMethod = "CreditCard"; break;
            case "2": paymentMethod = "DebitCard"; break;
            case "3": paymentMethod = "PayPal"; break;
            case "4": paymentMethod = "BankTransfer"; break;
            default: paymentMethod = "CreditCard";
        }

        System.out.print("Enter amount: $");
        String amountStr = scanner.nextLine();
        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            System.out.println("Invalid amount!");
            return;
        }

        try {
            String jsonData = "{\"bookingId\":\"" + bookingIdStr +
                "\",\"paymentMethod\":\"" + paymentMethod +
                "\",\"amount\":" + amount + "}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/payments/pay"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                String paymentId = extractJsonValue(response.body(), "paymentId");
                System.out.println("Payment successful! Transaction ID: " + paymentId);
            } else {
                String error = extractJsonValue(response.body(), "error");
                System.out.println("Payment failed: " + (error != null ? error : response.body()));
            }
        } catch (Exception e) {
            System.out.println("Error processing payment: " + e.getMessage());
        }
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
                type = "CreditCard";
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
                type = "DebitCard";
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
                type = "PayPal";
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

        // Try API first
        try {
            // Build masked details for storage
            String maskedDetails = getMaskedPaymentInfoForStorage(type, details);
            
            String jsonData = "{\"clientId\":\"" + currentUserId + "\",\"paymentType\":\"" + 
                type + "\",\"maskedDetails\":\"" + maskedDetails + "\"}";

            System.out.println("Sending payment method request to: " + apiBaseUrl + "/payment-methods/add");
            System.out.println("Request data: " + jsonData);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/payment-methods/add"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("API Response status: " + response.statusCode());
            System.out.println("API Response body: " + response.body());

            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("\n✓ Payment method added successfully to database!");
                return;
            } else {
                System.out.println("API failed (status: " + response.statusCode() + "), using local storage...");
            }
        } catch (Exception e) {
            System.out.println("Error connecting to API: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Using local storage...");
        }

        // Fallback to local
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
        List<Map<String, Object>> methods = clientService.getClientPaymentMethods(client);

        if (methods.isEmpty()) {
            System.out.println("No payment methods found. Please add a payment method first.");
            return;
        }

        System.out.println("You have " + methods.size() + " payment method(s):");
        int i = 1;
        for (Map<String, Object> paymentInfo : methods) {
            PaymentMethod method = (PaymentMethod) paymentInfo.get("method");
            @SuppressWarnings("unchecked")
            Map<String, String> details = (Map<String, String>) paymentInfo.get("details");
            String displayInfo = getMaskedPaymentInfo(method, details);
            System.out.printf("%d. %s - %s%n", i++, method, displayInfo);
        }
    }

    private void removePaymentMethod(Client client) {
        System.out.println("\n--- Remove Payment Method ---");
        List<Map<String, Object>> methods = clientService.getClientPaymentMethods(client);

        if (methods.isEmpty()) {
            System.out.println("No payment methods to remove.");
            return;
        }

        System.out.println("Your payment methods:");
        int i = 1;
        for (Map<String, Object> paymentInfo : methods) {
            PaymentMethod method = (PaymentMethod) paymentInfo.get("method");
            @SuppressWarnings("unchecked")
            Map<String, String> details = (Map<String, String>) paymentInfo.get("details");
            String displayInfo = getMaskedPaymentInfo(method, details);
            System.out.printf("%d. %s - %s%n", i++, method, displayInfo);
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

    private void viewPaymentHistory(Client client) {
        System.out.println("\n=== Payment History ===");
        List<backend.payment.PaymentTransaction> transactions = clientService.viewPaymentHistory(client);

        if (transactions.isEmpty()) {
            System.out.println("No payment history found.");
            return;
        }

        System.out.println("Your payment transactions:");
        int i = 1;
        for (backend.payment.PaymentTransaction transaction : transactions) {
            System.out.printf("%d. Transaction ID: %s | Amount: $%.2f | Method: %s | Status: %s | Date: %s%n",
                    i++,
                    transaction.getTransactionId(),
                    transaction.getAmount(),
                    transaction.getPaymentMethod(),
                    transaction.getStatus(),
                    transaction.getTimestamp().format(formatter));
        }
    }

    private void launchAIChatbot(Client client) {
        System.out.println("\n===========================================");
        System.out.println("   Welcome to AI Customer Assistant");
        System.out.println("===========================================");

        System.out.println("\n--- AI Chatbot ---");
        System.out.println("I can help you with booking, payments, services, and more.");
        System.out.println("Type 'quit' or 'exit' to leave.\n");

        while (true) {
            System.out.print("You: ");
            String userInput = scanner.nextLine();

            if (userInput.equalsIgnoreCase("quit") || userInput.equalsIgnoreCase("exit")) {
                System.out.println("Thank you for using AI Customer Assistant. Goodbye!");
                break;
            }

            // Call backend API
            try {
                String jsonData = "{\"message\":\"" + userInput.replace("\"", "\\\"") + "\"}";
                HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(apiBaseUrl + "/ai/chat"))
                    .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                    .header("Content-Type", "application/json")
                    .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200) {
                    String responseBody = response.body();
                    String aiResponse = extractJsonValue(responseBody, "response");
                    System.out.println("AI Assistant: " + aiResponse);
                } else {
                    System.out.println("AI Assistant: Sorry, I couldn't get a response. Please try again.");
                }
            } catch (Exception e) {
                System.out.println("AI Assistant: Error connecting to server. Using offline mode.");
                // Fallback to local chatbot
                AIChatbotService localChatbot = new AIChatbotService();
                String response = localChatbot.getResponse(userInput);
                System.out.println("AI Assistant: " + response);
            }
        }
    }

    // Consultant operations
    private void viewConsultantBookings(Consultant consultant) {
        System.out.println("\n=== My Bookings ===");

        // Try API first, fallback to local
        List<Booking> bookings = null;
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/bookings/consultant?consultantId=" + currentUserId))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200) {
                System.out.println("Loading from server...");
                // Parse API response - for now fall back to local
            }
        } catch (Exception e) {
            System.out.println("Using local data...");
        }

        // Fallback to local
        if (bookings == null) {
            bookings = consultingService.getConsultantBookings(consultant);
        }

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

        // Try API first
        try {
            String jsonData = "{\"bookingId\":\"" + bookingIdStr + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/bookings/confirm"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Booking accepted successfully!");
                return;
            }
        } catch (Exception e) {
            // Fallback to local
        }

        // Fallback to local
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

        // Try API
        try {
            String jsonData = "{\"bookingId\":\"" + bookingIdStr + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/bookings/reject"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Booking rejected successfully!");
                return;
            }
        } catch (Exception e) {
            // Fallback
        }

        // Fallback to local
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

        // Try API
        try {
            String jsonData = "{\"bookingId\":\"" + bookingIdStr + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/bookings/complete"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Booking completed successfully!");
                return;
            }
        } catch (Exception e) {
            // Fallback
        }

        // Fallback to local
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

        // Try API first
        try {
            String jsonData = "{\"consultantId\":\"" + currentUserId + "\",\"startTime\":\"" +
                start.toString().replace("T", " ") + "\",\"endTime\":\"" +
                end.toString().replace("T", " ") + "\"}";

            System.out.println("Sending availability request to: " + apiBaseUrl + "/availability/set");
            System.out.println("Request data: " + jsonData);

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/availability/set"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            System.out.println("API Response status: " + response.statusCode());
            System.out.println("API Response body: " + response.body());

            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Availability updated successfully via API!");
                return;
            } else {
                System.out.println("API update failed (status: " + response.statusCode() + "), using local update...");
            }
        } catch (Exception e) {
            System.out.println("Error connecting to API: " + e.getMessage());
            e.printStackTrace();
            System.out.println("Using local update...");
        }

        // Fallback to local
        consultingService.manageAvailability(consultant,
                List.of(new TimeSlot(start, end)));
        System.out.println("Availability updated locally!");
    }

    // Admin operations
    private void approveConsultant() {
        System.out.println("\n=== Approve Consultant ===");
        System.out.print("Enter consultant email: ");
        String email = scanner.nextLine();

        // Try API first
        try {
            String jsonData = "{\"email\":\"" + email + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/approve-consultant"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Consultant approved successfully!");
                return;
            }
        } catch (Exception e) {
            // Fallback to local
        }

        // Fallback to local
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

        // Try API
        try {
            String jsonData = "{\"email\":\"" + email + "\"}";
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/reject-consultant"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Consultant rejected successfully!");
                return;
            }
        } catch (Exception e) {
            // Fallback
        }

        // Fallback to local
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

    private void viewSystemStatus() {
        System.out.println("\n===========================================");
        System.out.println("          SYSTEM STATUS DASHBOARD");
        System.out.println("===========================================");

        Map<String, Object> status = adminService.getSystemStatus();

        System.out.println("\n--- User Statistics ---");
        System.out.printf("Total Users: %d%n", status.get("total_users"));
        System.out.printf("Total Clients: %d%n", status.get("total_clients"));
        System.out.printf("Total Consultants: %d%n", status.get("total_consultants"));
        System.out.printf("  - Approved: %d%n", status.get("approved_consultants"));
        System.out.printf("  - Pending Approval: %d%n", status.get("pending_consultants"));

        System.out.println("\n--- Service Information ---");
        System.out.printf("Total Services Available: %d%n", status.get("total_services"));

        System.out.println("\n--- Active Policies ---");
        System.out.printf("Cancellation Policy: %s%n", status.get("cancellation_policy"));
        System.out.printf("Pricing Strategy: %s%n", status.get("pricing_strategy"));

        System.out.println("\n--- System Health ---");
        System.out.println("Database Connection: " +
            (backend.database.DatabaseConnection.getInstance().isConnected() ? "✓ Connected" : "✗ Disconnected"));
        System.out.println("System Status: ✓ Operational");

        System.out.println("\n===========================================");
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
     * Get masked payment info for display
     */
    private String getMaskedPaymentInfo(PaymentMethod method, Map<String, String> details) {
        switch (method) {
            case CreditCard:
            case DebitCard:
                String cardNum = details.get("cardNumber");
                if (cardNum != null && cardNum.length() >= 4) {
                    return "**** **** **** " + cardNum.substring(cardNum.length() - 4);
                }
                return "Card";
            case PayPal:
                String email = details.get("email");
                return email != null ? email : "PayPal";
            case BankTransfer:
                String account = details.get("accountNumber");
                if (account != null && account.length() >= 4) {
                    return "Account ****" + account.substring(account.length() - 4);
                }
                return "Bank Transfer";
            default:
                return "";
        }
    }

    /**
     * Get masked payment info for database storage
     */
    private String getMaskedPaymentInfoForStorage(String type, Map<String, String> details) {
        StringBuilder masked = new StringBuilder();
        
        switch (type) {
            case "CreditCard":
            case "DebitCard":
                String cardNum = details.get("cardNumber");
                if (cardNum != null && cardNum.length() >= 4) {
                    masked.append("**** **** **** ").append(cardNum.substring(cardNum.length() - 4));
                } else {
                    masked.append("Card");
                }
                String expiry = details.get("expiry");
                if (expiry != null) {
                    masked.append(" (Exp: ").append(expiry).append(")");
                }
                break;
            
            case "PayPal":
                String email = details.get("email");
                masked.append(email != null ? email : "PayPal Account");
                break;
            
            case "BankTransfer":
                String account = details.get("accountNumber");
                if (account != null && account.length() >= 4) {
                    masked.append("Account ****").append(account.substring(account.length() - 4));
                } else {
                    masked.append("Bank Account");
                }
                String bankName = details.get("bankName");
                if (bankName != null) {
                    masked.append(" (").append(bankName).append(")");
                }
                break;
            
            default:
                masked.append(type);
        }
        
        return masked.toString().replace("\"", "'"); // Escape quotes for JSON
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
            case CreditCard:
            case DebitCard:
                System.out.print("Enter card number (16 digits): ");
                details.put("cardNumber", scanner.nextLine());
                System.out.print("Enter expiry (MM/YY): ");
                details.put("expiry", scanner.nextLine());
                System.out.print("Enter CVV: ");
                details.put("cvv", scanner.nextLine());
                break;

            case PayPal:
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

    /**
     * Helper method to extract simple JSON value
     * @param json JSON string
     * @param key Key to extract
     * @return Extracted value or empty string
     */
    private String extractJsonValue(String json, String key) {
        if (json == null || key == null) return "";

        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        int startIndex = keyIndex + searchKey.length();

        // Skip whitespace
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }

        if (startIndex >= json.length()) return "";

        char startChar = json.charAt(startIndex);

        if (startChar == '"') {
            // String value
            startIndex++;
            int endIndex = json.indexOf('"', startIndex);
            return endIndex > startIndex ? json.substring(startIndex, endIndex) : "";
        } else if (startChar == 't' || startChar == 'f') {
            // Boolean value
            int endIndex = json.indexOf(',', startIndex);
            if (endIndex == -1) endIndex = json.indexOf('}', startIndex);
            return endIndex > startIndex ? json.substring(startIndex, endIndex) : "";
        } else {
            // Number or other
            int endIndex = json.indexOf(',', startIndex);
            if (endIndex == -1) endIndex = json.indexOf('}', startIndex);
            return endIndex > startIndex ? json.substring(startIndex, endIndex) : "";
        }
    }
}
