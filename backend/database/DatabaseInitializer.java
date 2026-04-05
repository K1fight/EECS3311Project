package backend.database;

import java.sql.SQLException;
import java.sql.Statement;
import java.util.UUID;

/**
 * Database Schema Initializer
 * Creates all necessary tables for the consulting booking system
 */
public class DatabaseInitializer extends BaseDAO {

    /**
     * Initialize database schema
     * Creates all tables if they don't exist
     *
     * Set environment variable RESET_DB=true to drop all tables and
     * re-initialize from scratch (useful during development rebuilds).
     */
    public void initialize() {
        try {
            // Ensure connection is established
            if (!dbConnection.isConnected()) {
                dbConnection.connect();
            }

            // Optional: reset database on every startup (development only)
            String resetDb = System.getenv("RESET_DB");
            boolean shouldReset = "true".equalsIgnoreCase(resetDb);

            if (shouldReset) {
                System.out.println("[RESET_DB=true] Dropping all tables...");
                dropAllTables();
            }

            System.out.println("Initializing database schema...");

            // Create tables in order (respecting foreign key constraints)
            createUsersTable();
            createServicesTable();
            createBookingsTable();
            createPaymentsTable();
            createPaymentMethodsTable();
            createConsultantAvailabilityTable();

            // Schema migrations (handle existing tables that need ALTER)
            migrateBookingsTable();
            migratePaymentMethodsTable();
            migratePaymentsTable();

            System.out.println("Database schema initialized successfully.");

            // Seed initial data
            seedInitialData();

        } catch (SQLException e) {
            System.err.println("Error initializing database: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Seed initial data (users, services)
     */
    private void seedInitialData() {
        try {
            seedUsers();
            seedServices();
            System.out.println("Initial data seeded successfully.");
        } catch (SQLException e) {
            System.err.println("Error seeding initial data: " + e.getMessage());
        }
    }

    private void seedUsers() throws SQLException {
        // Check if admin exists
        String checkAdmin = "SELECT COUNT(*) FROM users WHERE email = 'admin@system.com'";
        var conn = getConnection();
        var stmt = conn.createStatement();
        var rs = stmt.executeQuery(checkAdmin);
        rs.next();
        if (rs.getInt(1) > 0) {
            System.out.println("  - Users already seeded, skipping...");
            return;
        }

        // Seed Admin
        String adminId = UUID.randomUUID().toString();
        String insertAdmin = String.format(
            "INSERT INTO users (user_id, name, email, password, account_type, is_approved) VALUES ('%s', 'System Admin', 'admin@system.com', 'admin123', 'Admin', true)",
            adminId);
        stmt.execute(insertAdmin);
        System.out.println("  ✓ Seeded Admin user");

        // Seed Client
        String clientId = UUID.randomUUID().toString();
        String insertClient = String.format(
            "INSERT INTO users (user_id, name, email, password, account_type, is_approved) VALUES ('%s', 'John Client', 'client@example.com', 'password123', 'Client', true)",
            clientId);
        stmt.execute(insertClient);
        System.out.println("  ✓ Seeded Client user");

        // Seed Consultant (approved)
        String consultantId = UUID.randomUUID().toString();
        String insertConsultant = String.format(
            "INSERT INTO users (user_id, name, email, password, account_type, is_approved) VALUES ('%s', 'Dr. Sarah Smith', 'sarah.consultant@example.com', 'password123', 'Consultant', true)",
            consultantId);
        stmt.execute(insertConsultant);
        System.out.println("  ✓ Seeded Consultant user (approved)");

        // Seed another Consultant
        String consultant2Id = UUID.randomUUID().toString();
        String insertConsultant2 = String.format(
            "INSERT INTO users (user_id, name, email, password, account_type, is_approved) VALUES ('%s', 'Mike IT Expert', 'mike.tech@example.com', 'password123', 'Consultant', true)",
            consultant2Id);
        stmt.execute(insertConsultant2);
        System.out.println("  ✓ Seeded second Consultant user (approved)");
    }

    private void seedServices() throws SQLException {
        // Check if services exist
        String checkServices = "SELECT COUNT(*) FROM consulting_services";
        var conn = getConnection();
        var stmt = conn.createStatement();
        var rs = stmt.executeQuery(checkServices);
        rs.next();
        if (rs.getInt(1) > 0) {
            return;
        }

        // Seed Career Counseling
        String service1Id = UUID.randomUUID().toString();
        String insertService1 = String.format(
            "INSERT INTO consulting_services (service_id, name, description, base_price, duration_minutes, category) VALUES ('%s', 'Career Counseling', 'Professional career guidance and advice', 100.00, 60, 'Career')",
            service1Id);
        stmt.execute(insertService1);

        // Seed IT Consulting
        String service2Id = UUID.randomUUID().toString();
        String insertService2 = String.format(
            "INSERT INTO consulting_services (service_id, name, description, base_price, duration_minutes, category) VALUES ('%s', 'IT Consulting', 'Technology and software development advice', 150.00, 90, 'Technology')",
            service2Id);
        stmt.execute(insertService2);

        // Seed Financial Advisory
        String service3Id = UUID.randomUUID().toString();
        String insertService3 = String.format(
            "INSERT INTO consulting_services (service_id, name, description, base_price, duration_minutes, category) VALUES ('%s', 'Financial Advisory', 'Financial planning and investment advice', 200.00, 60, 'Finance')",
            service3Id);
        stmt.execute(insertService3);

        System.out.println("  ✓ Seeded 3 consulting services");
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
                is_approved BOOLEAN DEFAULT FALSE,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement stmt = getConnection().createStatement()) {
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
                base_price DECIMAL(10,2) NOT NULL,
                duration_minutes INTEGER NOT NULL,
                category TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
            )
            """;
        
        try (Statement stmt = getConnection().createStatement()) {
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
                start_time TIMESTAMP NOT NULL,
                status TEXT NOT NULL CHECK(status IN ('Requested', 'Confirmed', 'PendingPayment', 'Paid', 'Rejected', 'Cancelled', 'Completed')),
                current_state TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (client_id) REFERENCES users(user_id) ON DELETE CASCADE,
                FOREIGN KEY (consultant_id) REFERENCES users(user_id) ON DELETE CASCADE,
                FOREIGN KEY (service_id) REFERENCES consulting_services(service_id)
            )
            """;

        try (Statement stmt = getConnection().createStatement()) {
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
                amount DECIMAL(10,2) NOT NULL,
                payment_method TEXT NOT NULL,
                payment_detail_masked TEXT,
                status TEXT NOT NULL CHECK(status IN ('PENDING', 'SUCCESS', 'FAILED')),
                failure_reason TEXT,
                transaction_timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (booking_id) REFERENCES bookings(booking_id) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = getConnection().createStatement()) {
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
                client_id TEXT,
                payment_type TEXT NOT NULL,
                details TEXT NOT NULL,
                created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                FOREIGN KEY (client_id) REFERENCES users(user_id) ON DELETE SET NULL
            )
            """;
        
        try (Statement stmt = getConnection().createStatement()) {
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
                start_time TIMESTAMP NOT NULL,
                end_time TIMESTAMP NOT NULL,
                is_available BOOLEAN DEFAULT TRUE,
                FOREIGN KEY (consultant_id) REFERENCES users(user_id) ON DELETE CASCADE
            )
            """;
        
        try (Statement stmt = getConnection().createStatement()) {
            stmt.execute(sql);
            System.out.println("✓ Consultant Availability table created/verified.");
        }
    }
    
    /**
     * Migrate bookings table: update status CHECK constraint to include PendingPayment
     * Safe to run multiple times — drops and recreates the constraint
     */
    private void migrateBookingsTable() {
        try {
            var conn = getConnection();
            var stmt = conn.createStatement();

            // Drop existing constraint if exists
            String dropConstraint = """
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1 FROM information_schema.table_constraints
                        WHERE constraint_name = 'bookings_status_check'
                        AND table_name = 'bookings'
                    ) THEN
                        ALTER TABLE bookings DROP CONSTRAINT bookings_status_check;
                    END IF;
                END $$
                """;
            stmt.execute(dropConstraint);

            // Add updated constraint with PendingPayment
            String addConstraint = """
                ALTER TABLE bookings
                ADD CONSTRAINT bookings_status_check
                CHECK (status IN ('Requested', 'Confirmed', 'PendingPayment', 'Paid', 'Rejected', 'Cancelled', 'Completed'))
                """;
            stmt.execute(addConstraint);

            System.out.println("✓ Bookings table migrated (PendingPayment status added).");
        } catch (SQLException e) {
            System.err.println("Migration warning (non-fatal): " + e.getMessage());
        }
    }

    /**
     * Migrate payments table: add method_id column linking to payment_methods
     * Safe to run multiple times — uses DO NOTHING ON CONFLICT logic
     */
    private void migratePaymentsTable() {
        try {
            String sql = """
                DO $$
                BEGIN
                    ALTER TABLE payments ADD COLUMN IF NOT EXISTS method_id TEXT
                        REFERENCES payment_methods(method_id) ON DELETE SET NULL;
                EXCEPTION WHEN OTHERS THEN
                    RAISE NOTICE 'payments.method_id column may already exist or migration failed: %', SQLERRM;
                END
                $$;
                """;
            var stmt = getConnection().createStatement();
            stmt.execute(sql);
            System.out.println("✓ Payments table migrated (method_id column added).");
        } catch (SQLException e) {
            System.err.println("Error migrating payments table: " + e.getMessage());
        }
    }

    /**
     * Migrate payment_methods table: drop FK constraint and allow NULL client_id
     * Safe to run multiple times — uses DO NOTHING ON CONFLICT logic
     */
    private void migratePaymentMethodsTable() {
        try {
            var conn = getConnection();
            var stmt = conn.createStatement();

            // 1. Remove the old NOT NULL constraint and FK on client_id
            //    PostgreSQL: drop FK constraint, then drop NOT NULL via ALTER COLUMN
            String dropFk = """
                DO $$
                BEGIN
                    IF EXISTS (
                        SELECT 1 FROM information_schema.table_constraints
                        WHERE constraint_name = 'payment_methods_client_id_fkey'
                        AND table_name = 'payment_methods'
                    ) THEN
                        ALTER TABLE payment_methods DROP CONSTRAINT payment_methods_client_id_fkey;
                    END IF;
                END $$
                """;
            stmt.execute(dropFk);

            // 2. Make client_id nullable (PostgreSQL)
            String makeNullable = "ALTER TABLE payment_methods ALTER COLUMN client_id DROP NOT NULL";
            try {
                stmt.execute(makeNullable);
            } catch (SQLException alreadyNullable) {
                // Ignore if already nullable
            }

            System.out.println("✓ payment_methods table migrated (client_id now nullable, FK removed).");
        } catch (SQLException e) {
            System.err.println("Migration warning (non-fatal): " + e.getMessage());
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
                try (Statement stmt = getConnection().createStatement()) {
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
