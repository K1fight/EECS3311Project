package backend.user;

/**
 * Proxy pattern: controls and guards access to a real User object.
 *
 * - All role-specific operations check login state before delegating.
 * - Only the real object's getAccountType() is forwarded.
 * - The proxy's own identity fields (name/email) also forward to realUser
 *   to stay consistent even if realUser's UUID changes.
 */
public class UserProxy extends User {

    private User realUser;

    // Proxy tracks login state independently so logout works cleanly
    private boolean isLoggedIn;

    // ======================== Constructors ========================

    public UserProxy(User realUser) {
        // Use a placeholder UUID — getUserID() always delegates to realUser
        super(realUser.getName(), realUser.getEmail(), realUser.getPassword());
        this.realUser = realUser;
        this.isLoggedIn = false;
    }

    // ======================== Identity Forwarding ========================

    /**
     * Returns the real user's UUID — never the proxy's own placeholder.
     */
    @Override
    public java.util.UUID getUserID() {
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

    // ======================== Session Management ========================

    /**
     * Log the user in through the proxy.
     * Calls realUser.logIn() and marks the proxy session active.
     */
    @Override
    public void logIn() {
        if (!isLoggedIn) {
            System.out.println("[UserProxy] Logging in " + realUser.getName() + "...");
            realUser.logIn();
            this.isLoggedIn = true;
        }
    }

    /**
     * Log the user out through the proxy.
     * Calls realUser.logout() and clears the proxy session.
     */
    @Override
    public void logout() {
        if (isLoggedIn) {
            System.out.println("[UserProxy] Logging out " + realUser.getName() + "...");
            realUser.logout();
            this.isLoggedIn = false;
        }
    }

    /**
     * Whether this proxy's session is currently active.
     */
    @Override
    public boolean isLoggedIn() {
        return isLoggedIn;
    }

    // ======================== Account Type ========================

    @Override
    public AccountType getAccountType() {
        return realUser.getAccountType();
    }

    // ======================== Guard ========================

    /**
     * Throws IllegalStateException if the proxy session is not active.
     * Call this at the top of every role-specific operation.
     */
    private void checkLogin() {
        if (!isLoggedIn) {
            throw new IllegalStateException(
                "Operation requires login. Call logIn() first.");
        }
    }

    // ======================== Client Operations ========================

    public void browseServices() {
        checkLogin();
        if (realUser instanceof backend.user.Client c) {
            c.browseServices();
        } else {
            throw new UnsupportedOperationException(
                "browseServices() is only available for Client accounts.");
        }
    }

    public void requestBooking(backend.core.ConsultingService service,
                              backend.user.Consultant consultant,
                              java.time.LocalDateTime startTime) {
        checkLogin();
        if (realUser instanceof backend.user.Client c) {
            c.requestBooking(service, consultant, startTime);
        } else {
            throw new UnsupportedOperationException(
                "requestBooking() is only available for Client accounts.");
        }
    }

    public void cancelBooking(backend.booking.Booking booking) {
        checkLogin();
        if (realUser instanceof backend.user.Client c) {
            c.cancelBooking(booking);
        } else {
            throw new UnsupportedOperationException(
                "cancelBooking() is only available for Client accounts.");
        }
    }

    public void viewBookingHistory() {
        checkLogin();
        if (realUser instanceof backend.user.Client c) {
            c.viewBookingHistory();
        } else {
            throw new UnsupportedOperationException(
                "viewBookingHistory() is only available for Client accounts.");
        }
    }

    public void processPayment(backend.payment.PaymentMethod method,
                               backend.payment.PaymentTransaction transaction) {
        checkLogin();
        if (realUser instanceof backend.user.Client c) {
            c.addPaymentToHistory(transaction);
            System.out.println("Payment processed via proxy for " + realUser.getName());
        } else {
            throw new UnsupportedOperationException(
                "processPayment() is only available for Client accounts.");
        }
    }

    public void addPaymentMethod(String type, java.util.Map<String, String> details) {
        checkLogin();
        if (realUser instanceof backend.user.Client c) {
            c.addPaymentMethod(type, details);
        } else {
            throw new UnsupportedOperationException(
                "addPaymentMethod() is only available for Client accounts.");
        }
    }

    public void listPaymentMethods() {
        checkLogin();
        if (realUser instanceof backend.user.Client c) {
            c.listPaymentMethods();
        } else {
            throw new UnsupportedOperationException(
                "listPaymentMethods() is only available for Client accounts.");
        }
    }

    public void removePaymentMethod(int index) {
        checkLogin();
        if (realUser instanceof backend.user.Client c) {
            c.removePaymentMethod(index);
        } else {
            throw new UnsupportedOperationException(
                "removePaymentMethod() is only available for Client accounts.");
        }
    }

    public void viewPaymentHistory() {
        checkLogin();
        if (realUser instanceof backend.user.Client c) {
            c.viewPaymentHistory();
        } else {
            throw new UnsupportedOperationException(
                "viewPaymentHistory() is only available for Client accounts.");
        }
    }

    // ======================== Admin Operations ========================

    public void approveConsultant(Consultant consultant) {
        checkLogin();
        if (realUser instanceof Admin a) {
            a.approveConsultant(consultant);
        } else {
            throw new UnsupportedOperationException(
                "approveConsultant() is only available for Admin accounts.");
        }
    }

    public void rejectConsultant(Consultant consultant) {
        checkLogin();
        if (realUser instanceof Admin a) {
            a.rejectConsultant(consultant);
        } else {
            throw new UnsupportedOperationException(
                "rejectConsultant() is only available for Admin accounts.");
        }
    }

    public void definePolicies() {
        checkLogin();
        if (realUser instanceof Admin a) {
            a.definePolicies();
        } else {
            throw new UnsupportedOperationException(
                "definePolicies() is only available for Admin accounts.");
        }
    }

    // ======================== Consultant Operations ========================

    public void provideConsultation(backend.booking.Booking booking) {
        checkLogin();
        if (realUser instanceof Consultant c) {
            c.provideConsultation(booking);
        } else {
            throw new UnsupportedOperationException(
                "provideConsultation() is only available for Consultant accounts.");
        }
    }

    public void reviewBookingRequest(backend.booking.Booking booking, boolean accept) {
        checkLogin();
        if (realUser instanceof Consultant c) {
            c.reviewBookingRequest(booking, accept);
        } else {
            throw new UnsupportedOperationException(
                "reviewBookingRequest() is only available for Consultant accounts.");
        }
    }

    public void completeConsultation(backend.booking.Booking booking) {
        checkLogin();
        if (realUser instanceof Consultant c) {
            c.completeConsultation(booking);
        } else {
            throw new UnsupportedOperationException(
                "completeConsultation() is only available for Consultant accounts.");
        }
    }

    // ======================== Debug ========================

    /**
     * Returns the underlying real user — use with caution.
     * Exposed for services that need direct access (e.g. DB persistence).
     */
    public User getRealUser() {
        return realUser;
    }

    @Override
    public String toString() {
        return String.format("UserProxy[real=%s, loggedIn=%s]",
            realUser.getUserID(), isLoggedIn);
    }
}
