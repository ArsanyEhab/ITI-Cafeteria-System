package app;

import CrossCutting.DiscountCalculator;
import contracts.*;
import domain.Student;
import domain.MenuItem;
import domain.Order;
import infrastructure.ConsoleNotificationService;
import infrastructure.InMemoryOrderRepository;
import infrastructure.InMemoryUserRepository;
import services.*;

import java.util.*;

public class CafeteriaApp {
    private static Scanner sc = new Scanner(System.in);

    // Infrastructure
    private static IUserRepository userRepo = new InMemoryUserRepository();
    private static IOrderRepository orderRepo = new InMemoryOrderRepository();
    private static INotificationService notificationService = new ConsoleNotificationService();

    // Services
    private static IRewardStrategy rewardStrategy = new PointsPerEGPReward();
    private static LoyaltyProgram loyaltyProgram = new LoyaltyProgram(rewardStrategy);
    private static DiscountCalculator discountCalculator = new DiscountCalculator();
    private static MenuManager menuManager = new MenuManager();
    private static MenuProvider menuProvider = new MenuProvider(menuManager.getItems());
    private static IReportGenerator reportGenerator = new ReportGeneratorImpl(orderRepo, loyaltyProgram);

    private static OrderProcessorImpl orderProcessor =
            new OrderProcessorImpl(orderRepo, loyaltyProgram, notificationService, discountCalculator);
private static IPaymentInterface paymentInterface=new PaymentTech();
    private static Student currentStudent = null; // logged in student

    public static void main(String[] args) {
        while (true) {
            System.out.println("\n=== Cafeteria System ===");
            System.out.println("1. Student");
            System.out.println("2. Staff");
            System.out.println("3. Admin Staff");
            System.out.println("0. Exit");
            System.out.print("Choose: ");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1 -> studentMenu();
                case 2 -> staffMenu();
                case 3 -> adminStaffMenu();
                case 0 -> {
                    System.out.println("Goodbye!");
                    return;
                }
                default -> System.out.println("Invalid choice");
            }
        }
    }

    // ================= STUDENT FLOW =================
    private static void studentMenu() {
        while (true) {
            System.out.println("\n--- Student Menu ---");
            System.out.println("1. Register");
            System.out.println("2. Login");
            System.out.println("0. Back");
            System.out.print("Choose: ");
            int choice = sc.nextInt(); sc.nextLine();

            switch (choice) {
                case 1 -> registerStudent();
                case 2 -> loginStudent();

                case 0 -> { return; }
                default -> System.out.println("Invalid choice");
            }
        }
    }

    private static void registerStudent() {
        System.out.print("Enter name: ");
        String name = sc.nextLine();
        System.out.print("Enter ID: ");
        String id = sc.nextLine();
        System.out.print("Enter password: ");
        String pass = sc.nextLine();

        Student s = new Student(name, id, pass);
        userRepo.save(s);
        System.out.println("Student registered successfully!");
        System.out.println("Login for Service");
    }

    private static void loginStudent() {
        System.out.print("Enter ID: ");
        String id = sc.nextLine();
        System.out.print("Enter password: ");
        String pass = sc.nextLine();

        Student s = userRepo.findById(id);
        if (s != null && s.getPassword().equals(pass)) {
            currentStudent = s;
            System.out.println("Welcome, " + s.getName());
            afterLogin();
        }

        else {
            System.out.println("Invalid credentials!");
        }

    }
    public static void afterLogin() {
        while (true) {

            System.out.println("1. Browse Menu");
            System.out.println("2. Place Order");
            System.out.println("3. View Loyalty Points");
            System.out.println("4. View Notifications");
            System.out.println("0. Back");
            System.out.print("Choose: ");
            int choice = sc.nextInt();
            sc.nextLine();
            switch (choice) {
                case 1 -> {
                    if (currentStudent != null) currentStudent.browseMenu(menuProvider);
                    else System.out.println("Login first!");
                }
                case 2 -> {
                    if
                        (currentStudent != null) {placeOrderFlow();
                    }

                    else System.out.println("Login first!");
                }
                case 3 -> {
                    if (currentStudent != null) currentStudent.viewLoyaltyBalance();
                    else System.out.println("Login first!");
                }
                case 4 -> {
                    if (currentStudent != null) {
                        notificationService.displayNotifications(currentStudent.getStudentID());
                    } else System.out.println("Login first!");
                }
                case 0 -> {
                    return;
                }
                default -> System.out.println("Invalid choice");
            }
        }

    }

    private static void placeOrderFlow() {
        Order order = new Order();

        while (true) {
            menuProvider.getMenu().forEach(item -> System.out.println(item.getDetails()));
            System.out.print("Enter item ID to add (or 0 to finish): ");
            int id = sc.nextInt(); sc.nextLine();
            if (id == 0) break;

            MenuItem chosen = menuProvider.getMenu().stream()
                    .filter(m -> m.getId() == id).findFirst().orElse(null);
            if (chosen != null) {
                order.addItem(chosen);
                System.out.println(chosen.getName() + " added!");
            }

            else {
                System.out.println("Invalid item!");
            }
        }

        System.out.println("Total cost = " + order.calculateTotalCost() + " EGP");
        System.out.print("Confirm order? (y/n): ");
        String confirm = sc.nextLine();
        if (confirm.equalsIgnoreCase("y")) {
            orderProcessor.placeOrder(order, currentStudent);
            System.out.println("Order placed!");
            paymentInterface.paymentTech(order.getOrderID());

        }

        else {
            System.out.println("Order canceled.");
        }


    }

    // ================= STAFF FLOW =================
    private static void staffMenu() {
        while (true) {
            System.out.println("\n--- Staff Menu ---");
            System.out.println("1. View Pending Orders");
            System.out.println("2. Update Order Status");
            System.out.println("0. Back");
            System.out.print("Choose: ");
            int choice = sc.nextInt(); sc.nextLine();

            switch (choice) {
                case 1 -> viewPendingOrders();
                case 2 -> updateOrderStatus();
                case 0 -> { return; }
                default -> System.out.println("Invalid choice");
            }
        }
    }

    // ================= ADMIN STAFF FLOW =================
    private static void adminStaffMenu() {
        while (true) {
            System.out.println("\n--- Admin Staff Menu ---");
            System.out.println("1. Add Menu Item");
            System.out.println("2. Edit Menu Item");
            System.out.println("3. Remove Menu Item");
            System.out.println("4. View Pending Orders");
            System.out.println("5. Update Order Status");
            System.out.println("6. View Daily Sales Report");
            System.out.println("7. View Weekly Sales Report");
            System.out.println("8. View Loyalty Point Redemptions");
            System.out.println("0. Back");
            System.out.print("Choose: ");
            int choice = sc.nextInt(); sc.nextLine();

            switch (choice) {
                case 1 -> addMenuItem();
                case 2 -> editMenuItem();
                case 3 -> removeMenuItem();
                case 4 -> viewPendingOrders();
                case 5 -> updateOrderStatus();
                case 6 -> viewDailySales();
                case 7 -> viewWeeklySales();
                case 8 -> viewLoyaltyRedemptions();
                case 0 -> { return; }
                default -> System.out.println("Invalid choice");
            }
        }

}

    // =========== Common Methods for Staff/Admin ===========
    private static void addMenuItem() {
        System.out.print("Name: ");
        String name = sc.nextLine();
        System.out.print("Description: ");
        String desc = sc.nextLine();
        System.out.print("Price: ");
        double price = sc.nextDouble(); sc.nextLine();
        System.out.print("Category: ");
        String cat = sc.nextLine();

        menuManager.addMenuItem(name, desc, price, cat);
        System.out.println("Item added.");
    }

    private static void editMenuItem() {
        menuProvider.getMenu().forEach(item -> System.out.println(item.getDetails()));
        System.out.print("Enter ID to edit: ");
        int id = sc.nextInt(); sc.nextLine();

        System.out.print("New Name: ");
        String name = sc.nextLine();
        System.out.print("New Description: ");
        String desc = sc.nextLine();
        System.out.print("New Price: ");
        double price = sc.nextDouble(); sc.nextLine();
        System.out.print("New Category: ");
        String cat = sc.nextLine();

        menuManager.editMenuItem(id, name, desc, price, cat);
        System.out.println("Item updated.");
    }

    private static void removeMenuItem() {
        menuProvider.getMenu().forEach(item -> System.out.println(item.getDetails()));
        System.out.print("Enter ID to remove: ");
        int id = sc.nextInt(); sc.nextLine();
        menuManager.removeMenuItem(id);
        System.out.println("Item removed.");
    }

    private static void viewPendingOrders() {
        orderProcessor.getPendingOrders().forEach(order ->
                System.out.println("Order " + order.getOrderID() + " - Status: " + order.getStatus())
        );
    }

    private static void updateOrderStatus() {
        System.out.print("Enter Order ID: ");
        int id = sc.nextInt(); sc.nextLine();
        System.out.print("New Status (Pending/Preparing/Ready for Pickup): ");
        String status = sc.nextLine();
        orderProcessor.updateOrderStatus(id, status);

        if (currentStudent != null) {
            notificationService.sendNotification(currentStudent.getStudentID(), status);
        }

        System.out.println("Order updated.");
    }
    private static void viewDailySales() {
        System.out.print("Enter date (yyyy-mm-dd): ");
        String dateStr = sc.nextLine();
        try {
            Date date = java.sql.Date.valueOf(dateStr);
            double sales = reportGenerator.viewDailySale(date);
            System.out.println("Daily sales for " + date + " = " + sales + " EGP");
        } catch (Exception e) {
            System.out.println("Invalid date format!");
        }
    }

    private static void viewWeeklySales() {
        System.out.print("Enter week start date (yyyy-mm-dd): ");
        String dateStr = sc.nextLine();
        try {
            Date start = java.sql.Date.valueOf(dateStr);
            double sales = reportGenerator.viewWeeklySales(start);
            System.out.println("Weekly sales starting " + start + " = " + sales + " EGP");
        } catch (Exception e) {
            System.out.println("Invalid date format!");
        }
    }

    private static void viewLoyaltyRedemptions() {
        int redemptions = reportGenerator.viewLoyaltyRedemptions();
        System.out.println("Total loyalty points redeemed: " + redemptions);
    }

}
