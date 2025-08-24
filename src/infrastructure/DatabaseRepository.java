package infrastructure;
import domain.Order;
import domain.MenuItem;
import domain.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class DatabaseRepository {
    private Connection con;

    // JDBC connection details (example for Aiven MySQL)
    private static final String URL = "jdbc:mysql://cafeteria-system-cafeteria-system.k.aivencloud.com:14411/defaultdb?sslmode=require";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_3QRmLrF1K5jfZ_qfPsn";

    public DatabaseRepository() {
        try {
            // Optional: load JDBC driver (needed for older Java versions)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            this.con = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to the database!");

        } catch (ClassNotFoundException e) {
            System.err.println("❌ JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
    }

    // Getter to use the connection elsewhere
    public Connection getConnection() {
        return this.con;
    }
    
    // Close the connection
    public void closeConnection() {
        if (con != null) {
            try {
                con.close();
                System.out.println("✅ Database connection closed!");
            } catch (SQLException e) {
                System.err.println("❌ Error closing database connection: " + e.getMessage());
            }
        }
    }

    // STUDENT OPERATIONS
    // =================================================================
    
    public Student getStudentById(String studentId) {
        String sql = "SELECT * FROM students WHERE student_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Student student = new Student(
                        rs.getString("name"),
                        rs.getString("student_id"),
                        rs.getString("password")
                    );
                    student.setLoyaltyPoints(rs.getInt("loyalty_points"));
                    return student;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch student: " + e.getMessage());
        }
        return null;
    }

    public boolean addStudent(Student student) {
        String sql = "INSERT INTO students (student_id, name, password) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, student.getStudentID());
            pstmt.setString(2, student.getName());
            pstmt.setString(3, student.getPassword());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to add student: " + e.getMessage());
            return false;
        }
    }

    public boolean updateStudent(Student student) {
        String sql = "UPDATE students SET name = ?, password = ?, loyalty_points = ? WHERE student_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, student.getName());
            pstmt.setString(2, student.getPassword());
            pstmt.setInt(3, student.getLoyaltyPoints());
            pstmt.setString(4, student.getStudentID());
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to update student: " + e.getMessage());
            return false;
        }
    }

    public boolean deleteStudent(String studentId) {
        String sql = "DELETE FROM students WHERE student_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete student: " + e.getMessage());
            return false;
        }
    }

    public List<Student> getAllStudents() {
        List<Student> students = new ArrayList<>();
        String sql = "SELECT * FROM students";
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Student student = new Student(
                    rs.getString("name"),
                    rs.getString("student_id"),
                    rs.getString("password")
                );
                student.setLoyaltyPoints(rs.getInt("loyalty_points"));
                students.add(student);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch students: " + e.getMessage());
        }
        return students;
    }

    public boolean validateStudentCredentials(String studentId, String password) {
        String sql = "SELECT * FROM students WHERE student_id = ? AND password = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Login validation failed: " + e.getMessage());
            return false;
        }
    }
    
    // LOYALTY OPERATIONS
    // =================================================================
    
    public boolean updateLoyaltyPoints(String studentId, int points) {
        String sql = "UPDATE students SET loyalty_points = ? WHERE student_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, points);
            pstmt.setString(2, studentId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to update loyalty points: " + e.getMessage());
            return false;
        }
    }
    
    public boolean addLoyaltyPoints(String studentId, int pointsToAdd) {
        String sql = "UPDATE students SET loyalty_points = loyalty_points + ? WHERE student_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, pointsToAdd);
            pstmt.setString(2, studentId);
            int rowsAffected = pstmt.executeUpdate();
            
            if (rowsAffected > 0) {
                // Add loyalty transaction record
                addLoyaltyTransaction(studentId, pointsToAdd, "Points earned");
                return true;
            }
            return false;
        } catch (SQLException e) {
            System.err.println("❌ Failed to add loyalty points: " + e.getMessage());
            return false;
        }
    }

    public boolean addLoyaltyTransaction(String studentId, int pointsChanged, String description) {
        String sql = "INSERT INTO loyalty_transactions (student_id, points_changed, description) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setInt(2, pointsChanged);
            pstmt.setString(3, description);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to add loyalty transaction: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getLoyaltyTransactions(String studentId) {
        List<Map<String, Object>> transactions = new ArrayList<>();
        String sql = "SELECT * FROM loyalty_transactions WHERE student_id = ? ORDER BY created_at DESC";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> transaction = new HashMap<>();
                    transaction.put("transaction_id", rs.getInt("transaction_id"));
                    transaction.put("points_changed", rs.getInt("points_changed"));
                    transaction.put("description", rs.getString("description"));
                    transaction.put("created_at", rs.getTimestamp("created_at"));
                    transactions.add(transaction);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch loyalty transactions: " + e.getMessage());
        }
        return transactions;
    }
    
    // REWARDS OPERATIONS
    // =================================================================
    
    public List<Map<String, Object>> getAllRewards() {
        List<Map<String, Object>> rewards = new ArrayList<>();
        String sql = "SELECT * FROM rewards";
        
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> reward = new HashMap<>();
                reward.put("reward_id", rs.getInt("reward_id"));
                reward.put("reward_name", rs.getString("reward_name"));
                reward.put("points_required", rs.getInt("points_required"));
                rewards.add(reward);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch rewards: " + e.getMessage());
        }
        return rewards;
    }

    public boolean addReward(String rewardName, int pointsRequired) {
        String sql = "INSERT INTO rewards (reward_name, points_required) VALUES (?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, rewardName);
            pstmt.setInt(2, pointsRequired);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to add reward: " + e.getMessage());
            return false;
        }
    }
    
    public boolean updateReward(int rewardId, String rewardName, int pointsRequired) {
        String sql = "UPDATE rewards SET reward_name = ?, points_required = ? WHERE reward_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, rewardName);
            pstmt.setInt(2, pointsRequired);
            pstmt.setInt(3, rewardId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to update reward: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deleteReward(int rewardId) {
        String sql = "DELETE FROM rewards WHERE reward_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, rewardId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete reward: " + e.getMessage());
            return false;
        }
    }

    public boolean redeemReward(String studentId, int rewardId) {
        // First check if student has enough points
        String checkSql = "SELECT s.loyalty_points, r.points_required FROM students s, rewards r " +
                        "WHERE s.student_id = ? AND r.reward_id = ?";
                        
        try (PreparedStatement checkStmt = con.prepareStatement(checkSql)) {
            checkStmt.setString(1, studentId);
            checkStmt.setInt(2, rewardId);
            
            try (ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next()) {
                    int currentPoints = rs.getInt("loyalty_points");
                    int requiredPoints = rs.getInt("points_required");
                    
                    if (currentPoints < requiredPoints) {
                        System.out.println("⚠️ Not enough loyalty points to redeem this reward");
                        return false;
                    }
                    
                    // If enough points, proceed with redemption (in transaction)
                    con.setAutoCommit(false);
                    
                    // 1. Insert redemption record
                    String redeemSql = "INSERT INTO student_rewards (student_id, reward_id) VALUES (?, ?)";
                    try (PreparedStatement redeemStmt = con.prepareStatement(redeemSql)) {
                        redeemStmt.setString(1, studentId);
                        redeemStmt.setInt(2, rewardId);
                        redeemStmt.executeUpdate();
                    }
                    
                    // 2. Deduct points
                    String updateSql = "UPDATE students SET loyalty_points = loyalty_points - ? WHERE student_id = ?";
                    try (PreparedStatement updateStmt = con.prepareStatement(updateSql)) {
                        updateStmt.setInt(1, requiredPoints);
                        updateStmt.setString(2, studentId);
                        updateStmt.executeUpdate();
                    }
                    
                    // 3. Add transaction record
                    String txnSql = "INSERT INTO loyalty_transactions (student_id, points_changed, description) " +
                                  "VALUES (?, ?, ?)";
                    try (PreparedStatement txnStmt = con.prepareStatement(txnSql)) {
                        txnStmt.setString(1, studentId);
                        txnStmt.setInt(2, -requiredPoints); // Negative because points are spent
                        txnStmt.setString(3, "Redeemed reward: " + getRewardName(rewardId));
                        txnStmt.executeUpdate();
                    }
                    
                    con.commit();
                    con.setAutoCommit(true);
                    return true;
                }
            }
        } catch (SQLException e) {
            try {
                con.rollback();
                con.setAutoCommit(true);
            } catch (SQLException rollbackEx) {
                System.err.println("❌ Rollback failed: " + rollbackEx.getMessage());
            }
            System.err.println("❌ Failed to redeem reward: " + e.getMessage());
        }
        return false;
    }

    private String getRewardName(int rewardId) {
        String sql = "SELECT reward_name FROM rewards WHERE reward_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, rewardId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("reward_name");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get reward name: " + e.getMessage());
        }
        return "Unknown Reward";
    }
    
    public List<Map<String, Object>> getStudentRedeemedRewards(String studentId) {
        List<Map<String, Object>> redeemedRewards = new ArrayList<>();
        String sql = "SELECT sr.*, r.reward_name, r.points_required FROM student_rewards sr " +
                   "JOIN rewards r ON sr.reward_id = r.reward_id " +
                   "WHERE sr.student_id = ? ORDER BY sr.redeemed_at DESC";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> reward = new HashMap<>();
                    reward.put("reward_id", rs.getInt("reward_id"));
                    reward.put("reward_name", rs.getString("reward_name"));
                    reward.put("points_required", rs.getInt("points_required"));
                    reward.put("redeemed_at", rs.getTimestamp("redeemed_at"));
                    redeemedRewards.add(reward);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch redeemed rewards: " + e.getMessage());
        }
        return redeemedRewards;
    }
    
    // MENU OPERATIONS
    // =================================================================

    public List<MenuItem> getAllMenuItems() {
        List<MenuItem> menuItems = new ArrayList<>();
        String sql = "SELECT * FROM menu_items";
        
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                MenuItem item = new MenuItem(
                    rs.getInt("menu_item_id"),
                    rs.getString("name"),
                    rs.getString("description"),
                    rs.getDouble("price"),
                    rs.getString("category")
                );
                menuItems.add(item);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch menu items: " + e.getMessage());
        }
        return menuItems;
    }
    
    public List<MenuItem> getMenuItemsByCategory(String category) {
        List<MenuItem> menuItems = new ArrayList<>();
        String sql = "SELECT * FROM menu_items WHERE category = ?";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, category);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    MenuItem item = new MenuItem(
                        rs.getInt("menu_item_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getString("category")
                    );
                    menuItems.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch menu items by category: " + e.getMessage());
        }
        return menuItems;
    }

    public MenuItem getMenuItemById(int menuItemId) {
        String sql = "SELECT * FROM menu_items WHERE menu_item_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, menuItemId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return new MenuItem(
                        rs.getInt("menu_item_id"),
                        rs.getString("name"),
                        rs.getString("description"),
                        rs.getDouble("price"),
                        rs.getString("category")
                    );
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch menu item: " + e.getMessage());
        }
        return null;
    }
    
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM menu_items WHERE category IS NOT NULL AND category != ''";
        
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                categories.add(rs.getString("category"));
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch categories: " + e.getMessage());
        }
        return categories;
    }

    public boolean addMenuItem(MenuItem item, String staffId) {
        String sql = "INSERT INTO menu_items (menu_item_id, name, description, price, category) VALUES (?, ?, ?, ?, ?)";
        try {
            // Start transaction
            con.setAutoCommit(false);
            
            // Insert menu item
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, item.getId());
                pstmt.setString(2, item.getName());
                pstmt.setString(3, ""); // Description may not be available in MenuItem object
                pstmt.setDouble(4, item.getPrice());
                pstmt.setString(5, ""); // Category may not be available in MenuItem object
                pstmt.executeUpdate();
            }
            
            // Log the action
            logMenuAction(staffId, "ADD", item.getId());
            
            con.commit();
            return true;
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.err.println("❌ Rollback failed: " + ex.getMessage());
            }
            System.err.println("❌ Failed to add menu item: " + e.getMessage());
            return false;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("❌ Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    public boolean updateMenuItem(MenuItem item, String staffId) {
        String sql = "UPDATE menu_items SET name = ?, description = ?, price = ?, category = ? WHERE menu_item_id = ?";
        try {
            // Start transaction
            con.setAutoCommit(false);
            
            // Get current menu item to determine what changed
            MenuItem currentItem = getMenuItemById(item.getId());
            if (currentItem == null) {
                con.setAutoCommit(true);
                return false;
            }
            
            // Update menu item
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, item.getName());
                pstmt.setString(2, ""); // Description may not be available
                pstmt.setDouble(3, item.getPrice());
                pstmt.setString(4, ""); // Category may not be available
                pstmt.setInt(5, item.getId());
                pstmt.executeUpdate();
            }
            
            // Log the action
            logMenuAction(staffId, "UPDATE", item.getId());
            
            con.commit();
            return true;
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.err.println("❌ Rollback failed: " + ex.getMessage());
            }
            System.err.println("❌ Failed to update menu item: " + e.getMessage());
            return false;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("❌ Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    public boolean deleteMenuItem(int menuItemId, String staffId) {
        String sql = "DELETE FROM menu_items WHERE menu_item_id = ?";
        try {
            // Start transaction
            con.setAutoCommit(false);
            
            // Delete menu item
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, menuItemId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected == 0) {
                    con.rollback();
                    con.setAutoCommit(true);
                    return false;
                }
            }
            
            // Log the action
            logMenuAction(staffId, "DELETE", menuItemId);
            
            con.commit();
            return true;
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.err.println("❌ Rollback failed: " + ex.getMessage());
            }
            System.err.println("❌ Failed to delete menu item: " + e.getMessage());
            return false;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("❌ Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    private void logMenuAction(String staffId, String action, int menuItemId) {
        String sql = "INSERT INTO menu_logs (staff_id, action, menu_item_id) VALUES (?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            pstmt.setString(2, action);
            pstmt.setInt(3, menuItemId);
            pstmt.executeUpdate();
        } catch (SQLException e) {
            System.err.println("❌ Failed to log menu action: " + e.getMessage());
        }
    }
    
    public List<Map<String, Object>> getMenuLogs() {
        List<Map<String, Object>> logs = new ArrayList<>();
        String sql = "SELECT ml.*, s.name as staff_name, mi.name as menu_item_name " +
                    "FROM menu_logs ml " +
                    "JOIN staff s ON ml.staff_id = s.staff_id " +
                    "JOIN menu_items mi ON ml.menu_item_id = mi.menu_item_id " +
                    "ORDER BY ml.log_time DESC";
        
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> log = new HashMap<>();
                log.put("log_id", rs.getInt("log_id"));
                log.put("staff_id", rs.getString("staff_id"));
                log.put("staff_name", rs.getString("staff_name"));
                log.put("action", rs.getString("action"));
                log.put("menu_item_id", rs.getInt("menu_item_id"));
                log.put("menu_item_name", rs.getString("menu_item_name"));
                log.put("log_time", rs.getTimestamp("log_time"));
                logs.add(log);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch menu logs: " + e.getMessage());
        }
        return logs;
    }
    
    // STAFF OPERATIONS
    // =================================================================
    
    public Map<String, Object> getStaffById(String staffId) {
        String sql = "SELECT * FROM staff WHERE staff_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    Map<String, Object> staff = new HashMap<>();
                    staff.put("staff_id", rs.getString("staff_id"));
                    staff.put("name", rs.getString("name"));
                    staff.put("role", rs.getString("role"));
                    return staff;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch staff member: " + e.getMessage());
        }
        return null;
    }
    
    public boolean validateStaffCredentials(String staffId, String password) {
        String sql = "SELECT * FROM staff WHERE staff_id = ? AND password = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            pstmt.setString(2, password);
            try (ResultSet rs = pstmt.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            System.err.println("❌ Staff login validation failed: " + e.getMessage());
            return false;
        }
    }

    public String getStaffRole(String staffId) {
        String sql = "SELECT role FROM staff WHERE staff_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getString("role");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get staff role: " + e.getMessage());
        }
        return null;
    }

    public List<Map<String, Object>> getAllStaff() {
        List<Map<String, Object>> staffList = new ArrayList<>();
        String sql = "SELECT staff_id, name, role FROM staff";
        
        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Map<String, Object> staff = new HashMap<>();
                staff.put("staff_id", rs.getString("staff_id"));
                staff.put("name", rs.getString("name"));
                staff.put("role", rs.getString("role"));
                staffList.add(staff);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch staff list: " + e.getMessage());
        }
        return staffList;
    }

    public boolean addStaffMember(String staffId, String name, String password, String role) {
        String sql = "INSERT INTO staff (staff_id, name, password, role) VALUES (?, ?, ?, ?)";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            pstmt.setString(2, name);
            pstmt.setString(3, password);
            pstmt.setString(4, role);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to add staff member: " + e.getMessage());
            return false;
        }
    }
    
    public boolean updateStaffMember(String staffId, String name, String password, String role) {
        StringBuilder sqlBuilder = new StringBuilder("UPDATE staff SET ");
        List<Object> params = new ArrayList<>();
        boolean hasUpdates = false;
        
        if (name != null && !name.isEmpty()) {
            sqlBuilder.append("name = ?");
            params.add(name);
            hasUpdates = true;
        }
        
        if (password != null && !password.isEmpty()) {
            if (hasUpdates) {
                sqlBuilder.append(", ");
            }
            sqlBuilder.append("password = ?");
            params.add(password);
            hasUpdates = true;
        }
        
        if (role != null && !role.isEmpty()) {
            if (hasUpdates) {
                sqlBuilder.append(", ");
            }
            sqlBuilder.append("role = ?");
            params.add(role);
            hasUpdates = true;
        }
        
        if (!hasUpdates) {
            return false; // Nothing to update
        }
        
        sqlBuilder.append(" WHERE staff_id = ?");
        params.add(staffId);
        
        try (PreparedStatement pstmt = con.prepareStatement(sqlBuilder.toString())) {
            for (int i = 0; i < params.size(); i++) {
                pstmt.setObject(i + 1, params.get(i));
            }
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to update staff member: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deleteStaffMember(String staffId) {
        String sql = "DELETE FROM staff WHERE staff_id = ?";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, staffId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete staff member: " + e.getMessage());
            return false;
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
    
    // ORDER OPERATIONS
    // =================================================================

    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, s.name as student_name FROM orders o JOIN students s ON o.student_id = s.student_id";

        try (Statement stmt = con.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                // Create student object
                Student student = new Student(
                    rs.getString("student_name"),
                    rs.getString("student_id"),
                    "" // Password not needed
                );
                
                // Create order object
                Order order = new Order(
                    rs.getInt("order_id"),
                    Integer.parseInt(rs.getString("student_id")),
                    rs.getDouble("total_cost"),
                    rs.getDouble("discount_applied"),
                    rs.getString("status"),
                    student,
                    rs.getTimestamp("order_date")
                );
                
                // Add order items
                getOrderItems(order.getOrderID()).forEach(order::addItem);
                
                orders.add(order);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch orders: " + e.getMessage());
        }
        return orders;
    }
    
    public Order getOrderById(int orderId) {
        String sql = "SELECT o.*, s.name as student_name FROM orders o JOIN students s ON o.student_id = s.student_id WHERE o.order_id = ?";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                if (rs.next()) {
                    // Create student object
                    Student student = new Student(
                        rs.getString("student_name"),
                        rs.getString("student_id"),
                        "" // Password not needed
                    );
                    
                    // Create order object
                    Order order = new Order(
                        rs.getInt("order_id"),
                        Integer.parseInt(rs.getString("student_id")),
                        rs.getDouble("total_cost"),
                        rs.getDouble("discount_applied"),
                        rs.getString("status"),
                        student,
                        rs.getTimestamp("order_date")
                    );
                    
                    // Add order items
                    getOrderItems(order.getOrderID()).forEach(order::addItem);
                    
                    return order;
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch order: " + e.getMessage());
        }
        return null;
    }

    public List<Order> getStudentOrders(String studentId) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.* FROM orders o WHERE o.student_id = ? ORDER BY o.order_date DESC";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student student = getStudentById(studentId);
                    
                    Order order = new Order(
                        rs.getInt("order_id"),
                        Integer.parseInt(rs.getString("student_id")),
                        rs.getDouble("total_cost"),
                        rs.getDouble("discount_applied"),
                        rs.getString("status"),
                        student,
                        rs.getTimestamp("order_date")
                    );
                    
                    // Add order items
                    getOrderItems(order.getOrderID()).forEach(order::addItem);
                    
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch student orders: " + e.getMessage());
        }
        return orders;
    }
    
    public List<Order> getOrdersByStatus(String status) {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, s.name as student_name FROM orders o JOIN students s ON o.student_id = s.student_id WHERE o.status = ?";

        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, status);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Create student object
                    Student student = new Student(
                        rs.getString("student_name"),
                        rs.getString("student_id"),
                        "" // Password not needed
                    );
                    
                    // Create order object
                    Order order = new Order(
                        rs.getInt("order_id"),
                        Integer.parseInt(rs.getString("student_id")),
                        rs.getDouble("total_cost"),
                        rs.getDouble("discount_applied"),
                        rs.getString("status"),
                        student,
                        rs.getTimestamp("order_date")
                    );
                    
                    // Add order items
                    getOrderItems(order.getOrderID()).forEach(order::addItem);
                    
                    orders.add(order);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch orders by status: " + e.getMessage());
        }
        return orders;
    }

    private List<MenuItem> getOrderItems(int orderId) {
        List<MenuItem> items = new ArrayList<>();
        String sql = "SELECT m.*, oi.quantity FROM order_items oi JOIN menu_items m ON oi.menu_item_id = m.menu_item_id WHERE oi.order_id = ?";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, orderId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    // Add each menu item according to quantity
                    for (int i = 0; i < rs.getInt("quantity"); i++) {
                        MenuItem item = new MenuItem(
                            rs.getInt("menu_item_id"),
                            rs.getString("name"),
                            rs.getString("description"),
                            rs.getDouble("price"),
                            rs.getString("category")
                        );
                        items.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch order items: " + e.getMessage());
        }
        return items;
    }

    public boolean placeOrder(Order order) {
        try {
            // Start transaction
            con.setAutoCommit(false);
            
            // 1. Insert order record
            String orderSql = "INSERT INTO orders (order_id, student_id, total_cost, discount_applied, status) " +
                            "VALUES (?, ?, ?, ?, ?)";
            
            try (PreparedStatement orderStmt = con.prepareStatement(orderSql)) {
                orderStmt.setInt(1, order.getOrderID());
                orderStmt.setString(2, order.getStudent().getStudentID());
                orderStmt.setDouble(3, order.getTotalCost());
                orderStmt.setDouble(4, 0); // Default discount
                orderStmt.setString(5, "PENDING");
                orderStmt.executeUpdate();
            }
            
            // 2. Insert order items
            String itemSql = "INSERT INTO order_items (order_id, menu_item_id, quantity) VALUES (?, ?, ?)";
            
            // Count item frequencies
            Map<Integer, Integer> itemCounts = new HashMap<>();
            for (MenuItem item : order.getItems()) {
                itemCounts.put(item.getId(), itemCounts.getOrDefault(item.getId(), 0) + 1);
            }
            
            // Insert each unique item with its quantity
            for (Map.Entry<Integer, Integer> entry : itemCounts.entrySet()) {
                try (PreparedStatement itemStmt = con.prepareStatement(itemSql)) {
                    itemStmt.setInt(1, order.getOrderID());
                    itemStmt.setInt(2, entry.getKey());
                    itemStmt.setInt(3, entry.getValue());
                    itemStmt.executeUpdate();
                }
            }
            
            // 3. Create a notification for the student
            String notificationSql = "INSERT INTO notifications (student_id, message) VALUES (?, ?)";
            try (PreparedStatement notificationStmt = con.prepareStatement(notificationSql)) {
                notificationStmt.setString(1, order.getStudent().getStudentID());
                notificationStmt.setString(2, "Your order #" + order.getOrderID() + " has been placed successfully.");
                notificationStmt.executeUpdate();
            }
            
            con.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.err.println("❌ Rollback failed: " + ex.getMessage());
            }
            System.err.println("❌ Failed to place order: " + e.getMessage());
            return false;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("❌ Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    public boolean updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try {
            // Start transaction
            con.setAutoCommit(false);
            
            // 1. Update order status
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, status);
                pstmt.setInt(2, orderId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected == 0) {
                    con.rollback();
                    con.setAutoCommit(true);
                    return false;
                }
            }
            
            // 2. Get student ID for notification
            String studentId = "";
            String getStudentSql = "SELECT student_id FROM orders WHERE order_id = ?";
            try (PreparedStatement getStudentStmt = con.prepareStatement(getStudentSql)) {
                getStudentStmt.setInt(1, orderId);
                try (ResultSet rs = getStudentStmt.executeQuery()) {
                    if (rs.next()) {
                        studentId = rs.getString("student_id");
                    }
                }
            }
            
            // 3. Create a notification for the student
            if (!studentId.isEmpty()) {
                String notificationSql = "INSERT INTO notifications (student_id, message) VALUES (?, ?)";
                try (PreparedStatement notificationStmt = con.prepareStatement(notificationSql)) {
                    notificationStmt.setString(1, studentId);
                    notificationStmt.setString(2, "Your order #" + orderId + " status has been updated to " + status);
                    notificationStmt.executeUpdate();
                }
            }
            
            // 4. If status is "COMPLETED", add loyalty points
            if ("COMPLETED".equalsIgnoreCase(status)) {
                String getOrderTotalSql = "SELECT total_cost FROM orders WHERE order_id = ?";
                try (PreparedStatement getOrderTotalStmt = con.prepareStatement(getOrderTotalSql)) {
                    getOrderTotalStmt.setInt(1, orderId);
                    try (ResultSet rs = getOrderTotalStmt.executeQuery()) {
                        if (rs.next()) {
                            double totalCost = rs.getDouble("total_cost");
                            // Add loyalty points (1 point per 10 units of currency spent)
                            int pointsToAdd = (int)(totalCost / 10);
                            if (pointsToAdd > 0 && !studentId.isEmpty()) {
                                String addPointsSql = "UPDATE students SET loyalty_points = loyalty_points + ? WHERE student_id = ?";
                                try (PreparedStatement addPointsStmt = con.prepareStatement(addPointsSql)) {
                                    addPointsStmt.setInt(1, pointsToAdd);
                                    addPointsStmt.setString(2, studentId);
                                    addPointsStmt.executeUpdate();
                                }
                                
                                // Record loyalty transaction
                                String loyaltyTxnSql = "INSERT INTO loyalty_transactions (student_id, points_changed, description) VALUES (?, ?, ?)";
                                try (PreparedStatement loyaltyTxnStmt = con.prepareStatement(loyaltyTxnSql)) {
                                    loyaltyTxnStmt.setString(1, studentId);
                                    loyaltyTxnStmt.setInt(2, pointsToAdd);
                                    loyaltyTxnStmt.setString(3, "Points earned from order #" + orderId);
                                    loyaltyTxnStmt.executeUpdate();
                                }
                                
                                // Notify student about earned points
                                String pointsNotificationSql = "INSERT INTO notifications (student_id, message) VALUES (?, ?)";
                                try (PreparedStatement pointsNotificationStmt = con.prepareStatement(pointsNotificationSql)) {
                                    pointsNotificationStmt.setString(1, studentId);
                                    pointsNotificationStmt.setString(2, "You earned " + pointsToAdd + " loyalty points from your order #" + orderId);
                                    pointsNotificationStmt.executeUpdate();
                                }
                            }
                        }
                    }
                }
            }
            
            con.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.err.println("❌ Rollback failed: " + ex.getMessage());
            }
            System.err.println("❌ Failed to update order status: " + e.getMessage());
            return false;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("❌ Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }

    public boolean applyDiscountToOrder(int orderId, double discountAmount) {
        String sql = "UPDATE orders SET discount_applied = ?, total_cost = total_cost - ? WHERE order_id = ? AND status = 'PENDING'";
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setDouble(1, discountAmount);
            pstmt.setDouble(2, discountAmount);
            pstmt.setInt(3, orderId);
            int rowsAffected = pstmt.executeUpdate();
            return rowsAffected > 0;
        } catch (SQLException e) {
            System.err.println("❌ Failed to apply discount: " + e.getMessage());
            return false;
        }
    }
    
    public boolean deleteOrder(int orderId) {
        try {
            // Start transaction
            con.setAutoCommit(false);
            
            // 1. Delete order items first (due to foreign key constraint)
            String deleteItemsSql = "DELETE FROM order_items WHERE order_id = ?";
            try (PreparedStatement deleteItemsStmt = con.prepareStatement(deleteItemsSql)) {
                deleteItemsStmt.setInt(1, orderId);
                deleteItemsStmt.executeUpdate();
            }
            
            // 2. Delete the order
            String deleteOrderSql = "DELETE FROM orders WHERE order_id = ?";
            try (PreparedStatement deleteOrderStmt = con.prepareStatement(deleteOrderSql)) {
                deleteOrderStmt.setInt(1, orderId);
                int rowsAffected = deleteOrderStmt.executeUpdate();
                if (rowsAffected == 0) {
                    con.rollback();
                    con.setAutoCommit(true);
                    return false;
                }
            }
            
            con.commit();
            return true;
            
        } catch (SQLException e) {
            try {
                con.rollback();
            } catch (SQLException ex) {
                System.err.println("❌ Rollback failed: " + ex.getMessage());
            }
            System.err.println("❌ Failed to delete order: " + e.getMessage());
            return false;
        } finally {
            try {
                con.setAutoCommit(true);
            } catch (SQLException e) {
                System.err.println("❌ Failed to reset auto-commit: " + e.getMessage());
            }
        }
    }
    
    public List<Map<String, Object>> getOrderStatistics(String startDate, String endDate) {
        List<Map<String, Object>> statistics = new ArrayList<>();
        String sql = "SELECT DATE(order_date) as date, COUNT(*) as order_count, SUM(total_cost) as total_revenue " +
                   "FROM orders " +
                   "WHERE order_date BETWEEN ? AND ? " +
                   "GROUP BY DATE(order_date) " +
                   "ORDER BY date";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, startDate);
            pstmt.setString(2, endDate);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> stat = new HashMap<>();
                    stat.put("date", rs.getDate("date"));
                    stat.put("order_count", rs.getInt("order_count"));
                    stat.put("total_revenue", rs.getDouble("total_revenue"));
                    statistics.add(stat);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get order statistics: " + e.getMessage());
        }
        return statistics;
    }
    
    public Map<String, Object> getTopSellingItems(int limit) {
        Map<String, Object> result = new HashMap<>();
        List<Map<String, Object>> items = new ArrayList<>();
        
        String sql = "SELECT m.menu_item_id, m.name, m.category, SUM(oi.quantity) as total_quantity " +
                   "FROM order_items oi " +
                   "JOIN menu_items m ON oi.menu_item_id = m.menu_item_id " +
                   "GROUP BY m.menu_item_id, m.name, m.category " +
                   "ORDER BY total_quantity DESC " +
                   "LIMIT ?";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, limit);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Map<String, Object> item = new HashMap<>();
                    item.put("menu_item_id", rs.getInt("menu_item_id"));
                    item.put("name", rs.getString("name"));
                    item.put("category", rs.getString("category"));
                    item.put("total_quantity", rs.getInt("total_quantity"));
                    items.add(item);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get top selling items: " + e.getMessage());
        }
        
        result.put("top_items", items);
        return result;
    }
}
