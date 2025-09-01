package services;

import contracts.IReportGenerator;
import infrastructure.DatabaseRepository;
import infrastructure.DatabaseOrderRepository;
import infrastructure.DatabaseLoyaltyRepository;

import java.sql.*;
import java.util.*;
import java.text.SimpleDateFormat;

public class ReportGeneratorImpl implements IReportGenerator {
    private final DatabaseOrderRepository orderRepo;
    private final DatabaseLoyaltyRepository loyaltyRepo;
    
    public ReportGeneratorImpl() {
        this.orderRepo = new DatabaseOrderRepository();
        this.loyaltyRepo = new DatabaseLoyaltyRepository(new PointsPerEGPReward());
    }

    @Override
    public double viewDailySale(java.util.Date date) {
        String sql = "SELECT COALESCE(SUM(total_cost), 0) as daily_sales FROM orders WHERE DATE(order_date) = DATE(?)";
        return executeSingleDoubleQuery(sql, date);
    }

    @Override
    public double viewWeeklySales(java.util.Date weekStart) {
        String sql = "SELECT COALESCE(SUM(total_cost), 0) as weekly_sales FROM orders WHERE order_date >= ? AND order_date < DATE_ADD(?, INTERVAL 7 DAY)";
        return executeSingleDoubleQuery(sql, weekStart, weekStart);
    }

    @Override
    public double viewMonthlySales(java.util.Date monthStart) {
        String sql = "SELECT COALESCE(SUM(total_cost), 0) as monthly_sales FROM orders WHERE order_date >= ? AND order_date < DATE_ADD(?, INTERVAL 1 MONTH)";
        return executeSingleDoubleQuery(sql, monthStart, monthStart);
    }

    @Override
    public int viewLoyaltyRedemptions() {
        String sql = "SELECT COUNT(*) as redemptions FROM student_rewards";
        return executeSingleIntQuery(sql);
    }

    @Override
    public Map<String, Object> getSalesAnalytics(java.util.Date startDate, java.util.Date endDate) {
        Map<String, Object> analytics = new HashMap<>();
        
        String sql = """
            SELECT 
                COUNT(*) as total_orders,
                COALESCE(SUM(total_cost), 0) as total_revenue,
                COALESCE(AVG(total_cost), 0) as avg_order_value,
                COUNT(DISTINCT student_id) as unique_customers,
                COALESCE(SUM(discount_applied), 0) as total_discounts
            FROM orders 
            WHERE order_date BETWEEN ? AND ?
        """;
        
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return analytics;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
                pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        analytics.put("total_orders", rs.getInt("total_orders"));
                        analytics.put("total_revenue", rs.getDouble("total_revenue"));
                        analytics.put("avg_order_value", rs.getDouble("avg_order_value"));
                        analytics.put("unique_customers", rs.getInt("unique_customers"));
                        analytics.put("total_discounts", rs.getDouble("total_discounts"));
                        
                        // Calculate additional metrics
                        int totalOrders = rs.getInt("total_orders");
                        int uniqueCustomers = rs.getInt("unique_customers");
                        if (uniqueCustomers > 0) {
                            analytics.put("orders_per_customer", (double) totalOrders / uniqueCustomers);
                        } else {
                            analytics.put("orders_per_customer", 0.0);
                        }
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get sales analytics: " + e.getMessage());
        }
        
        return analytics;
    }

    @Override
    public List<Map<String, Object>> getTopSellingItems(int limit) {
        List<Map<String, Object>> topItems = new ArrayList<>();
        
        String sql = """
            SELECT 
                m.menu_item_id,
                m.name,
                m.category,
                m.price,
                SUM(oi.quantity) as total_quantity,
                SUM(oi.quantity * oi.price) as total_revenue
            FROM order_items oi
            JOIN menu_items m ON oi.menu_item_id = m.menu_item_id
            JOIN orders o ON oi.order_id = o.order_id
            WHERE o.status != 'cancelled'
            GROUP BY m.menu_item_id, m.name, m.category, m.price
            ORDER BY total_quantity DESC
            LIMIT ?
        """;
        
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return topItems;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, limit);
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> item = new HashMap<>();
                        item.put("menu_item_id", rs.getInt("menu_item_id"));
                        item.put("name", rs.getString("name"));
                        item.put("category", rs.getString("category"));
                        item.put("price", rs.getDouble("price"));
                        item.put("total_quantity", rs.getInt("total_quantity"));
                        item.put("total_revenue", rs.getDouble("total_revenue"));
                        topItems.add(item);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get top selling items: " + e.getMessage());
        }
        
        return topItems;
    }

    @Override
    public Map<String, Double> getCategorySales(java.util.Date startDate, java.util.Date endDate) {
        Map<String, Double> categorySales = new HashMap<>();
        
        String sql = """
            SELECT 
                m.category,
                COALESCE(SUM(oi.quantity * oi.price), 0) as category_revenue
            FROM order_items oi
            JOIN menu_items m ON oi.menu_item_id = m.menu_item_id
            JOIN orders o ON oi.order_id = o.order_id
            WHERE o.order_date BETWEEN ? AND ? AND o.status != 'cancelled'
            GROUP BY m.category
            ORDER BY category_revenue DESC
        """;
        
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return categorySales;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
                pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        categorySales.put(rs.getString("category"), rs.getDouble("category_revenue"));
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get category sales: " + e.getMessage());
        }
        
        return categorySales;
    }

    @Override
    public Map<String, Integer> getOrderStatusDistribution() {
        Map<String, Integer> statusDistribution = new HashMap<>();
        
        String sql = "SELECT status, COUNT(*) as count FROM orders GROUP BY status";
        
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return statusDistribution;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                while (rs.next()) {
                    statusDistribution.put(rs.getString("status"), rs.getInt("count"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get order status distribution: " + e.getMessage());
        }
        
        return statusDistribution;
    }

    @Override
    public Map<String, Object> getCustomerAnalytics() {
        Map<String, Object> customerAnalytics = new HashMap<>();
        
        String sql = """
            SELECT 
                COUNT(DISTINCT student_id) as total_customers,
                COUNT(DISTINCT CASE WHEN order_date >= DATE_SUB(NOW(), INTERVAL 30 DAY) THEN student_id END) as active_customers_30d,
                COUNT(DISTINCT CASE WHEN order_date >= DATE_SUB(NOW(), INTERVAL 7 DAY) THEN student_id END) as active_customers_7d,
                AVG(total_cost) as avg_order_value,
                MAX(total_cost) as max_order_value
            FROM orders
            WHERE status != 'cancelled'
        """;
        
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return customerAnalytics;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                if (rs.next()) {
                    customerAnalytics.put("total_customers", rs.getInt("total_customers"));
                    customerAnalytics.put("active_customers_30d", rs.getInt("active_customers_30d"));
                    customerAnalytics.put("active_customers_7d", rs.getInt("active_customers_7d"));
                    customerAnalytics.put("avg_order_value", rs.getDouble("avg_order_value"));
                    customerAnalytics.put("max_order_value", rs.getDouble("max_order_value"));
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get customer analytics: " + e.getMessage());
        }
        
        return customerAnalytics;
    }

    @Override
    public Map<String, Object> getRevenueTrends(java.util.Date startDate, java.util.Date endDate) {
        Map<String, Object> trends = new HashMap<>();
        List<Map<String, Object>> dailyTrends = new ArrayList<>();
        
        String sql = """
            SELECT 
                DATE(order_date) as date,
                COUNT(*) as orders,
                COALESCE(SUM(total_cost), 0) as revenue
            FROM orders
            WHERE order_date BETWEEN ? AND ? AND status != 'cancelled'
            GROUP BY DATE(order_date)
            ORDER BY date
        """;
        
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return trends;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setDate(1, new java.sql.Date(startDate.getTime()));
                pstmt.setDate(2, new java.sql.Date(endDate.getTime()));
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> dayData = new HashMap<>();
                        dayData.put("date", rs.getDate("date"));
                        dayData.put("orders", rs.getInt("orders"));
                        dayData.put("revenue", rs.getDouble("revenue"));
                        dailyTrends.add(dayData);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get revenue trends: " + e.getMessage());
        }
        
        trends.put("daily_trends", dailyTrends);
        return trends;
    }

    @Override
    public List<Map<String, Object>> getDailySalesBreakdown(java.util.Date date) {
        List<Map<String, Object>> breakdown = new ArrayList<>();
        
        String sql = """
            SELECT 
                HOUR(order_date) as hour,
                COUNT(*) as orders,
                COALESCE(SUM(total_cost), 0) as revenue
            FROM orders
            WHERE DATE(order_date) = DATE(?)
            GROUP BY HOUR(order_date)
            ORDER BY hour
        """;
        
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return breakdown;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setDate(1, new java.sql.Date(date.getTime()));
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    while (rs.next()) {
                        Map<String, Object> hourData = new HashMap<>();
                        hourData.put("hour", rs.getInt("hour"));
                        hourData.put("orders", rs.getInt("orders"));
                        hourData.put("revenue", rs.getDouble("revenue"));
                        breakdown.add(hourData);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get daily sales breakdown: " + e.getMessage());
        }
        
        return breakdown;
    }

    @Override
    public List<Map<String, Object>> getHourlySalesPattern(java.util.Date date) {
        return getDailySalesBreakdown(date); // Same implementation for hourly pattern
    }

    @Override
    public Map<String, Object> getLoyaltyProgramAnalytics() {
        Map<String, Object> loyaltyAnalytics = new HashMap<>();
        
        // Get total rewards available
        String rewardsSql = "SELECT COUNT(*) as total_rewards, AVG(points_required) as avg_points_required FROM rewards";
        
        // Get total redemptions from student_rewards table
        String redemptionsSql = "SELECT COUNT(*) as total_redemptions FROM student_rewards";
        
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return loyaltyAnalytics;
            
            // Get rewards data
            try (PreparedStatement pstmt = con.prepareStatement(rewardsSql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                if (rs.next()) {
                    int totalRewards = rs.getInt("total_rewards");
                    double avgPointsRequired = rs.getDouble("avg_points_required");
                    
                    loyaltyAnalytics.put("total_rewards", totalRewards);
                    loyaltyAnalytics.put("avg_points_required", avgPointsRequired);
                }
            }
            
            // Get redemptions data
            try (PreparedStatement pstmt = con.prepareStatement(redemptionsSql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                if (rs.next()) {
                    int totalRedemptions = rs.getInt("total_redemptions");
                    loyaltyAnalytics.put("redeemed_rewards", totalRedemptions);
                    
                    // Calculate available rewards (total rewards - redeemed)
                    int totalRewards = (Integer) loyaltyAnalytics.get("total_rewards");
                    int availableRewards = Math.max(0, totalRewards - totalRedemptions);
                    loyaltyAnalytics.put("available_rewards", availableRewards);
                    
                    // Calculate redemption rate
                    if (totalRewards > 0) {
                        loyaltyAnalytics.put("redemption_rate", (double) totalRedemptions / totalRewards * 100);
                    } else {
                        loyaltyAnalytics.put("redemption_rate", 0.0);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get loyalty program analytics: " + e.getMessage());
        }
        
        return loyaltyAnalytics;
    }

    // Helper methods
    private double executeSingleDoubleQuery(String sql, Object... params) {
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return 0.0;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    if (params[i] instanceof java.util.Date) {
                        pstmt.setDate(i + 1, new java.sql.Date(((java.util.Date) params[i]).getTime()));
                    } else {
                        pstmt.setObject(i + 1, params[i]);
                    }
                }
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getDouble(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Query execution failed: " + e.getMessage());
        }
        return 0.0;
    }

    private int executeSingleIntQuery(String sql, Object... params) {
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return 0;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                for (int i = 0; i < params.length; i++) {
                    pstmt.setObject(i + 1, params[i]);
                }
                
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt(1);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Query execution failed: " + e.getMessage());
        }
        return 0;
    }
}
