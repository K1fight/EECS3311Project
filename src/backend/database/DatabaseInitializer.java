package backend.database;

import java.sql.SQLException;
import java.sql.Statement;

/**
 * Database Schema Initializer
 * Creates all necessary tables for the consulting booking system
 */
public class DatabaseInitializer extends BaseDAO {
    
    /**
     * Initialize database schema
     * Creates all tables if they don't exist
     */
    public void initialize() {
        try {
            // Ensure connection is established
            if (!dbConnection.isConnected()) {
                dbConnection.connect();
            }
            
            System.out.println("Initializing database schema...");
            
            // Create tables in order (respecting foreign key constraints)
            createUsersTable();
            createServicesTable();
            createBookingsTable();
            createPaymentsTable();
            createPaymentMethodsTable();
            createConsultantAvailabilityTable();
            
            System.out.println("Database schema initialized successfully.");
            
        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Create users table
     */
    private void createUsersTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS users (
                user_id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                email TEXT UNIQUE NOT NULL,
                password TEXT NOT NULL,
                account_type TEXT NOT NULL CHECK(account_type IN ('Admin', 'Client', 'Consultant')),
                is_approved INTEGER DEFAULT 0,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement stmt = createStatement(sql)) {
            stmt.execute(sql);
            System.out.println("✓ Users table created/verified.");
        }
    }
    
    /**
     * Create consulting services table
     */
    private void createServicesTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS consulting_services (
                service_id TEXT PRIMARY KEY,
                name TEXT NOT NULL,
                description TEXT,
                base_price REAL NOT NULL,
                duration_minutes INTEGER NOT NULL,
                category TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement stmt = createStatement(sql)) {
            stmt.execute(sql);
            System.out.println("✓ Consulting Services table created/verified.");
        }
    }
    
    /**
     * Create bookings table
     */
    private void createBookingsTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS bookings (
                booking_id TEXT PRIMARY KEY,
                client_id TEXT NOT NULL,
                consultant_id TEXT NOT NULL,
                service_id TEXT NOT NULL,
                start_time DATETIME NOT NULL,
                status TEXT NOT NULL CHECK(status IN ('Requested', 'Confirmed', 'Paid', 'Rejected', 'Cancelled', 'Completed')),
                current_state TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                updated_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (client_id) REFERENCES users(user_id) ON DELETE CASCADE,
                FOREIGN KEY (consultant_id) REFERENCES users(user_id) ON DELETE CASCADE,
                FOREIGN KEY (service_id) REFERENCES consulting_services(service_id)
            )
            """;
        
        try (Statement stmt = createStatement(sql)) {
            stmt.execute(sql);
            System.out.println("✓ Bookings table created/verified.");
        }
    }
    
    /**
     * Create payments table
     */
    private void createPaymentsTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS payments (
                payment_id TEXT PRIMARY KEY,
                booking_id TEXT NOT NULL,
                amount REAL NOT NULL,
                payment_method TEXT NOT NULL,
                payment_detail_masked TEXT,
                status TEXT NOT NULL CHECK(status IN ('PENDING', 'SUCCESS', 'FAILED')),
                failure_reason TEXT,
                transaction_timestamp DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = createStatement(sql)) {
            stmt.execute(sql);
            System.out.println("✓ Payments table created/verified.");
        }
    }
    
    /**
     * Create payment methods table
     */
    private void createPaymentMethodsTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS payment_methods (
                method_id TEXT PRIMARY KEY,
                client_id TEXT NOT NULL,
                payment_type TEXT NOT NULL,
                details TEXT NOT NULL,
                created_at DATETIME DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (client_id) REFERENCES users(user_id) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = createStatement(sql)) {
            stmt.execute(sql);
            System.out.println("✓ Payment Methods table created/verified.");
        }
    }
    
    /**
     * Create consultant availability table
     */
    private void createConsultantAvailabilityTable() throws SQLException {
        String sql = """
            CREATE TABLE IF NOT EXISTS consultant_availability (
                availability_id TEXT PRIMARY KEY,
                consultant_id TEXT NOT NULL,
                start_time DATETIME NOT NULL,
                end_time DATETIME NOT NULL,
                is_available INTEGER DEFAULT 1,
                FOREIGN KEY (consultant_id) REFERENCES users(user_id) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = createStatement(sql)) {
            stmt.execute(sql);
            System.out.println("✓ Consultant Availability table created/verified.");
        }
    }
    
    /**
     * Drop all tables (for testing/debugging)
     */
    public void dropAllTables() {
        String[] tables = {
            "consultant_availability",
            "payment_methods",
            "payments",
            "bookings",
            "consulting_services",
            "users"
        };
        
        try {
            for (String table : tables) {
                String sql = "DROP TABLE IF EXISTS " + table;
                try (Statement stmt = createStatement(sql)) {
                    stmt.execute(sql);
                    System.out.println("Dropped table: " + table);
                }
            }
            System.out.println("All tables dropped successfully.");
        } catch (SQLException e) {
            System.err.println("Error dropping tables: " + e.getMessage());
        }
    }
}
