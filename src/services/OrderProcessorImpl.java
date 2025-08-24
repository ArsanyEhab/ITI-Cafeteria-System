package services;

import contracts.*;
import domain.IStudent;
import domain.Order;
import domain.Student;

import java.util.List;
import java.util.stream.Collectors;

public class OrderProcessorImpl implements IOrderProcessor {
    private IOrderRepository orderRepo;
    private ILoyaltyProgram loyaltyProgram;
    private INotificationService notificationService;
    private IDiscountCalculator discountCalculator;

    public OrderProcessorImpl(IOrderRepository orderRepo,
                              ILoyaltyProgram loyaltyProgram,
                              INotificationService notificationService,
                              IDiscountCalculator discountCalculator) {
        this.orderRepo = orderRepo;
        this.loyaltyProgram = loyaltyProgram;
        this.notificationService = notificationService;
        this.discountCalculator = discountCalculator;
    }

    // FR3.1 - 3.4: Place order
    public void placeOrder(Order order, IStudent student) {
        order.setStudent((Student) student); // student owns the order
        order.generateOrderID();             // unique ID
        student.getOrders().add(order);      // add to student history
        order.calculateTotalCost();          // compute total
        order.setStatus("Preparing");
        loyaltyProgram.awardPoints( student, order.getTotalCost()); // add points
        orderRepo.save(order);               // persist
        notificationService.sendNotification(student.getStudentID(),
                "Your order has been placed! Total: " + order.getTotalCost() + " EGP "+"\n Order id is:"+order.getOrderID());
        notificationService.sendNotification(student.getStudentID(),
                "Your order Status: " + order.getStatus());
    }

    // Apply discount to an order
    public void applyPercentageDiscount(int orderId, double percentage) {
        Order order = orderRepo.findById(orderId);
        if (order != null) {
            double discount = discountCalculator.calculateDiscountAmount(order, percentage);
            order.setDiscountApplied(discount);
            order.calculateTotalCost();
            orderRepo.save(order);
        }
    }

    // Update status (Pending -> Preparing -> Ready for Pickup)
    public void updateOrderStatus(int orderID, String status) {
        Order order = orderRepo.findById(orderID);
        if (order != null) {
            order.updateStatus(status);
            if ("Ready for Pickup".equals(status)) {
                notificationService.sendNotification(order.getStudent().getStudentID(),
                        "Your order #" + order.getOrderID() + " is ready for pickup!");
            }
            }
            orderRepo.save(order);
        }


    // Get all pending orders (for staff view)
    public List<Order> getPendingOrders() {
        return orderRepo.findAll().stream()
                .filter(o -> "Pending".equals(o.getStatus()))
                .collect(Collectors.toList());
    }
}
