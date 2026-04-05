package backend.database;

import backend.payment.*;
import backend.booking.Booking;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for Payments
 * Provides database operations for payment management
 */
public class PaymentDAO extends BaseDAO {
    
    /**
     * Insert a new payment record into database
     * @param transaction PaymentTransaction to insert
     * @param bookingId Associated booking ID
     * @param methodId  Associated payment method ID (from payment_methods table), may be null
     * @return true if successful
     */
    public boolean insert(PaymentTransaction transaction, UUID bookingId, String methodId) {
        String sql = """
            INSERT INTO payments (payment_id, booking_id, method_id, amount, payment_method, payment_detail_masked, status, failure_reason)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        
        try {
            String status = transaction.getStatus() == PaymentStatus.SUCCESS ? "SUCCESS" : 
                           transaction.getStatus() == PaymentStatus.FAILED ? "FAILED" : "PENDING";
            
            int rows = executeUpdate(sql,
                transaction.getTransactionId().toString(),
                bookingId.toString(),
                methodId,
                transaction.getAmount(),
                transaction.getPaymentMethod().toString(),
                transaction.getMaskedDetails(),
                status,
                transaction.getStatus() == PaymentStatus.FAILED ? "Payment declined" : null
            );
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting payment: " + e.getMessage());
            return false;
        }
    }

    /**
     * Insert a new payment record (backward-compatible, no method_id)
     */
    public boolean insert(PaymentTransaction transaction, UUID bookingId) {
        return insert(transaction, bookingId, null);
    }
    
    /**
     * Find payment by ID
     * @param paymentId Payment ID
     * @return PaymentTransaction object or null
     */
    public PaymentTransaction findById(UUID paymentId) {
        String sql = "SELECT * FROM payments WHERE payment_id = ?";
        
        try (ResultSet rs = executeQuery(sql, paymentId.toString())) {
            if (rs.next()) {
                return mapResultSetToTransaction(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding payment by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get all payments for a booking
     * @param bookingId Booking ID
     * @return List of PaymentTransaction objects
     */
    public List<PaymentTransaction> findByBookingId(UUID bookingId) {
        String sql = "SELECT * FROM payments WHERE booking_id = ? ORDER BY transaction_timestamp DESC";
        List<PaymentTransaction> transactions = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql, bookingId.toString())) {
            while (rs.next()) {
                PaymentTransaction transaction = mapResultSetToTransaction(rs);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting booking payments: " + e.getMessage());
        }
        
        return transactions;
    }
    
    /**
     * Get all payments for a user (via bookings)
     * @param userId User ID (client)
     * @return List of PaymentTransaction objects
     */
    public List<PaymentTransaction> findByUserId(String userId) {
        String sql = """
            SELECT p.* FROM payments p
            JOIN bookings b ON p.booking_id = b.booking_id
            WHERE b.client_id = ?
            ORDER BY p.transaction_timestamp DESC
            """;
        
        List<PaymentTransaction> transactions = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql, userId)) {
            while (rs.next()) {
                PaymentTransaction transaction = mapResultSetToTransaction(rs);
                if (transaction != null) {
                    transactions.add(transaction);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting user payments: " + e.getMessage());
        }
        
        return transactions;
    }
    
    /**
     * Get payment history with summary
     * @param userId User ID
     * @return PaymentHistory object
     */
    public PaymentHistory getPaymentHistory(String userId) {
        List<PaymentTransaction> transactions = findByUserId(userId);
        PaymentHistory history = new PaymentHistory();
        
        for (PaymentTransaction transaction : transactions) {
            history.addTransaction(transaction);
        }
        
        return history;
    }
    
    /**
     * Get total amount paid by user
     * @param userId User ID
     * @return Total amount
     */
    public double getTotalPaidByUser(String userId) {
        String sql = """
            SELECT COALESCE(SUM(p.amount), 0) as total
            FROM payments p
            JOIN bookings b ON p.booking_id = b.booking_id
            WHERE b.client_id = ? AND p.status = 'SUCCESS'
            """;
        
        try (ResultSet rs = executeQuery(sql, userId)) {
            if (rs.next()) {
                return rs.getDouble("total");
            }
        } catch (SQLException e) {
            System.err.println("Error getting total paid: " + e.getMessage());
        }
        
        return 0.0;
    }
    
    /**
     * Map ResultSet to PaymentTransaction object
     * @param rs ResultSet
     * @return PaymentTransaction object
     * @throws SQLException if mapping fails
     */
    private PaymentTransaction mapResultSetToTransaction(ResultSet rs) throws SQLException {
        UUID transactionId = UUID.fromString(rs.getString("payment_id"));
        double amount = rs.getDouble("amount");
        PaymentMethod method = PaymentMethod.valueOf(rs.getString("payment_method"));
        String maskedDetails = rs.getString("payment_detail_masked");
        String statusStr = rs.getString("status");
        
        PaymentTransaction transaction = new PaymentTransaction(amount, method, maskedDetails);
        
        // Set status based on database value
        if ("SUCCESS".equals(statusStr)) {
            transaction.succeed();
        } else if ("FAILED".equals(statusStr)) {
            transaction.fail(rs.getString("failure_reason"));
        }
        
        return transaction;
    }
}
