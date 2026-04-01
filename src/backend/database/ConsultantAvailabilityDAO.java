package backend.database;

import backend.core.TimeSlot;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for Consultant Availability
 * Provides database operations for availability management
 */
public class ConsultantAvailabilityDAO extends BaseDAO {
    
    /**
     * Add a new availability slot for a consultant
     * @param consultantId Consultant ID
     * @param startTime Start time of availability
     * @param endTime End time of availability
     * @return true if successful
     */
    public boolean addAvailability(String consultantId, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
            INSERT INTO consultant_availability (availability_id, consultant_id, start_time, end_time, is_available)
            VALUES (?, ?, ?, ?, ?)
            """;
        
        try {
            String availabilityId = UUID.randomUUID().toString();
            System.out.println("Attempting to insert availability for consultant: " + consultantId);
            int rows = executeUpdate(sql,
                availabilityId,
                consultantId,
                startTime,
                endTime,
                true
            );
            if (rows > 0) {
                System.out.println("Successfully inserted availability into database");
                return true;
            } else {
                System.err.println("No rows inserted - check if consultant exists in database");
                return false;
            }
        } catch (SQLException e) {
            System.err.println("SQL Error adding availability: " + e.getMessage());
            System.err.println("SQL State: " + e.getSQLState());
            System.err.println("Error Code: " + e.getErrorCode());
            // Check if it's a foreign key violation
            if (e.getSQLState() != null && e.getSQLState().equals("23503")) {
                System.err.println("Foreign key violation: Consultant ID " + consultantId + " does not exist in users table!");
            }
            return false;
        }
    }
    
    /**
     * Get all available time slots for a consultant
     * @param consultantId Consultant ID
     * @return List of TimeSlot objects
     */
    public List<TimeSlot> getAvailableSlots(String consultantId) {
        return getAvailableSlots(consultantId, null, null);
    }
    
    /**
     * Get available time slots for a consultant within a date range
     * @param consultantId Consultant ID
     * @param startDate Start date (optional)
     * @param endDate End date (optional)
     * @return List of TimeSlot objects
     */
    public List<TimeSlot> getAvailableSlots(String consultantId, LocalDateTime startDate, LocalDateTime endDate) {
        StringBuilder sql = new StringBuilder(
            "SELECT * FROM consultant_availability WHERE consultant_id = ? AND is_available = 1"
        );
        
        List<Object> params = new ArrayList<>();
        params.add(consultantId);
        
        if (startDate != null) {
            sql.append(" AND end_time >= ?");
            params.add(startDate);
        }
        
        if (endDate != null) {
            sql.append(" AND start_time <= ?");
            params.add(endDate);
        }
        
        sql.append(" ORDER BY start_time");
        
        List<TimeSlot> slots = new ArrayList<>();
        
        try {
            Object[] paramsArray = params.toArray();
            try (ResultSet rs = executeQuery(sql.toString(), paramsArray)) {
                while (rs.next()) {
                    LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                    LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                    slots.add(new TimeSlot(startTime, endTime));
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting available slots: " + e.getMessage());
        }
        
        return slots;
    }
    
    /**
     * Check if a consultant is available at a specific time
     * @param consultantId Consultant ID
     * @param startTime Start time to check
     * @param endTime End time to check
     * @return true if available
     */
    public boolean isAvailable(String consultantId, LocalDateTime startTime, LocalDateTime endTime) {
        String sql = """
            SELECT COUNT(*) as count FROM consultant_availability
            WHERE consultant_id = ? AND is_available = 1
            AND start_time <= ? AND end_time >= ?
            """;
        
        try {
            try (ResultSet rs = executeQuery(sql, consultantId, startTime, endTime)) {
                if (rs.next()) {
                    return rs.getInt("count") > 0;
                }
            }
        } catch (SQLException e) {
            System.err.println("Error checking availability: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Mark a time slot as unavailable (booked)
     * @param consultantId Consultant ID
     * @param startTime Start time of the slot
     * @return true if successful
     */
    public boolean markUnavailable(String consultantId, LocalDateTime startTime) {
        String sql = """
            UPDATE consultant_availability 
            SET is_available = 0 
            WHERE consultant_id = ? AND start_time = ?
            """;
        
        try {
            int rows = executeUpdate(sql, consultantId, startTime);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error marking slot unavailable: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Remove an availability slot
     * @param availabilityId Availability slot ID
     * @return true if successful
     */
    public boolean removeAvailability(String availabilityId) {
        String sql = "DELETE FROM consultant_availability WHERE availability_id = ?";
        
        try {
            int rows = executeUpdate(sql, availabilityId);
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error removing availability: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Get all availability slots for a consultant (including unavailable)
     * @param consultantId Consultant ID
     * @return List of TimeSlot objects with availability status
     */
    public List<TimeSlot> getAllSlots(String consultantId) {
        String sql = "SELECT * FROM consultant_availability WHERE consultant_id = ? ORDER BY start_time";
        List<TimeSlot> slots = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql, consultantId)) {
            while (rs.next()) {
                LocalDateTime startTime = rs.getTimestamp("start_time").toLocalDateTime();
                LocalDateTime endTime = rs.getTimestamp("end_time").toLocalDateTime();
                boolean isAvailable = rs.getBoolean("is_available");
                
                TimeSlot slot = new TimeSlot(startTime, endTime);
                slot.setAvailable(isAvailable);
                slots.add(slot);
            }
        } catch (SQLException e) {
            System.err.println("Error getting all slots: " + e.getMessage());
        }
        
        return slots;
    }
}
