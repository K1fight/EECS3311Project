package backend.core;

import backend.user.Consultant;
import backend.user.Client;
import backend.booking.Booking;
import backend.policy.CancellationPolicy;
import backend.policy.PricingStrategy;
import backend.policy.SystemPolicy;

import java.util.*;

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

    /**
     * Get system status including statistics
     * @return Map containing system statistics
     */
    public Map<String, Object> getSystemStatus() {
        Map<String, Object> status = new HashMap<>();
        
        // Count users by type - use Collection instead of List
        Collection<Consultant> consultants = userService.getAllConsultants();
        Collection<Client> clients = userService.getAllClients();
        long approvedConsultants = consultants.stream().filter(Consultant::isApproved).count();
        long pendingConsultants = consultants.stream().filter(c -> !c.isApproved()).count();
        
        status.put("total_users", consultants.size() + clients.size() + 1); // +1 for admin
        status.put("total_clients", clients.size());
        status.put("total_consultants", consultants.size());
        status.put("approved_consultants", approvedConsultants);
        status.put("pending_consultants", pendingConsultants);
        
        status.put("total_services", 3); // Career Counseling, IT Consulting, Financial Advisory
        
        // Policy information
        status.put("cancellation_policy", SystemPolicy.getCancellationPolicy().getClass().getSimpleName());
        status.put("pricing_strategy", SystemPolicy.getPricingStrategy() != null ? 
            SystemPolicy.getPricingStrategy().getClass().getSimpleName() : "Default (Fixed)");
        
        return status;
    }
}
