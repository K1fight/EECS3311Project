package backend.user;

import backend.core.AdminService;
import backend.core.UserService;
import backend.core.ConsultingService;
import backend.policy.CancellationPolicy;
import backend.policy.PricingStrategy;
import backend.policy.SystemPolicy;

import java.util.Collection;
import java.util.Map;

/**
 * Admin: system administrator who approves consultant registrations,
 * manages policies, defines services, and monitors system health.
 */
public class Admin extends User {

    private AdminService adminService;

    // ======================== Constructors ========================

    /** Primary constructor. */
    public Admin(String name, String email, String password) {
        super(name, email, password);
    }

    /** Rehydration constructor (for DB loading). */
    public Admin(java.util.UUID userID, String name, String email, String password) {
        super(userID, name, email, password);
    }

    // ======================== Service Layer Injection ========================

    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    public AdminService getAdminService() {
        return adminService;
    }

    // ======================== Consultant Management ========================

    /**
     * Approve a pending consultant registration.
     * @param consultant the consultant to approve
     */
    public void approveConsultant(Consultant consultant) {
        if (consultant == null) {
            System.out.println("Consultant is required.");
            return;
        }
        if (consultant.isApproved()) {
            System.out.println(consultant.getName() + " is already approved.");
            return;
        }

        consultant.setApproved(true);

        if (adminService != null) {
            adminService.approveConsultant(consultant);
        } else {
            System.out.println("Consultant " + consultant.getName()
                + " (email: " + consultant.getEmail()
                + ") approved successfully.");
        }
    }

    /**
     * Reject a pending consultant registration.
     * @param consultant the consultant to reject
     */
    public void rejectConsultant(Consultant consultant) {
        if (consultant == null) {
            System.out.println("Consultant is required.");
            return;
        }
        if (!consultant.isApproved()) {
            System.out.println(consultant.getName() + " is already pending or rejected.");
            return;
        }

        consultant.setApproved(false);

        if (adminService != null) {
            adminService.rejectConsultant(consultant);
        } else {
            System.out.println("Consultant " + consultant.getName()
                + " (email: " + consultant.getEmail()
                + ") rejected.");
        }
    }

    /**
     * List all pending (unapproved) consultants.
     * @param allConsultants all registered consultants
     */
    public void listPendingConsultants(Collection<Consultant> allConsultants) {
        if (allConsultants == null || allConsultants.isEmpty()) {
            System.out.println("No consultants registered.");
            return;
        }
        boolean found = false;
        System.out.println("\n=== Pending Consultants ===");
        for (Consultant c : allConsultants) {
            if (!c.isApproved()) {
                System.out.printf("  %s | %s | %s%n",
                    c.getName(), c.getEmail(), c.getSpecialty());
                found = true;
            }
        }
        if (!found) {
            System.out.println("  No pending consultants.");
        }
    }

    /**
     * List all approved consultants.
     * @param allConsultants all registered consultants
     */
    public void listApprovedConsultants(Collection<Consultant> allConsultants) {
        if (allConsultants == null || allConsultants.isEmpty()) {
            System.out.println("No consultants registered.");
            return;
        }
        System.out.println("\n=== Approved Consultants ===");
        boolean found = false;
        for (Consultant c : allConsultants) {
            if (c.isApproved()) {
                System.out.printf("  %s | %s | %s%n",
                    c.getName(), c.getEmail(), c.getSpecialty());
                found = true;
            }
        }
        if (!found) {
            System.out.println("  No approved consultants.");
        }
    }

    // ======================== Service Management ========================

    /**
     * Define / create a new consulting service.
     * @param service the service to add to the system
     */
    public void addService(ConsultingService service) {
        if (service == null) {
            System.out.println("Service is required.");
            return;
        }
        System.out.println("Service created: " + service);
    }

    /**
     * Update an existing consulting service.
     * @param service the service to update
     * @param newName        new name (pass null to keep existing)
     * @param newDescription new description
     * @param newPrice       new base price (-1 to keep existing)
     * @param newDuration    new duration in minutes (-1 to keep existing)
     */
    public void updateService(ConsultingService service, String newName,
                              String newDescription, double newPrice, int newDuration) {
        if (service == null) {
            System.out.println("Service is required.");
            return;
        }
        if (newName != null && !newName.isBlank()) service.setName(newName);
        if (newDescription != null) service.setDescription(newDescription);
        if (newPrice >= 0) service.setBasePrice(newPrice);
        if (newDuration > 0) service.setDurationMinutes(newDuration);
        System.out.println("Service updated: " + service);
    }

    // ======================== Policy Management ========================

    /**
     * Set the global cancellation policy.
     * @param policy the new policy (e.g. DefaultCancellationPolicy, StrictCancellationPolicy)
     */
    public void defineCancellationPolicy(CancellationPolicy policy) {
        if (policy == null) {
            System.out.println("Policy is required.");
            return;
        }
        if (adminService != null) {
            adminService.setCancellationPolicy(policy);
        } else {
            SystemPolicy.setCancellationPolicy(policy);
        }
        System.out.println("Cancellation policy set to: " + policy.getClass().getSimpleName());
    }

    /**
     * Set the global pricing strategy.
     * @param strategy the new strategy (e.g. FixedPricingStrategy, DiscountPricingStrategy)
     */
    public void definePricingStrategy(PricingStrategy strategy) {
        if (strategy == null) {
            System.out.println("Strategy is required.");
            return;
        }
        if (adminService != null) {
            adminService.setPricingStrategy(strategy);
        } else {
            SystemPolicy.setPricingStrategy(strategy);
        }
        System.out.println("Pricing strategy set to: " + strategy.getClass().getSimpleName());
    }

    /**
     * Show the currently active policies.
     */
    public void showCurrentPolicies() {
        CancellationPolicy cp = SystemPolicy.getCancellationPolicy();
        PricingStrategy ps = SystemPolicy.getPricingStrategy();
        System.out.println("\n=== Active Policies ===");
        System.out.println("  Cancellation: " + (cp != null ? cp.getClass().getSimpleName() : "none"));
        System.out.println("  Pricing      : " + (ps != null ? ps.getClass().getSimpleName() : "default (fixed)"));
    }

    // ======================== System Status ========================

    /**
     * Print a summary of system statistics.
     * @param status a Map produced by AdminService.getSystemStatus()
     */
    public void showSystemStatus(Map<String, Object> status) {
        if (status == null || status.isEmpty()) {
            System.out.println("No system status available.");
            return;
        }
        System.out.println("\n=== System Status ===");
        System.out.printf("  Total Users        : %s%n", status.get("total_users"));
        System.out.printf("  Clients            : %s%n", status.get("total_clients"));
        System.out.printf("  Consultants (all)  : %s%n", status.get("total_consultants"));
        System.out.printf("  Approved           : %s%n", status.get("approved_consultants"));
        System.out.printf("  Pending            : %s%n", status.get("pending_consultants"));
        System.out.printf("  Services           : %s%n", status.get("total_services"));
        System.out.printf("  Cancellation Policy: %s%n", status.get("cancellation_policy"));
        System.out.printf("  Pricing Strategy   : %s%n", status.get("pricing_strategy"));
    }

    // ======================== Policy Management (combined view) ========================

    /**
     * Print a summary of both active policies.
     */
    public void definePolicies() {
        showCurrentPolicies();
    }

    // ======================== Account Type ========================

    @Override
    public AccountType getAccountType() {
        return AccountType.Admin;
    }
}
