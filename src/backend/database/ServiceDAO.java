package backend.database;

import backend.core.ConsultingService;
import backend.service.ServiceCategory;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Data Access Object for Consulting Services
 * Provides database operations for service catalog management
 */
public class ServiceDAO extends BaseDAO {
    
    /**
     * Insert a new service into database
     * @param service Service to insert
     * @return true if successful
     */
    public boolean insert(ConsultingService service) {
        String sql = """
            INSERT INTO consulting_services (service_id, name, description, base_price, duration_minutes, category)
            VALUES (?, ?, ?, ?, ?, ?)
            """;
        
        try {
            int rows = executeUpdate(sql,
                service.getServiceId().toString(),
                service.getName(),
                service.getDescription(),
                service.getBasePrice(),
                service.getDurationMinutes(),
                service.getCategory().toString()
            );
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error inserting service: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Find service by ID
     * @param serviceId Service ID
     * @return ConsultingService object or null
     */
    public ConsultingService findById(UUID serviceId) {
        String sql = "SELECT * FROM consulting_services WHERE service_id = ?";
        
        try (ResultSet rs = executeQuery(sql, serviceId.toString())) {
            if (rs.next()) {
                return mapResultSetToService(rs);
            }
        } catch (SQLException e) {
            System.err.println("Error finding service by ID: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Get all available services
     * @return List of ConsultingService objects
     */
    public List<ConsultingService> getAllServices() {
        String sql = "SELECT * FROM consulting_services ORDER BY name";
        List<ConsultingService> services = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql)) {
            while (rs.next()) {
                ConsultingService service = mapResultSetToService(rs);
                if (service != null) {
                    services.add(service);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting all services: " + e.getMessage());
        }
        
        return services;
    }
    
    /**
     * Get services by category
     * @param category Service category
     * @return List of ConsultingService objects
     */
    public List<ConsultingService> findByCategory(ServiceCategory category) {
        String sql = "SELECT * FROM consulting_services WHERE category = ? ORDER BY name";
        List<ConsultingService> services = new ArrayList<>();
        
        try (ResultSet rs = executeQuery(sql, category.toString())) {
            while (rs.next()) {
                ConsultingService service = mapResultSetToService(rs);
                if (service != null) {
                    services.add(service);
                }
            }
        } catch (SQLException e) {
            System.err.println("Error getting services by category: " + e.getMessage());
        }
        
        return services;
    }
    
    /**
     * Update service information
     * @param service Service to update
     * @return true if successful
     */
    public boolean update(ConsultingService service) {
        String sql = """
            UPDATE consulting_services 
            SET name = ?, description = ?, base_price = ?, duration_minutes = ?, category = ?
            WHERE service_id = ?
            """;
        
        try {
            int rows = executeUpdate(sql,
                service.getName(),
                service.getDescription(),
                service.getBasePrice(),
                service.getDurationMinutes(),
                service.getCategory().toString(),
                service.getServiceId().toString()
            );
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error updating service: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Delete service by ID
     * @param serviceId Service ID
     * @return true if successful
     */
    public boolean delete(UUID serviceId) {
        String sql = "DELETE FROM consulting_services WHERE service_id = ?";
        
        try {
            int rows = executeUpdate(sql, serviceId.toString());
            return rows > 0;
        } catch (SQLException e) {
            System.err.println("Error deleting service: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Map ResultSet to ConsultingService object
     * @param rs ResultSet
     * @return ConsultingService object
     * @throws SQLException if mapping fails
     */
    private ConsultingService mapResultSetToService(ResultSet rs) throws SQLException {
        UUID serviceId = UUID.fromString(rs.getString("service_id"));
        String name = rs.getString("name");
        String description = rs.getString("description");
        double basePrice = rs.getDouble("base_price");
        int durationMinutes = rs.getInt("duration_minutes");
        String categoryStr = rs.getString("category");
        
        ServiceCategory category = ServiceCategory.valueOf(categoryStr);
        
        ConsultingService service = new ConsultingService();
        // Use reflection or setter methods to populate
        // For now, create a new service with the data
        service.setName(name);
        service.setDescription(description);
        service.setBasePrice(basePrice);
        service.setDurationMinutes(durationMinutes);
        service.setCategory(category);
        
        return service;
    }
}
