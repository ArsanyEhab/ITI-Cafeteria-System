package infrastructure;

import contracts.IMenuAdmin;
import contracts.IMenuProvider;
import domain.MenuItem;
import CrossCutting.IdGenerator;

import java.sql.*;
import java.util.*;

/**
 * Unified Menu Operations Repository that handles both menu administration 
 * and menu data retrieval operations
 */
public class MenuOperationsRepository implements IMenuAdmin, IMenuProvider {
    private Connection con;

    public MenuOperationsRepository() {
        con = DatabaseRepository.getReliableConnection();
    }

    // ================= IMenuProvider Implementation =================
    
    @Override
    public List<MenuItem> getMenu() {
        List<MenuItem> menuItems = new ArrayList<>();
        
        // Always check connection status before using
        if (con == null || !DatabaseRepository.isConnectionValid()) {
            // Return some default menu items for demonstration (silent in offline mode)
            menuItems.add(new MenuItem(1, "Coffee", "Fresh brewed coffee", 25.0, "Beverages"));
            menuItems.add(new MenuItem(2, "Sandwich", "Grilled chicken sandwich", 45.0, "Main Course"));
            menuItems.add(new MenuItem(3, "Salad", "Fresh green salad", 30.0, "Healthy"));
            return menuItems;
        }
        
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
            // Return default items as fallback
            menuItems.add(new MenuItem(1, "Coffee", "Fresh brewed coffee", 25.0, "Beverages"));
            menuItems.add(new MenuItem(2, "Sandwich", "Grilled chicken sandwich", 45.0, "Main Course"));
        }
        return menuItems;
    }

    // Additional helper method for menu retrieval
    public List<MenuItem> getMenuByCategory(String category) {
        List<MenuItem> menuItems = new ArrayList<>();
        
        // Handle offline mode
        if (con == null || !DatabaseRepository.isConnectionValid()) {
            // Return filtered default items in offline mode
            // Filter by category if possible
            List<MenuItem> allItems = getMenu();
            return allItems.stream()
                .filter(item -> item.getCategory().equalsIgnoreCase(category))
                .collect(java.util.stream.Collectors.toList());
        }
        
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

    // ================= IMenuAdmin Implementation =================

    @Override
    public MenuItem addMenuItem(String name, String description, double price, String category) {
        // Handle offline mode
        if (con == null) {
            // Silent operation in offline mode
            return new MenuItem(0, name, description, price, category);
        }
        
        String sql = "INSERT INTO menu_items (name, description, price, category) VALUES (?, ?, ?, ?)";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setDouble(3, price);
            pstmt.setString(4, category);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                // Get the auto-generated ID
                try (ResultSet rs = pstmt.getGeneratedKeys()) {
                    if (rs.next()) {
                        int generatedId = rs.getInt(1);
                        MenuItem newItem = new MenuItem(generatedId, name, description, price, category);
                        System.out.println("✅ Menu item added successfully: " + name + " with ID: " + generatedId);
                        return newItem;
                    }
                }
                // Fallback if we can't get the generated key
                MenuItem newItem = new MenuItem(0, name, description, price, category);
                System.out.println("✅ Menu item added successfully: " + name + " (ID not retrieved)");
                return newItem;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to add menu item: " + e.getMessage());
            // Return item anyway for offline mode (silent)
            return new MenuItem(0, name, description, price, category);
        }
        return null;
    }

    @Override
    public void editMenuItem(int id, String name, String description, double price, String category) {
        // Handle offline mode
        if (con == null) {
            // Silent operation in offline mode
            return;
        }
        
        String sql = "UPDATE menu_items SET name = ?, description = ?, price = ?, category = ? WHERE menu_item_id = ?";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, name);
            pstmt.setString(2, description);
            pstmt.setDouble(3, price);
            pstmt.setString(4, category);
            pstmt.setInt(5, id);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Menu item updated successfully: " + name);
            } else {
                System.out.println("⚠️ No menu item found with ID: " + id);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to update menu item: " + e.getMessage());
        }
    }

    @Override
    public void removeMenuItem(int id) {
        // Handle offline mode
        if (con == null) {
            // Silent operation in offline mode
            return;
        }
        
        String sql = "DELETE FROM menu_items WHERE menu_item_id = ?";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, id);
            
            int rowsAffected = pstmt.executeUpdate();
            if (rowsAffected > 0) {
                System.out.println("✅ Menu item removed successfully");
            } else {
                System.out.println("⚠️ No menu item found with ID: " + id);
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to remove menu item: " + e.getMessage());
        }
    }

    // ================= Additional Utility Methods =================
    
    public MenuItem getMenuItemById(int id) {
        // Handle offline mode
        if (con == null || !DatabaseRepository.isConnectionValid()) {
            // Search in default items for offline mode
            return getMenu().stream()
                .filter(item -> item.getId() == id)
                .findFirst()
                .orElse(null);
        }
        
        String sql = "SELECT * FROM menu_items WHERE menu_item_id = ?";
        
        try (PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setInt(1, id);
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
            System.err.println("❌ Failed to fetch menu item by ID: " + e.getMessage());
        }
        return null;
    }
    
    public List<String> getAllCategories() {
        List<String> categories = new ArrayList<>();
        
        // Handle offline mode
        if (con == null || !DatabaseRepository.isConnectionValid()) {
            // Return default categories for offline mode
            categories.add("Beverages");
            categories.add("Main Course");
            categories.add("Healthy");
            return categories;
        }
        
        String sql = "SELECT DISTINCT category FROM menu_items ORDER BY category";
        
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
}
