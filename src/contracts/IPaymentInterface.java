package contracts;

import domain.Order;
import domain.Student;
import infrastructure.DatabaseOrderRepository;

public interface IPaymentInterface {
    void paymentTech(int orderId);
}
