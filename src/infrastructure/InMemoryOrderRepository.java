package infrastructure;
import contracts.IOrderRepository;
import domain.Order;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class InMemoryOrderRepository implements IOrderRepository {
    private Map<Integer, Order> orders = new HashMap<>();
    @Override
    public Order findById(int orderId) { return orders.get(orderId); }
    @Override
    public void save(Order order) { orders.put(order.getOrderID(), order); }
    @Override
    public List<Order> findAll() { return new ArrayList<>(orders.values()); }

}
