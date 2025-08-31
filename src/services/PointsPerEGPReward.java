package services;

import contracts.IRewardStrategy;
import domain.Student;

public class PointsPerEGPReward implements IRewardStrategy {

    @Override
    public void applyReward(Student student, double orderValue) {
        int points = calculatePoints(orderValue);
        student.setLoyaltyPoints(student.getLoyaltyPoints() + points);
        System.out.println(points + " points awarded! Total points: " + student.getLoyaltyPoints());
    }
    
    @Override
    public int calculatePoints(double orderValue) {
        return (int) (orderValue / 10); // 1 point for 10 EGP
    }
}