package backend.policy;

import backend.booking.Booking;
import java.time.Duration;
import java.time.LocalDateTime;

// Dynamic pricing: price varies based on demand and timing
public class DynamicPricingStrategy implements PricingStrategy {
    private double peakMultiplier = 1.5;
    private double offPeakMultiplier = 0.8;
    
    @Override
    public double calculatePrice(Booking booking) {
        double basePrice = booking.getService().getBasePrice();
        LocalDateTime startTime = booking.getStartTime();
        
        // Check if it's peak hours (9 AM - 5 PM on weekdays)
        int hour = startTime.getHour();
        int dayOfWeek = startTime.getDayOfWeek().getValue(); // 1=Monday, 7=Sunday
        
        boolean isPeakHours = (hour >= 9 && hour < 17) && dayOfWeek <= 5;
        boolean isWeekend = dayOfWeek >= 6;
        
        if (isPeakHours) {
            return basePrice * peakMultiplier;
        } else if (isWeekend) {
            return basePrice * 1.2;
        } else {
            return basePrice * offPeakMultiplier;
        }
    }
    
    public void setPeakMultiplier(double multiplier) {
        this.peakMultiplier = multiplier;
    }
    
    public void setOffPeakMultiplier(double multiplier) {
        this.offPeakMultiplier = multiplier;
    }
}
