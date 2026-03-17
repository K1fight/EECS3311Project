package backend.user;

public class Consultant extends User {
    private boolean isApproved = false; // Default to not approved
    
    public Consultant(String name, String email, String password) {
        super(name, email, password);
    }

    public void provideConsultation() {
        // Logic for providing consultation
    }

    @Override
    public AccountType getAccountType() {
        return AccountType.Consultant;
    }
    
    /**
     * Check if consultant is approved by admin
     * @return true if approved, false otherwise
     */
    public boolean isApproved() {
        return isApproved;
    }
    
    /**
     * Set consultant approval status
     * @param approved Approval status
     */
    public void setApproved(boolean approved) {
        this.isApproved = approved;
    }
}
