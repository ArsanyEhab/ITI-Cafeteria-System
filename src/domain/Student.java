package domain;
import contracts.IUser;
import services.MenuManager;
import services.MenuProvider;

import  java.util.*;
public class Student implements IStudent, IUser {
    private String studentID;
    private String password;
    private String name;
    private int loyaltyPoints;
    private List<Order> orders = new ArrayList<>();
    //constractor
    public Student(String name, String studentID,String password) {
        this.name = name;
        this.studentID = studentID;
        this.password=password;
    }
    //Getters and Setters
    public String getStudentID() { return studentID; }

    @Override
    public String getId() {
        return studentID;
    }

    public String getName() { return name; }
    public String getPassword(){return password;}
    public int getLoyaltyPoints() { return loyaltyPoints; }
    public void setLoyaltyPoints(int loyaltyPoints) { this.loyaltyPoints = loyaltyPoints; }
    
    public void setName(String name) { this.name = name; }
    public List<Order> getOrders() { return orders; }
    public void viewProfile() {
        System.out.println("Name: " + name + ", ID: " + studentID);
    }
    // Used Methods
    public void viewLoyaltyBalance() {
        System.out.println("Loyalty Points: " + loyaltyPoints);
    }
   public void browseMenu(MenuProvider menu) {
        menu.getMenu().forEach(item -> System.out.println(item.getDetails()));
    }





}
