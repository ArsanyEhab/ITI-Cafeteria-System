package infrastructure;

import java.sql.*;
import java.util.*;

public class RewardsProgramRepository {
    private Connection con;

    public RewardsProgramRepository() {
        con = DatabaseRepository.getInstance().getConnection();
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
}