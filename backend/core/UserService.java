package backend.core;

import backend.user.*;
import backend.database.UserDAO;
import java.util.*;
import java.time.LocalDateTime;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service layer for user management.
 * DB first, fallback to memory, then sync to memory.
 */
public class UserService {

    private Map<String, User> usersByEmail = new ConcurrentHashMap<>();
    private Map<String, User> usersById = new ConcurrentHashMap<>();
    private Map<String, Client> registeredClients = new ConcurrentHashMap<>();
    private Map<String, Consultant> registeredConsultants = new ConcurrentHashMap<>();
    private UserDAO userDAO;

    public void setUserDAO(UserDAO dao) {
        this.userDAO = dao;
    }

    // ======================== Client Registration ========================

    public Client registerClient(String name, String email, String password) {
        if (usersByEmail.containsKey(email)) {
            System.out.println("Email already registered: " + email);
            return null;
        }
        Client client = new Client(name, email, password);
        registeredClients.put(email, client);
        usersByEmail.put(email, client);
        usersById.put(client.getUserID().toString(), client);
        System.out.println("Client registered: " + email + " (ID: " + client.getUserID() + ")");
        return client;
    }

    public Client getClientByEmail(String email) {
        return registeredClients.get(email);
    }

    public Collection<Client> getAllClients() {
        if (userDAO != null) {
            try {
                List<Client> dbClients = userDAO.getAllClients();
                if (dbClients != null && !dbClients.isEmpty()) {
                    for (Client c : dbClients) {
                        registeredClients.put(c.getEmail(), c);
                        usersByEmail.put(c.getEmail(), c);
                        usersById.put(c.getUserID().toString(), c);
                    }
                    return dbClients;
                }
            } catch (Exception e) {
                System.err.println("Error fetching clients from DB: " + e.getMessage());
            }
        }
        return registeredClients.values();
    }

    // ======================== Consultant Registration ========================

    public Consultant registerConsultant(String name, String email, String password) {
        if (usersByEmail.containsKey(email)) {
            System.out.println("Email already registered: " + email);
            return null;
        }
        Consultant consultant = new Consultant(name, email, password);
        registeredConsultants.put(email, consultant);
        usersByEmail.put(email, consultant);
        usersById.put(consultant.getUserID().toString(), consultant);
        System.out.println("Consultant registered: " + email + " (ID: " + consultant.getUserID() + ")");
        return consultant;
    }

    public Consultant getConsultantByEmail(String email) {
        return registeredConsultants.get(email);
    }

    public Collection<Consultant> getAllConsultants() {
        if (userDAO != null) {
            try {
                List<Consultant> dbConsultants = userDAO.getAllConsultants();
                if (dbConsultants != null && !dbConsultants.isEmpty()) {
                    for (Consultant c : dbConsultants) {
                        registeredConsultants.put(c.getEmail(), c);
                        usersByEmail.put(c.getEmail(), c);
                        usersById.put(c.getUserID().toString(), c);
                    }
                    return dbConsultants;
                }
            } catch (Exception e) {
                System.err.println("Error fetching all consultants from DB: " + e.getMessage());
            }
        }
        return registeredConsultants.values();
    }

    public List<Consultant> getApprovedConsultants() {
        List<Consultant> approved = new ArrayList<>();
        if (userDAO != null) {
            try {
                List<Consultant> all = userDAO.getAllConsultants();
                for (Consultant c : all) {
                    if (c.isApproved()) {
                        approved.add(c);
                    }
                }
                if (!approved.isEmpty()) {
                    for (Consultant c : all) {
                        registeredConsultants.put(c.getEmail(), c);
                        usersByEmail.put(c.getEmail(), c);
                        usersById.put(c.getUserID().toString(), c);
                    }
                    return approved;
                }
            } catch (Exception e) {
                System.err.println("Error fetching approved consultants from DB: " + e.getMessage());
            }
        }
        for (User u : usersByEmail.values()) {
            if (u instanceof Consultant) {
                Consultant c = (Consultant) u;
                if (c.isApproved()) approved.add(c);
            }
        }
        return approved;
    }

    public List<Consultant> getPendingConsultants() {
        List<Consultant> pending = new ArrayList<>();
        if (userDAO != null) {
            try {
                List<Consultant> dbPending = userDAO.getPendingConsultants();
                if (dbPending != null && !dbPending.isEmpty()) {
                    for (Consultant c : dbPending) {
                        registeredConsultants.put(c.getEmail(), c);
                        usersByEmail.put(c.getEmail(), c);
                        usersById.put(c.getUserID().toString(), c);
                    }
                    return dbPending;
                }
            } catch (Exception e) {
                System.err.println("Error fetching pending consultants from DB: " + e.getMessage());
            }
        }
        for (User u : usersByEmail.values()) {
            if (u instanceof Consultant) {
                Consultant c = (Consultant) u;
                if (!c.isApproved()) pending.add(c);
            }
        }
        return pending;
    }

    public void approveConsultant(Consultant consultant) {
        consultant.setApproved(true);
        if (userDAO != null) {
            try {
                userDAO.updateApprovalStatus(consultant.getUserID().toString(), true);
            } catch (Exception e) {
                System.err.println("Error updating consultant approval in DB: " + e.getMessage());
            }
        }
    }

    public void rejectConsultant(Consultant consultant) {
        consultant.setApproved(false);
        if (userDAO != null) {
            try {
                userDAO.updateApprovalStatus(consultant.getUserID().toString(), false);
            } catch (Exception e) {
                System.err.println("Error updating consultant rejection in DB: " + e.getMessage());
            }
        }
    }

    // ======================== Authentication ========================

    public User authenticateUser(String email, String password) {
        User user = usersByEmail.get(email);
        if (user != null && user.getPassword().equals(password)) {
            return user;
        }
        return null;
    }

    public User findByEmail(String email) {
        return usersByEmail.get(email);
    }

    public User findById(String userId) {
        User user = usersById.get(userId);
        if (user != null) return user;
        for (User u : usersByEmail.values()) {
            if (u.getUserID().toString().equals(userId)) {
                return u;
            }
        }
        return null;
    }

    // ======================== Seed Data ========================

    public void seedData() {
        if (!usersByEmail.isEmpty()) {
            System.out.println("Seed data already loaded, skipping...");
            return;
        }
        System.out.println("Loading seed data...");

        Admin admin = new Admin("System Admin", "admin@example.com", "admin123");
        usersByEmail.put(admin.getEmail(), admin);
        usersById.put(admin.getUserID().toString(), admin);

        Consultant c1 = new Consultant("Dr. Sarah Johnson", "sarah@example.com", "password123");
        c1.setApproved(true);
        Consultant c2 = new Consultant("Prof. Michael Chen", "michael@example.com", "password123");
        c2.setApproved(true);
        Consultant c3 = new Consultant("Dr. Emily Brown", "emily@example.com", "password123");

        registeredConsultants.put(c1.getEmail(), c1);
        registeredConsultants.put(c2.getEmail(), c2);
        registeredConsultants.put(c3.getEmail(), c3);
        usersByEmail.put(c1.getEmail(), c1);
        usersByEmail.put(c2.getEmail(), c2);
        usersByEmail.put(c3.getEmail(), c3);
        usersById.put(c1.getUserID().toString(), c1);
        usersById.put(c2.getUserID().toString(), c2);
        usersById.put(c3.getUserID().toString(), c3);

        Client client1 = new Client("Alice Smith", "alice@example.com", "password123");
        Client client2 = new Client("Bob Johnson", "bob@example.com", "password123");
        registeredClients.put(client1.getEmail(), client1);
        registeredClients.put(client2.getEmail(), client2);
        usersByEmail.put(client1.getEmail(), client1);
        usersByEmail.put(client2.getEmail(), client2);
        usersById.put(client1.getUserID().toString(), client1);
        usersById.put(client2.getUserID().toString(), client2);

        System.out.println("Seed data loaded: " + usersByEmail.size() + " users");
        System.out.println("  Admin: " + admin.getEmail() + " (ID: " + admin.getUserID() + ")");
        System.out.println("  Consultants: " + registeredConsultants.size());
        System.out.println("  Clients: " + registeredClients.size());
    }
}