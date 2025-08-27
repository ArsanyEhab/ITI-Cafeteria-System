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
            System.out.println("2. Card");
            int choice = sc.nextInt();
            sc.nextLine();

            switch (choice) {
                case 1 -> System.out.println("Payment done by Cash ");
                case 2 -> {
                    System.out.print("Enter your Card number: ");
                    String card = sc.nextLine();
                    //Check if it is 16 and numbers only
                    if (card.matches("^[0-9]{16}$")) {
                        System.out.println("Card accepted ");
                        System.out.println( "Payment done by Card.");
                    } else {
                        System.out.println(" Invalid Card number. Payment failed.");
                    }
                }
                default -> System.out.println("Invalid choice");
            }
        } else {
            System.out.println("Order not found!");
        }
    }
}

