package backend.database;

import backend.booking.Booking;
import backend.booking.*;
import backend.user.Client;
import backend.user.Consultant;
import backend.core.ConsultingService;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for Bookings
 * Provides database operations for booking management
 */
public class BookingDAO extends BaseDAO {
    
    /**
     * Insert a new booking into database
     * @param booking Booking to insert
     * @return true if successful
     */
    public boolean insert(Booking booking) {
        String sql = """
            INSERT INTO bookings (booking_id, client_id, consultant_id, service_id, start_time, status, current_state)
            VALUES (?, ?, ?, ?, ?, ?, ?)
            """;
        
        try {
            int rows = executeUpdate(sql,
                booking.getBookingId().toString(),
                booking.getClient().getUserID(),
                booking.getConsultant().getUserID(),
                booking.getService().getServiceId().toString(),
                booking.getStartTime(),
                booking.getStatus().toString(),
                booking.getCurrentState().getClass().getSimpleName()
            );
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting booking: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find booking by ID
     * @param bookingId Booking ID
     * @return Booking object or null
     */
    public Booking findById(UUID bookingId) {
        String sql = "SELECT * FROM bookings WHERE booking_id = ?";
        
        try (ResultSet rs = executeQuery(sql, bookingId.toString())) {
            if (rs.next()) {
                return mapResultSetToBooking(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding booking by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get all bookings for a client
     * @param clientId Client ID
     * @return List of Booking objects
     */
    public List<Booking> findByClientId(String clientId) {
        String sql = "SELECT * FROM bookings WHERE client_id = ? ORDER BY start_time DESC";
        List<Booking> bookings = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql, clientId)) {
            while (rs.next()) {
                Booking booking = mapResultSetToBooking(rs);
                if (booking != null) {
                    bookings.add(booking);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting client bookings: " + e.getMessage());
        }
        
        return bookings;
    }
    
    /**
     * Get all bookings for a consultant
     * @param consultantId Consultant ID
     * @return List of Booking objects
     */
    public List<Booking> findByConsultantId(String consultantId) {
        String sql = "SELECT * FROM bookings WHERE consultant_id = ? ORDER BY start_time DESC";
        List<Booking> bookings = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql, consultantId)) {
            while (rs.next()) {
                Booking booking = mapResultSetToBooking(rs);
                if (booking != null) {
                    bookings.add(booking);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting consultant bookings: " + e.getMessage());
        }
        
        return bookings;
    }
    
    /**
     * Update booking status and state
     * @param booking Booking to update
     * @return true if successful
     */
    public boolean update(Booking booking) {
        String sql = """
            UPDATE bookings 
            SET status = ?, current_state = ?, updated_at = CURRENT_TIMESTAMP 
            WHERE booking_id = ?
            """;
        
        try {
            int rows = executeUpdate(sql,
                booking.getStatus().toString(),
                booking.getCurrentState().getClass().getSimpleName(),
                booking.getBookingId().toString()
            );
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating booking: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete booking by ID
     * @param bookingId Booking ID
     * @return true if successful
     */
    public boolean delete(UUID bookingId) {
        String sql = "DELETE FROM bookings WHERE booking_id = ?";
        
        try {
            int rows = executeUpdate(sql, bookingId.toString());
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting booking: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Map ResultSet to Booking object
     * Note: This is a simplified mapping. In production, you'd need to load related entities.
     * @param rs ResultSet
     * @return Booking object
     * @throws SQLException if mapping fails
     */
    private Booking mapResultSetToBooking(ResultSet rs) throws SQLException {
        // This is a simplified version - in real implementation,
        // you would fetch the related User and ConsultingService objects
        // from their respective DAOs
        
        UUID bookingId = UUID.fromString(rs.getString("booking_id"));
        String clientId = rs.getString("client_id");
        String consultantId = rs.getString("consultant_id");
        UUID serviceId = UUID.fromString(rs.getString("service_id"));
        LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
        BookingStatus status = BookingStatus.valueOf(rs.getString("status"));
        String stateClassName = rs.getString("current_state");
        
        // Create placeholder objects - in production, fetch actual objects
        Client client = new Client("", "", "");
        Consultant consultant = new Consultant("", "", "");
        ConsultingService service = new ConsultingService();
        
        // Use reflection or factory to create state object
        BookingState state = createBookingState(stateClassName);
        
        Booking booking = new Booking(client, consultant, service, startTime);
        // Override the auto-generated ID
        // Note: This requires making bookingId settable or using a special constructor
        
        return booking;
    }
    
    /**
     * Create BookingState object from class name
     * @param className State class name
     * @return BookingState instance
     */
    private BookingState createBookingState(String className) {
        try {
            Class<?> stateClass = Class.forName("backend.booking." + className);
            return (BookingState) stateClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            System.err.println("Error creating booking state: " + e.getMessage());
            return new RequestedState(); // Default fallback
        }
    }
}
