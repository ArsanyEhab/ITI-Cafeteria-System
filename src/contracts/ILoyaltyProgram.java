package contracts;

import domain.IStudent;
import java.util.List;

public interface ILoyaltyProgram {
    void awardPoints(IStudent student , double orderValue);
    void redeemPoints(IStudent student,String reward);
    void deductPoints(IStudent student , int points);
    void setRewardStrategy(IRewardStrategy strategy);
    int getTotalRedeemedPoints();


}
