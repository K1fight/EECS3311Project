package backend.user;

import java.time.LocalDateTime;
import java.util.Objects;
import java.util.UUID;

/**
 * Base class for all user types: Client, Consultant, Admin.
 * Handles common fields and session state.
 */
public abstract class User {
    private UUID userID;
    private String name;
    private String email;
    private String password;
    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;
    private boolean isLoggedIn;

    /**
     * Primary constructor — generates a new random UUID.
     */
    public User(String name, String email, String password) {
        this.userID = UUID.randomUUID();
        this.name = name;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
        this.isLoggedIn = false;
    }

    /**
     * Rehydration constructor — used when loading from database (preserves the stored UUID).
     */
    public User(UUID userID, String name, String email, String password) {
        this.userID = userID;
        this.name = name;
        this.email = email;
        this.password = password;
        this.createdAt = LocalDateTime.now();
        this.isLoggedIn = false;
    }

    // ======================== Session ========================

    /**
     * Log this user in. Sets session timestamp.
     */
    public void logIn() {
        if (!isLoggedIn) {
            this.lastLoginAt = LocalDateTime.now();
            this.isLoggedIn = true;
            System.out.println("[" + getAccountType() + "] " + name + " logged in at " + this.lastLoginAt);
        }
    }

    /**
     * Log this user out. Clears session.
     */
    public void logout() {
        if (isLoggedIn) {
            this.isLoggedIn = false;
            System.out.println("[" + getAccountType() + "] " + name + " logged out.");
        }
    }

    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    public LocalDateTime getLastLoginAt() {
        return lastLoginAt;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    // ======================== Getters & Setters ========================

    public UUID getUserID() {
        return userID;
    }

    public void setUserID(UUID userID) {
        this.userID = userID;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    // ======================== Identity ========================

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return userID != null && userID.equals(user.userID);
    }

    @Override
    public int hashCode() {
        return Objects.hash(userID);
    }

    @Override
    public String toString() {
        return String.format("%s[id=%s, name=%s, email=%s, loggedIn=%s]",
            getAccountType(), userID, name, email, isLoggedIn);
    }

    // ======================== Abstract ========================

    /** Returns this user's account type. */
    public abstract AccountType getAccountType();
}
