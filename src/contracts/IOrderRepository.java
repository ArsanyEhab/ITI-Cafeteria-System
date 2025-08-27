package contracts;

import domain.Order;
import java.util.List;


public interface IOrderRepository {
    Order findById(int orderId);
    boolean placeOrder(Order order);
    boolean updateOrderStatus(int orderId, String status);
    boolean applyDiscountToOrder(int orderId, double discountAmount);
    List<Order> getAllOrders();
}
