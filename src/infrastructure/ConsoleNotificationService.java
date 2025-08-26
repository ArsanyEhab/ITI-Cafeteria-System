package infrastructure;

import contracts.INotificationService;

import java.sql.*;
import java.util.*;

import java.util.*;

public class ConsoleNotificationService implements INotificationService {
    private Connection con;
    private Map<String, List<String>> notifications = new HashMap<>();

    public ConsoleNotificationService() {
        con = DatabaseRepository.getConnection();
    }
        // Send a notification to a specific user
        public void sendNotification(String userId, String message) {
            notifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(message);
            System.out.println("Notification sent to " + userId + ": " + message);
        }

        // Retrieve all notifications for a user
        public List<String> getNotificationsFor(String userId) {
            return notifications.getOrDefault(userId, Collections.emptyList());
        }

        // Display notifications for a user
        public void displayNotifications(String userId) {
            List<String> userNotifications = getNotificationsFor(userId); 
            if (userNotifications.isEmpty()) {
                System.out.println("No notifications for " + userId);
            } else {
                System.out.println("Notifications for " + userId + ":");
                for (String note : userNotifications) {
                    System.out.println("- " + note);
                }
            }
        }

        // NOTIFICATION OPERATIONS
    // =================================================================

    public boolean addNotification(String studentId, String message) {
        String sql = "INSERT INTO notifications (student_id, message) VALUES (?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, message);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to add notification: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getStudentNotifications(String studentId) {
        List<Map<String, Object>> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE student_id = ? ORDER BY created_at DESC";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> notification = new HashMap<>();
                    notification.put("notification_id", rs.getInt("notification_id"));
                    notification.put("message", rs.getString("message"));
                    notification.put("created_at", rs.getTimestamp("created_at"));
                    notifications.add(notification);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch notifications: " + e.getMessage());
        }
        return notifications;
    }
    
    public boolean deleteNotification(int notificationId) {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, notificationId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete notification: " + e.getMessage());
            return false;
        }
    }
    
    public boolean clearAllStudentNotifications(String studentId) {
        String sql = "DELETE FROM notifications WHERE student_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Failed to clear notifications: " + e.getMessage());
            return false;
        }
    }
    
    public boolean broadcastNotification(List<String> studentIds, String message) {
        boolean success = true;
        for (String studentId : studentIds) {
            if (!addNotification(studentId, message)) {
                success = false;
            }
        }
        return success;
    }
    
    public boolean notifyAllStudents(String message) {
        String sql = "INSERT INTO notifications (student_id, message) SELECT student_id, ? FROM students";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, message);
            pstmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.err.println("❌ Failed to notify all students: " + e.getMessage());
            return false;
        }
    }


    }

