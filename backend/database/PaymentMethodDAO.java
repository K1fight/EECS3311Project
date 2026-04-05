package backend.database;

import backend.payment.PaymentMethod;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Data Access Object for Payment Methods
 * Provides database operations for payment method management
 */
public class PaymentMethodDAO extends BaseDAO {
    
    /**
     * Get all available payment methods
     * @return List of PaymentMethod enums
     */
    public List<PaymentMethod> getAllPaymentMethods() {
        List<PaymentMethod> methods = new ArrayList<>();
        
        // Return all available payment methods from enum
        for (PaymentMethod method : PaymentMethod.values()) {
            methods.add(method);
        }
        
        return methods;
    }
    
    /**
     * Check if a payment method is available
     * @param method PaymentMethod to check
     * @return true if available
     */
    public boolean isMethodAvailable(PaymentMethod method) {
        // All payment methods are available by default
        return method != null;
    }
    
    /**
     * Get payment method by name
     * @param methodName Name of the payment method
     * @return PaymentMethod or null if not found
     */
    public PaymentMethod getPaymentMethodByName(String methodName) {
        try {
            return PaymentMethod.valueOf(methodName.toUpperCase());
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid payment method: " + methodName);
            return null;
        }
    }
    
    /**
     * Insert a new payment method for a client
     * @param clientId Client ID
     * @param paymentType Payment method type
     * @param maskedDetails Masked payment details
     * @return Method ID or empty string if failed
     */
    public String insert(String clientId, String paymentType, String maskedDetails) {
        String methodId = UUID.randomUUID().toString();
        // Escape double quotes in details so JSON stays valid
        String safeDetails = (maskedDetails != null)
            ? maskedDetails.replace("\\", "\\\\").replace("\"", "\\\"")
            : "";
        String sql = """
            INSERT INTO payment_methods (method_id, client_id, payment_type, details, created_at)
            VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
            """;

        try {
            int rows = executeUpdate(sql, methodId, clientId, paymentType, safeDetails);
            if (rows > 0) {
                return methodId;
            }
        } catch (SQLException e) {
            System.err.println("Error inserting payment method: " + e.getMessage());
        }

        return "";
    }
    
    /**
     * Find payment methods by client ID
     * @param clientId Client ID
     * @return List of payment method maps
     */
    public List<Map<String, String>> findByClientId(String clientId) {
        String sql = "SELECT method_id, payment_type, details FROM payment_methods WHERE client_id = ?";
        List<Map<String, String>> methods = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql, clientId)) {
            while (rs.next()) {
                Map<String, String> method = new HashMap<>();
                method.put("methodId", rs.getString("method_id"));
                method.put("paymentType", rs.getString("payment_type"));
                method.put("details", rs.getString("details"));
                methods.add(method);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payment methods: " + e.getMessage());
        }
        
        return methods;
    }
    
    /**
     * Delete a payment method
     * @param methodId Method ID
     * @param clientId Client ID (for verification, may be null after schema migration)
     * @return true if successful
     */
    public boolean delete(String methodId, String clientId) {
        String sql;
        int rows;
        try {
            if (clientId != null && !clientId.isEmpty()) {
                sql = "DELETE FROM payment_methods WHERE method_id = ? AND client_id = ?";
                rows = executeUpdate(sql, methodId, clientId);
            } else {
                // FIX: Support deletion when client_id is null (after schema migration)
                sql = "DELETE FROM payment_methods WHERE method_id = ?";
                rows = executeUpdate(sql, methodId);
            }
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting payment method: " + e.getMessage());
            return false;
        }
    }
}

