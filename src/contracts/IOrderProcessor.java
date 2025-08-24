package contracts;

import domain.IStudent;
import domain.Order;

import java.util.List;

public interface IOrderProcessor {
    void placeOrder(Order order , IStudent student);
   void  applyPercentageDiscount(int orderID,double percentage);
   void updateOrderStatus(int orderId,String status);
 List<Order> getPendingOrders();
}
