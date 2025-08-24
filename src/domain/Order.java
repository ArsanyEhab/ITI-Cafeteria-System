package domain;

import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
public class Order {
    private int orderID;
    private int studentID;
    private List<MenuItem> items = new ArrayList<>();
    private  double totalCost;
    private double discountApplied;
    private String status;
    private Student student;
    private Date date = new Date();

    public Order(int orderID, int studentID, double totalCost, double discountApplied, String status, Student student, Date date) {
        this.orderID = orderID;
        this.studentID = studentID;
        this.totalCost = totalCost;
        this.discountApplied = discountApplied;
        this.status = status;
        this.student = student;
        this.date = date;
    }

    public void addItem(MenuItem item) {
        items.add(item);
    }

    public int getOrderID() { return orderID; }
    public double getTotalCost() { return totalCost; }
    public void setDiscountApplied(double discountApplied) { this.discountApplied = discountApplied; }
    public void setStudent(Student student) { this.student = student; }
    public Student getStudent() { return student; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status=status; }

    public Date getDate() { return date; }


    public void generateOrderID() {
        this.orderID = CrossCutting.IdGenerator.getInstance().generateNewOrderId();
    }


    public void updateStatus(String newStatus) {
        this.status = newStatus;
    }
    public double calculateTotalCost() {
        totalCost = items.stream()
                .mapToDouble(MenuItem::getPrice)
                .sum() - discountApplied;
        return totalCost;
    }
}
