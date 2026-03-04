package backend.user;

public class Client extends User {
    
    public Client(String name, String email, String password) {
        super(name, email, password);
    }

    public void browseServices(){

    }
    public void requestBooking(){

    }
    public void cancelBooking(){

    }
    public void viewBookingHistory(){

    }
    public void processPayment(){

    }
    public void managePaymentMethod(){

    }
    public void viewPaymentHistory(){

    }
    @Override
    public AccountType getAccountType() {
        return AccountType.Client;
    }

}
