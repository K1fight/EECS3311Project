package backend.policy;

public class SystemPolicy {
    private static CancellationPolicy cancellationPolicy = new DefaultCancellationPolicy();
    private static PricingStrategy pricingStrategy = null;

    public static CancellationPolicy getCancellationPolicy() {
        return cancellationPolicy;
    }

    public static void setCancellationPolicy(CancellationPolicy policy) {
        cancellationPolicy = policy;
        System.out.println("System: Cancellation policy updated.");
    }

    public static PricingStrategy getPricingStrategy() {
        return pricingStrategy;
    }

    public static void setPricingStrategy(PricingStrategy strategy) {
        pricingStrategy = strategy;
        System.out.println("System: Pricing strategy updated.");
    }
}
