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
    private String apiBaseUrl = FrontendEnvConfig.getApiBaseUrl();
    private String currentUserId = null;
    private String currentUserEmail = null;

    // Maps are now loaded fresh on every request — do not cache here

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

    // ======================== API HELPERS ========================

    /**
     * Build an HttpRequest builder with Authorization header set if logged in.
     * Use for all authenticated API calls.
     */
    private HttpRequest.Builder authRequest(String uri, HttpRequest.BodyPublisher body) {
        HttpRequest.Builder builder = HttpRequest.newBuilder().uri(URI.create(uri));
        if (currentUserId != null) {
            builder.header("Authorization", currentUserId);
        }
        if (body != null) {
            builder.POST(body);
        } else {
            builder.GET();
        }
        return builder;
    }

    /**
     * Send an authenticated GET request.
     */
    private HttpResponse<String> apiGet(String uri) throws Exception {
        return httpClient.send(authRequest(uri, null).build(), HttpResponse.BodyHandlers.ofString());
    }

    /**
     * Send an authenticated POST request with a JSON body.
     */
    private HttpResponse<String> apiPost(String uri, String jsonBody) throws Exception {
        return httpClient.send(
            authRequest(uri, HttpRequest.BodyPublishers.ofString(jsonBody))
                .header("Content-Type", "application/json")
                .build(),
            HttpResponse.BodyHandlers.ofString());
    }

    // ======================== INITIALIZATION ========================

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
        // Always load services from database via API first
        availableServices = new ArrayList<>();

        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/services"))
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                if (responseBody.contains("\"services\":")) {
                    int start = responseBody.indexOf("\"services\":[");
                    int arrayStart = responseBody.indexOf("[", start);
                    int arrayEnd = responseBody.lastIndexOf("]");
                    if (arrayStart == -1 || arrayEnd == -1 || arrayEnd <= arrayStart) {
                        System.err.println("initializeServices: malformed services array");
                    } else {
                        String servicesJson = responseBody.substring(arrayStart, arrayEnd + 1);

                        // Use extractArray for robust parsing
                        List<Map<String, String>> serviceMaps = extractArray(responseBody, "services");
                        for (Map<String, String> m : serviceMaps) {
                            String serviceId = m.get("serviceId");
                            String name = m.get("name");
                            String description = m.get("description");
                            String priceStr = m.get("basePrice");
                            String durationStr = m.get("durationMinutes");
                            String category = m.get("category");

                            if (serviceId != null && name != null && priceStr != null && durationStr != null) {
                                try {
                                    double price = Double.parseDouble(priceStr);
                                    int duration = Integer.parseInt(durationStr);
                                    ServiceCategory cat = ServiceCategory.valueOf(category != null ? category : "Career");

                                    ConsultingService service = new ConsultingService(name, description, price, duration, cat);
                                    service.setServiceId(java.util.UUID.fromString(serviceId));
                                    availableServices.add(service);
                                    System.out.println("Loaded service: " + name + " (id=" + serviceId + ")");
                                } catch (Exception e) {
                                    System.err.println("Error parsing service: " + e.getMessage());
                                }
                            }
                        }

                        if (!availableServices.isEmpty()) {
                            System.out.println("Loaded " + availableServices.size() + " services from database.");
                            return;
                        }
                    }
                }
            } else {
                System.err.println("initializeServices: API returned status " + response.statusCode());
            }
        } catch (Exception e) {
            System.err.println("Error loading services from API: " + e.getMessage());
        }

        // Fallback to hardcoded services if database is empty
        System.out.println("WARNING: No services loaded from database. Using default services.");
        availableServices.add(new ConsultingService("Career Counseling", "Professional career guidance and advice", 100.0, 60, ServiceCategory.Career));
        availableServices.add(new ConsultingService("IT Consulting", "Technology and software development advice", 150.0, 90, ServiceCategory.Technology));
        availableServices.add(new ConsultingService("Financial Advisory", "Financial planning and investment advice", 200.0, 60, ServiceCategory.Finance));

        System.out.println("System initialized with " + availableServices.size() + " consulting services.");
    }

    // ======================== FRESH MAP LOADERS (called every request) ========================

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
            String jsonData = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"accountType\":\"Client\"}";

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
                    // Extract user ID from API response — THIS IS THE REAL DB UUID
                    currentUserId = extractJsonValue(responseBody, "userId");
                    currentUserEmail = email;
                    String userName = extractJsonValue(responseBody, "name");
                    String accountType = extractJsonValue(responseBody, "accountType");
                    String isApprovedStr = extractJsonValue(responseBody, "isApproved");
                    boolean isApproved = "true".equalsIgnoreCase(isApprovedStr);

                    // FIX: Rebuild User with the REAL UUID from database, not a random one
                    User realUser;
                    if ("Consultant".equals(accountType)) {
                        realUser = new Consultant(java.util.UUID.fromString(currentUserId), userName, email, "dummy", isApproved);
                    } else if ("Client".equals(accountType)) {
                        realUser = new Client(java.util.UUID.fromString(currentUserId), userName, email, "dummy");
                    } else {
                        realUser = new backend.user.Admin(java.util.UUID.fromString(currentUserId), userName, email, "dummy");
                    }

                    UserProxy userProxy = new UserProxy(realUser);
                    userProxy.logIn();
                    currentUser = userProxy;
                    isLoggedIn = true;
                    System.out.println("Welcome, " + userName + "!");
                    return;
                }
            }

            System.out.println("Login failed! Invalid credentials.");
        } catch (java.lang.NumberFormatException e) {
            System.out.println("Login failed: Server returned invalid data.");
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
                    // FIX: currentUserId must be the SAME UUID that DB insert used
                    currentUserId = client.getUserID().toString();
                    currentUserEmail = email;
                    UserProxy userProxy = new UserProxy(authenticatedUser);
                    currentUser = userProxy;
                    userProxy.logIn();
                    isLoggedIn = true;
                    System.out.println("Welcome, " + client.getName() + "!");
                } else {
                    System.out.println("Password incorrect.");
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
            String jsonData = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"accountType\":\"Consultant\"}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/login"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                if (responseBody.contains("\"success\":true")) {
                    // FIX: Use the REAL UUID from DB, not a random one
                    currentUserId = extractJsonValue(responseBody, "userId");
                    currentUserEmail = email;
                    String userName = extractJsonValue(responseBody, "name");
                    String isApprovedStr = extractJsonValue(responseBody, "isApproved");
                    boolean isApproved = "true".equalsIgnoreCase(isApprovedStr);

                    Consultant realConsultant = new Consultant(
                            java.util.UUID.fromString(currentUserId), userName, email, "dummy", isApproved);
                    UserProxy userProxy = new UserProxy(realConsultant);
                    userProxy.logIn();
                    currentUser = userProxy;
                    isLoggedIn = true;
                    System.out.println("Welcome, " + userName + "!");
                    return;
                } else {
                    String error = extractJsonValue(responseBody, "error");
                    System.out.println("Login failed: " + (error != null ? error : "Invalid credentials or pending approval."));
                    return;
                }
            }

            System.out.println("Login failed! Invalid credentials or account pending approval.");
        } catch (java.lang.NumberFormatException e) {
            System.out.println("Login failed: Server returned invalid data.");
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
                    // FIX: currentUserId must be the SAME UUID that DB insert used
                    currentUserId = consultant.getUserID().toString();
                    currentUserEmail = email;
                    UserProxy userProxy = new UserProxy(authenticatedUser);
                    currentUser = userProxy;
                    userProxy.logIn();
                    isLoggedIn = true;
                    System.out.println("Welcome, " + consultant.getName() + "!");
                } else {
                    System.out.println("Password incorrect.");
                }
            } else {
                System.out.println("Consultant not found! Please register first.");
            }
        }
    }

    private void loginAsAdmin() {
        System.out.print("Enter admin email: ");
        String email = scanner.nextLine();
        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        try {
            String jsonData = "{\"email\":\"" + email + "\",\"password\":\"" + password + "\",\"accountType\":\"Admin\"}";

            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(apiBaseUrl + "/users/login"))
                .POST(HttpRequest.BodyPublishers.ofString(jsonData))
                .header("Content-Type", "application/json")
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                if (responseBody.contains("\"success\":true")) {
                    // FIX: Use the REAL UUID from DB, not a random one
                    currentUserId = extractJsonValue(responseBody, "userId");
                    currentUserEmail = email;
                    String userName = extractJsonValue(responseBody, "name");

                    backend.user.Admin realAdmin = new backend.user.Admin(
                            java.util.UUID.fromString(currentUserId), userName, email, "dummy");
                    UserProxy userProxy = new UserProxy(realAdmin);
                    userProxy.logIn();
                    currentUser = userProxy;
                    isLoggedIn = true;
                    System.out.println("Welcome, Administrator!");
                    return;
                } else {
                    String error = extractJsonValue(responseBody, "error");
                    System.out.println("Login failed: " + (error != null ? error : "Invalid credentials."));
                    return;
                }
            }

            System.out.println("Login failed! Invalid admin credentials.");
        } catch (java.lang.NumberFormatException e) {
            System.out.println("Login failed: Server returned invalid data.");
        } catch (Exception e) {
            System.out.println("Error connecting to server: " + e.getMessage());
            System.out.println("Falling back to local authentication...");

            // Fallback: hardcoded admin (no UUID to match DB — only for offline mode)
            if ("admin@system.com".equals(email) && "admin".equals(password)) {
                Admin admin = new Admin("System Admin", email, password);
                currentUserId = admin.getUserID().toString(); // random UUID — admin actions will fail on DB
                currentUserEmail = email;
                UserProxy userProxy = new UserProxy(admin);
                currentUser = userProxy;
                userProxy.logIn();
                isLoggedIn = true;
                System.out.println("Welcome, Administrator! (offline mode — DB operations may fail)");
            } else {
                System.out.println("Invalid admin credentials.");
            }
        }
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
                // FIX: Extract real DB UUID from response so subsequent operations use it
                String dbUserId = extractJsonValue(response.body(), "userId");
                System.out.println("Registration successful! User ID: " + dbUserId);
                System.out.println("You can now login.");
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
                // FIX: Extract real DB UUID from response
                String dbUserId = extractJsonValue(response.body(), "userId");
                System.out.println("Registration successful! User ID: " + dbUserId);
                System.out.println("You can now login after admin approval.");
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
            HttpResponse<String> response = apiGet(apiBaseUrl + "/services");

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

        // Get consultants from API (each object now carries real DB UUID from getConsultantsFromAPI)
        List<Consultant> allConsultants = getConsultantsFromAPI();
        if (allConsultants.isEmpty()) {
            System.out.println("No consultants available yet. Please wait for consultants to register.");
            return;
        }

        // Filter: only show consultants who have set at least one available time slot
        List<Consultant> consultants = new ArrayList<>();
        for (Consultant c : allConsultants) {
            String cid = c.getUserID().toString();
            try {
                HttpResponse<String> resp = apiGet(apiBaseUrl + "/availability/get?consultantId=" + cid);
                if (resp.statusCode() == 200) {
                    List<Map<String, String>> slots = extractArray(resp.body(), "availability");
                    if (!slots.isEmpty()) {
                        consultants.add(c);
                    }
                }
            } catch (Exception e) {
                // Skip on error — don't show this consultant
            }
        }

        if (consultants.isEmpty()) {
            System.out.println("No consultants have set their availability yet.");
            System.out.println("Please wait for consultants to add available time slots.");
            return;
        }

        System.out.println("\nAvailable Consultants (showing only those with available time slots):");
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

        // Use real DB UUID stored in the Consultant object
        String consultantId = consultant.getUserID().toString();
        if (consultantId == null || consultantId.isEmpty()) {
            System.out.println("Error: Consultant ID not found. Please reload consultant list.");
            return;
        }

        // Enter start time
        System.out.print("Enter start time (yyyy-MM-dd HH:mm): ");
        String timeStr = scanner.nextLine();
        LocalDateTime startTime = LocalDateTime.parse(timeStr, formatter);

        // ===== Step 1: Check consultant availability from database =====
        boolean isAvailable = false;
        String availabilityInfo = "";
        try {
            HttpResponse<String> availResponse = apiGet(apiBaseUrl + "/availability/get?consultantId=" + consultantId);
            if (availResponse.statusCode() == 200 && availResponse.body().contains("\"success\":true")) {
                List<Map<String, String>> slots = extractArray(availResponse.body(), "availability");

                if (slots.isEmpty()) {
                    System.out.println("Error: This consultant has not set any available time slots.");
                    System.out.println("Please ask the consultant to set their availability first.");
                    return;
                }

                System.out.println("\nConsultant available time slots:");
                for (Map<String, String> slot : slots) {
                    String start = slot.getOrDefault("startTime", "?");
                    String end = slot.getOrDefault("endTime", "?");
                    System.out.println("  - " + start + " to " + end);

                    // Parse and check if requested time is within this slot
                    try {
                        LocalDateTime slotStart = LocalDateTime.parse(start.replace(" ", "T"));
                        LocalDateTime slotEnd = LocalDateTime.parse(end.replace(" ", "T"));
                        if (!startTime.isBefore(slotStart) && startTime.isBefore(slotEnd)) {
                            isAvailable = true;
                            availabilityInfo = slotStart + " to " + slotEnd;
                        }
                    } catch (Exception e) {
                        System.err.println("Error parsing time slot: " + e.getMessage());
                    }
                }

                if (!isAvailable) {
                    System.out.println("\nError: Selected time " + startTime + " is not within any available slot.");
                    return;
                }

                System.out.println("\n✓ Time slot verified: " + availabilityInfo);
            } else {
                System.out.println("Warning: Could not verify availability (API error). Proceeding anyway.");
                isAvailable = true; // Allow to proceed if API fails
            }
        } catch (Exception e) {
            System.out.println("Warning: Could not verify availability: " + e.getMessage());
            System.out.println("Proceeding without availability check...");
            isAvailable = true; // Allow fallback
        }

        // ===== Step 2: Create booking =====
        try {
            // Use the serviceId stored in the ConsultingService object (loaded from DB in initializeServices)
            String serviceId = service.getServiceId().toString();
            System.out.println("[DEBUG] serviceId=" + serviceId + ", serviceName=" + service.getName());
            if (serviceId == null || serviceId.isEmpty()) {
                System.out.println("Error: Service ID not found. Please reload services list.");
                return;
            }

            String jsonData = "{\"clientId\":\"" + currentUserId + "\",\"consultantId\":\"" +
                             consultantId + "\",\"serviceId\":\"" +
                             serviceId + "\",\"startTime\":\"" +
                             startTime.toString().replace("T", " ") + "\"}";

            HttpResponse<String> response = apiPost(apiBaseUrl + "/bookings/create", jsonData);

            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Booking created successfully via API!");
                return;
            } else {
                String error = extractJsonValue(response.body(), "error");
                System.out.println("Booking failed: " + (error != null ? error : response.body()));
            }
        } catch (Exception e) {
            System.out.println("API booking failed: " + e.getMessage());
            System.out.println("Falling back to local booking...");
        }

        // Fallback to local booking (in-memory only — DB FK will be wrong if API is down)
        try {
            Booking booking = clientService.requestBooking(client, service, consultant, startTime);
            System.out.println("Booking created locally (API unavailable) — ID: " + booking.getBookingId());
            System.out.println("Warning: This booking may not appear in database queries.");
        } catch (Exception e) {
            System.out.println("Failed to create booking: " + e.getMessage());
        }
    }

    private List<Consultant> getConsultantsFromAPI() {
        List<Consultant> consultants = new ArrayList<>();

        try {
            HttpResponse<String> response = apiGet(apiBaseUrl + "/users/consultants");

            if (response.statusCode() == 200) {
                String responseBody = response.body();
                if (responseBody.contains("\"consultants\":")) {
                    // Use extractArray for robust parsing
                    List<Map<String, String>> consultantMaps = extractArray(responseBody, "consultants");
                    for (Map<String, String> m : consultantMaps) {
                        String userId = m.get("userId");
                        String name = m.get("name");
                        String email = m.get("email");
                        String isApproved = m.get("isApproved");

                        if (userId != null && name != null && email != null) {
                            if ("true".equalsIgnoreCase(isApproved)) {
                                // Store real DB UUID in the Consultant object
                                Consultant c = new Consultant(
                                    java.util.UUID.fromString(userId), name, email, "dummy", true);
                                consultants.add(c);
                            }
                        }
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
            HttpResponse<String> response = apiGet(apiBaseUrl + "/bookings/client?clientId=" + currentUserId);

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
        String bookingIdStr = scanner.nextLine().trim();

        try {
            String jsonData = "{\"bookingId\":\"" + bookingIdStr + "\",\"clientId\":\"" + currentUserId + "\"}";
            HttpResponse<String> response = apiPost(apiBaseUrl + "/bookings/cancel", jsonData);
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
        String bookingIdStr = scanner.nextLine().trim();
        if (bookingIdStr.isEmpty()) {
            System.out.println("Invalid booking ID!");
            return;
        }

        // ① Fetch saved payment methods from DB
        System.out.println("\nFetching your saved payment methods...");
        String methodId = null;
        String paymentMethodDisplay = null;

        try {
            HttpResponse<String> listResp = apiGet(apiBaseUrl + "/payment-methods/list?clientId=" + currentUserId);

            if (listResp.statusCode() == 200) {
                List<Map<String, String>> savedMethods = extractArray(listResp.body(), "methods");

                if (!savedMethods.isEmpty()) {
                    // Show saved payment methods
                    System.out.println("\nYour saved payment methods:");
                    for (int i = 0; i < savedMethods.size(); i++) {
                        Map<String, String> m = savedMethods.get(i);
                        System.out.printf("%d. %s  [%s]%n",
                            i + 1,
                            m.getOrDefault("paymentType", "?"),
                            m.getOrDefault("details", "****"));
                    }
                    System.out.println((savedMethods.size() + 1) + ". Add a new payment method");
                    System.out.print("Choose (1-" + (savedMethods.size() + 1) + "): ");
                    String choice = scanner.nextLine().trim();

                    int idx;
                    try {
                        idx = Integer.parseInt(choice) - 1;
                    } catch (NumberFormatException e) {
                        System.out.println("Invalid choice!");
                        return;
                    }

                    if (idx >= 0 && idx < savedMethods.size()) {
                        // Use saved method
                        methodId = savedMethods.get(idx).get("methodId");
                        paymentMethodDisplay = savedMethods.get(idx).get("paymentType");
                    }
                    // idx == savedMethods.size() → falls through to add-new below
                }
            }
        } catch (Exception e) {
            System.out.println("Warning: could not fetch saved payment methods: " + e.getMessage());
        }

        // ② Add new payment method if no saved ones or user chose to add
        if (methodId == null) {
            System.out.println("\nAdd a new payment method:");
            System.out.println("1. Credit Card");
            System.out.println("2. Debit Card");
            System.out.println("3. PayPal");
            System.out.println("4. Bank Transfer");
            System.out.print("Choose type (1-4): ");
            String typeChoice = scanner.nextLine().trim();

            String paymentType;
            Map<String, String> details = new HashMap<>();
            switch (typeChoice) {
                case "1":
                    paymentType = "CreditCard";
                    System.out.print("Enter card number (16 digits): ");
                    details.put("cardNumber", scanner.nextLine().trim());
                    System.out.print("Enter expiry (MM/YY): ");
                    details.put("expiry", scanner.nextLine().trim());
                    System.out.print("Enter CVV: ");
                    details.put("cvv", scanner.nextLine().trim());
                    System.out.print("Enter cardholder name: ");
                    details.put("cardholderName", scanner.nextLine().trim());
                    break;
                case "2":
                    paymentType = "DebitCard";
                    System.out.print("Enter card number (16 digits): ");
                    details.put("cardNumber", scanner.nextLine().trim());
                    System.out.print("Enter expiry (MM/YY): ");
                    details.put("expiry", scanner.nextLine().trim());
                    System.out.print("Enter CVV: ");
                    details.put("cvv", scanner.nextLine().trim());
                    System.out.print("Enter cardholder name: ");
                    details.put("cardholderName", scanner.nextLine().trim());
                    break;
                case "3":
                    paymentType = "PayPal";
                    System.out.print("Enter PayPal email: ");
                    details.put("email", scanner.nextLine().trim());
                    break;
                case "4":
                    paymentType = "BankTransfer";
                    System.out.print("Enter account number: ");
                    details.put("accountNumber", scanner.nextLine().trim());
                    System.out.print("Enter routing number: ");
                    details.put("routingNumber", scanner.nextLine().trim());
                    System.out.print("Enter bank name: ");
                    details.put("bankName", scanner.nextLine().trim());
                    System.out.print("Enter account holder name: ");
                    details.put("accountHolderName", scanner.nextLine().trim());
                    break;
                default:
                    System.out.println("Invalid payment type!");
                    return;
            }

            // Generate masked details using the same method as addPaymentMethod
            String maskedDetails = getMaskedPaymentInfoForStorage(paymentType, details);

            // Save to DB
            try {
                String jsonData = "{\"clientId\":\"" + currentUserId +
                    "\",\"paymentType\":\"" + paymentType +
                    "\",\"maskedDetails\":\"" + maskedDetails + "\"}";
                HttpResponse<String> addResp = apiPost(apiBaseUrl + "/payment-methods/add", jsonData);

                if (addResp.statusCode() == 200 && addResp.body().contains("\"success\":true")) {
                    methodId = extractJsonValue(addResp.body(), "methodId");
                    paymentMethodDisplay = paymentType;
                    System.out.println("Payment method saved! ID: " + methodId);
                } else {
                    String error = extractJsonValue(addResp.body(), "error");
                    System.out.println("Failed to save payment method: " + (error != null ? error : addResp.body()));
                    return;
                }
            } catch (Exception e) {
                System.out.println("Error saving payment method: " + e.getMessage());
                return;
            }
        }

        // ③ Process payment using the selected/saved method
        try {
            String jsonData = "{\"bookingId\":\"" + bookingIdStr +
                "\",\"clientId\":\"" + currentUserId +
                "\",\"methodId\":\"" + methodId + "\"}";

            HttpResponse<String> response = apiPost(apiBaseUrl + "/payments/pay", jsonData);
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                String paymentId = extractJsonValue(response.body(), "paymentId");
                String amount = extractJsonValue(response.body(), "amount");
                System.out.println("Payment successful!");
                System.out.println("  Transaction ID: " + paymentId);
                System.out.println("  Amount: $" + amount);
                System.out.println("  Method: " + paymentMethodDisplay);
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

        // FIX: Verify currentUserId is set before any DB operation
        if (currentUserId == null || currentUserId.isEmpty()) {
            System.out.println("Error: Not logged in. Cannot add payment method.");
            return;
        }

        // Try API first
        try {
            // Build masked details for storage
            String maskedDetails = getMaskedPaymentInfoForStorage(type, details);

            // FIX: Use currentUserId directly — it's already the real DB UUID from login/register
            String jsonData = "{\"clientId\":\"" + currentUserId + "\",\"paymentType\":\"" +
                type + "\",\"maskedDetails\":\"" + maskedDetails + "\"}";

            HttpResponse<String> response = apiPost(apiBaseUrl + "/payment-methods/add", jsonData);

            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("\n✓ Payment method added successfully to database!");
                return;
            } else {
                String error = extractJsonValue(response.body(), "error");
                System.out.println("API failed (status: " + response.statusCode()
                    + "): " + (error != null ? error : response.body()));
            }
        } catch (Exception e) {
            System.out.println("Error connecting to API: " + e.getMessage());
        }

        // Fallback to local (in-memory only — DB may be unavailable)
        try {
            PaymentMethod method = clientService.addPaymentMethod(client, type, details);
            if (method != null) {
                System.out.println("\n✓ Payment method added locally (DB unavailable).");
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

        // Try to load from database first
        boolean loadedFromDb = false;
        try {
            HttpResponse<String> response = apiGet(apiBaseUrl + "/payment-methods/list?clientId=" + currentUserId);
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                List<Map<String, String>> dbMethods = extractArray(response.body(), "methods");
                if (!dbMethods.isEmpty()) {
                    System.out.println("You have " + dbMethods.size() + " payment method(s):");
                    int idx = 1;
                    for (Map<String, String> m : dbMethods) {
                        String type = m.getOrDefault("paymentType", "Unknown");
                        String details = m.getOrDefault("details", "");
                        System.out.printf("%d. %s - %s%n", idx++, type, details);
                    }
                    loadedFromDb = true;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load from database: " + e.getMessage());
        }

        // Fallback to local
        if (!loadedFromDb) {
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
    }

    private void removePaymentMethod(Client client) {
        System.out.println("\n--- Remove Payment Method ---");

        // Step 1: fetch list — try DB first, fallback to local
        List<Map<String, String>> dbMethods = null;
        List<Map<String, Object>> localMethods = null;

        try {
            HttpResponse<String> response = apiGet(apiBaseUrl + "/payment-methods/list?clientId=" + currentUserId);
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                dbMethods = extractArray(response.body(), "methods");
            }
        } catch (Exception e) {
            System.out.println("Could not load from database: " + e.getMessage());
        }

        if (dbMethods != null && !dbMethods.isEmpty()) {
            // Use DB data
            System.out.println("Your payment methods:");
            for (int i = 0; i < dbMethods.size(); i++) {
                Map<String, String> m = dbMethods.get(i);
                System.out.printf("%d. %s - %s%n", i + 1, m.getOrDefault("paymentType", "?"), m.getOrDefault("details", ""));
            }

            System.out.print("\nEnter the number to remove (1-" + dbMethods.size() + ", or 0 to cancel): ");
            String input = scanner.nextLine();
            try {
                int sel = Integer.parseInt(input);
                if (sel == 0) {
                    System.out.println("Cancelled.");
                    return;
                }
                if (sel < 1 || sel > dbMethods.size()) {
                    System.out.println("Invalid selection.");
                    return;
                }

                Map<String, String> chosen = dbMethods.get(sel - 1);
                String methodId = chosen.getOrDefault("methodId", "");
                String jsonData = "{\"methodId\":\"" + methodId + "\",\"clientId\":\"" + currentUserId + "\"}";
                HttpResponse<String> delResp = apiPost(apiBaseUrl + "/payment-methods/remove", jsonData);

                if (delResp.statusCode() == 200 && delResp.body().contains("\"success\":true")) {
                    System.out.println("✓ Payment method removed from database!");
                    return;
                } else {
                    System.out.println("DB removal failed — falling back to local...");
                }
            } catch (Exception e) {
                System.out.println("Error: " + e.getMessage() + " — falling back to local...");
            }
        }

        // Fallback to local
        localMethods = clientService.getClientPaymentMethods(client);
        if (localMethods.isEmpty()) {
            System.out.println("No payment methods to remove.");
            return;
        }
        System.out.println("Your payment methods:");
        int i = 1;
        for (Map<String, Object> paymentInfo : localMethods) {
            PaymentMethod method = (PaymentMethod) paymentInfo.get("method");
            @SuppressWarnings("unchecked")
            Map<String, String> details = (Map<String, String>) paymentInfo.get("details");
            String displayInfo = getMaskedPaymentInfo(method, details);
            System.out.printf("%d. %s - %s%n", i++, method, displayInfo);
        }

        System.out.print("\nEnter the number to remove (1-" + localMethods.size() + ", or 0 to cancel): ");
        String input = scanner.nextLine();

        try {
            int index = Integer.parseInt(input) - 1;
            if (index == -1) {
                System.out.println("Cancelled.");
                return;
            }
            if (index >= 0 && index < localMethods.size()) {
                boolean success = clientService.removePaymentMethod(client, index);
                System.out.println(success ? "✓ Payment method removed!" : "✗ Failed to remove.");
            } else {
                System.out.println("Invalid selection.");
            }
        } catch (NumberFormatException e) {
            System.out.println("Invalid input.");
        }
    }

    private void viewPaymentHistory(Client client) {
        System.out.println("\n=== Payment History ===");

        // Try API first
        try {
            HttpResponse<String> response = apiGet(apiBaseUrl + "/payments/history?clientId=" + currentUserId);
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                List<Map<String, String>> payments = extractArray(response.body(), "payments");
                if (payments.isEmpty()) {
                    System.out.println("No payment history found.");
                    return;
                }
                System.out.println("Your payment transactions:");
                int i = 1;
                for (Map<String, String> p : payments) {
                    System.out.printf("%d. Transaction ID: %s | Amount: $%s | Method: %s | Status: %s%n",
                        i++,
                        p.getOrDefault("paymentId", "?"),
                        p.getOrDefault("amount", "?"),
                        p.getOrDefault("paymentMethod", "?"),
                        p.getOrDefault("status", "?"));
                }
                return;
            }
        } catch (Exception e) {
            System.out.println("Could not load from API: " + e.getMessage());
        }

        // Fallback to local
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
                HttpResponse<String> response = apiPost(apiBaseUrl + "/ai/chat", jsonData);

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

        // Try API first
        try {
            HttpResponse<String> response = apiGet(apiBaseUrl + "/bookings/consultant?consultantId=" + currentUserId);
            if (response.statusCode() == 200 && response.body().contains("\"bookings\":")) {
                int arrayStart = response.body().indexOf("[", response.body().indexOf("\"bookings\":"));
                int arrayEnd = response.body().lastIndexOf("]");
                String bookingsJson = response.body().substring(arrayStart, arrayEnd + 1);

                if (bookingsJson.equals("[]")) {
                    System.out.println("No bookings found.");
                    return;
                }

                List<Map<String, String>> bookings = extractArray(response.body(), "bookings");
                int count = 0;
                for (Map<String, String> b : bookings) {
                    count++;
                    System.out.printf("%d. ID: %s | Client: %s | Service: %s | Time: %s | Status: %s%n",
                        count,
                        b.getOrDefault("bookingId", "?"),
                        b.getOrDefault("clientName", "?"),
                        b.getOrDefault("serviceName", "?"),
                        b.getOrDefault("startTime", "?"),
                        b.getOrDefault("status", "?"));
                }
                return;
            }
        } catch (Exception e) {
            System.out.println("Could not load from API: " + e.getMessage());
        }

        // Fallback to local
        List<Booking> bookings = consultingService.getConsultantBookings(consultant);
        if (bookings.isEmpty()) {
            System.out.println("No bookings found.");
            return;
        }
        for (Booking booking : bookings) {
            System.out.printf("ID: %s | Client: %s | Service: %s | Time: %s | Status: %s%n",
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
        String bookingIdStr = scanner.nextLine().trim();

        // Try API first — pass consultantId so backend can verify ownership
        try {
            String jsonData = "{\"bookingId\":\"" + bookingIdStr + "\",\"consultantId\":\"" + currentUserId + "\"}";
            HttpResponse<String> response = apiPost(apiBaseUrl + "/bookings/confirm", jsonData);

            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Booking accepted successfully!");
                return;
            } else {
                String error = extractJsonValue(response.body(), "error");
                System.out.println("Failed to accept: " + (error != null ? error : response.body()));
                return;
            }
        } catch (Exception e) {
            System.out.println("API error: " + e.getMessage());
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
        String bookingIdStr = scanner.nextLine().trim();

        // Try API — pass consultantId so backend can verify ownership
        try {
            String jsonData = "{\"bookingId\":\"" + bookingIdStr + "\",\"consultantId\":\"" + currentUserId + "\"}";
            HttpResponse<String> response = apiPost(apiBaseUrl + "/bookings/reject", jsonData);

            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Booking rejected successfully!");
                return;
            } else {
                String error = extractJsonValue(response.body(), "error");
                System.out.println("Failed to reject: " + (error != null ? error : response.body()));
                return;
            }
        } catch (Exception e) {
            System.out.println("API error: " + e.getMessage());
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
        String bookingIdStr = scanner.nextLine().trim();

        // Try API — pass consultantId so backend can verify ownership
        try {
            String jsonData = "{\"bookingId\":\"" + bookingIdStr + "\",\"consultantId\":\"" + currentUserId + "\"}";
            HttpResponse<String> response = apiPost(apiBaseUrl + "/bookings/complete", jsonData);

            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Booking completed successfully!");
                return;
            } else {
                String error = extractJsonValue(response.body(), "error");
                System.out.println("Failed to complete: " + (error != null ? error : response.body()));
                return;
            }
        } catch (Exception e) {
            System.out.println("API error: " + e.getMessage());
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

            HttpResponse<String> response = apiPost(apiBaseUrl + "/availability/set", jsonData);

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

        // FIX: Pass currentUserId so ApiServer can verify the admin is authenticated
        try {
            String jsonData = "{\"email\":\"" + email + "\",\"adminId\":\"" + currentUserId + "\"}";
            HttpResponse<String> response = apiPost(apiBaseUrl + "/users/approve-consultant", jsonData);
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Consultant approved successfully!");
                return;
            } else {
                String error = extractJsonValue(response.body(), "error");
                System.out.println("API approve failed: " + (error != null ? error : response.body()));
            }
        } catch (Exception e) {
            System.out.println("Error connecting to API: " + e.getMessage());
        }

        // Fallback to local
        Consultant consultant = userService.getConsultantByEmail(email);
        if (consultant != null) {
            userService.approveConsultant(consultant);
            System.out.println("Consultant " + consultant.getName() + " has been approved and can now login!");
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
            String jsonData = "{\"email\":\"" + email + "\",\"adminId\":\"" + currentUserId + "\"}";
            HttpResponse<String> response = apiPost(apiBaseUrl + "/users/reject-consultant", jsonData);
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                System.out.println("Consultant rejected successfully!");
                return;
            } else {
                String error = extractJsonValue(response.body(), "error");
                System.out.println("API reject failed: " + (error != null ? error : response.body()));
            }
        } catch (Exception e) {
            System.out.println("Error connecting to API: " + e.getMessage());
        }

        // Fallback to local
        Consultant consultant = userService.getConsultantByEmail(email);
        if (consultant != null) {
            userService.rejectConsultant(consultant);
            System.out.println("Consultant " + consultant.getName() + " has been rejected.");
        } else {
            System.out.println("Consultant not found!");
        }
    }

    private void viewPendingConsultants() {
        System.out.println("\n=== Pending Consultants ===");

        // Try API first — use dedicated pending-consultants endpoint
        try {
            HttpResponse<String> response = apiGet(apiBaseUrl + "/users/pending-consultants");
            if (response.statusCode() == 200 && response.body().contains("\"success\":true")) {
                List<Map<String, String>> pending = extractArray(response.body(), "consultants");

                System.out.println("Total pending consultants: " + pending.size());

                if (pending.isEmpty()) {
                    System.out.println("No pending consultants waiting for approval.");
                    return;
                }

                System.out.println("\nConsultants waiting for approval:");
                int i = 1;
                for (Map<String, String> c : pending) {
                    String name = c.get("name");
                    String email = c.get("email");
                    String userId = c.get("userId");
                    
                    // Debug: print raw values if name or email is missing
                    if (name == null || name.trim().isEmpty()) {
                        System.out.println("[DEBUG] Missing name for consultant " + userId + ", full data: " + c);
                        name = "Unknown";
                    }
                    if (email == null || email.trim().isEmpty()) {
                        System.out.println("[DEBUG] Missing email for consultant " + userId);
                        email = "unknown@email.com";
                    }
                    
                    System.out.printf("%d. %s - %s (ID: %s)%n",
                        i++,
                        name,
                        email,
                        userId != null ? userId : "N/A");
                }

                System.out.println("\nTo approve or reject, use options 1 or 2 from the admin menu.");
                return;
            } else if (response.statusCode() == 403) {
                System.out.println("Error: Admin access required.");
                return;
            }
        } catch (Exception e) {
            System.out.println("Could not load from API: " + e.getMessage());
            e.printStackTrace();
        }

        // Fallback to local
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

        // Fetch real counts from database via API
        int totalClients = 0, totalConsultants = 0, totalApproved = 0, totalPending = 0, totalServices = 0;

        try {
            // Count clients
            HttpResponse<String> clientResp = apiGet(apiBaseUrl + "/users/clients");
            if (clientResp.statusCode() == 200) {
                List<Map<String, String>> clients = extractArray(clientResp.body(), "clients");
                totalClients = clients.size();
            }
        } catch (Exception e) {
            System.out.println("Could not load clients count: " + e.getMessage());
        }

        try {
            // Count consultants
            HttpResponse<String> consultantResp = apiGet(apiBaseUrl + "/users/consultants");
            if (consultantResp.statusCode() == 200) {
                List<Map<String, String>> consultants = extractArray(consultantResp.body(), "consultants");
                totalConsultants = consultants.size();
                for (Map<String, String> c : consultants) {
                    if ("true".equalsIgnoreCase(c.getOrDefault("isApproved", "false"))) totalApproved++;
                    else totalPending++;
                }
            }
        } catch (Exception e) {
            System.out.println("Could not load consultants count: " + e.getMessage());
        }

        try {
            // Count services
            HttpResponse<String> svcResp = apiGet(apiBaseUrl + "/services");
            if (svcResp.statusCode() == 200) {
                List<Map<String, String>> services = extractArray(svcResp.body(), "services");
                totalServices = services.size();
            }
        } catch (Exception e) {
            System.out.println("Could not load services count: " + e.getMessage());
        }

        boolean dbConnected = false;
        try {
            HttpRequest req = HttpRequest.newBuilder().uri(URI.create(apiBaseUrl + "/health")).GET().build();
            HttpResponse<String> health = httpClient.send(req, HttpResponse.BodyHandlers.ofString());
            dbConnected = (health.statusCode() == 200);
        } catch (Exception ignored) {}

        // Fallback to local data if API returned nothing
        if (totalClients == 0 && totalConsultants == 0) {
            System.out.println("API unavailable — showing local (offline) statistics.");
            Map<String, Object> localStatus = adminService.getSystemStatus();
            totalClients = (Integer) localStatus.getOrDefault("total_clients", 0);
            totalConsultants = (Integer) localStatus.getOrDefault("total_consultants", 0);
            totalApproved = (Integer) localStatus.getOrDefault("approved_consultants", 0);
            totalPending = (Integer) localStatus.getOrDefault("pending_consultants", 0);
            totalServices = (Integer) localStatus.getOrDefault("total_services", 0);
        }

        System.out.println("\n--- User Statistics ---");
        System.out.printf("Total Users: %d%n", totalClients + totalConsultants + 1); // +1 admin
        System.out.printf("Total Clients: %d%n", totalClients);
        System.out.printf("Total Consultants: %d%n", totalConsultants);
        System.out.printf("  - Approved: %d%n", totalApproved);
        System.out.printf("  - Pending Approval: %d%n", totalPending);

        System.out.println("\n--- Service Information ---");
        System.out.printf("Total Services Available: %d%n", Math.max(totalServices, availableServices.size()));

        System.out.println("\n--- Active Policies ---");
        try {
            Map<String, Object> localStatus = adminService.getSystemStatus();
            System.out.printf("Cancellation Policy: %s%n", localStatus.getOrDefault("cancellation_policy", "?"));
            System.out.printf("Pricing Strategy: %s%n", localStatus.getOrDefault("pricing_strategy", "?"));
        } catch (Exception e) {
            System.out.println("Cancellation Policy: ?");
            System.out.println("Pricing Strategy: ?");
        }

        System.out.println("\n--- System Health ---");
        System.out.println("Database Connection: " + (dbConnected ? "✓ Connected" : "✗ Disconnected"));
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
     * Escape special characters for JSON string values.
     */
    private String escapeJson(String raw) {
        if (raw == null) return "";
        return raw.replace("\\", "\\\\")
                  .replace("\"", "\\\"")
                  .replace("\n", "\\n")
                  .replace("\r", "\\r")
                  .replace("\t", "\\t");
    }

    /**
     * Safely parse an integer from user input with validation.
     * @param input the raw input string
     * @param min minimum allowed value (inclusive), use Integer.MIN_VALUE for no min
     * @param max maximum allowed value (inclusive), use Integer.MAX_VALUE for no max
     * @param fieldName name of the field for error messages
     * @return parsed integer, or null if invalid
     */
    private Integer safeParseInt(String input, int min, int max, String fieldName) {
        if (input == null || input.trim().isEmpty()) {
            System.out.println(fieldName + " cannot be empty.");
            return null;
        }
        try {
            int value = Integer.parseInt(input.trim());
            if (value < min || value > max) {
                System.out.println(fieldName + " must be between " + min + " and " + max + ".");
                return null;
            }
            return value;
        } catch (NumberFormatException e) {
            System.out.println("Invalid " + fieldName.toLowerCase() + "! Please enter a valid number.");
            return null;
        }
    }

    /**
     * Helper method to extract simple JSON value with proper nested-brace awareness.
     * Finds the key within the nearest enclosing {} block only.
     * @param json JSON string
     * @param key Key to extract
     * @return Extracted value or empty string
     */
    private String extractJsonValue(String json, String key) {
        if (json == null || key == null || json.isEmpty()) return "";

        String searchKey = "\"" + key + "\":";
        int keyIndex = json.indexOf(searchKey);
        if (keyIndex == -1) return "";

        // Walk backward from key to find the { that opens this object's scope
        int braceStart = -1;
        int searchDepth = 0;
        for (int i = keyIndex - 1; i >= 0; i--) {
            char c = json.charAt(i);
            if (c == '}') searchDepth++;
            else if (c == '{') {
                if (searchDepth == 0) { braceStart = i; break; }
                searchDepth--;
            }
        }

        // Now find the matching closing brace for this object
        int braceEnd = json.length();
        if (braceStart >= 0) {
            int braceDepth = 0;
            for (int i = braceStart; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{') braceDepth++;
                else if (c == '}') {
                    braceDepth--;
                    if (braceDepth == 0) { braceEnd = i; break; }
                }
            }
        }

        // Re-search for key within the correct scope only
        int scopeStart = (braceStart >= 0) ? braceStart : 0;
        int scopeJson = keyIndex;
        keyIndex = -1;
        for (int i = scopeStart; i <= scopeJson; i++) {
            int found = json.indexOf(searchKey, i);
            if (found != -1 && found <= scopeJson) {
                keyIndex = found;
                break;
            }
        }
        if (keyIndex == -1) return "";

        int startIndex = keyIndex + searchKey.length();

        // Skip whitespace
        while (startIndex < json.length() && Character.isWhitespace(json.charAt(startIndex))) {
            startIndex++;
        }
        if (startIndex >= json.length()) return "";

        char startChar = json.charAt(startIndex);

        if (startChar == '"') {
            // String value — find closing " outside of escaped sequences
            startIndex++;
            int endIndex = startIndex;
            while (endIndex < json.length()) {
                if (json.charAt(endIndex) == '"') break;
                if (json.charAt(endIndex) == '\\') endIndex++; // skip escaped char
                endIndex++;
            }
            return (endIndex > startIndex && endIndex < json.length())
                ? json.substring(startIndex, endIndex) : "";
        } else if (startChar == '{' || startChar == '[') {
            // Nested object/array — skip the whole block
            int depth = 0;
            for (int i = startIndex; i < json.length(); i++) {
                char c = json.charAt(i);
                if (c == '{' || c == '[') depth++;
                else if (c == '}' || c == ']') {
                    depth--;
                    if (depth == 0) return json.substring(startIndex, i + 1);
                }
            }
            return "";
        } else {
            // Number, boolean, null
            int endIndex = startIndex;
            while (endIndex < json.length()) {
                char c = json.charAt(endIndex);
                if (c == ',' || c == '}') break;
                endIndex++;
            }
            return json.substring(startIndex, endIndex).trim();
        }
    }

    /**
     * Parse a JSON array field into a list of objects, each as a Map.
     * Works correctly even when array elements contain nested objects.
     * @param json   full JSON string containing the array field
     * @param key    the array field name (e.g. "methods")
     * @return list of Maps, each containing the fields of one array element
     */
    private List<Map<String, String>> extractArray(String json, String key) {
        List<Map<String, String>> results = new ArrayList<>();
        if (json == null || json.isEmpty() || key == null) return results;

        try {
            // Locate the key
            String searchKey = "\"" + key + "\":";
            int keyPos = json.indexOf(searchKey);
            if (keyPos == -1) return results;

            // Advance to opening '['
            int arrStart = json.indexOf('[', keyPos + searchKey.length());
            if (arrStart == -1) return results;

            // Walk the array, matching {} depth
            int i = arrStart + 1;
            int depth = 0;
            while (i < json.length()) {
                char c = json.charAt(i);
                if (c == '{') {
                    if (depth == 0) {
                        // Start of an object — find matching }
                        int objStart = i;
                        depth = 1;
                        int j = i + 1;
                        boolean inString = false;
                        boolean escaped = false;

                        while (j < json.length() && depth > 0) {
                            char x = json.charAt(j);
                            if (escaped) {
                                escaped = false;
                            } else if (x == '\\') {
                                escaped = true;
                            } else if (x == '"' && !escaped) {
                                inString = !inString;
                            } else if (!inString) {
                                if (x == '{') depth++;
                                else if (x == '}') depth--;
                            }
                            j++;
                        }
                        String objStr = json.substring(objStart, j);
                        Map<String, String> map = new HashMap<>();
                        parseFlatObject(objStr, map);
                        results.add(map);
                        i = j;
                    } else {
                        i++;
                    }
                } else if (c == ']' && depth == 0) {
                    break;
                } else {
                    i++;
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing array: " + e.getMessage());
        }
        return results;
    }

    /**
     * Parse a single flat JSON object (no deeply-nested values) into a Map.
     * Handles: {"key":"stringValue"}  {"key":123}  {"key":true}  {"key":false}  {"key":null}
     */
    private void parseFlatObject(String obj, Map<String, String> out) {
        int i = 0;
        // Strip outer braces so the loop always starts at the first key
        if (obj.length() >= 2 && obj.charAt(0) == '{' && obj.charAt(obj.length() - 1) == '}') {
            i = 1;
        }
        while (i < obj.length()) {
            // Skip whitespace / commas
            while (i < obj.length() && (obj.charAt(i) == ' ' || obj.charAt(i) == ',')) i++;
            if (i >= obj.length()) break;
            if (obj.charAt(i) == '}') break;               // end of object
            if (obj.charAt(i) != '"') { i++; continue; }   // skip noise, find next key

            // Parse key: "keyname"
            i++; // skip opening "
            int keyStart = i;
            while (i < obj.length() && obj.charAt(i) != '"') i++;
            String key = obj.substring(keyStart, i);
            i++; // skip closing "

            // Find colon
            while (i < obj.length() && obj.charAt(i) != ':') i++;
            i++; // skip colon
            while (i < obj.length() && obj.charAt(i) == ' ') i++; // skip whitespace

            if (i >= obj.length()) break;

            String value;
            if (obj.charAt(i) == '"') {
                // Quoted string value
                i++; // skip opening "
                int valStart = i;
                value = "";
                while (i < obj.length()) {
                    if (obj.charAt(i) == '\\') {
                        i += 2; // skip escaped char
                    } else if (obj.charAt(i) == '"') {
                        value = obj.substring(valStart, i);
                        i++; // skip closing "
                        break;
                    } else {
                        i++;
                    }
                }
            } else {
                // Unquoted: number, boolean, null — read until comma or }
                int valStart = i;
                while (i < obj.length()) {
                    char c = obj.charAt(i);
                    if (c == ',' || c == '}') break;
                    i++;
                }
                value = obj.substring(valStart, i).trim();
                if (value.isEmpty()) value = "";
            }

            out.put(key, value);
        }
    }
}
