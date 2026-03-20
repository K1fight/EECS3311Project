package backend.database;

import backend.user.AccountType;
import backend.user.Client;
import backend.user.Consultant;
import backend.user.User;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for Users
 * Provides database operations for user management
 */
public class UserDAO extends BaseDAO {
    
    /**
     * Insert a new user into database
     * @param user User to insert
     * @return true if successful
     */
    public boolean insert(User user) {
        String sql = """
            INSERT INTO users (user_id, name, email, password, account_type, is_approved)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try {
            int rows = executeUpdate(sql,
                user.getUserID(),
                user.getName(),
                user.getEmail(),
                user.getPassword(),
                user.getAccountType().toString(),
                (user instanceof Consultant consultant) ? (consultant.isApproved() ? 1 : 0) : 1
            );
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting user: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find user by ID
     * @param userId User ID
     * @return User object or null
     */
    public User findById(String userId) {
        String sql = "SELECT * FROM users WHERE user_id = ?";
        
        try (ResultSet rs = executeQuery(sql, userId)) {
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Find user by email
     * @param email User email
     * @return User object or null
     */
    public User findByEmail(String email) {
        String sql = "SELECT * FROM users WHERE email = ?";
        
        try (ResultSet rs = executeQuery(sql, email)) {
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding user by email: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get all clients
     * @return List of Client objects
     */
    public List<Client> getAllClients() {
        String sql = "SELECT * FROM users WHERE account_type = 'Client'";
        List<Client> clients = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql)) {
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                if (user instanceof Client client) {
                    clients.add(client);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting all clients: " + e.getMessage());
        }
        
        return clients;
    }
    
    /**
     * Get all consultants
     * @return List of Consultant objects
     */
    public List<Consultant> getAllConsultants() {
        String sql = "SELECT * FROM users WHERE account_type = 'Consultant'";
        List<Consultant> consultants = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql)) {
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                if (user instanceof Consultant consultant) {
                    consultants.add(consultant);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting all consultants: " + e.getMessage());
        }
        
        return consultants;
    }
    
    /**
     * Get pending consultants (not approved)
     * @return List of Consultant objects
     */
    public List<Consultant> getPendingConsultants() {
        String sql = "SELECT * FROM users WHERE account_type = 'Consultant' AND is_approved = 0";
        List<Consultant> consultants = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql)) {
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                if (user instanceof Consultant consultant) {
                    consultants.add(consultant);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting pending consultants: " + e.getMessage());
        }
        
        return consultants;
    }
    
    /**
     * Update user approval status
     * @param userId User ID
     * @param approved Approval status
     * @return true if successful
     */
    public boolean updateApprovalStatus(String userId, boolean approved) {
        String sql = "UPDATE users SET is_approved = ?, updated_at = CURRENT_TIMESTAMP WHERE user_id = ?";
        
        try {
            int rows = executeUpdate(sql, approved ? 1 : 0, userId);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating approval status: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete user by ID
     * @param userId User ID
     * @return true if successful
     */
    public boolean delete(String userId) {
        String sql = "DELETE FROM users WHERE user_id = ?";
        
        try {
            int rows = executeUpdate(sql, userId);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting user: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Map ResultSet to User object
     * @param rs ResultSet
     * @return User object
     * @throws SQLException if mapping fails
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        String userId = rs.getString("user_id");
        String name = rs.getString("name");
        String email = rs.getString("email");
        String password = rs.getString("password");
        AccountType accountType = AccountType.valueOf(rs.getString("account_type"));
        boolean isApproved = rs.getInt("is_approved") == 1;
        
        return switch (accountType) {
            case Admin -> new backend.user.Admin(name, email, password);
            case Client -> new Client(name, email, password);
            case Consultant -> {
                Consultant consultant = new Consultant(name, email, password);
                consultant.setApproved(isApproved);
                yield consultant;
            }
        };
    }
}
