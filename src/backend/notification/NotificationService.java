package backend.notification;

import backend.user.User;

// Simple notification service (Observer pattern extensible)
public class NotificationService {
    public static void sendEmail(User user, String message) {
        System.out.println("Email to " + user.getEmail() + ": " + message);
    }

    public static void sendSMS(String phone, String message) {
        System.out.println("SMS to " + phone + ": " + message);
    }
}
