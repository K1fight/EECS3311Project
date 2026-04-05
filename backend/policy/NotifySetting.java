package backend.policy;

import backend.user.User;

public interface NotifySetting {
    public void notifySetting();
    
    /**
     * Send notification to user
     * @param user The user to notify
     * @param message Notification message
     */
    default void sendNotification(User user, String message) {
        System.out.println("Notification sent to " + user.getName() + ": " + message);
    }
    
    /**
     * Enable email notifications
     */
    default void enableEmailNotifications() {
        System.out.println("Email notifications enabled");
    }
    
    /**
     * Disable email notifications
     */
    default void disableEmailNotifications() {
        System.out.println("Email notifications disabled");
    }
    
    /**
     * Check if notifications are enabled
     * @return true if enabled, false otherwise
     */
    default boolean areNotificationsEnabled() {
        return true;
    }
}
