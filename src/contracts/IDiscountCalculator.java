package contracts;

import domain.Order;

public interface IDiscountCalculator {
    double calculateDiscountAmount(Order order, double percentage);
    double calculateFinalPrice(double originalPrice, double discount);
}
