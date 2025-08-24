package contracts;

import domain.Student;

public interface IRewardStrategy {
    void applyReward(Student student , double orderValue);
}
