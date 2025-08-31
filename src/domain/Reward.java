package domain;

/**
 * Represents a reward that can be redeemed with loyalty points
 */
public class Reward {
    private int rewardId;
    private String rewardName;
    private int pointsRequired;
    private int rewardedItemId;

    public Reward(int rewardId, String rewardName, int pointsRequired, int rewardedItemId) {
        this.rewardId = rewardId;
        this.rewardName = rewardName;
        this.pointsRequired = pointsRequired;
        this.rewardedItemId = rewardedItemId;
    }

    // Getters and Setters
    public int getRewardId() { return rewardId; }
    public void setRewardId(int rewardId) { this.rewardId = rewardId; }
    
    public String getRewardName() { return rewardName; }
    public void setRewardName(String rewardName) { this.rewardName = rewardName; }
    
    public int getPointsRequired() { return pointsRequired; }
    public void setPointsRequired(int pointsRequired) { this.pointsRequired = pointsRequired; }
    
    public int getRewardedItemId() { return rewardedItemId; }
    public void setRewardedItemId(int rewardedItemId) { this.rewardedItemId = rewardedItemId; }

    @Override
    public String toString() {
        return rewardName + " (" + pointsRequired + " pts → Item ID: " + rewardedItemId + ")";
    }
}
