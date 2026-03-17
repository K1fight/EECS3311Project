package backend.user;

import java.util.UUID;

public class UserProxy extends User {
    private User realUser;
    private boolean loggedIn = false;

    public UserProxy(User realUser) {
        super(realUser.getName(), realUser.getEmail(), realUser.getPassword());
        this.realUser = realUser;
    }

    @Override
    public UUID getUserID() {
        return realUser.getUserID();
    }

    @Override
    public String getName() {
        return realUser.getName();
    }

    @Override
    public String getEmail() {
        return realUser.getEmail();
    }

    @Override
    public String getPassword() {
        return realUser.getPassword();
    }

    @Override
    public void logIn() {
        // Pre-processing (e.g., logging)
        System.out.println("Proxy: Logging in...");
        realUser.logIn();
        loggedIn = true;
    }

    @Override
    public void logout() {
        System.out.println("Proxy: Logging out...");
        realUser.logout();
        loggedIn = false;
    }

    @Override
    public AccountType getAccountType() {
        return realUser.getAccountType();
    }

    // ========== Client method ==========
    public void browseServices() {
        checkLogin();
        if (realUser instanceof Client) {
            ((Client) realUser).browseServices();
        } else {
            throw new UnsupportedOperationException("Not a client");
        }
    }

    public void requestBooking() {
        checkLogin();
        if (realUser instanceof Client) {
            ((Client) realUser).requestBooking();
        } else {
            throw new UnsupportedOperationException("Not a client");
        }
    }

    public void cancelBooking() {
        checkLogin();
        if (realUser instanceof Client) {
            ((Client) realUser).cancelBooking();
        } else {
            throw new UnsupportedOperationException("Not a client");
        }
    }

    public void viewBookingHistory() {
        checkLogin();
        if (realUser instanceof Client) {
            ((Client) realUser).viewBookingHistory();
        } else {
            throw new UnsupportedOperationException("Not a client");
        }
    }

    public void processPayment() {
        checkLogin();
        if (realUser instanceof Client) {
            ((Client) realUser).processPayment();
        } else {
            throw new UnsupportedOperationException("Not a client");
        }
    }

    public void managePaymentMethod() {
        checkLogin();
        if (realUser instanceof Client) {
            ((Client) realUser).managePaymentMethod();
        } else {
            throw new UnsupportedOperationException("Not a client");
        }
    }

    public void viewPaymentHistory() {
        checkLogin();
        if (realUser instanceof Client) {
            ((Client) realUser).viewPaymentHistory();
        } else {
            throw new UnsupportedOperationException("Not a client");
        }
    }

    // ========== Admin method ==========
    public void approveConsultant() {
        checkLogin();
        if (realUser instanceof Admin) {
            ((Admin) realUser).approveConsultant();
        } else {
            throw new UnsupportedOperationException("Not an admin");
        }
    }

    public void rejectConsultant() {
        checkLogin();
        if (realUser instanceof Admin) {
            ((Admin) realUser).rejectConsultant();
        } else {
            throw new UnsupportedOperationException("Not an admin");
        }
    }

    public void definePolicies() {
        checkLogin();
        if (realUser instanceof Admin) {
            ((Admin) realUser).definePolicies();
        } else {
            throw new UnsupportedOperationException("Not an admin");
        }
    }

    // ========== Consultant method ==========
    public void provideConsultation() {
        checkLogin();
        if (realUser instanceof Consultant) {
            ((Consultant) realUser).provideConsultation();
        } else {
            throw new UnsupportedOperationException("Not a consultant");
        }
    }

    private void checkLogin() {
        if (!loggedIn) {
            throw new IllegalStateException("User not logged in");
        }
    }
}
