package contracts;

import domain.Order;
import domain.Student;
import infrastructure.InMemoryOrderRepository;

public interface IPaymentInterface {
    void paymentTech(int orderId);
}
