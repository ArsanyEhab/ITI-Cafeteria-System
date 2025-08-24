package contracts;

import domain.Order;
import java.util.List;


public interface IOrderRepository {
    Order findById(int orderId);
    void save(Order order);
    List<Order> findAll();
}
