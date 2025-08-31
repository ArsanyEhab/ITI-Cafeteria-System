package domain;

import java.util.Date;

/**
 * Represents a loyalty points transaction
 */
public class LoyaltyTransaction {
    private int transactionId;
    private String studentId;
    private int pointsChanged;
    private String description;
    private Date createdAt;
    
    public LoyaltyTransaction(int transactionId, String studentId, int pointsChanged, String description, Date createdAt) {
        this.transactionId = transactionId;
        this.studentId = studentId;
        this.pointsChanged = pointsChanged;
        this.description = description;
        this.createdAt = createdAt;
    }
    
    // Getters
    public int getTransactionId() { return transactionId; }
    public String getStudentId() { return studentId; }
    public int getPointsChanged() { return pointsChanged; }
    public String getDescription() { return description; }
    public Date getCreatedAt() { return createdAt; }
    
    // Setters
    public void setTransactionId(int transactionId) { this.transactionId = transactionId; }
    public void setStudentId(String studentId) { this.studentId = studentId; }
    public void setPointsChanged(int pointsChanged) { this.pointsChanged = pointsChanged; }
    public void setDescription(String description) { this.description = description; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    
    @Override
    public String toString() {
        String sign = pointsChanged >= 0 ? "+" : "";
        return String.format("%s%d points - %s", sign, pointsChanged, description);
    }
}
