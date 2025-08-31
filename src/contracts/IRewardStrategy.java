package contracts;

import domain.Student;

public interface IRewardStrategy {
    void applyReward(Student student , double orderValue);
    
    /**
     * Calculate the number of points to award for a given order value
     * @param orderValue The total value of the order
     * @return The number of points to award
     */
    int calculatePoints(double orderValue);
}
