package backend.database;

import java.io.FileInputStream;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Database Connection Manager
 * Handles database connections using singleton pattern
 */
public class DatabaseConnection {
    private static DatabaseConnection instance;
    private Connection connection;
    private String url;
    private String username;
    private String password;
    
    // Database configuration loaded from properties file
    private Properties dbProperties;
    
    private DatabaseConnection() {
        this.dbProperties = new Properties();
        loadDatabaseProperties();

        // Environment variables override properties file (for Docker)
        String dbHost = System.getenv("DB_HOST");
        String dbPort = System.getenv("DB_PORT");
        String dbName = System.getenv("DB_NAME");
        String dbUser = System.getenv("DB_USER");
        String dbPass = System.getenv("DB_PASSWORD");

        if (dbHost != null) {
            String port = (dbPort != null) ? dbPort : "5432";
            String name = (dbName != null) ? dbName : dbProperties.getProperty("db.url", "").replaceAll(".*/(\\w+)$", "$1");
            dbProperties.setProperty("db.url", "jdbc:postgresql://" + dbHost + ":" + port + "/" + name);
        }
        if (dbUser != null) dbProperties.setProperty("db.username", dbUser);
        if (dbPass != null) dbProperties.setProperty("db.password", dbPass);

        this.url = dbProperties.getProperty("db.url");
        this.username = dbProperties.getProperty("db.username");
        this.password = dbProperties.getProperty("db.password");
    }
    
    /**
     * Load database properties from database.properties file
     */
    private void loadDatabaseProperties() {
        try (FileInputStream input = new FileInputStream("database.properties")) {
            dbProperties.load(input);
            System.out.println("Database properties loaded successfully.");
            System.out.println("Using database: " + dbProperties.getProperty("db.type"));
        } catch (IOException e) {
            System.err.println("Error loading database.properties: " + e.getMessage());
            // Set default values if properties file not found
            dbProperties.setProperty("db.type", "postgresql");
            dbProperties.setProperty("db.url", "jdbc:postgresql://localhost:5432/consulting_booking");
            dbProperties.setProperty("db.username", "postgres");
            dbProperties.setProperty("db.password", "postgres");
            System.out.println("Using default database configuration.");
        }
    }
    
    /**
     * Get singleton instance of DatabaseConnection
     * @return DatabaseConnection instance
     */
    public static synchronized DatabaseConnection getInstance() {
        if (instance == null) {
            instance = new DatabaseConnection();
        }
        return instance;
    }
    
    /**
     * Connect to database with default configuration
     * @throws SQLException if connection fails
     */
    public void connect() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return; // Already connected
        }
        
        try {
            // Load PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");
            
            // Establish connection
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            connection = DriverManager.getConnection(url, props);
            
            System.out.println("Connected to database successfully.");
        } catch (ClassNotFoundException e) {
            throw new SQLException("PostgreSQL JDBC driver not found.", e);
        }
    }
    
    /**
     * Connect to database with custom configuration
     * @param url Database URL
     * @param username Database username
     * @param password Database password
     * @throws SQLException if connection fails
     */
    public void connect(String url, String username, String password) throws SQLException {
        if (connection != null && !connection.isClosed()) {
            return; // Already connected
        }
        
        this.url = url;
        this.username = username;
        this.password = password;
        
        try {
            // Load PostgreSQL JDBC driver
            Class.forName("org.postgresql.Driver");
            
            Properties props = new Properties();
            props.setProperty("user", username);
            props.setProperty("password", password);
            connection = DriverManager.getConnection(url, props);
            
            System.out.println("Connected to database: " + url);
        } catch (ClassNotFoundException e) {
            throw new SQLException("JDBC driver not found for: " + url, e);
        }
    }
    
    /**
     * Get current database connection
     * @return Connection object
     * @throws SQLException if not connected
     */
    public Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            throw new SQLException("Not connected to database. Call connect() first.");
        }
        return connection;
    }
    
    /**
     * Check if database connection is active
     * @return true if connected, false otherwise
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Close database connection
     */
    public void disconnect() {
        if (connection != null) {
            try {
                connection.close();
                connection = null;
                System.out.println("Database connection closed.");
            } catch (SQLException e) {
                System.err.println("Error closing database connection: " + e.getMessage());
            }
        }
    }
    
    /**
     * Set database URL for future connections
     * @param url Database URL
     */
    public void setUrl(String url) {
        this.url = url;
    }
    
    /**
     * Get current database URL
     * @return Database URL
     */
    public String getUrl() {
        return url;
    }
}
