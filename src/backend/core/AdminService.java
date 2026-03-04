package backend.core;

import backend.user.Consultant;
import backend.policy.CancellationPolicy;
import backend.policy.PricingStrategy;

public class AdminService {
    private ConsultantService consultantService;

    public AdminService() {
        this.consultantService = new ConsultantService();
    }

    public AdminService(ConsultantService consultantService) {
        this.consultantService = consultantService;
    }

    public void approveConsultant(Consultant consultant) {
        // Set consultant status to approved
        System.out.println("Consultant " + consultant.getName() + " approved.");
    }

    public void rejectConsultant(Consultant consultant) {
        // Reject
        System.out.println("Consultant " + consultant.getName() + " rejected.");
    }

    public void setCancellationPolicy(CancellationPolicy policy) {
        // Global policy replacement
    }

    public void setPricingStrategy(PricingStrategy strategy) {
        // Global policy replacement
    }
}
