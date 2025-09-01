package infrastructure;

import contracts.IMenuAdmin;
import contracts.IMenuProvider;
import domain.MenuItem;
import CrossCutting.IdGenerator;

import java.sql.*;
import java.util.*;

/**
 * Unified Menu Operations Repository that handles both menu administration 
 * and menu data retrieval operations with optimized connection pooling
 */
public class MenuOperationsRepository implements IMenuAdmin, IMenuProvider {

    private String currentStaffId; // For logging purposes

    public MenuOperationsRepository() {
        // No need to store connection - use pool for each operation
    }
    
    public MenuOperationsRepository(String staffId) {
        this.currentStaffId = staffId;
    }
    
    public void setCurrentStaffId(String staffId) {
        this.currentStaffId = staffId;
    }
    
    /**
     * Get all unique categories from the menu
     */
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM menu_items ORDER BY category";
        Connection con = null;
        
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return categories;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                while (rs.next()) {
                    categories.add(rs.getString("category"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get categories: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        
        return categories;
    }
    
    /**
     * Log menu changes to the menu_logs table
     */
    private void logMenuChange(String action, int menuItemId) {
        if (currentStaffId == null) {
            System.err.println("⚠️ Warning: No staff ID set for menu logging");
            return;
        }
        
        String sql = "INSERT INTO menu_logs (staff_id, action, menu_item_id, log_time) VALUES (?, ?, ?, NOW())";
        Connection con = null;
        
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, currentStaffId);
                pstmt.setString(2, action);
                pstmt.setInt(3, menuItemId);
                
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("📝 Menu change logged: " + action + " for item ID: " + menuItemId);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to log menu change: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
    }

    // ================= IMenuProvider Implementation =================
    
    @Override
    public List<MenuItem> getMenu() {
        List<MenuItem> menuItems = new ArrayList<>();
        
        // No need to check connection status - we'll create connection when needed
        
        String sql = "SELECT * FROM menu_items";
        Connection con = null;
        
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) {
                // Fallback to default items
                return getDefaultMenuItems();
            }
            
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
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch menu items: " + e.getMessage());
            // Return default items as fallback
            return getDefaultMenuItems();
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return menuItems;
    }

    // Additional helper method for menu retrieval
    public List<MenuItem> getMenuByCategory(String category) {
        List<MenuItem> menuItems = new ArrayList<>();
        
        // No need to check connection status - we'll create connection when needed
        
        String sql = "SELECT * FROM menu_items WHERE category = ?";
        Connection con = null;
        
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return menuItems;
            
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
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch menu items by category: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return menuItems;
    }

    // ================= IMenuAdmin Implementation =================

    @Override
    public MenuItem addMenuItem(String name, String description, double price, String category) {
        if (name == null || name.trim().isEmpty()) {
            System.err.println("❌ Menu item name cannot be empty");
            return null;
        }
        
        if (price < 0) {
            System.err.println("❌ Menu item price cannot be negative");
            return null;
        }
        
        if (category == null || category.trim().isEmpty()) {
            System.err.println("❌ Menu item category cannot be empty");
            return null;
        }
        
        String sql = "INSERT INTO menu_items (name, description, price, category) VALUES (?, ?, ?, ?)";
        Connection con = null;
        
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return null;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
                pstmt.setString(1, name.trim());
                pstmt.setString(2, description != null ? description.trim() : "");
                pstmt.setDouble(3, price);
                pstmt.setString(4, category.trim());
                
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    // Get the auto-generated ID
                    try (ResultSet rs = pstmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            int menuItemId = rs.getInt(1);
                            MenuItem newItem = new MenuItem(menuItemId, name.trim(), 
                                description != null ? description.trim() : "", price, category.trim());
                            System.out.println("✅ Menu item added successfully: " + name + " with ID: " + menuItemId);
                            
                            // Log the menu addition
                            logMenuChange("ADD", menuItemId);
                            
                            return newItem;
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to add menu item: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return null;
    }

    @Override
    public void editMenuItem(int id, String name, String description, double price, String category) {
        if (name == null || name.trim().isEmpty()) {
            System.err.println("❌ Menu item name cannot be empty");
            return;
        }
        
        if (price < 0) {
            System.err.println("❌ Menu item price cannot be negative");
            return;
        }
        
        if (category == null || category.trim().isEmpty()) {
            System.err.println("❌ Menu item category cannot be empty");
            return;
        }
        
        String sql = "UPDATE menu_items SET name = ?, description = ?, price = ?, category = ? WHERE menu_item_id = ?";
        Connection con = null;
        
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, name.trim());
                pstmt.setString(2, description != null ? description.trim() : "");
                pstmt.setDouble(3, price);
                pstmt.setString(4, category.trim());
                pstmt.setInt(5, id);
                
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("✅ Menu item updated successfully: " + name);
                    
                    // Log the menu edit
                    logMenuChange("EDIT", id);
                } else {
                    System.err.println("❌ Menu item not found with ID: " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to update menu item: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
    }

    @Override
    public void removeMenuItem(int id) {
        String sql = "DELETE FROM menu_items WHERE menu_item_id = ?";
        Connection con = null;
        
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, id);
                
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    System.out.println("✅ Menu item deleted successfully with ID: " + id);
                    
                    // Log the menu removal
                    logMenuChange("DELETE", id);
                } else {
                    System.err.println("❌ Menu item not found with ID: " + id);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to delete menu item: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
    }

    // Additional helper methods
    public MenuItem getMenuItemById(int menuItemId) {
        String sql = "SELECT * FROM menu_items WHERE menu_item_id = ?";
        Connection con = null;
        
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return null;
            
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
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch menu item by ID: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return null;
    }

    public List<String> getMenuCategories() {
        List<String> categories = new ArrayList<>();
        String sql = "SELECT DISTINCT category FROM menu_items ORDER BY category";
        Connection con = null;
        
        try {
            con = DatabaseRepository.getConnection();
            if (con == null) return categories;
            
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                
                while (rs.next()) {
                    String category = rs.getString("category");
                    if (category != null && !category.trim().isEmpty()) {
                        categories.add(category.trim());
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to fetch menu categories: " + e.getMessage());
        } finally {
            DatabaseRepository.returnConnection(con);
        }
        return categories;
    }

    /**
     * Get default menu items for offline mode
     */
    private List<MenuItem> getDefaultMenuItems() {
        List<MenuItem> defaultItems = new ArrayList<>();
        defaultItems.add(new MenuItem(1, "Coffee", "Fresh brewed coffee", 25.0, "Beverages"));
        defaultItems.add(new MenuItem(2, "Sandwich", "Grilled chicken sandwich", 45.0, "Main Course"));
        defaultItems.add(new MenuItem(3, "Salad", "Fresh green salad", 30.0, "Healthy"));
        return defaultItems;
    }
}
