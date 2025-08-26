package contracts;

import domain.Order;
import java.util.List;


public interface IOrderRepository {
    Order findById(int orderId);
    boolean placeOrder(Order order);
    List<Order> getAllOrders();
}
