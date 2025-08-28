package infrastructure;
import contracts.IOrderRepository;
import domain.MenuItem;
import domain.Order;
import domain.Student;

import java.sql.*;
import java.util.*;

public class DatabaseOrderRepository implements IOrderRepository {

    public DatabaseOrderRepository() { }
    // ORDER OPERATIONS
    // =================================================================

    @Override
    public List<Order> getAllOrders() {
        List<Order> orders = new ArrayList<>();
        String sql = "SELECT o.*, s.name as student_name FROM orders o JOIN students s ON o.student_id = s.student_id";

        try (Connection con = DatabaseRepository.createNewConnection();
             Statement stmt = con != null ? con.createStatement() : null;
             ResultSet rs = stmt != null ? stmt.executeQuery(sql) : null) {
            if (stmt == null || rs == null) return orders;

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

    public List<Order> getStudentOrders(String studentId) {
        List<Order> orders = new ArrayList<>();
        infrastructure.InMemoryUserRepository db = new infrastructure.InMemoryUserRepository();
        String sql = "SELECT o.* FROM orders o WHERE o.student_id = ? ORDER BY o.order_date DESC";

        try (Connection con = DatabaseRepository.createNewConnection();
             PreparedStatement pstmt = con != null ? con.prepareStatement(sql) : null) {
            if (pstmt == null) return orders;
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    Student std = db.findById(studentId);

                    Order order = new Order(
                            rs.getInt("order_id"),
                            Integer.parseInt(rs.getString("student_id")),
                            rs.getDouble("total_cost"),
                            rs.getDouble("discount_applied"),
                            rs.getString("status"),
                            std,
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

        try (Connection con = DatabaseRepository.createNewConnection();
             PreparedStatement pstmt = con != null ? con.prepareStatement(sql) : null) {
            if (pstmt == null) return orders;
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

        try (Connection con = DatabaseRepository.createNewConnection();
             PreparedStatement pstmt = con != null ? con.prepareStatement(sql) : null) {
            if (pstmt == null) return items;
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

    @Override
    public boolean placeOrder(Order order) {
        try (Connection con = DatabaseRepository.createNewConnection()) {
            if (con == null) return false;
            con.setAutoCommit(false);

            // 1. Insert order record
            String orderSql = "INSERT INTO orders (order_id, student_id, total_cost, discount_applied, status) " +
                    "VALUES (?, ?, ?, ?, ?)";

            try (PreparedStatement orderStmt = con.prepareStatement(orderSql)) {
                orderStmt.setInt(1, order.getOrderID());
                orderStmt.setString(2, order.getStudent().getStudentID());
                orderStmt.setDouble(3, order.getTotalCost());
                orderStmt.setDouble(4, 0); // Default discount
                orderStmt.setString(5, order.getStatus().toUpperCase()); // Use normalized status
                orderStmt.executeUpdate();
            }

            // 2. Insert order items
            String itemSql = "INSERT INTO order_items (order_id, menu_item_id, quantity) VALUES (?, ?, ?)";

            // Count item frequencies
            Map<Integer, Integer> itemCounts = new HashMap<>();
            for (MenuItem item : getOrderItems(order.getOrderID())) {
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
            System.err.println("❌ Failed to place order: " + e.getMessage());
            System.err.println("❌ Failed to place order: " + e.getMessage());
            return false;
        }
    }

    public boolean updateOrderStatus(int orderId, String status) {
        String sql = "UPDATE orders SET status = ? WHERE order_id = ?";
        try (Connection con = DatabaseRepository.createNewConnection()) {
            if (con == null) return false;
            con.setAutoCommit(false);

            // 1. Update order status
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, status);
                pstmt.setInt(2, orderId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected == 0) {
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
                            int pointsToAdd = (int) (totalCost / 10);
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
            System.err.println("❌ Failed to update order status: " + e.getMessage());
            return false;
        }
    }

    public boolean applyDiscountToOrder(int orderId, double discountAmount) {
        String sql = "UPDATE orders SET discount_applied = ?, total_cost = total_cost - ? WHERE order_id = ? AND UPPER(status) = 'PENDING'";
        try (Connection con = DatabaseRepository.createNewConnection();
             PreparedStatement pstmt = con != null ? con.prepareStatement(sql) : null) {
            if (pstmt == null) return false;
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
        try (Connection con = DatabaseRepository.createNewConnection()) {
            if (con == null) return false;
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
                    return false;
                }
            }

            con.commit();
            return true;

        } catch (SQLException e) {
            System.err.println("❌ Failed to delete order: " + e.getMessage());
            return false;
        }
    }

    public List<Map<String, Object>> getOrderStatistics(String startDate, String endDate) {
        List<Map<String, Object>> statistics = new ArrayList<>();
        String sql = "SELECT DATE(order_date) as date, COUNT(*) as order_count, SUM(total_cost) as total_revenue " +
                "FROM orders " +
                "WHERE order_date BETWEEN ? AND ? " +
                "GROUP BY DATE(order_date) " +
                "ORDER BY date";

        try (Connection con = DatabaseRepository.createNewConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
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

        try (Connection con = DatabaseRepository.createNewConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
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

    @Override
    public Order findById(int orderId) {
        String sql = "SELECT o.*, s.name as student_name FROM orders o JOIN students s ON o.student_id = s.student_id WHERE o.order_id = ?";

        try (Connection con = DatabaseRepository.createNewConnection();
             PreparedStatement pstmt = con != null ? con.prepareStatement(sql) : null) {
            if (pstmt == null) return null;
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
}



