package backend.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import backend.database.DatabaseConnection;
import backend.database.DatabaseInitializer;
import backend.database.UserDAO;
import backend.database.BookingDAO;
import backend.database.ServiceDAO;
import backend.database.PaymentDAO;
import backend.database.PaymentMethodDAO;
import backend.database.ConsultantAvailabilityDAO;
import backend.core.UserService;
import backend.core.BookingService;
import backend.core.PaymentService;
import backend.core.ClientService;
import backend.core.AIChatbotService;
import backend.user.User;
import backend.user.Client;
import backend.user.Consultant;
import backend.booking.Booking;
import backend.booking.BookingStatus;
import backend.service.ServiceCategory;
import backend.payment.PaymentMethod;
import backend.payment.PaymentTransaction;

import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * REST API Server
 * Provides HTTP endpoints for frontend consumption
 * NO Gson | Pure Java | All endpoints enabled
 */
public class ApiServer {
    private HttpServer server;
    private DatabaseConnection dbConnection;
    private UserService userService;
    private BookingService bookingService;
    private backend.core.ConsultingService consultingService;
    private PaymentService paymentService;
    private ClientService clientService;
    private AIChatbotService chatbotService;
    private AIChatbotService aiChatService;
    private UserDAO userDAO;
    private BookingDAO bookingDAO;
    private ServiceDAO serviceDAO;
    private PaymentDAO paymentDAO;
    private PaymentMethodDAO paymentMethodDAO;
    private ConsultantAvailabilityDAO availabilityDAO;

    private static final int PORT = 8080;

    // In-memory storage for demo (fallback when DB not available)
    private static Map<String, User> usersByEmail = new HashMap<>();
    private static Map<UUID, backend.core.ConsultingService> services = new HashMap<>();
    private static Map<UUID, Booking> bookings = new HashMap<>();
    private static Map<String, PaymentTransaction> payments = new HashMap<>();

    static {
        // Initialize demo services
        services.put(UUID.randomUUID(), new backend.core.ConsultingService("Career Counseling", "Professional career guidance and advice", 100.00, 60, ServiceCategory.Career));
        services.put(UUID.randomUUID(), new backend.core.ConsultingService("IT Consulting", "Technology and software development advice", 150.00, 90, ServiceCategory.Technology));
        services.put(UUID.randomUUID(), new backend.core.ConsultingService("Financial Advisory", "Financial planning and investment advice", 200.00, 60, ServiceCategory.Finance));
    }

    public ApiServer() {
        try {
            // Initialize database connection
            dbConnection = DatabaseConnection.getInstance();
            try {
                dbConnection.connect();
                System.out.println("Database connected successfully.");

                // Initialize database schema
                DatabaseInitializer initializer = new DatabaseInitializer();
                initializer.initialize();

                // Initialize DAOs
                userDAO = new UserDAO();
                bookingDAO = new BookingDAO();
                serviceDAO = new ServiceDAO();
                paymentDAO = new PaymentDAO();
                paymentMethodDAO = new PaymentMethodDAO();
                availabilityDAO = new ConsultantAvailabilityDAO();

                // Initialize services
                userService = new UserService();
                bookingService = new BookingService();
                consultingService = new backend.core.ConsultingService();
                paymentService = new PaymentService();
                clientService = new ClientService();
                chatbotService = new AIChatbotService();
                aiChatService = new AIChatbotService();

                System.out.println("Database mode: ENABLED");
            } catch (Exception e) {
                System.out.println("Database connection failed, running in DEMO mode: " + e.getMessage());
                userService = new UserService();
                bookingService = new BookingService();
                consultingService = new backend.core.ConsultingService();
                paymentService = new PaymentService();
                clientService = new ClientService();
                chatbotService = new AIChatbotService();
                aiChatService = new AIChatbotService();
            }

            // Create HTTP server
            server = HttpServer.create(new InetSocketAddress(PORT), 0);

            // Setup routes
            setupRoutes();

            System.out.println("API Server started on port " + PORT);

        } catch (Exception e) {
            System.err.println("Failed to start API server: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void setupRoutes() {
        // User endpoints
        server.createContext("/api/users/register", new RegisterHandler(this));
        server.createContext("/api/users/login", new LoginHandler(this));
        server.createContext("/api/users/profile", new ProfileHandler(this));
        server.createContext("/api/users/consultants", new GetConsultantsHandler(this));
        server.createContext("/api/users/clients", new GetClientsHandler(this));
        server.createContext("/api/users/approve-consultant", new ApproveConsultantHandler(this));
        server.createContext("/api/users/reject-consultant", new RejectConsultantHandler(this));

        // Service endpoints
        server.createContext("/api/services", new GetServicesHandler(this));
        server.createContext("/api/services/create", new CreateServiceHandler(this));

        // Booking endpoints
        server.createContext("/api/bookings/create", new CreateBookingHandler(this));
        server.createContext("/api/bookings/client", new GetClientBookingsHandler(this));
        server.createContext("/api/bookings/consultant", new GetConsultantBookingsHandler(this));
        server.createContext("/api/bookings/confirm", new ConfirmBookingHandler(this));
        server.createContext("/api/bookings/cancel", new CancelBookingHandler(this));
        server.createContext("/api/bookings/complete", new CompleteBookingHandler(this));

        // Availability endpoints
        server.createContext("/api/availability/set", new SetAvailabilityHandler(this));
        server.createContext("/api/availability/get", new GetAvailabilityHandler(this));

        // Payment endpoints
        server.createContext("/api/payments/pay", new MakePaymentHandler(this));
        server.createContext("/api/payments/history", new PaymentHistoryHandler(this));
        server.createContext("/api/payment-methods/add", new AddPaymentMethodHandler(this));
        server.createContext("/api/payment-methods/list", new ListPaymentMethodsHandler(this));
        server.createContext("/api/payment-methods/remove", new RemovePaymentMethodHandler(this));

        // AI Chatbot endpoint
        server.createContext("/api/ai/chat", new AIChatHandler(this));

        // Health check
        server.createContext("/api/health", new HealthCheckHandler(this));
    }

    public void start() {
        if (server != null) {
            server.setExecutor(null);
            server.start();
            System.out.println("API Server is running at http://localhost:" + PORT);
        }
    }

    public void stop() {
        if (server != null) {
            server.stop(0);
            if (dbConnection != null) dbConnection.disconnect();
            System.out.println("API Server stopped");
        }
    }

    // Helper method to send JSON response
    public void sendResponse(HttpExchange exchange, String jsonResponse, int statusCode) throws IOException {
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("Access-Control-Allow-Origin", "*");
        exchange.getResponseHeaders().set("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
        exchange.getResponseHeaders().set("Access-Control-Allow-Headers", "Content-Type");

        if ("OPTIONS".equalsIgnoreCase(exchange.getRequestMethod())) {
            exchange.sendResponseHeaders(204, -1);
            return;
        }

        byte[] res = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, res.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(res);
        }
    }

    // Helper to read request body
    public String readRequestBody(HttpExchange exchange) throws IOException {
        return new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
    }

    // Helper to parse JSON body - handles values containing colons (e.g. timestamps, URLs)
    public Map<String, String> parseJsonBody(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;

        body = body.trim();
        if (!body.startsWith("{") || !body.endsWith("}")) return map;
        body = body.substring(1, body.length() - 1).trim();

        // State-machine parser: handles "key":"value" where value may contain ':'
        int i = 0;
        while (i < body.length()) {
            // skip whitespace / commas
            while (i < body.length() && (body.charAt(i) == ',' || body.charAt(i) == ' ')) i++;
            if (i >= body.length()) break;

            // read key (must be quoted)
            if (body.charAt(i) != '"') { i++; continue; }
            i++; // skip opening quote
            int keyStart = i;
            while (i < body.length() && body.charAt(i) != '"') i++;
            String key = body.substring(keyStart, i);
            i++; // skip closing quote

            // skip whitespace and colon
            while (i < body.length() && (body.charAt(i) == ' ' || body.charAt(i) == ':')) i++;
            if (i >= body.length()) break;

            // read value
            String value;
            if (body.charAt(i) == '"') {
                // quoted string value
                i++; // skip opening quote
                StringBuilder sb = new StringBuilder();
                while (i < body.length()) {
                    char c = body.charAt(i);
                    if (c == '\\' && i + 1 < body.length()) {
                        i++;
                        sb.append(body.charAt(i));
                    } else if (c == '"') {
                        break;
                    } else {
                        sb.append(c);
                    }
                    i++;
                }
                value = sb.toString();
                i++; // skip closing quote
            } else {
                // unquoted value (number, boolean, null) — ends at comma or }
                int valStart = i;
                while (i < body.length() && body.charAt(i) != ',' && body.charAt(i) != '}') i++;
                value = body.substring(valStart, i).trim();
            }

            if (!key.isEmpty()) map.put(key, value);
        }
        return map;
    }

    // ======================== HANDLERS ========================

    static class HealthCheckHandler implements HttpHandler {
        private final ApiServer api;
        public HealthCheckHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            api.sendResponse(exchange, "{\"status\":\"ok\",\"message\":\"API server is running\"}", 200);
        }
    }

    static class RegisterHandler implements HttpHandler {
        private final ApiServer api;
        public RegisterHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            String name = data.get("name");
            String email = data.get("email");
            String password = data.get("password");
            String accountType = data.get("accountType");

            if (name == null || email == null || password == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Missing required fields\"}", 400);
                return;
            }

            try {
                if ("Client".equals(accountType)) {
                    Client client = api.userService.registerClient(name, email, password);
                    if (client != null) {
                        // Save to database
                        if (api.userDAO != null) {
                            api.userDAO.insert(client);
                        }
                        api.usersByEmail.put(email, client);
                        api.sendResponse(exchange, "{\"success\":true,\"userId\":\"" + client.getUserID() + "\",\"message\":\"User registered successfully\"}", 200);
                    } else {
                        api.sendResponse(exchange, "{\"success\":false,\"error\":\"Registration failed\"}", 400);
                    }
                } else if ("Consultant".equals(accountType)) {
                    Consultant consultant = api.userService.registerConsultant(name, email, password);
                    if (consultant != null) {
                        // Save to database
                        if (api.userDAO != null) {
                            api.userDAO.insert(consultant);
                        }
                        api.usersByEmail.put(email, consultant);
                        api.sendResponse(exchange, "{\"success\":true,\"userId\":\"" + consultant.getUserID() + "\",\"message\":\"User registered successfully\"}", 200);
                    } else {
                        api.sendResponse(exchange, "{\"success\":false,\"error\":\"Registration failed\"}", 400);
                    }
                } else {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Invalid account type\"}", 400);
                }
            } catch (Exception e) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    static class LoginHandler implements HttpHandler {
        private final ApiServer api;
        public LoginHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            String email = data.get("email");
            String password = data.get("password");

            if (email == null || password == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Missing email or password\"}", 400);
                return;
            }

            // Try database first
            User user = null;
            if (api.userDAO != null) {
                user = api.userDAO.findByEmail(email);
                if (user != null && !user.getPassword().equals(password)) {
                    user = null; // wrong password
                }
                // Consultant must be approved
                if (user instanceof Consultant c && !c.isApproved()) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Account pending approval\"}", 401);
                    return;
                }
            }

            // Fallback to in-memory
            if (user == null) {
                user = api.userService.authenticateUser(email, password);
            }

            if (user != null) {
                String response = "{\"success\":true,\"user\":{\"userId\":\"" + user.getUserID() +
                    "\",\"name\":\"" + user.getName() +
                    "\",\"email\":\"" + user.getEmail() +
                    "\",\"accountType\":\"" + user.getAccountType() + "\"}}";
                api.sendResponse(exchange, response, 200);
            } else {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Invalid credentials\"}", 401);
            }
        }
    }

    static class ProfileHandler implements HttpHandler {
        private final ApiServer api;
        public ProfileHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String userId = query != null && query.contains("userId=") ?
                query.split("userId=")[1].split("&")[0] : null;

            if (userId == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"userId required\"}", 400);
                return;
            }

            // Try database first
            User u = null;
            if (api.userDAO != null) {
                u = api.userDAO.findById(userId);
            }
            // Fallback to memory
            if (u == null) {
                for (User mem : api.usersByEmail.values()) {
                    if (mem.getUserID().toString().equals(userId)) { u = mem; break; }
                }
            }

            if (u != null) {
                String response = "{\"success\":true,\"user\":{\"userId\":\"" + u.getUserID() +
                    "\",\"name\":\"" + u.getName() +
                    "\",\"email\":\"" + u.getEmail() +
                    "\",\"accountType\":\"" + u.getAccountType() + "\"}}";
                api.sendResponse(exchange, response, 200);
            } else {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"User not found\"}", 404);
            }
        }
    }

    static class GetConsultantsHandler implements HttpHandler {
        private final ApiServer api;
        public GetConsultantsHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> consultants = new ArrayList<>();

            // Try to get from database first
            if (api.userDAO != null) {
                try {
                    List<Consultant> dbConsultants = api.userDAO.getAllConsultants();
                    for (Consultant c : dbConsultants) {
                        // Only show approved consultants to clients
                        if (c.isApproved()) {
                            consultants.add("{\"userId\":\"" + c.getUserID() +
                                "\",\"name\":\"" + c.getName() +
                                "\",\"email\":\"" + c.getEmail() +
                                "\",\"isApproved\":true}");
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching consultants from DB: " + e.getMessage());
                }
            }

            // Fallback to memory if DB returned nothing
            if (consultants.isEmpty()) {
                for (User u : api.usersByEmail.values()) {
                    if (u instanceof Consultant) {
                        Consultant c = (Consultant) u;
                        // Only show approved consultants to clients
                        if (c.isApproved()) {
                            consultants.add("{\"userId\":\"" + c.getUserID() +
                                "\",\"name\":\"" + c.getName() +
                                "\",\"email\":\"" + c.getEmail() +
                                "\",\"isApproved\":true}");
                        }
                    }
                }
            }

            // Add demo consultants if none exist
            if (consultants.isEmpty()) {
                consultants.add("{\"userId\":\"CONS-001\",\"name\":\"Dr. Sarah Johnson\",\"email\":\"sarah@example.com\",\"isApproved\":true}");
                consultants.add("{\"userId\":\"CONS-002\",\"name\":\"Prof. Michael Chen\",\"email\":\"michael@example.com\",\"isApproved\":true}");
            }

            api.sendResponse(exchange, "{\"success\":true,\"consultants\":[" + String.join(",", consultants) + "]}", 200);
        }
    }

    static class GetClientsHandler implements HttpHandler {
        private final ApiServer api;
        public GetClientsHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> clients = new ArrayList<>();
            for (User u : api.usersByEmail.values()) {
                if (u instanceof Client) {
                    Client c = (Client) u;
                    clients.add("{\"userId\":\"" + c.getUserID() +
                        "\",\"name\":\"" + c.getName() +
                        "\",\"email\":\"" + c.getEmail() + "\"}");
                }
            }

            api.sendResponse(exchange, "{\"success\":true,\"clients\":[" + String.join(",", clients) + "]}", 200);
        }
    }

    static class ApproveConsultantHandler implements HttpHandler {
        private final ApiServer api;
        public ApproveConsultantHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            // Support both consultantId and email
            String consultantId = data.get("consultantId");
            String email = data.get("email");

            // Find and approve consultant
            for (User u : api.usersByEmail.values()) {
                if (u instanceof Consultant) {
                    boolean match = (consultantId != null && u.getUserID().toString().equals(consultantId))
                                 || (email != null && u.getEmail().equals(email));
                    if (match) {
                        ((Consultant) u).setApproved(true);
                        // Update in database
                        if (api.userDAO != null) {
                            api.userDAO.updateApprovalStatus(u.getUserID().toString(), true);
                        }
                        api.sendResponse(exchange, "{\"success\":true,\"message\":\"Consultant approved successfully\"}", 200);
                        return;
                    }
                }
            }

            api.sendResponse(exchange, "{\"success\":false,\"error\":\"Consultant not found\"}", 404);
        }
    }

    static class RejectConsultantHandler implements HttpHandler {
        private final ApiServer api;
        public RejectConsultantHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            // Support both consultantId and email
            String consultantId = data.get("consultantId");
            String email = data.get("email");

            // Find and reject consultant
            for (User u : api.usersByEmail.values()) {
                if (u instanceof Consultant) {
                    boolean match = (consultantId != null && u.getUserID().toString().equals(consultantId))
                                 || (email != null && u.getEmail().equals(email));
                    if (match) {
                        ((Consultant) u).setApproved(false);
                        api.sendResponse(exchange, "{\"success\":true,\"message\":\"Consultant rejected successfully\"}", 200);
                        return;
                    }
                }
            }

            api.sendResponse(exchange, "{\"success\":false,\"error\":\"Consultant not found\"}", 404);
        }
    }

    static class GetServicesHandler implements HttpHandler {
        private final ApiServer api;
        public GetServicesHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> serviceList = new ArrayList<>();

            // Try to get from database first
            if (api.serviceDAO != null) {
                try {
                    List<backend.core.ConsultingService> dbServices = api.serviceDAO.getAllServices();
                    for (backend.core.ConsultingService s : dbServices) {
                        serviceList.add("{\"serviceId\":\"" + s.getServiceId() +
                            "\",\"name\":\"" + s.getName() +
                            "\",\"description\":\"" + s.getDescription() +
                            "\",\"basePrice\":" + s.getBasePrice() +
                            ",\"durationMinutes\":" + s.getDurationMinutes() +
                            ",\"category\":\"" + s.getCategory() + "\"}");
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching services from DB: " + e.getMessage());
                }
            }

            // Fallback to memory if DB returned nothing
            if (serviceList.isEmpty()) {
                for (backend.core.ConsultingService s : api.services.values()) {
                    serviceList.add("{\"serviceId\":\"" + s.getServiceId() +
                        "\",\"name\":\"" + s.getName() +
                        "\",\"description\":\"" + s.getDescription() +
                        "\",\"basePrice\":" + s.getBasePrice() +
                        ",\"durationMinutes\":" + s.getDurationMinutes() +
                        ",\"category\":\"" + s.getCategory() + "\"}");
                }
            }

            api.sendResponse(exchange, "{\"success\":true,\"services\":[" + String.join(",", serviceList) + "]}", 200);
        }
    }

    static class CreateServiceHandler implements HttpHandler {
        private final ApiServer api;
        public CreateServiceHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            String name = data.get("name");
            String description = data.get("description");
            double basePrice = Double.parseDouble(data.getOrDefault("basePrice", "100"));
            int durationMinutes = Integer.parseInt(data.getOrDefault("durationMinutes", "60"));
            String category = data.getOrDefault("category", "CAREER");

            backend.core.ConsultingService service = new backend.core.ConsultingService(
                name, description, basePrice, durationMinutes, ServiceCategory.valueOf(category));

            api.services.put(service.getServiceId(), service);

            api.sendResponse(exchange, "{\"success\":true,\"serviceId\":\"" + service.getServiceId() + "\",\"message\":\"Service created successfully\"}", 200);
        }
    }

    static class CreateBookingHandler implements HttpHandler {
        private final ApiServer api;
        public CreateBookingHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            String clientId = data.get("clientId");
            String consultantId = data.get("consultantId");
            String serviceId = data.get("serviceId");
            String startTime = data.get("startTime");

            try {
                // Find client
                Client client = null;
                Consultant consultant = null;
                backend.core.ConsultingService service = null;

                if (api.userDAO != null) {
                    client = (Client) api.userDAO.findById(clientId);
                    consultant = (Consultant) api.userDAO.findById(consultantId);
                    service = api.serviceDAO.findById(UUID.fromString(serviceId));
                }

                // Fallback to memory if DB not available
                if (client == null) {
                    for (User u : api.usersByEmail.values()) {
                        if (u instanceof Client && u.getUserID().toString().equals(clientId)) {
                            client = (Client) u;
                            break;
                        }
                    }
                }
                if (consultant == null) {
                    for (User u : api.usersByEmail.values()) {
                        if (u instanceof Consultant && u.getUserID().toString().equals(consultantId)) {
                            consultant = (Consultant) u;
                            break;
                        }
                    }
                }
                if (service == null) {
                    service = api.services.values().stream().findFirst().orElse(null);
                }

                if (client == null || consultant == null || service == null) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Invalid client, consultant or service\"}", 400);
                    return;
                }

                // Check if consultant is approved
                if (!consultant.isApproved()) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Consultant is not approved yet\"}", 403);
                    return;
                }

                java.time.LocalDateTime start = startTime != null ?
                    java.time.LocalDateTime.parse(startTime.replace(" ", "T")) :
                    java.time.LocalDateTime.now().plusDays(1);

                Booking booking = new Booking(client, consultant, service, start);
                booking.setStatus(BookingStatus.Requested);

                // Save to database
                if (api.bookingDAO != null) {
                    api.bookingDAO.insert(booking);
                }

                api.bookings.put(booking.getBookingId(), booking);

                api.sendResponse(exchange, "{\"success\":true,\"bookingId\":\"" + booking.getBookingId() +
                    "\",\"status\":\"Requested\",\"message\":\"Booking created successfully\"}", 200);
            } catch (Exception e) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    static class GetClientBookingsHandler implements HttpHandler {
        private final ApiServer api;
        public GetClientBookingsHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String clientId = query != null && query.contains("clientId=") ?
                query.split("clientId=")[1].split("&")[0] : null;

            List<String> bookingList = new ArrayList<>();

            // Try database first
            if (api.bookingDAO != null && clientId != null) {
                try {
                    List<Booking> dbBookings = api.bookingDAO.findByClientId(clientId);
                    for (Booking b : dbBookings) {
                        bookingList.add("{\"bookingId\":\"" + b.getBookingId() +
                            "\",\"clientName\":\"" + b.getClient().getName() +
                            "\",\"consultantName\":\"" + b.getConsultant().getName() +
                            "\",\"serviceName\":\"" + b.getService().getName() +
                            "\",\"startTime\":\"" + b.getStartTime() +
                            "\",\"status\":\"" + b.getStatus() + "\"}");
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching client bookings from DB: " + e.getMessage());
                }
            }

            // Fallback to memory
            if (bookingList.isEmpty()) {
                for (Booking b : api.bookings.values()) {
                    if (clientId == null || b.getClient().getUserID().toString().equals(clientId)) {
                        bookingList.add("{\"bookingId\":\"" + b.getBookingId() +
                            "\",\"clientName\":\"" + b.getClient().getName() +
                            "\",\"consultantName\":\"" + b.getConsultant().getName() +
                            "\",\"serviceName\":\"" + b.getService().getName() +
                            "\",\"startTime\":\"" + b.getStartTime() +
                            "\",\"status\":\"" + b.getStatus() + "\"}");
                    }
                }
            }

            api.sendResponse(exchange, "{\"success\":true,\"bookings\":[" + String.join(",", bookingList) + "]}", 200);
        }
    }

    static class GetConsultantBookingsHandler implements HttpHandler {
        private final ApiServer api;
        public GetConsultantBookingsHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String consultantId = query != null && query.contains("consultantId=") ?
                query.split("consultantId=")[1].split("&")[0] : null;

            List<String> bookingList = new ArrayList<>();

            // Try database first
            if (api.bookingDAO != null && consultantId != null) {
                try {
                    List<Booking> dbBookings = api.bookingDAO.findByConsultantId(consultantId);
                    for (Booking b : dbBookings) {
                        bookingList.add("{\"bookingId\":\"" + b.getBookingId() +
                            "\",\"clientName\":\"" + b.getClient().getName() +
                            "\",\"serviceName\":\"" + b.getService().getName() +
                            "\",\"startTime\":\"" + b.getStartTime() +
                            "\",\"status\":\"" + b.getStatus() + "\"}");
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching consultant bookings from DB: " + e.getMessage());
                }
            }

            // Fallback to memory
            if (bookingList.isEmpty()) {
                for (Booking b : api.bookings.values()) {
                    if (consultantId == null || b.getConsultant().getUserID().toString().equals(consultantId)) {
                        bookingList.add("{\"bookingId\":\"" + b.getBookingId() +
                            "\",\"clientName\":\"" + b.getClient().getName() +
                            "\",\"serviceName\":\"" + b.getService().getName() +
                            "\",\"startTime\":\"" + b.getStartTime() +
                            "\",\"status\":\"" + b.getStatus() + "\"}");
                    }
                }
            }

            api.sendResponse(exchange, "{\"success\":true,\"bookings\":[" + String.join(",", bookingList) + "]}", 200);
        }
    }

    static class ConfirmBookingHandler implements HttpHandler {
        private final ApiServer api;
        public ConfirmBookingHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);
            String bookingId = data.get("bookingId");

            // Try database first
            if (api.bookingDAO != null && bookingId != null) {
                try {
                    Booking b = api.bookingDAO.findById(UUID.fromString(bookingId));
                    if (b != null) {
                        b.confirm();
                        api.bookingDAO.update(b);
                        api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking confirmed successfully\"}", 200);
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("DB confirm error: " + e.getMessage());
                }
            }

            // Fallback to memory
            for (Booking b : api.bookings.values()) {
                if (b.getBookingId().toString().equals(bookingId)) {
                    b.confirm();
                    api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking confirmed successfully\"}", 200);
                    return;
                }
            }

            api.sendResponse(exchange, "{\"success\":false,\"error\":\"Booking not found\"}", 404);
        }
    }

    static class CancelBookingHandler implements HttpHandler {
        private final ApiServer api;
        public CancelBookingHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);
            String bookingId = data.get("bookingId");

            // Try database first
            if (api.bookingDAO != null && bookingId != null) {
                try {
                    Booking b = api.bookingDAO.findById(UUID.fromString(bookingId));
                    if (b != null) {
                        b.cancel();
                        api.bookingDAO.update(b);
                        api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking cancelled successfully\"}", 200);
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("DB cancel error: " + e.getMessage());
                }
            }

            // Fallback to memory
            for (Booking b : api.bookings.values()) {
                if (b.getBookingId().toString().equals(bookingId)) {
                    try {
                        b.cancel();
                        api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking cancelled successfully\"}", 200);
                        return;
                    } catch (IllegalStateException e) {
                        api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 400);
                        return;
                    }
                }
            }

            api.sendResponse(exchange, "{\"success\":false,\"error\":\"Booking not found\"}", 404);
        }
    }

    static class CompleteBookingHandler implements HttpHandler {
        private final ApiServer api;
        public CompleteBookingHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);
            String bookingId = data.get("bookingId");

            // Try database first
            if (api.bookingDAO != null && bookingId != null) {
                try {
                    Booking b = api.bookingDAO.findById(UUID.fromString(bookingId));
                    if (b != null) {
                        b.complete();
                        api.bookingDAO.update(b);
                        api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking completed successfully\"}", 200);
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("DB complete error: " + e.getMessage());
                }
            }

            // Fallback to memory
            for (Booking b : api.bookings.values()) {
                if (b.getBookingId().toString().equals(bookingId)) {
                    b.complete();
                    api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking completed successfully\"}", 200);
                    return;
                }
            }

            api.sendResponse(exchange, "{\"success\":false,\"error\":\"Booking not found\"}", 404);
        }
    }

    static class MakePaymentHandler implements HttpHandler {
        private final ApiServer api;
        public MakePaymentHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            try {
                String body = api.readRequestBody(exchange);
                Map<String, String> data = api.parseJsonBody(body);
                String bookingId = data.get("bookingId");
                String paymentMethod = data.getOrDefault("paymentMethod", "CreditCard");
                double amount = Double.parseDouble(data.getOrDefault("amount", "100"));

                PaymentMethod method = PaymentMethod.valueOf(paymentMethod);
                PaymentTransaction transaction = new PaymentTransaction(amount, method, "****1234");
                transaction.succeed();

                // Update booking in database
                if (api.bookingDAO != null && bookingId != null) {
                    try {
                        Booking b = api.bookingDAO.findById(UUID.fromString(bookingId));
                        if (b != null) {
                            b.markPaid();
                            api.bookingDAO.update(b);
                        }
                    } catch (Exception e) {
                        System.err.println("DB payment update error: " + e.getMessage());
                    }
                }

                // Also update in memory
                for (Booking b : api.bookings.values()) {
                    if (b.getBookingId().toString().equals(bookingId)) {
                        b.markPaid();
                        break;
                    }
                }

                // Save payment to database
                if (api.paymentDAO != null) {
                    try {
                        UUID bookingUUID = bookingId != null ? UUID.fromString(bookingId) : null;
                        api.paymentDAO.insert(transaction, bookingUUID);
                    } catch (Exception e) {
                        System.err.println("DB payment insert error: " + e.getMessage());
                    }
                }
                api.payments.put(transaction.getTransactionId().toString(), transaction);

                api.sendResponse(exchange, "{\"success\":true,\"paymentId\":\"" + transaction.getTransactionId() +
                    "\",\"status\":\"SUCCESS\",\"message\":\"Payment processed successfully\"}", 200);
            } catch (Exception e) {
                System.err.println("Payment error: " + e.getMessage());
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    static class PaymentHistoryHandler implements HttpHandler {
        private final ApiServer api;
        public PaymentHistoryHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String clientId = query != null && query.contains("clientId=") ?
                query.split("clientId=")[1].split("&")[0] : null;

            List<String> paymentList = new ArrayList<>();

            // Try database first
            if (api.paymentDAO != null && clientId != null) {
                try {
                    List<PaymentTransaction> dbPayments = api.paymentDAO.findByUserId(clientId);
                    for (PaymentTransaction t : dbPayments) {
                        paymentList.add("{\"paymentId\":\"" + t.getTransactionId() +
                            "\",\"amount\":" + t.getAmount() +
                            ",\"paymentMethod\":\"" + t.getPaymentMethod() +
                            "\",\"status\":\"" + t.getStatus() + "\"}");
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching payment history from DB: " + e.getMessage());
                }
            }

            // Fallback to memory
            if (paymentList.isEmpty()) {
                for (PaymentTransaction t : api.payments.values()) {
                    paymentList.add("{\"paymentId\":\"" + t.getTransactionId() +
                        "\",\"amount\":" + t.getAmount() +
                        ",\"paymentMethod\":\"" + t.getPaymentMethod() +
                        "\",\"status\":\"" + t.getStatus() + "\"}");
                }
            }

            api.sendResponse(exchange, "{\"success\":true,\"payments\":[" + String.join(",", paymentList) + "]}", 200);
        }
    }

    static class AddPaymentMethodHandler implements HttpHandler {
        private final ApiServer api;
        public AddPaymentMethodHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            String clientId = data.get("clientId");
            String paymentType = data.get("paymentType");
            String maskedDetails = data.get("maskedDetails");

            if (clientId == null || paymentType == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Missing required fields\"}", 400);
                return;
            }

            System.out.println("Adding payment method for client: " + clientId + ", type: " + paymentType);

            // Ensure client exists in database first
            if (api.userDAO != null) {
                try {
                    backend.user.User existingClient = api.userDAO.findById(clientId);
                    if (existingClient == null) {
                        System.out.println("Client not found in DB, checking memory...");
                        // Try to find client in memory first
                        backend.user.Client memoryClient = null;
                        for (User u : api.usersByEmail.values()) {
                            if (u instanceof backend.user.Client && u.getUserID().toString().equals(clientId)) {
                                memoryClient = (backend.user.Client) u;
                                break;
                            }
                        }
                        
                        if (memoryClient != null) {
                            // Found in memory, save to database
                            boolean created = api.userDAO.insert(memoryClient);
                            if (created) {
                                System.out.println("Client created in database successfully from memory");
                            } else {
                                System.err.println("Failed to create client in database");
                            }
                        } else {
                            System.err.println("Client not found in memory either. Cannot create payment method.");
                            api.sendResponse(exchange, "{\"success\":false,\"error\":\"Client not found\"}", 404);
                            return;
                        }
                    }
                } catch (Exception e) {
                    System.err.println("Error ensuring client exists: " + e.getMessage());
                    e.printStackTrace();
                }
            }

            // Try database first
            if (api.paymentMethodDAO != null) {
                try {
                    String methodId = api.paymentMethodDAO.insert(clientId, paymentType, maskedDetails != null ? maskedDetails : "");
                    if (methodId != null && !methodId.isEmpty()) {
                        System.out.println("Payment method saved to database successfully! Method ID: " + methodId);
                        api.sendResponse(exchange, "{\"success\":true,\"methodId\":\"" + methodId + "\",\"message\":\"Payment method added successfully\"}", 200);
                        return;
                    } else {
                        System.err.println("Payment method insert returned null or empty methodId");
                    }
                } catch (Exception e) {
                    System.err.println("Error saving payment method to database: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("PaymentMethodDAO is null - database not initialized");
            }

            // Fallback response
            api.sendResponse(exchange, "{\"success\":false,\"error\":\"Failed to save payment method to database\"}", 500);
        }
    }

    static class ListPaymentMethodsHandler implements HttpHandler {
        private final ApiServer api;
        public ListPaymentMethodsHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String clientId = query != null && query.contains("clientId=") ?
                query.split("clientId=")[1].split("&")[0] : null;

            if (clientId == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"clientId required\"}", 400);
                return;
            }

            List<String> methodList = new ArrayList<>();

            if (api.paymentMethodDAO != null) {
                List<Map<String, String>> methods = api.paymentMethodDAO.findByClientId(clientId);
                for (Map<String, String> m : methods) {
                    methodList.add("{\"methodId\":\"" + m.get("methodId") +
                        "\",\"paymentType\":\"" + m.get("paymentType") +
                        "\",\"details\":\"" + m.get("details") + "\"}");
                }
            }

            api.sendResponse(exchange, "{\"success\":true,\"methods\":[" + String.join(",", methodList) + "]}", 200);
        }
    }

    static class RemovePaymentMethodHandler implements HttpHandler {
        private final ApiServer api;
        public RemovePaymentMethodHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            String methodId = data.get("methodId");
            String clientId = data.get("clientId");

            if (methodId == null || clientId == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Missing methodId or clientId\"}", 400);
                return;
            }

            if (api.paymentMethodDAO != null) {
                boolean deleted = api.paymentMethodDAO.delete(methodId, clientId);
                if (deleted) {
                    api.sendResponse(exchange, "{\"success\":true,\"message\":\"Payment method removed\"}", 200);
                    return;
                }
            }

            api.sendResponse(exchange, "{\"success\":false,\"error\":\"Payment method not found\"}", 404);
        }
    }

    static class SetAvailabilityHandler implements HttpHandler {
        private final ApiServer api;
        public SetAvailabilityHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            String consultantId = data.get("consultantId");
            String startTime = data.get("startTime");
            String endTime = data.get("endTime");

            if (consultantId == null || startTime == null || endTime == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Missing required fields\"}", 400);
                return;
            }

            try {
                java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime.replace(" ", "T"));
                java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime.replace(" ", "T"));

                System.out.println("Setting availability for consultant: " + consultantId + " from " + start + " to " + end);

                // Try database first
                if (api.availabilityDAO != null) {
                    boolean success = api.availabilityDAO.addAvailability(consultantId, start, end);
                    if (success) {
                        System.out.println("Availability saved to database successfully!");
                        api.sendResponse(exchange, "{\"success\":true,\"message\":\"Availability set successfully\"}", 200);
                        return;
                    } else {
                        System.err.println("Failed to save availability to database");
                    }
                } else {
                    System.err.println("AvailabilityDAO is null - database not initialized");
                }

                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Failed to set availability\"}", 500);
            } catch (Exception e) {
                System.err.println("Error setting availability: " + e.getMessage());
                e.printStackTrace();
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    static class GetAvailabilityHandler implements HttpHandler {
        private final ApiServer api;
        public GetAvailabilityHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String query = exchange.getRequestURI().getQuery();
            String consultantId = query != null && query.contains("consultantId=") ?
                query.split("consultantId=")[1].split("&")[0] : null;

            if (consultantId == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"consultantId required\"}", 400);
                return;
            }

            List<String> slots = new ArrayList<>();

            System.out.println("Getting availability for consultant: " + consultantId);

            if (api.availabilityDAO != null) {
                try {
                    List<backend.core.TimeSlot> dbSlots = api.availabilityDAO.getAvailableSlots(consultantId);
                    System.out.println("Found " + dbSlots.size() + " availability slots from database");
                    for (backend.core.TimeSlot slot : dbSlots) {
                        slots.add("{\"startTime\":\"" + slot.getStartTime() +
                            "\",\"endTime\":\"" + slot.getEndTime() + "\"}");
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching availability from DB: " + e.getMessage());
                    e.printStackTrace();
                }
            } else {
                System.err.println("AvailabilityDAO is null - cannot fetch from database");
            }

            api.sendResponse(exchange, "{\"success\":true,\"availability\":[" + String.join(",", slots) + "]}", 200);
        }
    }

    static class AIChatHandler implements HttpHandler {
        private final ApiServer api;
        public AIChatHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);
            String message = data.get("message");

            if (message == null || message.trim().isEmpty()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Message required\"}", 400);
                return;
            }

            // 优先使用阿里云 AI 服务，如果失败则回退到本地规则
            String response;
            if (api.aiChatService != null) {
                try {
                    response = api.aiChatService.chat(message);
                    System.out.println("AI Response (Alibaba): " + response.substring(0, Math.min(50, response.length())) + "...");
                } catch (Exception e) {
                    System.err.println("Alibaba AI failed, using fallback: " + e.getMessage());
                    response = api.chatbotService.getResponse(message);
                }
            } else {
                response = api.chatbotService.getResponse(message);
            }

            api.sendResponse(exchange, "{\"success\":true,\"response\":\"" + response.replace("\"", "\\\"") + "\"}", 200);
        }
    }

    public static void main(String[] args) {
        ApiServer apiServer = new ApiServer();
        apiServer.start();
    }
}
