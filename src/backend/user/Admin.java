package backend.user;

public class Admin extends User {
    public Admin(String name, String email, String password) {
        super(name, email, password);
    }

    public void approveConsultant() {
    }

    public void rejectConsultant() {
    }

    public void definePolicies() {
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.Admin;
    }
}