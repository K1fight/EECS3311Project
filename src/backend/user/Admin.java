package backend.user;

public class Admin extends User {
    public Admin(String name, String email, String password) {
        super(name, email, password);
    }

    public void approveConsultant() {
        System.out.println("Approving consultant...");
        // Implementation delegated to AdminService
    }

    public void rejectConsultant() {
        System.out.println("Rejecting consultant...");
        // Implementation delegated to AdminService
    }

    public void definePolicies() {
        System.out.println("Defining system policies...");
        // Implementation delegated to AdminService
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.Admin;
    }
}