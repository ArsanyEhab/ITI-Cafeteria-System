package services;

import contracts.ILoyaltyProgram;
import contracts.IRewardStrategy;
import domain.IStudent;

import java.util.List;


public class LoyaltyProgram implements ILoyaltyProgram {
    private ILoyaltyProgram loyaltyRepository;

    // Backward compatible constructor for existing app
    public LoyaltyProgram(IRewardStrategy rewardStrategy) {
        this.loyaltyRepository = new infrastructure.DatabaseLoyaltyRepository(rewardStrategy);
    }

    // New constructor for dependency injection
    public LoyaltyProgram(ILoyaltyProgram loyaltyRepository, IRewardStrategy rewardStrategy) {
        this.loyaltyRepository = loyaltyRepository;
        // rewardStrategy is passed to the repository during construction
    }

    @Override
    public void awardPoints(IStudent student, double orderValue) {
        loyaltyRepository.awardPoints(student, orderValue);
    }

    @Override
    public void redeemPoints(IStudent student, String reward) {
        loyaltyRepository.redeemPoints(student, reward);
    }

    @Override
    public void deductPoints(IStudent student, int points) {
        loyaltyRepository.deductPoints(student, points);
    }

    @Override
    public void setRewardStrategy(IRewardStrategy strategy) {
        loyaltyRepository.setRewardStrategy(strategy);
    }

    public int getTotalRedeemedPoints() {
        // Delegate to repository if it has this method
        if (loyaltyRepository instanceof infrastructure.DatabaseLoyaltyRepository) {
            return ((infrastructure.DatabaseLoyaltyRepository) loyaltyRepository).getTotalRedeemedPoints();
        }
        return 0;
    }


}
