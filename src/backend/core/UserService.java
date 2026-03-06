package backend.core;

import backend.user.Client;
import backend.user.Consultant;
import backend.user.User;
import java.util.*;

public class UserService {
    private Map<String, Client> registeredClients = new HashMap<>();
    private Map<String, Consultant> registeredConsultants = new HashMap<>();
    private Map<String, User> allUsersByEmail = new HashMap<>();

    /**
     * Register a new client
     * @param name Client's name
     * @param email Client's email (will be used as unique identifier)
     * @param password Client's password
     * @return The registered Client object, or null if registration fails
     */
    public Client registerClient(String name, String email, String password) {
        // Validate input
        if (!validateEmail(email)) {
            System.out.println("Invalid email format!");
            return null;
        }
        
        if (!validatePassword(password)) {
            System.out.println("Password must be at least 6 characters!");
            return null;
        }
        
        // Check if email already exists
        if (allUsersByEmail.containsKey(email)) {
            System.out.println("Email already registered!");
            return null;
        }
        
        // Create and store client
        Client client = new Client(name, email, password);
        registeredClients.put(email, client);
        allUsersByEmail.put(email, client);
        
        System.out.println("Client registered successfully: " + name + " (" + email + ")");
        return client;
    }

    /**
     * Register a new consultant
     * @param name Consultant's name
     * @param email Consultant's email (will be used as unique identifier)
     * @param password Consultant's password
     * @return The registered Consultant object, or null if registration fails
     */
    public Consultant registerConsultant(String name, String email, String password) {
        // Validate input
        if (!validateEmail(email)) {
            System.out.println("Invalid email format!");
            return null;
        }
        
        if (!validatePassword(password)) {
            System.out.println("Password must be at least 6 characters!");
            return null;
        }
        
        // Check if email already exists
        if (allUsersByEmail.containsKey(email)) {
            System.out.println("Email already registered!");
            return null;
        }
        
        // Create and store consultant
        Consultant consultant = new Consultant(name, email, password);
        registeredConsultants.put(email, consultant);
        allUsersByEmail.put(email, consultant);
        
        System.out.println("Consultant registered successfully: " + name + " (" + email + ")");
        return consultant;
    }

    /**
     * Authenticate a user by email and password
     * @param email User's email
     * @param password User's password
     * @return The authenticated User object, or null if authentication fails
     */
    public User authenticateUser(String email, String password) {
        User user = allUsersByEmail.get(email);
        
        if (user == null) {
            System.out.println("User not found!");
            return null;
        }
        
        if (!user.getPassword().equals(password)) {
            System.out.println("Incorrect password!");
            return null;
        }
        
        // Check if consultant is approved
        if (user instanceof Consultant) {
            Consultant consultant = (Consultant) user;
            if (!consultant.isApproved()) {
                System.out.println("Consultant account pending admin approval. Please wait for approval.");
                return null;
            }
        }
        
        System.out.println("Authentication successful for: " + user.getName());
        return user;
    }

    /**
     * Get a client by email
     * @param email Client's email
     * @return The Client object, or null if not found
     */
    public Client getClientByEmail(String email) {
        return registeredClients.get(email);
    }

    /**
     * Get a consultant by email
     * @param email Consultant's email
     * @return The Consultant object, or null if not found
     */
    public Consultant getConsultantByEmail(String email) {
        return registeredConsultants.get(email);
    }

    /**
     * Check if an email is already registered
     * @param email Email to check
     * @return true if email is registered, false otherwise
     */
    public boolean isEmailRegistered(String email) {
        return allUsersByEmail.containsKey(email);
    }

    /**
     * Get all registered clients
     * @return Collection of all clients
     */
    public Collection<Client> getAllClients() {
        return registeredClients.values();
    }

    /**
     * Get all registered consultants
     * @return Collection of all consultants
     */
    public Collection<Consultant> getAllConsultants() {
        return registeredConsultants.values();
    }

    /**
     * Validate email format (simple validation)
     * @param email Email to validate
     * @return true if valid email format, false otherwise
     */
    private boolean validateEmail(String email) {
        return email != null && email.matches("^[A-Za-z0-9+_.-]+@(.+)$");
    }

    /**
     * Validate password strength
     * @param password Password to validate
     * @return true if valid password, false otherwise
     */
    private boolean validatePassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Remove a client from the system (for admin use)
     * @param email Client's email
     * @return true if removed successfully, false if not found
     */
    public boolean removeClient(String email) {
        Client client = registeredClients.remove(email);
        if (client != null) {
            allUsersByEmail.remove(email);
            System.out.println("Client removed: " + client.getName());
            return true;
        }
        return false;
    }

    /**
     * Remove a consultant from the system (for admin use)
     * @param email Consultant's email
     * @return true if removed successfully, false if not found
     */
    public boolean removeConsultant(String email) {
        Consultant consultant = registeredConsultants.remove(email);
        if (consultant != null) {
            allUsersByEmail.remove(email);
            System.out.println("Consultant removed: " + consultant.getName());
            return true;
        }
        return false;
    }
    
    /**
     * Approve a consultant (for admin use)
     * @param email Consultant's email
     * @return true if approved successfully, false if not found or already approved
     */
    public boolean approveConsultant(String email) {
        Consultant consultant = registeredConsultants.get(email);
        if (consultant != null) {
            if (consultant.isApproved()) {
                System.out.println("Consultant " + consultant.getName() + " is already approved.");
                return false;
            }
            consultant.setApproved(true);
            System.out.println("Consultant " + consultant.getName() + " has been approved.");
            return true;
        }
        System.out.println("Consultant not found!");
        return false;
    }
    
    /**
     * Reject a consultant (for admin use)
     * @param email Consultant's email
     * @return true if rejected successfully, false if not found
     */
    public boolean rejectConsultant(String email) {
        Consultant consultant = registeredConsultants.get(email);
        if (consultant != null) {
            consultant.setApproved(false);
            System.out.println("Consultant " + consultant.getName() + " has been rejected.");
            return true;
        }
        System.out.println("Consultant not found!");
        return false;
    }
    
    /**
     * Check if a consultant is approved
     * @param email Consultant's email
     * @return true if approved, false otherwise
     */
    public boolean isConsultantApproved(String email) {
        Consultant consultant = registeredConsultants.get(email);
        return consultant != null && consultant.isApproved();
    }
    
    /**
     * Get all pending consultants (not yet approved)
     * @return Collection of pending consultants
     */
    public Collection<Consultant> getPendingConsultants() {
        Collection<Consultant> pending = new ArrayList<>();
        for (Consultant consultant : registeredConsultants.values()) {
            if (!consultant.isApproved()) {
                pending.add(consultant);
            }
        }
        return pending;
    }
}
