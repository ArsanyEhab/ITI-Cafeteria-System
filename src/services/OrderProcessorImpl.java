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
        order.setStatus("pending");          // Set to pending initially (consistent with database)
        loyaltyProgram.awardPoints( student, order.getTotalCost()); // add points
        orderRepo.placeOrder(order);               // persist
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
            
            // Apply discount in database first
            boolean success = orderRepo.applyDiscountToOrder(orderId, discount);
            if (success) {
                // Update the in-memory object as well
                order.setDiscountApplied(discount);
                order.calculateTotalCost();
                System.out.println("✅ Discount of " + percentage + "% (₹" + discount + ") applied to order #" + orderId);
            } else {
                System.out.println("❌ Failed to apply discount in database");
            }
        } else {
            System.out.println("❌ Order not found with ID: " + orderId);
            System.out.println("Cannot apply discount to non-existent order.");
        }
    }

    // Update status (Pending -> Preparing -> Ready for Pickup)
    public void updateOrderStatus(int orderID, String status) {
        Order order = orderRepo.findById(orderID);
        if (order != null) {
            // Normalize status to uppercase for consistency
            String normalizedStatus = status.toUpperCase();
            
            // Use the proper updateOrderStatus method instead of placeOrder
            boolean success = orderRepo.updateOrderStatus(orderID, normalizedStatus);
            if (success) {
                // Update the in-memory object as well
                order.updateStatus(normalizedStatus);
                // Check for ready status (case-insensitive)
                if ("READY".equalsIgnoreCase(status) || "READY FOR PICKUP".equalsIgnoreCase(status)) {
                    notificationService.sendNotification(order.getStudent().getStudentID(),
                            "Your order #" + order.getOrderID() + " is ready for pickup!");
                }
                System.out.println("✅ Order #" + orderID + " status updated to: " + normalizedStatus);
            } else {
                System.out.println("❌ Failed to update order status in database");
            }
        } else {
            System.out.println("❌ Order not found with ID: " + orderID);
            System.out.println("Please check the order ID and try again.");
        }
    }


    // Get all pending orders (for staff view)
    public List<Order> getPendingOrders() {
        return orderRepo.getAllOrders().stream()
                .filter(o -> "pending".equalsIgnoreCase(o.getStatus()))
                .collect(Collectors.toList());
    }
}
