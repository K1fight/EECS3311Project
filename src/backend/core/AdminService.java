package backend.core;

import backend.user.Consultant;
import backend.policy.CancellationPolicy;
import backend.policy.PricingStrategy;

public class AdminService {
    public void approveConsultant(Consultant consultant) {
        // Set consultant status to approved
    }

    public void rejectConsultant(Consultant consultant) {
        // Reject
    }

    public void setCancellationPolicy(CancellationPolicy policy) {
        // Global policy replacement
    }

    public void setPricingStrategy(PricingStrategy strategy) {
        // Global policy replacement
    }
}
