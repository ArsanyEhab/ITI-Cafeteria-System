package CrossCutting;

import contracts.IDiscountCalculator;
import domain.MenuItem;
import domain.Order;

public class DiscountCalculator implements IDiscountCalculator {
    @Override
    public double calculateDiscountAmount(Order order, double percentage) {
        double originalCost = order.calculateTotalCost();
        return originalCost * (percentage / 100.0);
    }

    @Override
    public double calculateFinalPrice(double originalPrice, double discount) {
        return Math.max(0, originalPrice - discount); // Take care about negative prices
    }
}
