package infrastructure;

import contracts.ILoyaltyProgram;
import contracts.IRewardStrategy;
import domain.IStudent;
import domain.Student;
import domain.Reward;
import domain.MenuItem;
import domain.LoyaltyTransaction;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DatabaseLoyaltyRepository implements ILoyaltyProgram {
    private IRewardStrategy rewardStrategy;

    public DatabaseLoyaltyRepository(IRewardStrategy rewardStrategy) {
        this.rewardStrategy = rewardStrategy;
    }

    @Override
    public void awardPoints(IStudent student, double orderValue) {
        // Apply reward strategy to calculate points
        rewardStrategy.applyReward((Student) student, orderValue);
        
        // Update student points in database
        updateStudentPoints(student);
        
        // ✅ Record transaction for points awarded
        int pointsAwarded = 0;
        try {
            // Try to get points from reward strategy if it supports it
            if (rewardStrategy instanceof services.PointsPerEGPReward) {
                pointsAwarded = ((services.PointsPerEGPReward) rewardStrategy).calculatePoints(orderValue);
            } else {
                // Fallback: calculate points manually (1 point per 10 EGP)
                pointsAwarded = (int) (orderValue / 10);
            }
        } catch (Exception e) {
            // Fallback: calculate points manually (1 point per 10 EGP)
            pointsAwarded = (int) (orderValue / 10);
            System.err.println("⚠️ Using fallback points calculation: " + e.getMessage());
        }
        
        recordLoyaltyTransaction(student.getStudentID(), pointsAwarded, "Points awarded for order completion");
        
        System.out.println("Points awarded! Current Balance: " + student.getLoyaltyPoints());
    }

    @Override
    public void redeemPoints(IStudent student, String reward) {
        int currentPoints = student.getLoyaltyPoints();
        
        // Get reward details from database
        Reward rewardDetails = getRewardByName(reward);
        if (rewardDetails == null) {
            System.out.println("Invalid reward option: " + reward);
            return;
        }
        
        if (currentPoints >= rewardDetails.getPointsRequired()) {
            // Regular item reward
            deductPoints(student, rewardDetails.getPointsRequired());
            recordRedemption(student.getStudentID(), rewardDetails.getRewardId());
            System.out.println("Redeemed " + rewardDetails.getPointsRequired() + " points for reward ID: " + rewardDetails.getRewardId() + "!");
        } else {
            System.out.println("Not enough points for reward ID: " + rewardDetails.getRewardId() + ". Need " + rewardDetails.getPointsRequired() + " points.");
        }
    }
    
    /**
     * Get the actual menu item for a reward
     */
    public MenuItem getRewardedMenuItem(int rewardId) {
        String sql = "SELECT m.* FROM menu_items m " +
                    "JOIN rewards r ON m.menu_item_id = r.rewarded_item_id " +
                    "WHERE r.reward_id = ?";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return null;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, rewardId);
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
            System.err.println("❌ Failed to get rewarded menu item: " + e.getMessage());
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
        return null;
    }

    @Override
    public void deductPoints(IStudent student, int points) {
        int currentPoints = student.getLoyaltyPoints();
        if (currentPoints >= points) {
            student.setLoyaltyPoints(currentPoints - points);
            updateStudentPoints(student);
            
            // ✅ Record transaction for points deduction
            recordLoyaltyTransaction(student.getStudentID(), -points, "Points deducted for reward redemption");
        } else {
            System.out.println("Not enough points to deduct.");
        }
    }

    @Override
    public void setRewardStrategy(IRewardStrategy strategy) {
        this.rewardStrategy = strategy;
    }
    
    /**
     * ✅ Record a loyalty transaction in the loyalty_transactions table
     */
    private void recordLoyaltyTransaction(String studentId, int pointsChanged, String description) {
        String sql = "INSERT INTO loyalty_transactions (student_id, points_changed, description, created_at) VALUES (?, ?, ?, NOW())";
        
        try (Connection con = DatabaseRepository.createNewConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            pstmt.setInt(2, pointsChanged);
            pstmt.setString(3, description);
            pstmt.executeUpdate();
            
            System.out.println("✅ Loyalty transaction recorded: " + description + " for student " + studentId);
        } catch (SQLException e) {
            System.err.println("❌ Failed to record loyalty transaction: " + e.getMessage());
        }
    }
    
    /**
     * ✅ Get loyalty transaction history for a student
     */
    public List<LoyaltyTransaction> getStudentTransactionHistory(String studentId) {
        List<LoyaltyTransaction> transactions = new ArrayList<>();
        String sql = "SELECT * FROM loyalty_transactions WHERE student_id = ? ORDER BY created_at DESC";
        
        try (Connection con = DatabaseRepository.createNewConnection();
             PreparedStatement pstmt = con.prepareStatement(sql)) {
            pstmt.setString(1, studentId);
            try (ResultSet rs = pstmt.executeQuery()) {
                while (rs.next()) {
                    LoyaltyTransaction transaction = new LoyaltyTransaction(
                        rs.getInt("transaction_id"),
                        rs.getString("student_id"),
                        rs.getInt("points_changed"),
                        rs.getString("description"),
                        rs.getTimestamp("created_at")
                    );
                    transactions.add(transaction);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get loyalty transaction history: " + e.getMessage());
        }
        return transactions;
    }

    private void updateStudentPoints(IStudent student) {
        String sql = "UPDATE students SET loyalty_points = ? WHERE student_id = ?";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, student.getLoyaltyPoints());
                pstmt.setString(2, student.getStudentID());
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to update student points: " + e.getMessage());
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
    }

    private void recordRedemption(String studentId, int rewardId) {
        String sql = "INSERT INTO student_rewards (student_id, reward_id, redeemed_at) VALUES (?, ?, NOW())";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                pstmt.setInt(2, rewardId);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to record redemption: " + e.getMessage());
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
    }

    public int getTotalRedeemedPoints() {
        String sql = "SELECT COUNT(*) as total FROM student_rewards";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return 0;
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt("total");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get total redeemed points: " + e.getMessage());
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
        return 0;
    }

    // Additional methods for loyalty program management
    public int getStudentTotalRedemptions(String studentId) {
        String sql = "SELECT COUNT(*) as total FROM student_rewards WHERE student_id = ?";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return 0;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        return rs.getInt("total");
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get student redemptions: " + e.getMessage());
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
        return 0;
    }
    
    /**
     * Get reward details by name from database
     */
    private Reward getRewardByName(String rewardName) {
        String sql = "SELECT * FROM rewards WHERE reward_name = ?";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return null;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, rewardName);
                try (ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                                return new Reward(
            rs.getInt("reward_id"),
            rs.getString("reward_name"),
            rs.getInt("points_required"),
            rs.getInt("rewarded_item_id")
        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get reward details: " + e.getMessage());
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
        return null;
    }
    
    /**
     * Get all available rewards from database
     */
    public List<Reward> getAllRewards() {
        List<Reward> rewards = new ArrayList<>();
        String sql = "SELECT * FROM rewards ORDER BY points_required ASC";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return rewards;
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                while (rs.next()) {
                    Reward reward = new Reward(
                        rs.getInt("reward_id"),
                        rs.getString("reward_name"),
                        rs.getInt("points_required"),
                        rs.getInt("rewarded_item_id")
                    );
                    rewards.add(reward);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get all rewards: " + e.getMessage());
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
        return rewards;
    }
    
    /**
     * Get a database connection for external use
     */
    public Connection getConnection() {
        return DatabaseRepository.createNewConnection();
    }
    

    
    /**
     * Get current exchange rate (points per EGP)
     */
    public int getExchangeRate() {
        String sql = "SELECT points_per_egp FROM exchange_points ORDER BY last_updated DESC LIMIT 1";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return 50; // Default fallback
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getInt("points_per_egp");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get exchange rate: " + e.getMessage());
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
        return 50; // Default fallback
    }
    
    /**
     * Get the EGP value per point from the exchange rate table
     */
    public double getEGPPerPoint() {
        String sql = "SELECT egp_per_points FROM exchange_points ORDER BY last_updated DESC LIMIT 1";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return 0.02; // Default fallback (1/50)
            try (Statement stmt = con.createStatement();
                 ResultSet rs = stmt.executeQuery(sql)) {
                if (rs.next()) {
                    return rs.getDouble("egp_per_points");
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get EGP per point: " + e.getMessage());
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
        return 0.02; // Default fallback (1/50)
    }
    
    /**
     * Update exchange rate (admin function)
     */
    public boolean updateExchangeRate(int newPointsPerEGP) {
        // Calculate the corresponding EGP per point value
        double egpPerPoint = 1.0 / newPointsPerEGP;
        
        String sql = "INSERT INTO exchange_points (points_per_egp, egp_per_points) VALUES (?, ?)";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return false;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, newPointsPerEGP);
                pstmt.setDouble(2, egpPerPoint);
                int rowsAffected = pstmt.executeUpdate();
                return rowsAffected > 0;
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to update exchange rate: " + e.getMessage());
            return false;
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
    }
    
    /**
     * Exchange points for order discount (student function)
     * Returns the discount amount in EGP, or -1 if exchange failed
     */
    public double exchangePointsForOrderDiscount(IStudent student, int pointsToExchange) {
        int currentPoints = student.getLoyaltyPoints();
        
        // Check if student has enough points
        if (currentPoints < pointsToExchange) {
            System.out.println("Not enough points to exchange.");
            return -1;
        }
        
        // Get current exchange rate
        int exchangeRate = getExchangeRate();
        
        // Check if points to exchange meets minimum requirement (at least 1 EGP worth)
        if (pointsToExchange < exchangeRate) {
            System.out.println("Points to exchange must be at least " + exchangeRate + " to get 1 EGP.");
            return -1;
        }
        
        // Calculate discount amount by dividing points by the exchange rate
        double discountAmount = (double) pointsToExchange / exchangeRate;
        
        // Only deduct points if all validations pass
        deductPoints(student, pointsToExchange);
        
        // ✅ Record transaction for points exchange
        recordLoyaltyTransaction(student.getStudentID(), -pointsToExchange, 
            "Points exchanged for " + String.format("%.2f", discountAmount) + " EGP order discount");
        
        // Record the exchange for order discount
        System.out.println("Exchanged " + pointsToExchange + " points for " + String.format("%.2f", discountAmount) + " EGP discount");
        
        return discountAmount;
    }
}
