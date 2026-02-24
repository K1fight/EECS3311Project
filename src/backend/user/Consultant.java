package backend.user;

public class Consultant extends User {
    
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
}
