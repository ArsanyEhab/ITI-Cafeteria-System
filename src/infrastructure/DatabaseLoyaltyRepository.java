package infrastructure;

import contracts.ILoyaltyProgram;
import contracts.IRewardStrategy;
import domain.IStudent;
import domain.Student;

import java.sql.*;

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
        
        System.out.println("Points awarded! Current Balance: " + student.getLoyaltyPoints());
    }

    @Override
    public void redeemPoints(IStudent student, String reward) {
        int currentPoints = student.getLoyaltyPoints();

        switch (reward.toLowerCase()) {
            case "coffee":
                if (currentPoints >= 100) {
                    deductPoints(student, 100);
                    recordRedemption(student.getStudentID(), "coffee", 100);
                    System.out.println("Redeemed 100 points for a FREE Coffee!");
                } else {
                    System.out.println("Not enough points for free coffee.");
                }
                break;

            case "discount10":
                if (currentPoints >= 50) {
                    deductPoints(student, 50);
                    recordRedemption(student.getStudentID(), "discount10", 50);
                    System.out.println("Redeemed 50 points for a 10 EGP discount!");
                } else {
                    System.out.println("Not enough points for discount.");
                }
                break;

            default:
                System.out.println("Invalid reward option.");
        }
    }

    @Override
    public void deductPoints(IStudent student, int points) {
        int currentPoints = student.getLoyaltyPoints();
        if (currentPoints >= points) {
            student.setLoyaltyPoints(currentPoints - points);
            updateStudentPoints(student);
        } else {
            System.out.println("Not enough points to deduct.");
        }
    }

    @Override
    public void setRewardStrategy(IRewardStrategy strategy) {
        this.rewardStrategy = strategy;
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

    private void recordRedemption(String studentId, String rewardType, int pointsUsed) {
        String sql = "INSERT INTO loyalty_redemptions (student_id, reward_type, points_used, redemption_date) VALUES (?, ?, ?, NOW())";
        Connection con = DatabaseRepository.createNewConnection();
        try {
            if (con == null) return;
            try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, studentId);
                pstmt.setString(2, rewardType);
                pstmt.setInt(3, pointsUsed);
                pstmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to record redemption: " + e.getMessage());
        } finally {
            if (con != null) { try { con.close(); } catch (SQLException ignored) {} }
        }
    }

    public int getTotalRedeemedPoints() {
        String sql = "SELECT SUM(points_used) as total FROM loyalty_redemptions";
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
        String sql = "SELECT SUM(points_used) as total FROM loyalty_redemptions WHERE student_id = ?";
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
}
