package services;

import contracts.ILoyaltyProgram;
import contracts.IRewardStrategy;
import domain.IStudent;
import domain.Student;

public class LoyaltyProgram implements ILoyaltyProgram {
    private IRewardStrategy rewardStrategy;
    private static int totalRedeemedPoints =0;

    public LoyaltyProgram(IRewardStrategy rewardStrategy) {
        this.rewardStrategy = rewardStrategy;
    }


    @Override
    public void awardPoints(IStudent student, double orderValue) {
        rewardStrategy.applyReward((Student) student,orderValue);
        System.out.println("Points awarded! Current Balance: " + student.getLoyaltyPoints());

    }

    @Override
    public void redeemPoints(IStudent student, String reward) {
        int currentPoints = student.getLoyaltyPoints();

        switch (reward.toLowerCase()) {
            case "coffee":
                if (currentPoints >= 100) {
                    deductPoints(student, 100);
                    System.out.println("Redeemed 100 points for a FREE Coffee!");

                } else {
                    System.out.println("Not enough points for free coffee.");
                }
                break;

            case "discount10":
                if (currentPoints >= 50) {
                    deductPoints(student, 50);
                    System.out.println("Redeemed 50 points for a 10 EGP discount!");
                    //  OrderProcessor
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
            totalRedeemedPoints += points;
        } else {
            System.out.println("Not enough points to deduct.");
        }

    }

    @Override
    public void setRewardStrategy(IRewardStrategy strategy) {
        this.rewardStrategy = strategy;

    }
    public int getTotalRedeemedPoints() {
        return totalRedeemedPoints;
    }
}
