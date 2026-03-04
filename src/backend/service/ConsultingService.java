package backend.service;

import java.util.UUID;

public class ConsultingService {
    private UUID serviceId;
    private String name;
    private String description;
    private double basePrice;
    private int durationMinutes; // e.g., 60 minutes
    private ServiceCategory category;

    public ConsultingService(String name, String description, double basePrice, int durationMinutes, ServiceCategory category) {
        this.serviceId = UUID.randomUUID();
        this.name = name;
        this.description = description;
        this.basePrice = basePrice;
        this.durationMinutes = durationMinutes;
        this.category = category;
    }

    // constructor, getters...
    public double getBasePrice() { return basePrice; }
    public int getDurationMinutes() { return durationMinutes; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public ServiceCategory getCategory() { return category; }
    public UUID getServiceId() { return serviceId; }
    
    @Override
    public String toString() {
        return String.format("[%s] %s - $%.2f (%d min): %s", category, name, basePrice, durationMinutes, description);
    }
}
