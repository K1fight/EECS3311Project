package backend.policy;

/**
 * Class to define and initialize system-wide policies
 * Provides centralized policy management for the consulting booking system
 */
public class DefineSystemPolicies {
    
    /**
     * Initialize default system policies
     */
    public static void initializeDefaultPolicies() {
        // Set default cancellation policy
        SystemPolicy.setCancellationPolicy(new DefaultCancellationPolicy());
        
        // Set default pricing strategy
        SystemPolicy.setPricingStrategy(new FixedPricingStrategy());
        
        System.out.println("Default system policies initialized.");
    }
    
    /**
     * Configure flexible cancellation policy
     * @param fullRefundHours Hours before booking for full refund
     * @param partialRefundHours Hours before booking for partial refund
     * @param partialRefundPercentage Partial refund percentage (0.0 to 1.0)
     */
    public static void configureFlexibleCancellation(int fullRefundHours, int partialRefundHours, double partialRefundPercentage) {
        FlexibleCancellationPolicy policy = new FlexibleCancellationPolicy(
            fullRefundHours, 
            partialRefundHours, 
            partialRefundPercentage
        );
        SystemPolicy.setCancellationPolicy(policy);
        System.out.println("Flexible cancellation policy configured.");
    }
    
    /**
     * Configure dynamic pricing strategy
     * @param peakMultiplier Peak hours price multiplier
     * @param offPeakMultiplier Off-peak hours price multiplier
     */
    public static void configureDynamicPricing(double peakMultiplier, double offPeakMultiplier) {
        DynamicPricingStrategy strategy = new DynamicPricingStrategy();
        strategy.setPeakMultiplier(peakMultiplier);
        strategy.setOffPeakMultiplier(offPeakMultiplier);
        SystemPolicy.setPricingStrategy(strategy);
        System.out.println("Dynamic pricing strategy configured.");
    }
    
    /**
     * Apply strict cancellation policy (no refunds)
     */
    public static void applyStrictCancellationPolicy() {
        CancellationPolicy strictPolicy = new CancellationPolicy() {
            @Override
            public boolean canCancel(backend.booking.Booking booking, java.time.LocalDateTime cancellationTime) {
                return cancellationTime.isBefore(booking.getStartTime().minusDays(7));
            }
            
            @Override
            public double getRefundPercentage(backend.booking.Booking booking, java.time.LocalDateTime cancellationTime) {
                long hoursBefore = java.time.Duration.between(cancellationTime, booking.getStartTime()).toHours();
                if (hoursBefore >= 168) return 1.0; // 7 days
                else if (hoursBefore >= 72) return 0.5; // 3 days
                else return 0.0; // No refund
            }
        };
        SystemPolicy.setCancellationPolicy(strictPolicy);
        System.out.println("Strict cancellation policy applied.");
    }
    
    /**
     * Apply lenient cancellation policy (full refund anytime)
     */
    public static void applyLenientCancellationPolicy() {
        CancellationPolicy lenientPolicy = new CancellationPolicy() {
            @Override
            public boolean canCancel(backend.booking.Booking booking, java.time.LocalDateTime cancellationTime) {
                return true; // Allow cancellation anytime
            }
            
            @Override
            public double getRefundPercentage(backend.booking.Booking booking, java.time.LocalDateTime cancellationTime) {
                return 1.0; // Full refund always
            }
        };
        SystemPolicy.setCancellationPolicy(lenientPolicy);
        System.out.println("Lenient cancellation policy applied.");
    }
    
    /**
     * Get current policy summary
     * @return Summary of current policies
     */
    public static String getPolicySummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== Current System Policies ===\n");
        summary.append("Cancellation Policy: ").append(SystemPolicy.getCancellationPolicy().getClass().getSimpleName()).append("\n");
        
        PricingStrategy pricingStrategy = SystemPolicy.getPricingStrategy();
        if (pricingStrategy != null) {
            summary.append("Pricing Strategy: ").append(pricingStrategy.getClass().getSimpleName()).append("\n");
        } else {
            summary.append("Pricing Strategy: Not set (using base price)\n");
        }
        
        return summary.toString();
    }
}
