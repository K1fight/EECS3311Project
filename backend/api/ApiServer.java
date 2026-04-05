package backend.api;

import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import backend.EnvConfig;
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

    private final int PORT = EnvConfig.getInt("SERVER_PORT", 8080);

    // In-memory storage for demo (fallback when DB not available)
    private static Map<String, User> usersByEmail = new HashMap<>();
    private static Map<String, User> usersById = new HashMap<>();  // userId (String) → User
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
                bookingService.setBookingDAO(bookingDAO);  // inject DAO so local fallback persists to DB
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
        server.createContext("/api/users/pending-consultants", new GetPendingConsultantsHandler(this));
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
        server.createContext("/api/bookings/reject", new RejectBookingHandler(this));
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

        // Log ALL outgoing responses for debugging
        String method = exchange.getRequestMethod();
        String uri = exchange.getRequestURI().toString();
        System.out.println("\n========== API RESPONSE ==========");
        System.out.println("[" + method + "] " + uri);
        System.out.println("HTTP Status: " + statusCode);
        System.out.println("Body: " + jsonResponse);
        System.out.println("==================================\n");

        byte[] res = jsonResponse.getBytes(StandardCharsets.UTF_8);
        exchange.sendResponseHeaders(statusCode, res.length);

        try (OutputStream os = exchange.getResponseBody()) {
            os.write(res);
        }
    }

    // Helper to read request body
    public String readRequestBody(HttpExchange exchange) throws IOException {
        String method = exchange.getRequestMethod();
        String uri = exchange.getRequestURI().toString();
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);

        System.out.println("\n<<<< API REQUEST >>>>");
        System.out.println("[" + method + "] " + uri);
        System.out.println("Authorization: " + (auth != null ? auth : "(none)"));
        if (!body.isBlank()) {
            System.out.println("Body: " + body);
        }
        System.out.println("<<<< END REQUEST >>>>\n");

        return body;
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

    /**
     * Wrap a raw string in a JSON-safe quoted string.
     * Escapes backslash and double-quote characters.
     * Called from static inner classes — must be static.
     */
    private static String safeJson(String raw) {
        if (raw == null) return "null";
        String escaped = raw.replace("\\", "\\\\").replace("\"", "\\\"");
        return "\"" + escaped + "\"";
    }

    // ======================== AUTH HELPER ========================

    /**
     * Authenticate the caller via Authorization header: {userId}
     * Returns the authenticated User, or null on failure (response already sent).
     */
    User authenticate(HttpExchange exchange, boolean requireAuth) throws IOException {
        String auth = exchange.getRequestHeaders().getFirst("Authorization");
        System.out.println("[authenticate] Authorization header=" + (auth != null ? auth : "null"));
        if (auth == null || auth.isBlank()) {
            if (requireAuth) {
                sendResponse(exchange, "{\"success\":false,\"error\":\"Authorization header required\"}", 401);
                return null;
            }
            return null;
        }
        String userId = auth.trim();

        User user = null;
        if (userDAO != null) {
            try {
                user = userDAO.findById(userId);
                System.out.println("[authenticate] userId=" + userId + " → userDAO.findById=" + (user != null ? user.getEmail() + "/" + user.getUserID() : "null"));
            } catch (Exception e) {
                System.err.println("[authenticate] userDAO.findById error: " + e.getMessage());
            }
        }
        if (user == null) {
            user = usersById.get(userId);            // direct O(1) lookup
            System.out.println("[authenticate] userId=" + userId + " → usersById.get=" + (user != null ? user.getEmail() : "null"));
        }
        if (user == null) {
            for (User u : usersByEmail.values()) {
                if (u.getUserID().toString().equals(userId)) {
                    user = u;
                    break;
                }
            }
            System.out.println("[authenticate] userId=" + userId + " → usersByEmail scan=" + (user != null ? user.getEmail() : "null"));
        }
        if (user == null) {
            if (requireAuth) {
                System.err.println("[authenticate] User not found: " + userId);
                sendResponse(exchange, "{\"success\":false,\"error\":\"User not found\"}", 401);
            }
            return null;
        }
        System.out.println("[authenticate] Success: " + user.getEmail() + " (" + user.getAccountType() + ")");
        return user;
    }

    /**
     * Require the caller to have one of the given account types.
     * Sends 403 and returns false if the check fails.
     */
    boolean requireRole(HttpExchange exchange, User user, String... allowed) throws IOException {
        if (user == null) {
            sendResponse(exchange, "{\"success\":false,\"error\":\"Unauthorized\"}", 401);
            return false;
        }
        String actual = user.getAccountType().toString();
        for (String role : allowed) {
            if (actual.equalsIgnoreCase(role)) {
                return true;
            }
        }
        sendResponse(exchange,
            "{\"success\":false,\"error\":\"Forbidden: requires \" + String.join(\" or \", allowed) + \" role\"}",
            403);
        return false;
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
                    // Database-first: register directly in DB
                    if (api.userDAO != null) {
                        User existing = api.userDAO.findByEmail(email);
                        if (existing != null) {
                            api.sendResponse(exchange, "{\"success\":false,\"error\":\"Email already registered\"}", 400);
                            return;
                        }
                        Client client = new Client(name, email, password);
                        boolean inserted = api.userDAO.insert(client);
                        if (inserted) {
                            User dbUser = api.userDAO.findByEmail(email);
                            String userId = dbUser != null ? dbUser.getUserID().toString() : "unknown";
                            System.out.println("Client registered in DB: userId=" + userId + ", email=" + email);
                            api.sendResponse(exchange, "{\"success\":true,\"userId\":\"" + userId + "\",\"message\":\"User registered successfully\"}", 200);
                            return;
                        }
                    }
                    // Memory fallback only if DB is unavailable
                    Client client = api.userService.registerClient(name, email, password);
                    if (client != null) {
                        api.sendResponse(exchange, "{\"success\":true,\"userId\":\"" + client.getUserID().toString() + "\",\"message\":\"User registered successfully (memory mode)\"}", 200);
                    } else {
                        api.sendResponse(exchange, "{\"success\":false,\"error\":\"Registration failed\"}", 400);
                    }
                } else if ("Consultant".equals(accountType)) {
                    // Database-first: register directly in DB
                    if (api.userDAO != null) {
                        User existing = api.userDAO.findByEmail(email);
                        if (existing != null) {
                            api.sendResponse(exchange, "{\"success\":false,\"error\":\"Email already registered\"}", 400);
                            return;
                        }
                        Consultant consultant = new Consultant(java.util.UUID.randomUUID(), name, email, password, false);
                        boolean inserted = api.userDAO.insert(consultant);
                        if (inserted) {
                            User dbUser = api.userDAO.findByEmail(email);
                            String userId = dbUser != null ? dbUser.getUserID().toString() : "unknown";
                            System.out.println("Consultant registered in DB: userId=" + userId + ", email=" + email);
                            api.sendResponse(exchange, "{\"success\":true,\"userId\":\"" + userId + "\",\"message\":\"User registered successfully\"}", 200);
                            return;
                        }
                    }
                    // Memory fallback only if DB is unavailable
                    Consultant consultant = api.userService.registerConsultant(name, email, password);
                    if (consultant != null) {
                        api.sendResponse(exchange, "{\"success\":true,\"userId\":\"" + consultant.getUserID().toString() + "\",\"message\":\"User registered successfully (memory mode)\"}", 200);
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
            String accountType = data.get("accountType"); // optional: enforce role

            if (email == null || password == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Missing email or password\"}", 400);
                return;
            }

            // Database-first: always query DB for latest data
            User user = null;
            if (api.userDAO != null) {
                user = api.userDAO.findByEmail(email);
                if (user != null) {
                    if (!user.getPassword().equals(password)) {
                        api.sendResponse(exchange, "{\"success\":false,\"error\":\"Invalid credentials\"}", 401);
                        return;
                    }
                    if (user instanceof Consultant c && !c.isApproved()) {
                        api.sendResponse(exchange, "{\"success\":false,\"error\":\"Account pending approval\"}", 401);
                        return;
                    }
                    if (accountType != null && !accountType.isEmpty()
                            && !user.getAccountType().toString().equalsIgnoreCase(accountType)) {
                        api.sendResponse(exchange,
                            "{\"success\":false,\"error\":\"Account type mismatch: cannot log in as \" + accountType}",
                            403);
                        return;
                    }
                }
            }

            // Fallback to in-memory ONLY when DB is not available (userDAO == null)
            if (user == null && api.userDAO == null) {
                user = api.userService.authenticateUser(email, password);
                if (user == null) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Invalid credentials\"}", 401);
                    return;
                }
            }

            if (user == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Invalid credentials\"}", 401);
                return;
            }

            String actualUserId = user.getUserID().toString();
            boolean isApproved = false;
            if (user instanceof Consultant c) isApproved = c.isApproved();

            String response = "{\"success\":true,\"userId\":\"" + actualUserId +
                "\",\"name\":\"" + user.getName() +
                "\",\"email\":\"" + user.getEmail() +
                "\",\"accountType\":\"" + user.getAccountType() +
                "\",\"isApproved\":" + isApproved + "}";
            api.sendResponse(exchange, response, 200);
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

            // Authenticate caller
            User caller = api.authenticate(exchange, true);
            if (caller == null) return;

            // Only the user themselves or an Admin can view this profile
            boolean isSelf = caller.getUserID().toString().equals(userId);
            boolean isAdmin = "Admin".equalsIgnoreCase(caller.getAccountType().toString());
            if (!isSelf && !isAdmin) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            // Database-first: always query DB for latest data
            User u = null;
            if (api.userDAO != null) {
                u = api.userDAO.findById(userId);
            } else {
                // Memory fallback only if DB is unavailable
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
            // Authenticate — Admin and Client can view consultants; Consultant can view too
            User caller = api.authenticate(exchange, true);
            if (caller == null) return;
            String role = caller.getAccountType().toString();
            if (!"Admin".equalsIgnoreCase(role) && !"Client".equalsIgnoreCase(role)
                    && !"Consultant".equalsIgnoreCase(role)) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            // FIX: Admin sees ALL (approved + pending); Client/Consultant only sees approved
            boolean showOnlyApproved = !"Admin".equalsIgnoreCase(role);
            List<String> consultants = new ArrayList<>();

            // Database-first: always query DB for latest data
            if (api.userDAO != null) {
                try {
                    List<Consultant> dbConsultants = api.userDAO.getAllConsultants();
                    for (Consultant c : dbConsultants) {
                        if (showOnlyApproved && !c.isApproved()) continue;
                        consultants.add("{\"userId\":\"" + c.getUserID() +
                            "\",\"name\":\"" + c.getName() +
                            "\",\"email\":\"" + c.getEmail() +
                            "\",\"isApproved\":" + c.isApproved() + "}");
                    }
                    System.out.println("[GetConsultants] Fetched " + consultants.size() + " from DB");
                } catch (Exception e) {
                    System.err.println("Error fetching consultants from DB: " + e.getMessage());
                    e.printStackTrace();
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database error\"}", 500);
                    return;
                }
            } else {
                // Memory fallback only if DB is unavailable
                for (User u : api.usersByEmail.values()) {
                    if (u instanceof Consultant) {
                        Consultant c = (Consultant) u;
                        if (showOnlyApproved && !c.isApproved()) continue;
                        consultants.add("{\"userId\":\"" + c.getUserID() +
                            "\",\"name\":\"" + c.getName() +
                            "\",\"email\":\"" + c.getEmail() +
                            "\",\"isApproved\":" + c.isApproved() + "}");
                    }
                }
            }

            // Return whatever we found (empty array is a valid response)
            api.sendResponse(exchange, "{\"success\":true,\"consultants\":[" + String.join(",", consultants) + "]}", 200);
        }
    }

    /**
     * Dedicated endpoint to list ALL pending (unapproved) consultants.
     * Admin-only.
     */
    static class GetPendingConsultantsHandler implements HttpHandler {
        private final ApiServer api;
        public GetPendingConsultantsHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            User caller = api.authenticate(exchange, true);
            if (caller == null) return;
            if (!"Admin".equalsIgnoreCase(caller.getAccountType().toString())) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            List<String> pending = new ArrayList<>();

            // Database-first: always query DB for latest data
            if (api.userDAO != null) {
                try {
                    List<Consultant> dbPending = api.userDAO.getPendingConsultants();
                    for (Consultant c : dbPending) {
                        pending.add("{\"userId\":\"" + c.getUserID() +
                            "\",\"name\":\"" + (c.getName() != null ? c.getName() : "") +
                            "\",\"email\":\"" + (c.getEmail() != null ? c.getEmail() : "") +
                            "\",\"isApproved\":false}");
                    }
                    System.out.println("[GetPendingConsultants] Found " + pending.size() + " pending in DB");
                } catch (Exception e) {
                    System.err.println("Error fetching pending consultants from DB: " + e.getMessage());
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database error\"}", 500);
                    return;
                }
            } else {
                // Memory fallback only if DB is unavailable
                for (User u : api.usersByEmail.values()) {
                    if (u instanceof Consultant c && !c.isApproved()) {
                        pending.add("{\"userId\":\"" + c.getUserID() +
                            "\",\"name\":\"" + c.getName() +
                            "\",\"email\":\"" + c.getEmail() +
                            "\",\"isApproved\":false}");
                    }
                }
            }

            api.sendResponse(exchange,
                "{\"success\":true,\"consultants\":[" + String.join(",", pending) + "]}",
                200);
        }
    }

    static class GetClientsHandler implements HttpHandler {
        private final ApiServer api;
        public GetClientsHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Authenticate — Admin and Consultant can view clients
            User caller = api.authenticate(exchange, true);
            if (caller == null) return;
            String role = caller.getAccountType().toString();
            if (!"Admin".equalsIgnoreCase(role) && !"Consultant".equalsIgnoreCase(role)) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden: requires Admin or Consultant role\"}", 403);
                return;
            }

            List<String> clients = new ArrayList<>();

            // Database-first: always query DB for latest data
            if (api.userDAO != null) {
                try {
                    List<Client> dbClients = api.userDAO.getAllClients();
                    for (Client c : dbClients) {
                        clients.add("{\"userId\":\"" + c.getUserID() +
                            "\",\"name\":\"" + (c.getName() != null ? c.getName() : "") +
                            "\",\"email\":\"" + (c.getEmail() != null ? c.getEmail() : "") + "\"}");
                    }
                    System.out.println("[GetClients] Fetched " + clients.size() + " from DB");
                } catch (Exception e) {
                    System.err.println("Error fetching clients from DB: " + e.getMessage());
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database error\"}", 500);
                    return;
                }
            } else {
                // Memory fallback only if DB is unavailable
                for (User u : api.usersByEmail.values()) {
                    if (u instanceof Client) {
                        Client c = (Client) u;
                        clients.add("{\"userId\":\"" + c.getUserID() +
                            "\",\"name\":\"" + c.getName() +
                            "\",\"email\":\"" + c.getEmail() + "\"}");
                    }
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
            // Authenticate — Admin only
            User caller = api.authenticate(exchange, true);
            if (caller == null) return;
            if (!"Admin".equalsIgnoreCase(caller.getAccountType().toString())) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden: requires Admin role\"}", 403);
                return;
            }

            String body = api.readRequestBody(exchange);
            System.out.println("[ApproveConsultant] raw body: " + body);
            Map<String, String> data = api.parseJsonBody(body);
            System.out.println("[ApproveConsultant] parsed data: " + data);

            // Support both consultantId and email
            String consultantId = data.get("consultantId");
            String email = data.get("email");

            // Database-first: lookup and update directly in DB
            if (api.userDAO != null) {
                Consultant c = null;
                if (consultantId != null) {
                    System.out.println("[ApproveConsultant] Looking up by consultantId: " + consultantId);
                    User u = api.userDAO.findById(consultantId);
                    if (u instanceof Consultant) c = (Consultant) u;
                } else if (email != null) {
                    System.out.println("[ApproveConsultant] Looking up by email: " + email);
                    User u = api.userDAO.findByEmail(email);
                    if (u instanceof Consultant) c = (Consultant) u;
                }
                if (c != null) {
                    boolean dbUpdated = api.userDAO.updateApprovalStatus(c.getUserID().toString(), true);
                    System.out.println("[ApproveConsultant] DB update result: " + dbUpdated);
                    if (dbUpdated) {
                        api.sendResponse(exchange, "{\"success\":true,\"message\":\"Consultant approved successfully\"}", 200);
                    } else {
                        api.sendResponse(exchange, "{\"success\":false,\"error\":\"Failed to update approval status in database\"}", 500);
                    }
                    return;
                } else {
                    System.out.println("[ApproveConsultant] Consultant NOT found in DB");
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
            // Authenticate — Admin only
            User caller = api.authenticate(exchange, true);
            if (caller == null) return;
            if (!"Admin".equalsIgnoreCase(caller.getAccountType().toString())) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden: requires Admin role\"}", 403);
                return;
            }

            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            // Support both consultantId and email
            String consultantId = data.get("consultantId");
            String email = data.get("email");

            // Database-first: lookup and update directly in DB
            if (api.userDAO != null) {
                Consultant c = null;
                if (consultantId != null) {
                    User u = api.userDAO.findById(consultantId);
                    if (u instanceof Consultant) c = (Consultant) u;
                } else if (email != null) {
                    User u = api.userDAO.findByEmail(email);
                    if (u instanceof Consultant) c = (Consultant) u;
                }
                if (c != null) {
                    boolean dbUpdated = api.userDAO.updateApprovalStatus(c.getUserID().toString(), false);
                    if (dbUpdated) {
                        System.out.println("Consultant rejected in DB: " + c.getEmail());
                        api.sendResponse(exchange, "{\"success\":true,\"message\":\"Consultant rejected successfully\"}", 200);
                    } else {
                        api.sendResponse(exchange, "{\"success\":false,\"error\":\"Failed to update rejection in database\"}", 500);
                    }
                    return;
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
            // No auth required — services are public (any logged-in user can view)
            User caller = api.authenticate(exchange, false); // don't require auth header

            List<String> serviceList = new ArrayList<>();

            // Database-first: always query DB for latest data
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
                    System.out.println("[GetServices] Fetched " + serviceList.size() + " from DB");
                } catch (Exception e) {
                    System.err.println("Error fetching services from DB: " + e.getMessage());
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database error\"}", 500);
                    return;
                }
            } else {
                // Memory fallback only if DB is unavailable
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
            // Authenticate — Admin only
            User caller = api.authenticate(exchange, true);
            if (caller == null) return;
            if (!"Admin".equalsIgnoreCase(caller.getAccountType().toString())) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden: requires Admin role\"}", 403);
                return;
            }

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
            // Authenticate — Client only; rely on clientId in body as primary auth
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            String clientId = data.get("clientId");
            String consultantId = data.get("consultantId");
            String serviceId = data.get("serviceId");
            String startTime = data.get("startTime");

            if (clientId == null || clientId.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"clientId required\"}", 400);
                return;
            }

            // Auth: verify caller matches clientId from body, or is Admin
            User caller = api.authenticate(exchange, false);
            boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
            boolean isSelf = caller != null && caller.getUserID().toString().equals(clientId);
            if (!isAdmin && !isSelf) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden: cannot create booking for another user\"}", 403);
                return;
            }

            try {
                // Find client
                Client client = null;
                Consultant consultant = null;
                backend.core.ConsultingService service = null;

                if (api.userDAO != null) {
                    User dbClient = api.userDAO.findById(clientId);
                    System.out.println("CreateBooking: Looking up clientId=" + clientId + ", found=" + (dbClient != null ? dbClient.getClass().getSimpleName() : "null"));
                    if (dbClient instanceof Client) {
                        client = (Client) dbClient;
                        // FIX: Use database userId so FK constraint matches
                        clientId = client.getUserID().toString();
                    }
                    User dbConsultant = api.userDAO.findById(consultantId);
                    if (dbConsultant instanceof Consultant) {
                        consultant = (Consultant) dbConsultant;
                        // FIX: Use database userId so FK constraint matches
                        consultantId = consultant.getUserID().toString();
                    }
                    if (serviceId != null) {
                        try {
                            service = api.serviceDAO.findById(UUID.fromString(serviceId));
                        } catch (Exception e) {
                            System.err.println("CreateBooking: serviceId lookup failed: " + e.getMessage());
                        }
                    }
                }

                // Database required — no fallback to memory
                if (client == null || consultant == null || service == null) {
                    String missing = client == null ? "client" : (consultant == null ? "consultant" : "service");
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + missing + " not found in database\"}", 400);
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

                // Server-side availability check: verify consultant has an available slot covering this time
                if (api.availabilityDAO != null) {
                    boolean available = api.availabilityDAO.isAvailable(
                        consultantId, start, start.plusMinutes(service.getDurationMinutes()));
                    if (!available) {
                        api.sendResponse(exchange,
                            "{\"success\":false,\"error\":\"Consultant is not available at the requested time. Please check their availability schedule.\"}",
                            400);
                        return;
                    }
                    System.out.println("CreateBooking: availability verified for consultant " + consultantId + " at " + start);
                }

                Booking booking = new Booking(client, consultant, service, start);
                booking.setStatus(BookingStatus.Requested);

                // Save to database (required)
                if (api.bookingDAO != null) {
                    api.bookingDAO.insert(booking);
                } else {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database not available\"}", 500);
                    return;
                }

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

            if (clientId == null || clientId.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"clientId required\"}", 400);
                return;
            }

            // Authenticate caller — don't fail hard if header is missing (clientId in query is primary auth)
            User caller = api.authenticate(exchange, false);
            boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
            boolean isSelf = caller != null && caller.getUserID().toString().equals(clientId);

            // Client can only view their own bookings; Admin can view all
            if (!isAdmin && !isSelf) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            List<String> bookingList = new ArrayList<>();

            // Database-first: always query DB for latest data
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
                    System.out.println("[GetClientBookings] Fetched " + bookingList.size() + " from DB for clientId=" + clientId);
                } catch (Exception e) {
                    System.err.println("Error fetching client bookings from DB: " + e.getMessage());
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database error\"}", 500);
                    return;
                }
            } else {
                // Memory fallback only if DB is unavailable
                for (Booking b : api.bookings.values()) {
                    if (b.getClient().getUserID().toString().equals(clientId)) {
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

            if (consultantId == null || consultantId.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"consultantId required\"}", 400);
                return;
            }

            // Authenticate caller — don't fail hard if header is missing (consultantId in query is primary auth)
            User caller = api.authenticate(exchange, false);
            boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
            boolean isSelf = caller != null && caller.getUserID().toString().equals(consultantId);

            // Consultant can only view their own bookings; Admin can view all
            if (!isAdmin && !isSelf) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            List<String> bookingList = new ArrayList<>();

            // Database-first: always query DB for latest data
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
                    System.out.println("[GetConsultantBookings] Fetched " + bookingList.size() + " from DB for consultantId=" + consultantId);
                } catch (Exception e) {
                    System.err.println("Error fetching consultant bookings from DB: " + e.getMessage());
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database error\"}", 500);
                    return;
                }
            } else {
                // Memory fallback only if DB is unavailable
                for (Booking b : api.bookings.values()) {
                    if (b.getConsultant().getUserID().toString().equals(consultantId)) {
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
            // Prefer consultantId from request body for ownership verification
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);
            String bookingId = data.get("bookingId");
            String consultantId = data.get("consultantId"); // from request body

            if (bookingId == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"bookingId required\"}", 400);
                return;
            }

            // Database required
            if (api.bookingDAO == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database not available\"}", 500);
                return;
            }
            try {
                Booking b = api.bookingDAO.findById(UUID.fromString(bookingId.trim()));
                if (b == null) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Booking not found in database\"}", 404);
                    return;
                }

                // Auth: verify the caller owns this booking
                // Try Authorization header first (exact userId lookup), fall back to request-body consultantId
                User caller = api.authenticate(exchange, false); // don't require auth header
                boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
                boolean isOwner = false;

                if (isAdmin) {
                    isOwner = true;
                } else if (consultantId != null && !consultantId.isBlank()) {
                    // Primary auth: match consultantId from request body
                    isOwner = b.getConsultant().getUserID().toString().equals(consultantId);
                } else if (caller != null) {
                    // Fallback: match Authorization header userId
                    isOwner = b.getConsultant().getUserID().toString().equals(caller.getUserID().toString());
                }

                if (!isOwner) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden: you do not own this booking\"}", 403);
                    return;
                }

                b.confirm();
                api.bookingDAO.update(b);
                api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking confirmed successfully\"}", 200);
            } catch (IllegalStateException e) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 400);
            } catch (Exception e) {
                System.err.println("DB confirm error: " + e.getMessage());
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 500);
            }
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
            String clientId = data.get("clientId"); // from request body

            if (bookingId == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"bookingId required\"}", 400);
                return;
            }

            // Database required
            if (api.bookingDAO == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database not available\"}", 500);
                return;
            }
            try {
                Booking b = api.bookingDAO.findById(UUID.fromString(bookingId.trim()));
                if (b == null) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Booking not found\"}", 404);
                    return;
                }

                // Auth: verify ownership via request body clientId or Authorization header
                User caller = api.authenticate(exchange, false);
                boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
                boolean isOwner = false;

                if (isAdmin) {
                    isOwner = true;
                } else if (clientId != null && !clientId.isBlank()) {
                    // Primary auth: match clientId from request body
                    isOwner = b.getClient().getUserID().toString().equals(clientId);
                } else if (caller != null) {
                    // Fallback: match Authorization header userId
                    isOwner = b.getClient().getUserID().toString().equals(caller.getUserID().toString());
                }

                if (!isOwner) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden: you do not own this booking\"}", 403);
                    return;
                }

                b.cancel();
                api.bookingDAO.update(b);
                api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking cancelled successfully\"}", 200);
            } catch (IllegalStateException e) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 400);
            } catch (Exception e) {
                System.err.println("DB cancel error: " + e.getMessage());
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    static class CompleteBookingHandler implements HttpHandler {
        private final ApiServer api;
        public CompleteBookingHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Prefer consultantId from request body for ownership verification
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);
            String bookingId = data.get("bookingId");
            String consultantId = data.get("consultantId"); // from request body

            if (bookingId == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"bookingId required\"}", 400);
                return;
            }

            // Database required
            if (api.bookingDAO == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database not available\"}", 500);
                return;
            }
            try {
                System.out.println("[CompleteBooking] raw bookingId='" + bookingId + "' (len=" + (bookingId != null ? bookingId.length() : 0) + ")");
                System.out.println("[CompleteBooking] raw consultantId='" + consultantId + "'");
                System.out.println("[CompleteBooking] raw body='" + body + "'");
                Booking b = api.bookingDAO.findById(UUID.fromString(bookingId.trim()));
                System.out.println("[CompleteBooking] loaded booking=" + (b != null ? b.getBookingId() : "null"));
                if (b == null) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Booking not found in database\"}", 404);
                    return;
                }

                // Auth: verify the caller owns this booking
                User caller = api.authenticate(exchange, false); // don't require auth header
                boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
                boolean isOwner = false;

                if (isAdmin) {
                    isOwner = true;
                } else if (consultantId != null && !consultantId.isBlank()) {
                    // Primary auth: match consultantId from request body
                    isOwner = b.getConsultant().getUserID().toString().equals(consultantId);
                    System.out.println("[CompleteBooking] isOwner via consultantId=" + isOwner + " (consultantId=" + consultantId + " vs b.consultant=" + b.getConsultant().getUserID() + ")");
                } else if (caller != null) {
                    // Fallback: match Authorization header userId
                    isOwner = b.getConsultant().getUserID().toString().equals(caller.getUserID().toString());
                    System.out.println("[CompleteBooking] isOwner via caller=" + isOwner + " (caller=" + caller.getUserID() + " vs b.consultant=" + b.getConsultant().getUserID() + ")");
                }

                if (!isOwner) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden: you do not own this booking\"}", 403);
                    return;
                }

                b.complete();
                api.bookingDAO.update(b);
                api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking completed successfully\"}", 200);
            } catch (IllegalStateException e) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 400);
            } catch (Exception e) {
                System.err.println("DB complete error: " + e.getMessage());
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    /**
     * Handler for consultant rejecting a booking.
     * Maps to POST /api/bookings/reject
     */
    static class RejectBookingHandler implements HttpHandler {
        private final ApiServer api;
        public RejectBookingHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);
            String bookingId = data.get("bookingId");
            String consultantId = data.get("consultantId"); // from request body

            if (bookingId == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"bookingId required\"}", 400);
                return;
            }

            if (api.bookingDAO == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database not available\"}", 500);
                return;
            }
            try {
                System.out.println("[RejectBooking] raw bookingId='" + bookingId + "' (len=" + (bookingId != null ? bookingId.length() : 0) + ")");
                System.out.println("[RejectBooking] raw body='" + body + "'");
                Booking b = api.bookingDAO.findById(UUID.fromString(bookingId.trim()));
                System.out.println("[RejectBooking] loaded booking=" + (b != null ? b.getBookingId() : "null"));
                if (b == null) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Booking not found in database\"}", 404);
                    return;
                }

                // Auth: verify ownership via request body consultantId or Authorization header
                User caller = api.authenticate(exchange, false);
                boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
                boolean isOwner = false;

                if (isAdmin) {
                    isOwner = true;
                } else if (consultantId != null && !consultantId.isBlank()) {
                    isOwner = b.getConsultant().getUserID().toString().equals(consultantId);
                } else if (caller != null) {
                    isOwner = b.getConsultant().getUserID().toString().equals(caller.getUserID().toString());
                }

                if (!isOwner) {
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden: you do not own this booking\"}", 403);
                    return;
                }

                b.reject();
                api.bookingDAO.update(b);
                api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking rejected successfully\"}", 200);
            } catch (IllegalStateException e) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 400);
            } catch (Exception e) {
                System.err.println("DB reject error: " + e.getMessage());
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    static class MakePaymentHandler implements HttpHandler {
        private final ApiServer api;
        public MakePaymentHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Auth: rely on clientId in body as primary auth
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);
            String bookingId = data.get("bookingId");
            String clientId = data.get("clientId");
            String methodId = data.get("methodId");   // selected from saved payment methods
            String paymentType = data.get("paymentType"); // used when adding a new method inline

            if (bookingId == null || bookingId.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"bookingId required\"}", 400);
                return;
            }
            if (clientId == null || clientId.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"clientId required\"}", 400);
                return;
            }

            // Auth: verify caller matches clientId from body, or is Admin
            User caller = api.authenticate(exchange, false);
            boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
            boolean isSelf = caller != null && caller.getUserID().toString().equals(clientId);
            if (!isAdmin && !isSelf) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            // ① Load booking from DB to get amount and verify ownership
            Booking targetBooking = null;
            double amount = 0;
            if (api.bookingDAO != null) {
                try {
                    targetBooking = api.bookingDAO.findById(UUID.fromString(bookingId.trim()));
                } catch (Exception e) {
                    System.err.println("[MakePayment] booking lookup error: " + e.getMessage());
                }
            }
            if (targetBooking == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Booking not found in database\"}", 404);
                return;
            }
            if (!targetBooking.getClient().getUserID().toString().equals(clientId) && !isAdmin) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden: booking does not belong to this client\"}", 403);
                return;
            }

            // Get amount from booking's service (not from request body — prevents manipulation)
            try {
                amount = targetBooking.getService().getBasePrice();
            } catch (Exception e) {
                System.err.println("[MakePayment] could not get service price: " + e.getMessage());
                amount = Double.parseDouble(data.getOrDefault("amount", "0"));
            }

            // ② Resolve payment method: if methodId provided, validate it belongs to this client
            String resolvedMethodId = null;
            String resolvedPaymentType = null;
            String maskedDetails = "****";

            if (methodId != null && !methodId.isBlank()) {
                // Validate: methodId must belong to this client
                if (api.paymentMethodDAO != null) {
                    List<Map<String, String>> savedMethods = api.paymentMethodDAO.findByClientId(clientId);
                    for (Map<String, String> m : savedMethods) {
                        if (methodId.equals(m.get("methodId"))) {
                            resolvedMethodId = methodId;
                            resolvedPaymentType = m.get("paymentType");
                            maskedDetails = m.get("details");
                            break;
                        }
                    }
                    if (resolvedMethodId == null) {
                        api.sendResponse(exchange,
                            "{\"success\":false,\"error\":\"Payment method not found or does not belong to this client\"}",
                            400);
                        return;
                    }
                }
            } else if (paymentType != null && !paymentType.isBlank()) {
                // No saved method — add a new one inline for this payment
                String details = data.getOrDefault("details", "****");
                if (api.paymentMethodDAO != null) {
                    resolvedMethodId = api.paymentMethodDAO.insert(clientId, paymentType, details);
                    resolvedPaymentType = paymentType;
                    maskedDetails = details;
                    if (resolvedMethodId == null || resolvedMethodId.isEmpty()) {
                        api.sendResponse(exchange,
                            "{\"success\":false,\"error\":\"Failed to save payment method\"}",
                            500);
                        return;
                    }
                    System.out.println("[MakePayment] Added new payment method id=" + resolvedMethodId);
                }
            } else {
                // Neither methodId nor paymentType provided — return saved methods so client can pick one
                if (api.paymentMethodDAO != null) {
                    List<Map<String, String>> savedMethods = api.paymentMethodDAO.findByClientId(clientId);
                    List<String> methodList = new ArrayList<>();
                    for (Map<String, String> m : savedMethods) {
                        methodList.add("{\"methodId\":\"" + m.get("methodId") +
                            "\",\"paymentType\":\"" + m.get("paymentType") +
                            "\",\"details\":\"" + safeJson(m.get("details")) + "\"}");
                    }
                    if (methodList.isEmpty()) {
                        api.sendResponse(exchange,
                            "{\"success\":false,\"error\":\"No saved payment methods — please add one first\",\"needsPaymentMethod\":true}",
                            400);
                    } else {
                        api.sendResponse(exchange,
                            "{\"success\":false,\"error\":\"Please select a saved payment method\",\"methods\":[" + String.join(",", methodList) + "],\"bookingAmount\":" + amount + "}",
                            400);
                    }
                } else {
                    api.sendResponse(exchange,
                        "{\"success\":false,\"error\":\"No payment method provided\"}",
                        400);
                }
                return;
            }

            // ③ Process payment
            try {
                PaymentMethod pm = PaymentMethod.valueOf(resolvedPaymentType != null ? resolvedPaymentType : "CreditCard");
                PaymentTransaction transaction = new PaymentTransaction(amount, pm, maskedDetails);
                transaction.succeed();

                // Update booking status to Paid
                targetBooking.markPaid();
                api.bookingDAO.update(targetBooking);
                System.out.println("[MakePayment] Booking " + bookingId + " marked as Paid");

                // Save payment record with methodId
                if (api.paymentDAO != null) {
                    try {
                        boolean saved = api.paymentDAO.insert(transaction,
                            UUID.fromString(bookingId.trim()),
                            resolvedMethodId);
                        System.out.println("[MakePayment] Payment record saved: " + saved);
                    } catch (Exception e) {
                        System.err.println("[MakePayment] payment DAO insert error: " + e.getMessage());
                    }
                }

                api.sendResponse(exchange,
                    "{\"success\":true,\"paymentId\":\"" + transaction.getTransactionId() +
                    "\",\"amount\":" + amount +
                    ",\"paymentMethod\":\"" + resolvedPaymentType +
                    "\",\"status\":\"SUCCESS\",\"message\":\"Payment processed successfully\"}",
                    200);

            } catch (Exception e) {
                System.err.println("Payment error: " + e.getMessage());
                api.sendResponse(exchange,
                    "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}",
                    500);
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

            if (clientId == null || clientId.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"clientId required\"}", 400);
                return;
            }

            // Auth: body clientId is primary, auth header is fallback
            User caller = api.authenticate(exchange, false);
            boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
            boolean isSelf = caller != null && caller.getUserID().toString().equals(clientId);
            if (!isAdmin && !isSelf) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            List<String> paymentList = new ArrayList<>();

            // Database-first: always query DB for latest data
            if (api.paymentDAO != null && clientId != null) {
                try {
                    List<PaymentTransaction> dbPayments = api.paymentDAO.findByUserId(clientId);
                    for (PaymentTransaction t : dbPayments) {
                        paymentList.add("{\"paymentId\":\"" + t.getTransactionId() +
                            "\",\"amount\":" + t.getAmount() +
                            ",\"paymentMethod\":\"" + t.getPaymentMethod() +
                            ",\"status\":\"" + t.getStatus() + "\"}");
                    }
                } catch (Exception e) {
                    System.err.println("Error fetching payment history from DB: " + e.getMessage());
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database error\"}", 500);
                    return;
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

            if (clientId == null || clientId.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"clientId required\"}", 400);
                return;
            }
            if (paymentType == null || paymentType.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"paymentType required\"}", 400);
                return;
            }

            // Auth: verify caller matches clientId from body, or is Admin
            User caller = api.authenticate(exchange, false);
            boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
            boolean isSelf = caller != null && caller.getUserID().toString().equals(clientId);
            if (!isAdmin && !isSelf) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            // Resolve clientId from DB to get the real user_id
            String resolvedClientId = clientId;
            if (api.userDAO != null) {
                try {
                    backend.user.User existingClient = api.userDAO.findById(clientId);
                    if (existingClient != null) {
                        resolvedClientId = existingClient.getUserID().toString();
                    } else {
                        api.sendResponse(exchange, "{\"success\":false,\"error\":\"Client not found in database\"}", 404);
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Error resolving client: " + e.getMessage());
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database error\"}", 500);
                    return;
                }
            }

            // Insert payment method into database
            if (api.paymentMethodDAO != null) {
                try {
                    String methodId = api.paymentMethodDAO.insert(
                        resolvedClientId,
                        paymentType,
                        maskedDetails != null ? maskedDetails : "");
                    if (methodId != null && !methodId.isEmpty()) {
                        System.out.println("[AddPaymentMethod] Saved methodId=" + methodId + " for clientId=" + resolvedClientId);
                        api.sendResponse(exchange,
                            "{\"success\":true,\"methodId\":\"" + methodId +
                            "\",\"paymentType\":\"" + paymentType +
                            "\",\"details\":\"" + (maskedDetails != null ? maskedDetails : "****") +
                            "\",\"message\":\"Payment method added successfully\"}",
                            200);
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Error saving payment method: " + e.getMessage());
                }
            }

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

            if (clientId == null || clientId.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"clientId required\"}", 400);
                return;
            }

            // Auth: body consultantId is primary, auth header is fallback
            User caller = api.authenticate(exchange, false);
            boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
            boolean isSelf = caller != null && caller.getUserID().toString().equals(clientId);
            if (!isAdmin && !isSelf) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            List<String> methodList = new ArrayList<>();

            if (api.paymentMethodDAO != null) {
                // NOTE: DAO returns Map with keys: methodId, paymentType, details
                List<Map<String, String>> methods = api.paymentMethodDAO.findByClientId(clientId);
                System.out.println("[ListPaymentMethods] Found " + methods.size() + " methods for clientId=" + clientId);
                for (Map<String, String> m : methods) {
                    String methodId = safeJson(m.get("methodId"));
                    String paymentType = safeJson(m.get("paymentType"));
                    String details = safeJson(m.get("details"));
                    methodList.add("{\"methodId\":" + methodId +
                        ",\"paymentType\":" + paymentType +
                        ",\"details\":" + details + "}");
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
            // Read body before auth (body read does not consume anything on HttpExchange)
            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            String methodId = data.get("methodId");
            String clientId = data.get("clientId");

            if (methodId == null || methodId.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"methodId required\"}", 400);
                return;
            }
            if (clientId == null || clientId.isBlank()) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"clientId required\"}", 400);
                return;
            }

            // Auth: body clientId is primary, auth header is fallback
            User caller = api.authenticate(exchange, false);
            boolean isAdmin = caller != null && "Admin".equalsIgnoreCase(caller.getAccountType().toString());
            boolean isSelf = caller != null && caller.getUserID().toString().equals(clientId);
            if (!isAdmin && !isSelf) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            if (api.paymentMethodDAO != null) {
                boolean deleted = api.paymentMethodDAO.delete(methodId, clientId);
                if (deleted) {
                    System.out.println("[RemovePaymentMethod] Removed methodId=" + methodId + " for clientId=" + clientId);
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
            // Authenticate
            User caller = api.authenticate(exchange, true);
            if (caller == null) return;

            String body = api.readRequestBody(exchange);
            Map<String, String> data = api.parseJsonBody(body);

            String consultantId = data.get("consultantId");
            String startTime = data.get("startTime");
            String endTime = data.get("endTime");

            if (consultantId == null || startTime == null || endTime == null) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Missing required fields\"}", 400);
                return;
            }

            // Consultant can only set their own availability; Admin can set for any consultant
            boolean isAdmin = "Admin".equalsIgnoreCase(caller.getAccountType().toString());
            if (!isAdmin && !caller.getUserID().toString().equals(consultantId)) {
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Forbidden\"}", 403);
                return;
            }

            // Database-first: resolve consultantId from DB only
            String resolvedConsultantId = consultantId;
            if (api.userDAO != null) {
                try {
                    User c = api.userDAO.findById(consultantId);
                    if (c != null) {
                        resolvedConsultantId = c.getUserID().toString();
                    } else {
                        api.sendResponse(exchange,
                            "{\"success\":false,\"error\":\"Consultant not found in database\"}", 404);
                        return;
                    }
                } catch (Exception e) {
                    System.err.println("Error resolving consultant: " + e.getMessage());
                    api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database error\"}", 500);
                    return;
                }
            }

            try {
                java.time.LocalDateTime start = java.time.LocalDateTime.parse(startTime.replace(" ", "T"));
                java.time.LocalDateTime end = java.time.LocalDateTime.parse(endTime.replace(" ", "T"));

                System.out.println("Setting availability for consultant: " + resolvedConsultantId + " from " + start + " to " + end);

                // Database required
                if (api.availabilityDAO != null) {
                    boolean success = api.availabilityDAO.addAvailability(resolvedConsultantId, start, end);
                    if (success) {
                        api.sendResponse(exchange, "{\"success\":true,\"message\":\"Availability set successfully\"}", 200);
                        return;
                    } else {
                        api.sendResponse(exchange,
                            "{\"success\":false,\"error\":\"Failed to set availability. Consultant may not exist in database.\"}", 500);
                        return;
                    }
                }

                api.sendResponse(exchange, "{\"success\":false,\"error\":\"Database not available\"}", 500);
            } catch (Exception e) {
                System.err.println("Error setting availability: " + e.getMessage());
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    static class GetAvailabilityHandler implements HttpHandler {
        private final ApiServer api;
        public GetAvailabilityHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            // Authenticate — any logged-in user can view availability (for booking)
            User caller = api.authenticate(exchange, true);
            if (caller == null) return;

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
            // Authenticate — any logged-in user
            User caller = api.authenticate(exchange, true);
            if (caller == null) return;

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
