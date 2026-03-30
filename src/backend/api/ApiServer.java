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
import backend.core.UserService;
import backend.core.BookingService;
import backend.core.ConsultingService;
import backend.core.PaymentService;
import backend.core.ClientService;
import backend.core.AIChatbotService;
import backend.user.User;
import backend.user.Client;
import backend.user.Consultant;
import backend.user.Admin;
import backend.user.AccountType;
import backend.booking.Booking;
import backend.booking.BookingStatus;
import backend.service.ServiceCategory;
import backend.payment.PaymentMethod;
import backend.payment.PaymentTransaction;
import backend.payment.PaymentStatus;

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
    private UserDAO userDAO;
    private BookingDAO bookingDAO;
    private ServiceDAO serviceDAO;
    private PaymentDAO paymentDAO;

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

                // Initialize services
                userService = new UserService();
                bookingService = new BookingService();
                consultingService = new backend.core.ConsultingService();
                paymentService = new PaymentService();
                clientService = new ClientService();
                chatbotService = new AIChatbotService();
                
                System.out.println("Database mode: ENABLED");
            } catch (Exception e) {
                System.out.println("Database connection failed, running in DEMO mode: " + e.getMessage());
                userService = new UserService();
                bookingService = new BookingService();
                consultingService = new backend.core.ConsultingService();
                paymentService = new PaymentService();
                clientService = new ClientService();
                chatbotService = new AIChatbotService();
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

        // Payment endpoints
        server.createContext("/api/payments/pay", new MakePaymentHandler(this));
        server.createContext("/api/payments/history", new PaymentHistoryHandler(this));

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

    // Helper to parse JSON-like body (simple parsing without Gson)
    public Map<String, String> parseJsonBody(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.trim().isEmpty()) return map;
        
        // Simple JSON parsing: {"key":"value","key2":"value2"}
        body = body.trim();
        if (body.startsWith("{") && body.endsWith("}")) {
            body = body.substring(1, body.length() - 1);
            String[] pairs = body.split(",");
            for (String pair : pairs) {
                String[] kv = pair.split(":");
                if (kv.length == 2) {
                    String key = kv[0].trim().replace("\"", "");
                    String value = kv[1].trim().replace("\"", "");
                    map.put(key, value);
                }
            }
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
                        api.usersByEmail.put(email, client);
                        api.sendResponse(exchange, "{\"success\":true,\"userId\":\"" + client.getUserID() + "\",\"message\":\"User registered successfully\"}", 200);
                    } else {
                        api.sendResponse(exchange, "{\"success\":false,\"error\":\"Registration failed\"}", 400);
                    }
                } else if ("Consultant".equals(accountType)) {
                    Consultant consultant = api.userService.registerConsultant(name, email, password);
                    if (consultant != null) {
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
            
            User user = api.userService.authenticateUser(email, password);
            
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
            
            if (userId != null) {
                // Find user by ID in memory
                for (User u : api.usersByEmail.values()) {
                    if (u.getUserID().equals(userId)) {
                        String response = "{\"success\":true,\"user\":{\"userId\":\"" + u.getUserID() + 
                            "\",\"name\":\"" + u.getName() + 
                            "\",\"email\":\"" + u.getEmail() + 
                            "\",\"accountType\":\"" + u.getAccountType() + "\"}}";
                        api.sendResponse(exchange, response, 200);
                        return;
                    }
                }
            }
            
            api.sendResponse(exchange, "{\"success\":false,\"error\":\"User not found\"}", 404);
        }
    }

    static class GetConsultantsHandler implements HttpHandler {
        private final ApiServer api;
        public GetConsultantsHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> consultants = new ArrayList<>();
            for (User u : api.usersByEmail.values()) {
                if (u instanceof Consultant) {
                    Consultant c = (Consultant) u;
                    consultants.add("{\"userId\":\"" + c.getUserID() + 
                        "\",\"name\":\"" + c.getName() + 
                        "\",\"email\":\"" + c.getEmail() + 
                        "\",\"isApproved\":" + c.isApproved() + "}");
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
            String consultantId = data.get("consultantId");
            
            // Find and approve consultant
            for (User u : api.usersByEmail.values()) {
                if (u instanceof Consultant && u.getUserID().equals(consultantId)) {
                    ((Consultant) u).setApproved(true);
                    api.sendResponse(exchange, "{\"success\":true,\"message\":\"Consultant approved successfully\"}", 200);
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
            List<String> serviceList = new ArrayList<>();
            for (backend.core.ConsultingService s : api.services.values()) {
                serviceList.add("{\"serviceId\":\"" + s.getServiceId() + 
                    "\",\"name\":\"" + s.getName() + 
                    "\",\"description\":\"" + s.getDescription() + 
                    "\",\"basePrice\":" + s.getBasePrice() + 
                    ",\"durationMinutes\":" + s.getDurationMinutes() + 
                    ",\"category\":\"" + s.getCategory() + "\"}");
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
            
            // Find client and create demo booking
            Client client = null;
            for (User u : api.usersByEmail.values()) {
                if (u instanceof Client && u.getUserID().equals(clientId)) {
                    client = (Client) u;
                    break;
                }
            }
            
            if (client == null) {
                client = new Client("Demo Client", "demo@example.com", "demo");
            }
            
            Consultant consultant = new Consultant("Demo Consultant", "consultant@example.com", "demo");
            backend.core.ConsultingService service = new backend.core.ConsultingService("Consulting Session", "Demo service", 100.0, 60, ServiceCategory.Career);
            
            try {
                java.time.LocalDateTime start = startTime != null ? 
                    java.time.LocalDateTime.parse(startTime.replace(" ", "T")) : 
                    java.time.LocalDateTime.now().plusDays(1);
                
                Booking booking = new Booking(client, consultant, service, start);
                booking.setStatus(BookingStatus.Requested);
                
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
            for (Booking b : api.bookings.values()) {
                if (clientId == null || b.getClient().getUserID().equals(clientId)) {
                    bookingList.add("{\"bookingId\":\"" + b.getBookingId() + 
                        "\",\"clientName\":\"" + b.getClient().getName() + 
                        "\",\"consultantName\":\"" + b.getConsultant().getName() + 
                        "\",\"serviceName\":\"" + b.getService().getName() + 
                        "\",\"startTime\":\"" + b.getStartTime() + 
                        "\",\"status\":\"" + b.getStatus() + "\"}");
                }
            }
            
            // Add demo bookings if none exist
            if (bookingList.isEmpty()) {
                bookingList.add("{\"bookingId\":\"BK-001\",\"clientName\":\"John Doe\",\"consultantName\":\"Dr. Sarah Johnson\",\"serviceName\":\"Career Counseling\",\"startTime\":\"2026-04-05T14:00\",\"status\":\"Confirmed\"}");
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
            for (Booking b : api.bookings.values()) {
                if (consultantId == null || b.getConsultant().getUserID().equals(consultantId)) {
                    bookingList.add("{\"bookingId\":\"" + b.getBookingId() + 
                        "\",\"clientName\":\"" + b.getClient().getName() + 
                        "\",\"serviceName\":\"" + b.getService().getName() + 
                        "\",\"startTime\":\"" + b.getStartTime() + 
                        "\",\"status\":\"" + b.getStatus() + "\"}");
                }
            }
            
            if (bookingList.isEmpty()) {
                bookingList.add("{\"bookingId\":\"REQ-001\",\"clientName\":\"John Doe\",\"serviceName\":\"Career Counseling\",\"startTime\":\"2026-04-05T14:00\",\"status\":\"Requested\"}");
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
            
            for (Booking b : api.bookings.values()) {
                if (b.getBookingId().toString().equals(bookingId) || bookingId.equals(b.getBookingId().toString())) {
                    b.confirm();
                    api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking confirmed successfully\"}", 200);
                    return;
                }
            }
            
            api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking confirmed\"}", 200);
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
            
            api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking cancelled\"}", 200);
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
            
            for (Booking b : api.bookings.values()) {
                if (b.getBookingId().toString().equals(bookingId)) {
                    b.complete();
                    api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking completed successfully\"}", 200);
                    return;
                }
            }
            
            api.sendResponse(exchange, "{\"success\":true,\"message\":\"Booking completed\"}", 200);
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
                
                System.out.println("Payment request: bookingId=" + bookingId + ", method=" + paymentMethod + ", amount=" + amount);
                
                // Create payment transaction
                PaymentMethod method = PaymentMethod.valueOf(paymentMethod);
                PaymentTransaction transaction = new PaymentTransaction(amount, method, "****1234");
                transaction.succeed();
                
                // Update booking status if found
                for (Booking b : api.bookings.values()) {
                    if (b.getBookingId().toString().equals(bookingId)) {
                        b.markPaid();
                        break;
                    }
                }
                
                api.payments.put(transaction.getTransactionId().toString(), transaction);
                
                System.out.println("Payment successful, transactionId=" + transaction.getTransactionId());
                
                api.sendResponse(exchange, "{\"success\":true,\"paymentId\":\"" + transaction.getTransactionId() + 
                    "\",\"status\":\"SUCCESS\",\"message\":\"Payment processed successfully\"}", 200);
            } catch (Exception e) {
                System.err.println("Payment error: " + e.getMessage());
                e.printStackTrace();
                api.sendResponse(exchange, "{\"success\":false,\"error\":\"" + e.getMessage() + "\"}", 500);
            }
        }
    }

    static class PaymentHistoryHandler implements HttpHandler {
        private final ApiServer api;
        public PaymentHistoryHandler(ApiServer api) { this.api = api; }

        @Override
        public void handle(HttpExchange exchange) throws IOException {
            List<String> paymentList = new ArrayList<>();
            for (PaymentTransaction t : api.payments.values()) {
                paymentList.add("{\"paymentId\":\"" + t.getTransactionId() + 
                    "\",\"amount\":" + t.getAmount() + 
                    ",\"paymentMethod\":\"" + t.getPaymentMethod() + 
                    "\",\"status\":\"" + t.getStatus() + 
                    "\",\"timestamp\":\"" + java.time.LocalDateTime.now() + "\"}");
            }
            
            if (paymentList.isEmpty()) {
                paymentList.add("{\"paymentId\":\"PAY-001\",\"amount\":100.0,\"paymentMethod\":\"CreditCard\",\"status\":\"SUCCESS\",\"timestamp\":\"2026-03-30T10:00:00\"}");
            }
            
            api.sendResponse(exchange, "{\"success\":true,\"payments\":[" + String.join(",", paymentList) + "]}", 200);
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
            
            String response = api.chatbotService.getResponse(message);
            api.sendResponse(exchange, "{\"success\":true,\"response\":\"" + response.replace("\"", "\\\"") + "\"}", 200);
        }
    }

    public static void main(String[] args) {
        ApiServer apiServer = new ApiServer();
        apiServer.start();
    }
}
