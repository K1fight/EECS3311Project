package backend.database;

/**
 * Database Module Usage Example
 * Demonstrates how to use the database module
 */
public class DatabaseExample {
    
    public static void main(String[] args) {
        // Get database connection instance
        DatabaseConnection db = DatabaseConnection.getInstance();
        
        try {
            // Connect to database (uses PostgreSQL by default)
            db.connect();
            
            // Initialize database schema
            DatabaseInitializer initializer = new DatabaseInitializer();
            initializer.initialize();
            
            // Example: Create and insert a user
            UserDAO userDAO = new UserDAO();
            backend.user.Client client = new backend.user.Client(
                "John Doe", 
                "john@example.com", 
                "password123"
            );
            
            if (userDAO.insert(client)) {
                System.out.println("✓ User inserted successfully");
            }
            
            // Example: Find user by email
            backend.user.User foundUser = userDAO.findByEmail("john@example.com");
            if (foundUser != null) {
                System.out.println("✓ Found user: " + foundUser.getName());
            }
            
            // Example: Get all clients
            var clients = userDAO.getAllClients();
            System.out.println("✓ Total clients: " + clients.size());
            
            // Example: Booking operations
            BookingDAO bookingDAO = new BookingDAO();
            // ... booking operations
            
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        } finally {
            // Close connection when done
            db.disconnect();
        }
    }
}
