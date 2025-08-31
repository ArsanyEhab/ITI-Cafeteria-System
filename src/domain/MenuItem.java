package domain;

public class MenuItem {
    private int id;
    private String name;
    private String description;
    private  double price;
    private String category;
    //constractor
    public MenuItem(int id, String name, String description, double price, String category) {
        this.id = id; this.name = name; this.description = description;
        this.price = price; this.category = category;
    }
    //Setters and Getters
    public int getId() { return id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public double getPrice() { return price; }
    public String getCategory() { return category; }
    public void setPrice(double price) { this.price = price; }

    // Used Methods
    public String getDetails() {
        return String.format("[%d] %s - %s (%.2f EGP) Category: %s", id, name, description, price, category);
    }


}
