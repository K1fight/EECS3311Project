package backend.user;

import java.util.UUID;

public abstract class User {
    private UUID userID;
    private String name;
    private String email;
    private String password;

    public User(String name, String email, String password) {
        this.userID = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.password = password;
    }

    public void logIn(){
        // Logic for login
    }
    public void logout(){
        // Logic for logout
    }
    public abstract AccountType getAccountType();

    public String getName() {
        return name;
    }

    public String getEmail() {
        return email;
    }
    
    public String getPassword() {
        return password;
    }
    
    public UUID getUserID() {
        return userID;
    }
}
