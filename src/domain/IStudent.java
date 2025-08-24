package domain;

import java.util.List;

public interface IStudent {
    String getStudentID();
    String getName();
    String getPassword();
    int getLoyaltyPoints();
    List<Order>getOrders();
    void setLoyaltyPoints(int points);
}
