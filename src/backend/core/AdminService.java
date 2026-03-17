package backend.core;

import backend.user.Consultant;
import backend.policy.CancellationPolicy;
import backend.policy.PricingStrategy;
import backend.policy.SystemPolicy;

public class AdminService {
    private ConsultingService consultingService;
    private UserService userService;

    public AdminService() {
        this.consultingService = new ConsultingService();
        this.userService = new UserService();
    }

    public AdminService(ConsultingService consultingService) {
        this.consultingService = consultingService;
        this.userService = new UserService();
    }
    
    public AdminService(ConsultingService consultingService, UserService userService) {
        this.consultingService = consultingService;
        this.userService = userService;
    }

    public void approveConsultant(Consultant consultant) {
        // Set consultant status to approved
        if (userService != null) {
            userService.approveConsultant(consultant.getEmail());
        } else {
            System.out.println("Consultant " + consultant.getName() + " approved.");
        }
    }

    public void rejectConsultant(Consultant consultant) {
        // Reject
        if (userService != null) {
            userService.rejectConsultant(consultant.getEmail());
        } else {
            System.out.println("Consultant " + consultant.getName() + " rejected.");
        }
    }

    public void setCancellationPolicy(CancellationPolicy policy) {
        // Global policy replacement
        SystemPolicy.setCancellationPolicy(policy);
    }

    public void setPricingStrategy(PricingStrategy strategy) {
        // Global policy replacement
        SystemPolicy.setPricingStrategy(strategy);
    }
}
