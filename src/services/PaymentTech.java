package services;

import contracts.IPaymentInterface;
import java.util.Scanner;


public class PaymentTech implements IPaymentInterface {
    private static Scanner sc = new Scanner(System.in);

    @Override
    public void paymentTech(int orderId) {
        if (orderId != 0) {
            System.out.println("Choose Way to pay");
            System.out.println("1. Cash");
            System.out.println("2. Visa");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1 -> System.out.println("Payment done by Cash ");
                case 2 -> {
                    System.out.print("Enter your Visa number: ");
                    String visa = sc.nextLine();
                    //Check if it is 16 and start with 4
                    if (visa.matches("^4[0-9]{15}$")) {
                        System.out.println("Visa accepted ");
                        System.out.println( "Payment done by Visa.");
                    } else {
                        System.out.println(" Invalid Visa number. Payment failed.");
                    }
                }
                default -> System.out.println("Invalid choice");
            }
        } else {
            System.out.println("Order not found!");
        }
    }
}

