package infrastructure;

import contracts.INotificationService;

import java.sql.*;
import java.util.*;

import java.util.*;

public class ConsoleNotificationService implements INotificationService {
    private Map<String, List<String>> notifications = new HashMap<>();
    
    // ✅ Track last notification count per student for new notification detection
    private Map<String, Integer> lastNotificationCounts = new HashMap<>();

    public ConsoleNotificationService() { }
        // Send a notification to a specific user
        public void sendNotification(String userId, String message) {
            // Store in memory for immediate access
            notifications.computeIfAbsent(userId, k -> new ArrayList<>()).add(message);
            
            // ✅ Store in database for persistence
            addNotification(userId, message);
        }

        // Retrieve all notifications for a user
        public List<String> getNotificationsFor(String userId) {
            return notifications.getOrDefault(userId, Collections.emptyList());
        }

        // Display notifications for a user
        public void displayNotifications(String userId) {
            // Silent operation - no console output
        }

        // NOTIFICATION OPERATIONS
    // =================================================================

    public boolean addNotification(String studentId, String message) {
        String sql = "INSERT INTO notifications (student_id, message) VALUES (?, ?)";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                pstmt.setString(2, message);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to add notification: " + e.getMessage());
            return false;
        } finally {
            DatabaseRepository.returnConnection(con);
        }
    }

    public List<Map<String, Object>> getStudentNotifications(String studentId) {
        List<Map<String, Object>> notifications = new ArrayList<>();
        String sql = "SELECT * FROM notifications WHERE student_id = ? ORDER BY created_at DESC";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return notifications;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> notification = new HashMap<>();
                        notification.put("notification_id", rs.getInt("notification_id"));
                        notification.put("message", rs.getString("message"));
                        notification.put("created_at", rs.getTimestamp("created_at"));
                        
                        // ✅ Handle missing is_read column gracefully
                        try {
                            notification.put("is_read", rs.getBoolean("is_read"));
                        } catch (SQLException e) {
                            // If is_read column doesn't exist, default to false (unread)
                            notification.put("is_read", false);
                        }
                        
                        notifications.add(notification);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch notifications: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return notifications;
    }
    
    public boolean deleteNotification(int notificationId) {
        String sql = "DELETE FROM notifications WHERE notification_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, notificationId);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete notification: " + e.getMessage());
            return false;
        } finally {
            DatabaseRepository.returnConnection(con);
        }
    }
    
    public boolean clearAllStudentNotifications(String studentId) {
        String sql = "DELETE FROM notifications WHERE student_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to clear notifications: " + e.getMessage());
            return false;
        } finally {
            DatabaseRepository.returnConnection(con);
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
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, message);
                pstmt.executeUpdate();
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to notify all students: " + e.getMessage());
            return false;
        } finally {
            DatabaseRepository.returnConnection(con);
        }
    }
    
    /**
     * ✅ Mark notification as read
     */
    public boolean markNotificationAsRead(int notificationId) {
        // ✅ Check if is_read column exists first
        if (!columnExists("notifications", "is_read")) {
            // If is_read column doesn't exist, just return success (no-op)
            return true;
        }
        
        String sql = "UPDATE notifications SET is_read = 1 WHERE notification_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, notificationId);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to mark notification as read: " + e.getMessage());
            return false;
        } finally {
            DatabaseRepository.returnConnection(con);
        }
    }
    
    /**
     * ✅ Get unread notifications count for a student
     */
    public int getUnreadNotificationsCount(String studentId) {
        // ✅ Check if is_read column exists first
        if (!columnExists("notifications", "is_read")) {
            // If is_read column doesn't exist, return total count (all notifications are unread)
            return getTotalNotificationsCount(studentId);
        }
        
        String sql = "SELECT COUNT(*) FROM notifications WHERE student_id = ? AND (is_read = 0 OR is_read IS NULL)";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return 0;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get unread notifications count: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return 0;
    }
    
    /**
     * ✅ Get total notifications count for a student
     */
    private int getTotalNotificationsCount(String studentId) {
        String sql = "SELECT COUNT(*) FROM notifications WHERE student_id = ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return 0;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get total notifications count: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return 0;
    }
    
    /**
     * ✅ Check if a column exists in a table
     */
    private boolean columnExists(String tableName, String columnName) {
        String sql = "SHOW COLUMNS FROM " + tableName + " LIKE ?";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return false;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, columnName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    return rs.next(); // Returns true if column exists
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to check if column exists: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return false;
    }
    
    /**
     * ✅ Check if there are new notifications for a student (SIMPLIFIED)
     * @param studentId The student ID to check
     * @return true if new notifications detected, false otherwise
     */
    public boolean hasNewNotifications(String studentId) {
        int currentCount = getTotalNotificationsCount(studentId);
        int lastCount = lastNotificationCounts.getOrDefault(studentId, 0);
        
        // Return true if there are more notifications than before
        return currentCount > lastCount;
    }
    
    /**
     * ✅ Get the count of new notifications for a student (SIMPLIFIED)
     * @param studentId The student ID to check
     * @return Number of new notifications
     */
    public int getNewNotificationsCount(String studentId) {
        int currentCount = getTotalNotificationsCount(studentId);
        int lastCount = lastNotificationCounts.getOrDefault(studentId, 0);
        
        // Return the difference
        return Math.max(0, currentCount - lastCount);
    }
    
    /**
     * ✅ Update the last count AFTER checking (your suggested approach)
     * @param studentId The student ID to update
     */
    public void updateLastNotificationCount(String studentId) {
        int currentCount = getTotalNotificationsCount(studentId);
        lastNotificationCounts.put(studentId, currentCount);
    }
    
    /**
     * ✅ Get the latest notification message for a student
     * @param studentId The student ID to get the latest notification for
     * @return The latest notification message, or null if no notifications
     */
    public String getLatestNotificationMessage(String studentId) {
        String sql = "SELECT message FROM notifications WHERE student_id = ? ORDER BY created_at DESC LIMIT 1";
        Connection con = null;
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return null;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getString("message");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get latest notification: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return null;
    }
    
    /**
     * ✅ Initialize notification count for a student (call this when student logs in)
     * @param studentId The student ID to initialize
     */
    public void initializeNotificationCount(String studentId) {
        int currentCount = getTotalNotificationsCount(studentId);
        lastNotificationCounts.put(studentId, currentCount);
    }
}

