package ui;

import contracts.IOrderRepository;
import domain.Order;
import domain.Staff;
import domain.MenuItem;
import infrastructure.DatabaseOrderRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.Alert;
import javafx.scene.layout.VBox;
import javafx.scene.layout.HBox;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;
import ui.LoginRegisterView;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.util.TimeZone;

public class StaffDashboardView extends BorderPane {
    private final Staff staff;
    private final IOrderRepository orderRepo = new DatabaseOrderRepository();
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final ListView<Order> list = new ListView<>(orders);
    
    // Order details panel
    private final VBox orderDetailsPanel = new VBox(10);
    private final Label orderDetailsTitle = new Label("Order Details");
    private final VBox orderDetailsContent = new VBox(8);
    private final HBox orderActionButtons = new HBox(10);

    public StaffDashboardView(Stage stage, Staff s) {
        this.staff = s;
        setPadding(new Insets(16));

        // Top section
        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Button logout = new Button("Logout");
        logout.setOnAction(e -> stage.getScene().setRoot(new LoginRegisterView(stage)));
        Label title = new Label("Order Management - Pending, Preparing & Ready Orders");
        title.getStyleClass().add("title");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        top.getChildren().addAll(title, new Label(" | Staff: " + staff.getName()), logout);
        setTop(top);

        // Enhanced order list with better formatting
        list.setCellFactory(l -> new ListCell<>() {
            @Override protected void updateItem(Order o, boolean empty) {
                super.updateItem(o, empty);
                if (empty || o == null) { 
                    setText(null); 
                } else {
                    String statusColor;
                    if ("preparing".equalsIgnoreCase(o.getStatus())) {
                        statusColor = "🟡"; // Yellow for preparing
                    } else if ("ready".equalsIgnoreCase(o.getStatus())) {
                        statusColor = "🔵"; // Blue for ready
                    } else {
                        statusColor = "🟢"; // Green for pending
                    }
                    
                    // Get all items in the order
                    List<MenuItem> items = o.getItems();
                    String itemsDisplay;
                    if (items != null && !items.isEmpty()) {
                        // Group items by name to show quantities
                        Map<String, Long> itemCounts = items.stream()
                            .collect(Collectors.groupingBy(MenuItem::getName, Collectors.counting()));
                        
                        // Show first 2 unique items with quantities, then count if more
                        List<String> uniqueItems = itemCounts.entrySet().stream()
                            .map(entry -> {
                                String name = entry.getKey();
                                Long count = entry.getValue();
                                return count == 1 ? name : name + " x" + count;
                            })
                            .collect(Collectors.toList());
                        
                        if (uniqueItems.size() == 1) {
                            itemsDisplay = uniqueItems.get(0);
                        } else if (uniqueItems.size() == 2) {
                            itemsDisplay = uniqueItems.get(0) + ", " + uniqueItems.get(1);
                        } else {
                            itemsDisplay = uniqueItems.get(0) + ", " + uniqueItems.get(1) + " +" + (uniqueItems.size() - 2) + " more";
                        }
                    } else {
                        itemsDisplay = "No items";
                    }
                    
                    setText(String.format("#%d - %s - %s - %s %s", 
                        o.getOrderID(), 
                        o.getStudentId(), 
                        itemsDisplay,
                        statusColor,
                        o.getStatus().toUpperCase()
                    ));
                }
            }
        });

        // Order selection listener
        list.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showOrderDetails(newSelection);
            } else {
                hideOrderDetails();
            }
        });

        // Control buttons
        Button refresh = new Button("🔄 Refresh");
        refresh.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        refresh.setOnAction(e -> load());

        // Order details panel setup
        setupOrderDetailsPanel();

        // Main layout
        VBox center = new VBox(15);
        center.getStyleClass().add("card");
        center.setPadding(new Insets(16));
        
        // Orders section
        VBox ordersSection = new VBox(10);
        Label ordersLabel = new Label("📋 Orders Queue (Pending → Preparing → Ready)");
        ordersLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        ordersSection.getChildren().addAll(ordersLabel, list, refresh);
        
        center.getChildren().addAll(ordersSection, orderDetailsPanel);
        setCenter(center);
        
        load();
    }

    private void setupOrderDetailsPanel() {
        orderDetailsPanel.getStyleClass().add("card");
        orderDetailsPanel.setPadding(new Insets(16));
        orderDetailsPanel.setVisible(false);
        orderDetailsPanel.setManaged(false);
        
        orderDetailsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        // Action buttons
        Button startPreparing = new Button("🔥 Start Preparing");
        startPreparing.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
        startPreparing.setOnAction(e -> {
            Order selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                updateOrderStatus(selected, "preparing");
            }
        });

        Button markReady = new Button("✅ Mark Ready");
        markReady.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        markReady.setOnAction(e -> {
            Order selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                updateOrderStatus(selected, "ready");
            }
        });

        Button cancelOrder = new Button("❌ Cancel Order");
        cancelOrder.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");
        cancelOrder.setOnAction(e -> {
            Order selected = list.getSelectionModel().getSelectedItem();
            if (selected != null) {
                updateOrderStatus(selected, "cancelled");
            }
        });

        orderActionButtons.getChildren().addAll(startPreparing, markReady, cancelOrder);
        orderActionButtons.setAlignment(Pos.CENTER);
        
        orderDetailsPanel.getChildren().addAll(orderDetailsTitle, orderDetailsContent, orderActionButtons);
    }

    private void showOrderDetails(Order order) {
        orderDetailsContent.getChildren().clear();
        
        // Order ID and Status
        Label orderIdLabel = new Label("Order #" + order.getOrderID());
        orderIdLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        
        Label statusLabel = new Label("Status: " + order.getStatus().toUpperCase());
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        // Student Information
        Label studentLabel = new Label("Student ID: " + order.getStudentId());
        studentLabel.setStyle("-fx-font-size: 12px;");
        
        // Item Details - Show all items in the order
        List<MenuItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            Label itemsHeaderLabel = new Label("Order Items:");
            itemsHeaderLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
            orderDetailsContent.getChildren().add(itemsHeaderLabel);
            
            // Group items by name and show quantities
            Map<String, Long> itemCounts = items.stream()
                .collect(Collectors.groupingBy(MenuItem::getName, Collectors.counting()));
            
            for (Map.Entry<String, Long> entry : itemCounts.entrySet()) {
                String itemName = entry.getKey();
                Long count = entry.getValue();
                
                // Find the first item to get the price
                MenuItem firstItem = items.stream()
                    .filter(item -> item.getName().equals(itemName))
                    .findFirst()
                    .orElse(null);
                
                if (firstItem != null) {
                    String quantityText = count == 1 ? "" : " x" + count;
                    Label itemLabel = new Label("• " + itemName + quantityText + " - " + String.format("%.0f", firstItem.getPrice()) + " EGP");
                    itemLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #333;");
                    orderDetailsContent.getChildren().add(itemLabel);
                }
            }
        } else {
            Label noItemsLabel = new Label("No items in order");
            noItemsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666; -fx-font-style: italic;");
            orderDetailsContent.getChildren().add(noItemsLabel);
        }
        
        // Order Summary
        Label summaryLabel = new Label("Order Summary:");
        summaryLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        int itemCount = items != null ? items.size() : 0;
        Label quantityLabel = new Label("Total Items: " + itemCount);
        quantityLabel.setStyle("-fx-font-size: 12px;");
        
        Label priceLabel = new Label("Total Cost: " + String.format("%.0f", order.getTotalCost()) + " EGP");
        priceLabel.setStyle("-fx-font-size: 12px;");
        
        // Order Date
        if (order.getDate() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy 'at' HH:mm:ss");
            String formattedDate = sdf.format(order.getDate());
            
            Label dateLabel = new Label("Order Date: " + formattedDate);
            dateLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
            orderDetailsContent.getChildren().add(dateLabel);
        }
        
        // Add all labels to content
        orderDetailsContent.getChildren().addAll(
            orderIdLabel, statusLabel, studentLabel, summaryLabel, quantityLabel, priceLabel
        );
        
        // Show action buttons based on current status
        updateActionButtons(order.getStatus());
        
        orderDetailsPanel.setVisible(true);
        orderDetailsPanel.setManaged(true);
    }

    private void hideOrderDetails() {
        orderDetailsPanel.setVisible(false);
        orderDetailsPanel.setManaged(false);
    }

    private void updateActionButtons(String currentStatus) {
        orderActionButtons.getChildren().clear();
        
        if ("pending".equalsIgnoreCase(currentStatus)) {
            Button startPreparing = new Button("🔥 Start Preparing");
            startPreparing.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
            startPreparing.setOnAction(e -> {
                Order selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    updateOrderStatus(selected, "preparing");
                }
            });
            
            Button cancelOrder = new Button("❌ Cancel Order");
            cancelOrder.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");
            cancelOrder.setOnAction(e -> {
                Order selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    updateOrderStatus(selected, "cancelled");
                }
            });
            
            orderActionButtons.getChildren().addAll(startPreparing, cancelOrder);
        } else if ("preparing".equalsIgnoreCase(currentStatus)) {
            Button markReady = new Button("✅ Mark Ready");
            markReady.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
            markReady.setOnAction(e -> {
                Order selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    updateOrderStatus(selected, "ready");
                }
            });
            
            orderActionButtons.getChildren().addAll(markReady);
        } else if ("ready".equalsIgnoreCase(currentStatus)) {
            Button markPickedUp = new Button("📦 Mark Picked Up");
            markPickedUp.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold;");
            markPickedUp.setOnAction(e -> {
                Order selected = list.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    updateOrderStatus(selected, "completed");
                }
            });
            
            orderActionButtons.getChildren().addAll(markPickedUp);
        }
        
        orderActionButtons.setAlignment(Pos.CENTER);
    }

    private void updateOrderStatus(Order order, String newStatus) {
        try {
            orderRepo.updateOrderStatus(order.getOrderID(), newStatus);
            info("Order #" + order.getOrderID() + " status updated to: " + newStatus.toUpperCase());
        load();
            
            // Update the selected order details
            Order updatedOrder = list.getSelectionModel().getSelectedItem();
            if (updatedOrder != null) {
                showOrderDetails(updatedOrder);
            }
        } catch (Exception e) {
            error("Failed to update order status: " + e.getMessage());
        }
    }

    private void load() {
        orders.clear();
        try {
            List<Order> allOrders = orderRepo.getAllOrders();
            for (Order o : allOrders) {
                // Show pending, preparing, and ready orders for staff to manage
                if ("pending".equalsIgnoreCase(o.getStatus()) || 
                    "preparing".equalsIgnoreCase(o.getStatus()) || 
                    "ready".equalsIgnoreCase(o.getStatus())) {
                orders.add(o);
                }
            }
            
            if (orders.isEmpty()) {
                info("No pending, preparing, or ready orders at the moment.");
            }
        } catch (Exception e) {
            error("Failed to load orders: " + e.getMessage());
        }
    }

    private void info(String m) { 
        Alert alert = new Alert(Alert.AlertType.INFORMATION, m);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.showAndWait(); 
    }
    
    private void error(String m) { 
        Alert alert = new Alert(Alert.AlertType.ERROR, m);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.showAndWait(); 
    }
}