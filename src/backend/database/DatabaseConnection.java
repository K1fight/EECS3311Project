package backend.database;

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
    
    // Database configuration
    private static final String DEFAULT_URL = "jdbc:sqlite:consulting_booking.db";
    private static final String SQLITE_DRIVER = "org.sqlite.JDBC";
    
    private DatabaseConnection() {
        this.url = DEFAULT_URL;
        this.username = "";
        this.password = "";
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
            // Load SQLite JDBC driver
            Class.forName(SQLITE_DRIVER);
            
            // Establish connection
            Properties props = new Properties();
            props.setProperty("PRAGMA foreign_keys", "true"); // Enable foreign key support
            connection = DriverManager.getConnection(url, props);
            
            System.out.println("Connected to database successfully.");
        } catch (ClassNotFoundException e) {
            throw new SQLException("SQLite JDBC driver not found.", e);
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
            // Load appropriate JDBC driver based on URL
            if (url.contains("sqlite")) {
                Class.forName(SQLITE_DRIVER);
                Properties props = new Properties();
                props.setProperty("PRAGMA foreign_keys", "true");
                connection = DriverManager.getConnection(url, props);
            } else if (url.contains("mysql")) {
                Class.forName("com.mysql.cj.jdbc.Driver");
                connection = DriverManager.getConnection(url, username, password);
            } else if (url.contains("postgresql")) {
                Class.forName("org.postgresql.Driver");
                connection = DriverManager.getConnection(url, username, password);
            }
            
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
