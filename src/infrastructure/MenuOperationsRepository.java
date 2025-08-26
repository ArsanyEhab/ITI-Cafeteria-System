package infrastructure;

import java.sql.*;
import java.util.*;

public class MenuOperationsRepository {
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
}
