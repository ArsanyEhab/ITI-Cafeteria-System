package ui;

import contracts.ILoyaltyProgram;
import contracts.IOrderRepository;
import domain.MenuItem;
import domain.Order;
import domain.Student;

import domain.Reward;
import infrastructure.DatabaseLoyaltyRepository;
import infrastructure.DatabaseOrderRepository;
import infrastructure.MenuOperationsRepository;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.PointsPerEGPReward;
import services.RecommendationSystem;
import infrastructure.DatabaseRepository;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

public class StudentDashboardView extends BorderPane {
    private final Student student;
    private final IOrderRepository orderRepo = new DatabaseOrderRepository();
    private final ILoyaltyProgram loyalty = new DatabaseLoyaltyRepository(new PointsPerEGPReward());
    private final MenuOperationsRepository menuRepo = new MenuOperationsRepository();
    private final RecommendationSystem recommendationSystem = new RecommendationSystem();
    private Button refreshAllButton; // Reference to refresh button for later configuration


    // Menu and cart data
    private final ObservableList<MenuItem> menuData = FXCollections.observableArrayList();
    private final ObservableList<CartItem> cartItems = FXCollections.observableArrayList();
    private final ObservableList<Order> studentOrders = FXCollections.observableArrayList();
    
    // UI Components
    private final ListView<CartItem> cartList = new ListView<>(cartItems);
    private final ListView<Order> ordersList = new ListView<>(studentOrders);
    
    // Labels
    private final Label pointsLbl = new Label();
    private final Label nameLbl = new Label();
    private final Label idLbl = new Label();
    private final Label cartTotalLbl = new Label("Cart Total: 0 EGP");
    private final Label cartCountLbl = new Label("Items: 0");
    
    // Order discount tracking
    private double currentOrderDiscount = 0.0;
    private final Label discountLbl = new Label("Discount: 0 EGP");
    private final Label discountPreviewLbl = new Label("Preview: Loading...");
    
    // Timer for debouncing preview updates
    private Timer previewUpdateTimer;
    
    // Cache exchange rate to avoid repeated database calls
    private int cachedExchangeRate = -1;
    
    // ✅ Notification-related fields
    private ListView<Map<String, Object>> notificationsList;
    private java.util.Timer notificationTimer;
    private Label newNotificationIndicator; // ✅ New notification indicator
    private infrastructure.ConsoleNotificationService notificationService; // ✅ Notification service instance

    // Cart item class for managing quantity
    private static class CartItem {
        private final MenuItem item;
        private int quantity;
        private final boolean isFreeItem; // Flag to identify free items (redeemed rewards)
        
        public CartItem(MenuItem item, int quantity) {
            this.item = item;
            this.quantity = quantity;
            this.isFreeItem = (item.getPrice() == 0.0); // Auto-detect free items
        }
        
        public CartItem(MenuItem item, int quantity, boolean isFreeItem) {
            this.item = item;
            this.quantity = quantity;
            this.isFreeItem = isFreeItem;
        }
        
        public MenuItem getItem() { return item; }
        public int getQuantity() { return quantity; }
        public void setQuantity(int quantity) { 
            // Prevent increasing quantity for free items
            if (isFreeItem && quantity > 1) {
                this.quantity = 1; // Keep free items at quantity 1
            } else {
                this.quantity = quantity;
            }
        }
        public double getTotalPrice() { return item.getPrice() * quantity; }
        public boolean isFreeItem() { return isFreeItem; }
        
        @Override
        public String toString() {
            if (isFreeItem) {
                return item.getName() + " x" + quantity + " (Redeemed Reward)";
            } else {
                return item.getName() + " x" + quantity + " (" + (int)item.getPrice() + " EGP each) - " + (int)getTotalPrice() + " EGP";
            }
        }
    }

    public StudentDashboardView(Stage stage, Student s) {
        this.student = s;
        this.notificationService = new infrastructure.ConsoleNotificationService(); // ✅ Initialize notification service
        setPadding(new Insets(16));

        // Top bar
        HBox top = new HBox(16);
        top.setAlignment(Pos.CENTER_LEFT);
        nameLbl.setText("Name: " + student.getName());
        idLbl.setText("ID: " + student.getStudentID());
        
        // Right side of top bar
        HBox topRight = new HBox(10);
        topRight.setAlignment(Pos.CENTER_RIGHT);
        
        Button refreshAll = new Button("🔄 Refresh All");
        refreshAll.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        // Store reference to refresh button for later configuration
        this.refreshAllButton = refreshAll;
        
        Button logout = new Button("Logout");
        logout.setOnAction(e -> stage.getScene().setRoot(new LoginRegisterView(stage)));
        
        topRight.getChildren().addAll(refreshAll, logout);
        
        // Use BorderPane layout to put left content and right content
        top.getChildren().addAll(nameLbl, idLbl);
        HBox spacer = new HBox();
        HBox.setHgrow(spacer, Priority.ALWAYS); // This pushes the right content to the right
        top.getChildren().addAll(spacer, topRight);
        setTop(top);

        // Left: loyalty and cart
        VBox left = new VBox(10);
        left.getStyleClass().add("card");
        left.setPadding(new Insets(12));
        left.setPrefWidth(450);
        
        // Loyalty section
        VBox loyaltyBox = new VBox(6);
        loyaltyBox.getStyleClass().add("card");
        loyaltyBox.setPadding(new Insets(6));
        pointsLbl.setText("Points: " + student.getLoyaltyPoints());
        Button refresh = new Button("Refresh Points");
        refresh.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        refresh.setOnAction(e -> {
            refresh.setDisable(true);
            refresh.setText("Refreshing...");
            refreshStudentPointsFromDatabase();
            // Re-enable button after a short delay
            new Thread(() -> {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                }
                javafx.application.Platform.runLater(() -> {
                    refresh.setDisable(false);
                    refresh.setText("Refresh Points");
                });
            }).start();
        });

        
        // Exchange points section
        VBox exchangeBox = new VBox(6);
        exchangeBox.getStyleClass().add("card");
        exchangeBox.setPadding(new Insets(6));
        
        Label exchangeRateLbl = new Label("Exchange Rate: Loading...");
        exchangeRateLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666;");
        
                // Points input for exchange
        HBox exchangeInputs = new HBox(8);
        exchangeInputs.setAlignment(Pos.CENTER_LEFT);

        TextField pointsToExchange = new TextField("50");
        pointsToExchange.setPrefWidth(80);
        pointsToExchange.setPrefHeight(25);
        pointsToExchange.setPromptText("Enter points");
        
        // Add debounced listener to update preview when text field changes
        pointsToExchange.textProperty().addListener((obs, oldVal, newVal) -> {
            // Clear any existing timer
            if (previewUpdateTimer != null) {
                previewUpdateTimer.cancel();
            }
            
            // Create new timer with 500ms delay
            previewUpdateTimer = new Timer();
            previewUpdateTimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    Platform.runLater(() -> {
                        try {
                            if (!newVal.isEmpty()) {
                                int points = Integer.parseInt(newVal);
                                updateDiscountPreview(points, discountPreviewLbl);
                            } else {
                                discountPreviewLbl.setText("Preview: Enter points to see discount");
                                discountPreviewLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-style: italic;");
                            }
                        } catch (NumberFormatException e) {
                            // Handle invalid input
                            discountPreviewLbl.setText("Preview: Please enter a valid number");
                            discountPreviewLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #f44336; -fx-font-style: italic;");
                        }
                    });
                }
            }, 500); // 500ms delay
        });

        Button exchangeBtn = new Button("Exchange for Discount");
        exchangeBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        exchangeBtn.setOnAction(e -> {
            try {
                int points = Integer.parseInt(pointsToExchange.getText());
                exchangePoints(points);
            } catch (NumberFormatException ex) {
                alert("Please enter a valid number of points.");
            }
        });

        exchangeInputs.getChildren().addAll(
            new Label("Points:"), pointsToExchange, exchangeBtn
        );
        
        // Preview label showing discount amount
        discountPreviewLbl.setText("Preview: 50 points = 1.00 EGP discount");
        discountPreviewLbl.setStyle("-fx-font-size: 11px; -fx-text-fill: #666; -fx-font-style: italic;");
        discountPreviewLbl.setAlignment(Pos.CENTER);
        
        // Initialize preview with current exchange rate
        updateDiscountPreview(50, discountPreviewLbl);
        
        exchangeBox.getChildren().addAll(
            new Label("Exchange Points"),
            exchangeRateLbl,
            exchangeInputs,
            discountPreviewLbl
        );
        
        // Load exchange rate
        loadExchangeRate(exchangeRateLbl);
        
        loyaltyBox.getChildren().addAll(new Label("Loyalty Points"), pointsLbl, refresh, exchangeBox);
        
        // Cart section
        VBox cartBox = new VBox(6);
        cartBox.getStyleClass().add("card");
        cartBox.setPadding(new Insets(6));
        
        // Cart list with custom cell factory for item management
        cartList.setFixedCellSize(35); // Make rows more compact
        cartList.setCellFactory(list -> new ListCell<CartItem>() {
            @Override
            protected void updateItem(CartItem cartItem, boolean empty) {
                super.updateItem(cartItem, empty);
                if (empty || cartItem == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    // Create controls for each cart item
                    HBox itemControls = new HBox(12);
                    itemControls.setAlignment(Pos.CENTER_LEFT);
                    
                    // Item info with special styling for free items
                    HBox itemInfoBox = new HBox(8);
                    itemInfoBox.setAlignment(Pos.CENTER_LEFT);
                    
                    // Add a special badge for free items
                    if (cartItem.isFreeItem()) {
                        Label freeBadge = new Label("🎁 FREE");
                        freeBadge.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; -fx-padding: 2 6; -fx-background-radius: 10;");
                        itemInfoBox.getChildren().add(freeBadge);
                    }
                    
                    Label itemInfo = new Label(cartItem.toString());
                    itemInfo.setPrefWidth(200); // Reduced width to accommodate badge
                    if (cartItem.isFreeItem()) {
                        itemInfo.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
                        itemInfo.setTooltip(new Tooltip("Redeemed reward - quantity cannot be changed"));
                    }
                    
                    itemInfoBox.getChildren().add(itemInfo);
                    
                    // Quantity spinner (with built-in up/down arrows)
                    Spinner<Integer> qtySpinner = new Spinner<>(1, 999, cartItem.getQuantity());
                    qtySpinner.setPrefWidth(60);
                    qtySpinner.setPrefHeight(25);
                    
                    // ✅ FREE ITEMS ARE COMPLETELY NON-EDITABLE
                    if (cartItem.isFreeItem()) {
                        qtySpinner.setDisable(true);
                        qtySpinner.setTooltip(new Tooltip("Redeemed reward - quantity cannot be changed"));
                        qtySpinner.setStyle("-fx-opacity: 0.5; -fx-background-color: #f0f0f0;");
                    }
                    
                    qtySpinner.valueProperty().addListener((obs, oldVal, newVal) -> {
                        if (newVal != null && newVal != oldVal) {
                            // For free items, completely prevent any quantity changes
                            if (cartItem.isFreeItem()) {
                                qtySpinner.getValueFactory().setValue(1); // Force back to 1
                                alert("⚠️ " + cartItem.getItem().getName() + " is a redeemed reward and cannot be modified!");
                                return;
                            }
                            cartItem.setQuantity(newVal);
                            updateCartDisplay();
                        }
                    });
                    
                    // Remove item button
                    Button removeBtn = new Button("x");
                    removeBtn.setStyle("-fx-font-size: 10px; -fx-min-width: 25px; -fx-min-height: 25px;");
                    
                    // ✅ FREE ITEMS CANNOT BE REMOVED (they're redeemed rewards)
                    if (cartItem.isFreeItem()) {
                        removeBtn.setDisable(true);
                        removeBtn.setStyle("-fx-font-size: 10px; -fx-min-width: 25px; -fx-min-height: 25px; -fx-opacity: 0.3; -fx-background-color: #cccccc;");
                        removeBtn.setTooltip(new Tooltip("Redeemed rewards cannot be removed from cart"));
                    }
                    
                    removeBtn.setOnAction(e -> {
                        if (cartItem.isFreeItem()) {
                            alert("⚠️ " + cartItem.getItem().getName() + " is a redeemed reward and cannot be removed!\n\n" +
                                  "You must use it in your order or lose the points.");
                            return;
                        }
                        removeFromCart(cartItem);
                    });
                    
                    itemControls.getChildren().addAll(itemInfoBox, qtySpinner, removeBtn);
                    setGraphic(itemControls);
                    setText(null);
                }
            }
        });
        
        cartBox.getChildren().addAll(
            new Label("Shopping Cart"),
            cartCountLbl,
            cartTotalLbl,
            cartList,
            buildCartControls()
        );
        
        left.getChildren().addAll(loyaltyBox, cartBox);
        setLeft(left);

        // Center: menu with category grouping
        loadMenu();
        
        // Create menu controls first (so they can be referenced in the TreeView listener)
        HBox menuControls = new HBox(10);
        Spinner<Integer> qty = new Spinner<>(1, 999, 1);
        Button addToCart = new Button("Add to Cart");
        addToCart.setDisable(true); // Disabled until item is selected
        menuControls.getChildren().addAll(new Label("Quantity:"), qty, addToCart);
        menuControls.setAlignment(Pos.CENTER_LEFT);
        
        // Create TreeView for category grouping with optimized performance
        TreeView<String> menuTree = new TreeView<>();
        menuTree.setPrefWidth(450);
        menuTree.setPrefHeight(400);
        menuTree.setShowRoot(false);
        menuTree.setFixedCellSize(25); // Fixed cell size for better performance
        
        // Build category tree with lazy loading
        TreeItem<String> root = new TreeItem<>();
        
        // Group menu items by category
        Map<String, List<MenuItem>> menuByCategory = menuData.stream()
            .collect(Collectors.<MenuItem, String>groupingBy(MenuItem::getCategory));
        
        // Create category nodes with optimized structure
        for (Map.Entry<String, List<MenuItem>> entry : menuByCategory.entrySet()) {
            String category = entry.getKey();
            List<MenuItem> items = entry.getValue();
            
            TreeItem<String> categoryNode = new TreeItem<>(category);
            categoryNode.setExpanded(true);
            
            // Add items under category with optimized creation
            for (MenuItem item : items) {
                TreeItem<String> itemNode = new TreeItem<>(item.getName() + " - " + (int)item.getPrice() + " EGP");
                categoryNode.getChildren().add(itemNode);
            }
            
            root.getChildren().add(categoryNode);
        }
        
        menuTree.setRoot(root);
        
        // Handle item selection
        menuTree.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null && newSelection.isLeaf() && newSelection.getParent() != root) {
                // Find the selected MenuItem
                String selectedText = newSelection.getValue();
                String itemName = selectedText.split(" - ")[0];
                
                MenuItem selectedItem = menuData.stream()
                    .filter(item -> item.getName().equals(itemName))
                    .findFirst()
                    .orElse(null);
                
                if (selectedItem != null) {
                    // Update quantity spinner and enable add to cart
                    qty.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 999, 1));
                    addToCart.setDisable(false);
                    addToCart.setOnAction(e -> addToCart(selectedItem, qty.getValue()));
                }
            }
        });
        
        VBox center = new VBox(10);
        center.getStyleClass().add("Card");
        center.setPadding(new Insets(12));
        
        // Loyalty redemption section
        VBox loyaltyRedemptionBox = new VBox(6);
        loyaltyRedemptionBox.getStyleClass().add("card");
        loyaltyRedemptionBox.setPadding(new Insets(8));
        loyaltyRedemptionBox.getChildren().add(new Label("Loyalty Redemption:"));
        
        // Dynamic rewards list
        ListView<Reward> rewardsList = new ListView<>();
        rewardsList.setPrefHeight(120);
        rewardsList.setCellFactory(list -> new ListCell<Reward>() {
            @Override
            protected void updateItem(Reward reward, boolean empty) {
                super.updateItem(reward, empty);
                if (empty || reward == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox rewardRow = new HBox(10);
                    rewardRow.setAlignment(Pos.CENTER_LEFT);
                    
                    Label rewardInfo = new Label(reward.getRewardName() + " (" + reward.getPointsRequired() + " pts)");
                    rewardInfo.setPrefWidth(200);
                    
                    Button redeemBtn = new Button("Redeem");
                    redeemBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-size: 10px;");
                    redeemBtn.setOnAction(e -> redeemReward(reward));
                    
                    rewardRow.getChildren().addAll(rewardInfo, redeemBtn);
                    setGraphic(rewardRow);
                    setText(null);
                }
            }
        });
        
        // Load rewards from database
        loadRewards(rewardsList);
        
        loyaltyRedemptionBox.getChildren().addAll(rewardsList);
        
        // Recommendations section
        VBox recommendationsBox = createRecommendationsSection();
        
        center.getChildren().addAll(new Label("Menu & Loyalty"), menuTree, menuControls, loyaltyRedemptionBox, recommendationsBox);
        setCenter(center);

        // Right: orders and order details
        VBox right = new VBox(10);
        right.getStyleClass().add("card");
        right.setPadding(new Insets(12));
        right.setPrefWidth(400);
        
        // Orders section
        VBox ordersBox = new VBox(8);
        ordersBox.getStyleClass().add("card");
        ordersBox.setPadding(new Insets(8));
        
        Button refreshOrders = new Button("Refresh Orders");
        refreshOrders.setOnAction(e -> loadStudentOrders());
        
        ordersList.setCellFactory(list -> new ListCell<>() {
            @Override protected void updateItem(Order order, boolean empty) {
                super.updateItem(order, empty);
                if (empty || order == null) setText(null);
                else {
                    String status = order.getStatus();
                    String statusColor = switch (status.toLowerCase()) {
                        case "pending" -> "🟡";
                        case "preparing" -> "🟠";
                        case "ready" -> "🟢";
                        case "completed" -> "✅";
                        default -> "⚪";
                    };
                    
                    // Show local time for the order
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
                    String localTime = sdf.format(order.getDate());
                    
                    setText(statusColor + " #" + order.getOrderID() + " - " + 
                           (int)order.getTotalCost() + " EGP - " + status + " - " + localTime);
                }
            }
        });
        
        // Add click listener to show order details
        ordersList.setOnMouseClicked(e -> {
            Order selectedOrder = ordersList.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                showOrderDetails(selectedOrder);
            }
        });
        
        // Add timezone info
        Label timezoneInfo = new Label("Local Time");
        timezoneInfo.setStyle("-fx-font-size: 10px; -fx-text-fill: #666; -fx-font-style: italic;");
        
        // Points History button
        Button pointsHistoryBtn = new Button("📊 Points History");
        pointsHistoryBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white; -fx-font-size: 11px;");
        pointsHistoryBtn.setOnAction(e -> showPointsHistory());
        
        // Notifications section
        VBox notificationsBox = new VBox(8);
        notificationsBox.getStyleClass().add("card");
        notificationsBox.setPadding(new Insets(8));
        
        Label notificationsTitle = new Label("🔔 Notifications");
        notificationsTitle.setStyle("-fx-font-weight: bold;");
        
        // ✅ Add new notification indicator
        this.newNotificationIndicator = new Label("✨ New");
        this.newNotificationIndicator.setStyle("-fx-text-fill: #FF6B6B; -fx-font-weight: bold; -fx-font-size: 10px;");
        this.newNotificationIndicator.setVisible(false);
        this.newNotificationIndicator.setManaged(false);
        
        HBox notificationsHeader = new HBox(8);
        notificationsHeader.setAlignment(Pos.CENTER_LEFT);
        notificationsHeader.getChildren().addAll(notificationsTitle, newNotificationIndicator);
        
        notificationsList = new ListView<>();
        notificationsList.setPrefHeight(120);
        notificationsList.setCellFactory(list -> new ListCell<Map<String, Object>>() {
            @Override
            protected void updateItem(Map<String, Object> notification, boolean empty) {
                super.updateItem(notification, empty);
                if (empty || notification == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    HBox notificationRow = new HBox(8);
                    notificationRow.setAlignment(Pos.CENTER_LEFT);
                    
                    // Notification icon based on read status (with fallback)
                    Boolean isRead = (Boolean) notification.get("is_read");
                    if (isRead == null) {
                        isRead = false; // Default to unread if is_read is null
                    }
                    
                    Label icon = new Label(isRead ? "✓" : "🔔");
                    icon.setStyle("-fx-font-size: 14px;");
                    
                    VBox notificationContent = new VBox(2);
                    Label message = new Label((String) notification.get("message"));
                    message.setWrapText(true);
                    message.setMaxWidth(200);
                    
                    SimpleDateFormat sdf = new SimpleDateFormat("dd/MM HH:mm");
                    String localTime = sdf.format((java.util.Date) notification.get("created_at"));
                    Label time = new Label(localTime);
                    time.setStyle("-fx-font-size: 10px; -fx-text-fill: #666;");
                    
                    notificationContent.getChildren().addAll(message, time);
                    
                    // Mark as read button (only show if is_read column exists)
                    Button markReadBtn = new Button("✓");
                    markReadBtn.setStyle("-fx-font-size: 10px; -fx-min-width: 20px; -fx-min-height: 20px;");
                    markReadBtn.setOnAction(e -> markNotificationAsRead(notification));
                    
                    // Hide mark as read button if is_read column doesn't exist
                    if (isRead == null) {
                        markReadBtn.setVisible(false);
                        markReadBtn.setManaged(false);
                    }
                    
                    notificationRow.getChildren().addAll(icon, notificationContent, markReadBtn);
                    setGraphic(notificationRow);
                    setText(null);
                }
            }
        });
        
        Button refreshNotifications = new Button("🔄 Refresh");
        refreshNotifications.setStyle("-fx-font-size: 11px;");
        refreshNotifications.setOnAction(e -> loadNotifications());
        
        HBox notificationButtons = new HBox(8);
        notificationButtons.getChildren().addAll(refreshNotifications);
        
        notificationsBox.getChildren().addAll(notificationsHeader, notificationsList, notificationButtons);
        
        // Configure refresh button after variables are declared
        configureRefreshButton();
        
        ordersBox.getChildren().addAll(new Label("My Orders"), timezoneInfo, refreshOrders, pointsHistoryBtn, notificationsBox, ordersList);
        
        // Order details panel (initially hidden)
        VBox orderDetailsBox = new VBox(8);
        orderDetailsBox.getStyleClass().add("card");
        orderDetailsBox.setPadding(new Insets(8));
        orderDetailsBox.setVisible(false);
        orderDetailsBox.setManaged(false);
        
        Label detailsTitle = new Label("Order Details");
        detailsTitle.getStyleClass().add("title");
        
        VBox detailsContent = new VBox(6);
        detailsContent.setId("orderDetailsContent");
        
        // Action buttons
        HBox actionButtons = new HBox(8);
        actionButtons.setAlignment(Pos.CENTER);
        
        Button closeDetails = new Button("Close Details");
        closeDetails.setOnAction(e -> {
            orderDetailsBox.setVisible(false);
            orderDetailsBox.setManaged(false);
        });
        
        Button orderPickedUp = new Button("Order Picked Up");
        orderPickedUp.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        orderPickedUp.setOnAction(e -> {
            Order selectedOrder = ordersList.getSelectionModel().getSelectedItem();
            if (selectedOrder != null) {
                markOrderAsPickedUp(selectedOrder);
            }
        });
        
        actionButtons.getChildren().addAll(closeDetails, orderPickedUp);
        
        orderDetailsBox.getChildren().addAll(detailsTitle, detailsContent, actionButtons);
        
        right.getChildren().addAll(ordersBox, orderDetailsBox);
        setRight(right);
        
        // Load initial data
        loadStudentOrders();
        refreshPoints();
        
        // ✅ Initialize notification count for this student
        notificationService.initializeNotificationCount(student.getStudentID());
        
        // Load initial notifications
        loadNotifications();
        
        // ✅ Start auto-refresh timer for notifications (every 30 seconds)
        startNotificationTimer();
    }

    private void loadMenu() {
        menuData.clear();
        menuData.addAll(menuRepo.getMenu());
        if (menuData.isEmpty()) {
            // fallback hardcoded
            menuData.addAll(List.of(
                new MenuItem(1,"Shawarma","",150,"Food"),
                new MenuItem(2,"Pizza Mix Cheese","",200,"Food"),
                new MenuItem(3,"Pizza Chicken","",250,"Food"),
                new MenuItem(4,"Pizza Meat","",350,"Food"),
                new MenuItem(5,"Single Burger","",100,"Food"),
                new MenuItem(6,"Double Burger","",150,"Food"),
                new MenuItem(7,"Coffee","",30,"Drink"),
                new MenuItem(8,"Tea","",15,"Drink"),
                new MenuItem(9,"Espresso","",40,"Drink"),
                new MenuItem(10,"Nescafe","",50,"Drink"),
                new MenuItem(11,"Cappuccino","",70,"Drink"),
                new MenuItem(12,"Fresh Juice","",120,"Drink")
            ));
        }
        
        // Refresh the menu tree if it exists
        Platform.runLater(() -> {
            if (getCenter() != null && getCenter() instanceof VBox) {
                VBox center = (VBox) getCenter();
                for (Node node : center.getChildren()) {
                    if (node instanceof TreeView) {
                        refreshMenuTree((TreeView<String>) node);
                        break;
                    }
                }
            }
        });
    }
    
    private void refreshMenuTree(TreeView<String> menuTree) {
        if (menuTree == null) return;
        
        // Clear existing tree
        TreeItem<String> root = new TreeItem<>();
        
        // Group menu items by category
        Map<String, List<MenuItem>> menuByCategory = menuData.stream()
            .collect(Collectors.<MenuItem, String>groupingBy(MenuItem::getCategory));
        
        // Create category nodes with optimized structure
        for (Map.Entry<String, List<MenuItem>> entry : menuByCategory.entrySet()) {
            String category = entry.getKey();
            List<MenuItem> items = entry.getValue();
            
            TreeItem<String> categoryNode = new TreeItem<>(category);
            categoryNode.setExpanded(true);
            
            // Add items under category with optimized creation
            for (MenuItem item : items) {
                TreeItem<String> itemNode = new TreeItem<>(item.getName() + " - " + (int)item.getPrice() + " EGP");
                categoryNode.getChildren().add(itemNode);
            }
            
            root.getChildren().add(categoryNode);
        }
        
        menuTree.setRoot(root);
    }

    private void addToCart(MenuItem item, int quantity) {
        if (item == null) {
            alert("Please select an item first.");
            return;
        }
        
        // ✅ ALLOW SEPARATE ROWS FOR FREE AND REGULAR ITEMS
        
        // Check if item already exists in cart as a regular item
        for (CartItem cartItem : cartItems) {
            if (cartItem.getItem().getId() == item.getId() && !cartItem.isFreeItem()) {
                // Only merge with regular items, not free items
                cartItem.setQuantity(cartItem.getQuantity() + quantity);
                updateCartDisplay();
                alert("Added " + quantity + "x " + item.getName() + " to cart!");
                return;
            }
        }
        
        // Add new item to cart (will be in its own row)
        cartItems.add(new CartItem(item, quantity));
        updateCartDisplay();
        alert("Added " + quantity + "x " + item.getName() + " to cart!");
    }

    private void removeFromCart(CartItem cartItem) {
        // ✅ FREE ITEMS CANNOT BE REMOVED (they're redeemed rewards)
        if (cartItem.isFreeItem()) {
            alert("⚠️ " + cartItem.getItem().getName() + " is a redeemed reward and cannot be removed!\n\n" +
                  "You must use it in your order or lose the points.\n\n" +
                  "If you want to remove it, you'll need to place the order first.");
            return;
        }
        
        cartItems.remove(cartItem);
        updateCartDisplay();
        alert("Removed " + cartItem.getItem().getName() + " from cart.");
    }

    private void updateCartDisplay() {
        int totalItems = cartItems.stream().mapToInt(CartItem::getQuantity).sum();
        double totalCost = cartItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
        
        cartCountLbl.setText("Items: " + totalItems);
        
        // Update cart list to reflect changes
        cartList.refresh();
        
        // Visual feedback for empty cart
        if (cartItems.isEmpty()) {
            cartCountLbl.setText("Items: 0 (Cart is empty)");
            cartCountLbl.setStyle("-fx-text-fill: #666; -fx-font-style: italic;");
        } else {
            cartCountLbl.setStyle("-fx-text-fill: black; -fx-font-style: normal;");
        }
        
        updateDiscountDisplay();
    }
    
    private void updateDiscountDisplay() {
        double totalCost = cartItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
        double finalCost = totalCost - currentOrderDiscount;
        
        if (currentOrderDiscount > 0) {
            discountLbl.setText("Discount: " + String.format("%.2f", currentOrderDiscount) + " EGP");
            discountLbl.setStyle("-fx-text-fill: #4CAF50; -fx-font-weight: bold;");
        } else {
            discountLbl.setText("Discount: 0 EGP");
            discountLbl.setStyle("-fx-text-fill: #666;");
        }
        
        // Update final cost display
        if (finalCost > 0) {
            cartTotalLbl.setText("Cart Total: " + (int)totalCost + " EGP - " + 
                                String.format("%.2f", currentOrderDiscount) + " EGP = " + 
                                String.format("%.2f", finalCost) + " EGP");
        } else {
            cartTotalLbl.setText("Cart Total: " + (int)totalCost + " EGP - " + 
                                String.format("%.2f", currentOrderDiscount) + " EGP = FREE!");
        }
    }

    private Node buildCartControls() {
        VBox controls = new VBox(8);
        
        // Payment method selection
        VBox paymentBox = new VBox(6);
        paymentBox.getStyleClass().add("card");
        paymentBox.setPadding(new Insets(8));
        paymentBox.getChildren().add(new Label("Payment Method:"));
        
        ToggleGroup paymentGroup = new ToggleGroup();
        RadioButton cashRadio = new RadioButton("💵 Cash");
        RadioButton cardRadio = new RadioButton("💳 Card");
        
        cashRadio.setToggleGroup(paymentGroup);
        cardRadio.setToggleGroup(paymentGroup);
        cashRadio.setSelected(true); // Default to cash
        
        paymentBox.getChildren().addAll(cashRadio, cardRadio);
        
        // Cart management buttons
        HBox cartButtons = new HBox(8);
        Button clearCart = new Button("Clear Cart");
        clearCart.setStyle("-fx-background-color: #ff6b6b; -fx-text-fill: white;");
        clearCart.setOnAction(e -> {
            if (!cartItems.isEmpty()) {
                // Check if there are free items in the cart
                boolean hasFreeItems = cartItems.stream().anyMatch(CartItem::isFreeItem);
                
                if (hasFreeItems) {
                    // Warn about losing points for free items
                    boolean confirmed = showConfirmDialog(
                        "Clear Cart with Free Items",
                        "⚠️ Warning: Free Items in Cart",
                        "You have redeemed rewards in your cart. Clearing the cart will cause you to lose these items and the points you spent.\n\n" +
                        "Are you sure you want to clear the cart?"
                    );
                    
                    if (!confirmed) {
                        return; // User cancelled
                    }
                }
                
                cartItems.clear();
                currentOrderDiscount = 0.0; // Reset discount when clearing cart
                updateCartDisplay();
                alert("Cart cleared successfully!");
            }
        });
        
        // Discount display
        discountLbl.setStyle("-fx-text-fill: #666;");
        discountLbl.setAlignment(Pos.CENTER);
        

        
        Button placeOrder = new Button("Place Order");
        placeOrder.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        placeOrder.setOnAction(e -> {
            String paymentMethod = cashRadio.isSelected() ? "Cash" : "Card";
            placeOrderFromCart(paymentMethod);
        });
        
        cartButtons.getChildren().addAll(clearCart, placeOrder);
        
        controls.getChildren().addAll(paymentBox, discountLbl, cartButtons);
        return controls;
    }

    private void placeOrderFromCart(String paymentMethod) {
        if (cartItems.isEmpty()) {
            alert("Your cart is empty!");
            return;
        }
        
        double totalCost = cartItems.stream().mapToDouble(CartItem::getTotalPrice).sum();
        double finalCost = totalCost - currentOrderDiscount;
        
        // Validate final cost
        if (finalCost < 0) {
            alert("❌ Invalid discount!\n\n" +
                  "Total cost: " + (int)totalCost + " EGP\n" +
                  "Discount: " + String.format("%.2f", currentOrderDiscount) + " EGP\n" +
                  "Final cost cannot be negative.\n\n" +
                  "Please reduce your discount or add more items.");
            return;
        }
        
        // Create order with discount using Cairo time
        Order order = new Order(
            0, // orderID will be auto-generated by database
            student.getStudentID(),
            totalCost, // Original total cost
            currentOrderDiscount, // Applied discount
            "pending", 
            student, 
            CrossCutting.TimeZoneConverter.getCurrentCairoTime() // Use Cairo time
        );
        
        // Add all cart items to order
        for (CartItem cartItem : cartItems) {
            for (int i = 0; i < cartItem.getQuantity(); i++) {
                order.addItem(cartItem.getItem());
            }
        }
        
        // Place order
        boolean success = orderRepo.placeOrder(order);
        if (!success) {
            alert("Failed to place order. Please try again.");
            return;
        }
        
        // Update recommendation system with the new order
        recommendationSystem.updateFromOrder(order);
        

        
        // Show order confirmation
        StringBuilder bill = new StringBuilder();
        bill.append("✅ Order Placed Successfully!\n");
        bill.append("Name: ").append(student.getName()).append("\n");
        bill.append("ID: ").append(student.getStudentID()).append("\n");
        bill.append("Order #: ").append(order.getOrderID()).append("\n");
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
        bill.append("Date: ").append(sdf.format(order.getDate())).append("\n\n");
        bill.append("Items:\n");
        
        for (CartItem cartItem : cartItems) {
            bill.append("  ").append(cartItem.getItem().getName())
                .append(" x").append(cartItem.getQuantity())
                .append(" - ").append((int)cartItem.getTotalPrice()).append(" EGP\n");
        }
        
        bill.append("\nSubtotal: ").append((int)totalCost).append(" EGP\n");
        
        if (currentOrderDiscount > 0) {
            bill.append("Discount: -").append(String.format("%.2f", currentOrderDiscount)).append(" EGP\n");
            bill.append("Final Cost: ").append(String.format("%.2f", finalCost)).append(" EGP\n");
        } else {
            bill.append("Total: ").append((int)finalCost).append(" EGP\n");
        }
        
        bill.append("Payment: ").append(paymentMethod).append("\n");
        bill.append("Status: pending\n\n");
        bill.append("Note: Loyalty points will be awarded when your order is confirmed by staff.");
        
        alert(bill.toString());
        
        // Clear cart and reset discount
        cartItems.clear();
        currentOrderDiscount = 0.0;
        
        updateCartDisplay();
        loadStudentOrders();
        refreshPoints();
    }

    private void loadStudentOrders() {
        studentOrders.clear();
        List<Order> allOrders = orderRepo.getAllOrders();
        
        // Filter orders for this student
        for (Order order : allOrders) {
            if (Integer.parseInt(order.getStudentId()) == Integer.parseInt(student.getStudentID())) {
                studentOrders.add(order);
            }
        }
    }

    private void refreshPoints() {
        // Refresh student data to get updated points
        pointsLbl.setText("Points: " + student.getLoyaltyPoints());
        // Points refreshed silently
    }

    private void refreshStudentPointsFromDatabase() {
        // Get fresh points from database and update student object
        try {
            // Create a new connection to get fresh data
            Connection con = DatabaseRepository.getConnection();
            if (con != null) {
                try {
                    String sql = "SELECT loyalty_points FROM students WHERE student_id = ?";
                    try (PreparedStatement pstmt = con.prepareStatement(sql)) {
                        pstmt.setString(1, student.getStudentID());
                        try (ResultSet rs = pstmt.executeQuery()) {
                            if (rs.next()) {
                                int freshPoints = rs.getInt("loyalty_points");
                                student.setLoyaltyPoints(freshPoints);
                                pointsLbl.setText("Points: " + freshPoints);

                            }
                        }
                    }
                } finally {
                    DatabaseRepository.returnConnection(con);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to refresh loyalty points: " + e.getMessage());
            // Fallback to current value
            pointsLbl.setText("Points: " + student.getLoyaltyPoints());
        }
    }



    private void loadRewards(ListView<Reward> rewardsList) {
        try {
            DatabaseLoyaltyRepository loyaltyRepo = (DatabaseLoyaltyRepository) loyalty;
            List<Reward> availableRewards = loyaltyRepo.getAllRewards();
            rewardsList.getItems().clear();
            rewardsList.getItems().addAll(availableRewards);
            
            if (availableRewards.isEmpty()) {
                rewardsList.getItems().add(new Reward(0, "No rewards available", 0, 0) {
                    @Override
                    public String toString() {
                        return "No rewards available - Contact admin";
                    }
                });
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to load rewards: " + e.getMessage());
            rewardsList.getItems().clear();
            rewardsList.getItems().add(new Reward(0, "Error loading rewards", 0, 0) {
                @Override
                public String toString() {
                    return "Error loading rewards - Try again later";
                }
            });
        }
    }

    private void redeemReward(Reward reward) {
        if (reward.getRewardId() == 0) {
            alert("Invalid reward selected.");
            return;
        }
        
        if (student.getLoyaltyPoints() < reward.getPointsRequired()) {
            alert("Not enough points! You need " + reward.getPointsRequired() + " points to redeem this reward.");
            return;
        }
        

        
        // Get the actual menu item for this reward
        DatabaseLoyaltyRepository loyaltyRepo = (DatabaseLoyaltyRepository) loyalty;
        MenuItem rewardedItem = loyaltyRepo.getRewardedMenuItem(reward.getRewardId());
        
        if (rewardedItem == null) {
            alert("❌ Error: Could not find the rewarded item. Please contact admin.\n\n" +
                  "Debug Info:\n" +
                  "Reward ID: " + reward.getRewardId() + "\n" +
                  "Rewarded Item ID: " + reward.getRewardedItemId());
            return;
        }
        

        
        // ✅ CHECK CART AVAILABILITY BEFORE DEDUCTING POINTS
        
        // Create the free item to check cart availability
        MenuItem freeItem = new MenuItem(
            rewardedItem.getId(),
            rewardedItem.getName(),
            rewardedItem.getDescription(),
            0.0, // Free item
            rewardedItem.getCategory()
        );
        
        // ✅ ALLOW FREE ITEMS TO BE ADDED EVEN IF REGULAR ITEMS EXIST
        
        // Only check for duplicate free items (not regular items)
        for (CartItem existingItem : cartItems) {
            if (existingItem.getItem().getId() == freeItem.getId() && existingItem.isFreeItem()) {
                alert("⚠️ You already have " + freeItem.getName() + " in your cart as a redeemed reward!\n\n" +
                      "You cannot redeem the same reward multiple times.");
                return; // Exit without deducting points
            }
        }
        
        // ✅ FREE ITEMS CAN COEXIST WITH REGULAR ITEMS OF THE SAME TYPE
        
        // ✅ POINTS ARE ONLY DEDUCTED AFTER SUCCESSFUL CART VALIDATION
        
        // Now redeem the reward (deduct points and record redemption)
        loyalty.redeemPoints(student, reward.getRewardName());
        
        // ✅ ADD TO CART AFTER POINTS ARE DEDUCTED
        
        // Add to cart as a free item (cannot increase quantity)
        cartItems.add(new CartItem(freeItem, 1, true)); // Explicitly mark as free item
        updateCartDisplay();
        
        // Show success message
        alert("✅ You redeemed " + reward.getPointsRequired() + " points for a FREE " + rewardedItem.getName() + "!\n\n" +
              "The item has been added to your cart with 0 EGP price.");
        
        // Refresh points AFTER successful cart addition
        refreshStudentPointsFromDatabase();
    }







    private boolean showConfirmDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }

    private void showOrderDetails(Order order) {
        // Find the order details panel in the right side
        VBox rightPanel = (VBox) getRight();
        VBox orderDetailsBox = (VBox) rightPanel.getChildren().get(1); // Second child is order details
        
        // Make the panel visible
        orderDetailsBox.setVisible(true);
        orderDetailsBox.setManaged(true);
        
        // Get the details content area
        VBox detailsContent = (VBox) orderDetailsBox.getChildren().get(1);
        detailsContent.getChildren().clear();
        
        // Order header
        HBox orderHeader = new HBox(10);
        orderHeader.setAlignment(Pos.CENTER_LEFT);
        
        String status = order.getStatus();
        String statusColor = switch (status.toLowerCase()) {
            case "pending" -> "🟡";
            case "preparing" -> "🟠";
            case "ready" -> "🟢";
            case "completed" -> "✅";
            default -> "⚪";
        };
        
        Label orderIdLabel = new Label("Order #" + order.getOrderID());
        orderIdLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label statusLabel = new Label(statusColor + " " + status.toUpperCase());
        statusLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 12px;");
        
        orderHeader.getChildren().addAll(orderIdLabel, statusLabel);
        
        // Order details
        VBox details = new VBox(6);
        details.getChildren().addAll(
            new Label("Date: " + new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(order.getDate())),
            new Label("Total Cost: " + (int)order.getTotalCost() + " EGP"),
            new Label("Discount Applied: " + (int)order.getDiscountApplied() + " EGP"),
            new Label("Final Cost: " + (int)(order.getTotalCost() - order.getDiscountApplied()) + " EGP")
        );
        
        // Order items
        VBox itemsBox = new VBox(6);
        itemsBox.getChildren().add(new Label("Items:"));
        
        if (!order.getItems().isEmpty()) {
            // Group items by name and count quantities
            Map<String, Integer> itemCounts = new HashMap<>();
            for (MenuItem item : order.getItems()) {
                itemCounts.merge(item.getName(), 1, Integer::sum);
            }
            
            for (Map.Entry<String, Integer> entry : itemCounts.entrySet()) {
                HBox itemRow = new HBox(10);
                itemRow.setAlignment(Pos.CENTER_LEFT);
                
                Label itemName = new Label(entry.getKey());
                Label itemQty = new Label("x" + entry.getValue());
                itemQty.setStyle("-fx-font-weight: bold;");
                
                itemRow.getChildren().addAll(itemName, itemQty);
                itemsBox.getChildren().add(itemRow);
            }
        } else {
            itemsBox.getChildren().add(new Label("No items found"));
        }
        
        // Add all sections to the details content
        detailsContent.getChildren().addAll(orderHeader, details, itemsBox);
        
        // Show/hide "Order Picked Up" button based on status
        HBox actionButtons = (HBox) orderDetailsBox.getChildren().get(2);
        Button orderPickedUp = (Button) actionButtons.getChildren().get(1);
        
        // Only show the button for orders that are ready
        if ("ready".equalsIgnoreCase(order.getStatus())) {
            orderPickedUp.setVisible(true);
            orderPickedUp.setManaged(true);
        } else if ("completed".equalsIgnoreCase(order.getStatus())) {
            orderPickedUp.setVisible(false);
            orderPickedUp.setManaged(false);
            // Add completion message
            Label completionMsg = new Label("🎉 Order completed and picked up!");
            completionMsg.setStyle("-fx-font-weight: bold; -fx-text-fill: #4CAF50;");
            detailsContent.getChildren().add(completionMsg);
        } else {
            orderPickedUp.setVisible(false);
            orderPickedUp.setManaged(false);
        }
        
        // Scroll to the order details
        orderDetailsBox.requestFocus();
    }

    private void markOrderAsPickedUp(Order order) {
        // Update order status to "completed" in the database
        boolean success = orderRepo.updateOrderStatus(order.getOrderID(), "completed");
        
        if (success) {
            // Update the order object in memory
            order.setStatus("completed");
            
            // Show confirmation
            alert("Order #" + order.getOrderID() + " marked as picked up successfully!");
            
            // Refresh the orders list to show updated status
            loadStudentOrders();
            
            // Refresh the order details if they're currently shown
            showOrderDetails(order);
            
            // Refresh loyalty points (in case any were awarded)
            refreshStudentPointsFromDatabase();
        } else {
            alert("Failed to update order status. Please try again.");
        }
    }

    private VBox createRecommendationsSection() {
        VBox recommendationsBox = new VBox(8);
        recommendationsBox.getStyleClass().add("card");
        recommendationsBox.setPadding(new Insets(8));
        
        Label recommendationsTitle = new Label("🎯 Personalized Recommendations");
        recommendationsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        ListView<MenuItem> recommendationsList = new ListView<>();
        recommendationsList.setPrefHeight(100);
        recommendationsList.setCellFactory(list -> new ListCell<MenuItem>() {
            @Override
            protected void updateItem(MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText("⭐ " + item.getName() + " - " + String.format("%.0f", item.getPrice()) + " EGP");
                }
            }
        });
        
        // Add click listener to add recommended item to cart
        recommendationsList.setOnMouseClicked(e -> {
            MenuItem selectedItem = recommendationsList.getSelectionModel().getSelectedItem();
            if (selectedItem != null) {
                addToCart(selectedItem, 1);
                alert("✅ Added " + selectedItem.getName() + " to cart!");
            }
        });
        
        Button refreshRecommendations = new Button("🔄 Refresh");
        refreshRecommendations.setStyle("-fx-font-size: 11px;");
        refreshRecommendations.setOnAction(e -> loadRecommendations(recommendationsList));
        
        Button addAllRecommendations = new Button("📦 Add All");
        addAllRecommendations.setStyle("-fx-font-size: 11px; -fx-background-color: #4CAF50; -fx-text-fill: white;");
        addAllRecommendations.setOnAction(e -> {
            List<MenuItem> recommendations = new ArrayList<>();
            for (int i = 0; i < recommendationsList.getItems().size(); i++) {
                recommendations.add(recommendationsList.getItems().get(i));
            }
            
            if (!recommendations.isEmpty()) {
                for (MenuItem item : recommendations) {
                    addToCart(item, 1);
                }
                alert("✅ Added all " + recommendations.size() + " recommended items to cart!");
            }
        });
        
        HBox recommendationButtons = new HBox(8);
        recommendationButtons.getChildren().addAll(refreshRecommendations, addAllRecommendations);
        
        recommendationsBox.getChildren().addAll(recommendationsTitle, recommendationsList, recommendationButtons);
        
        // Load initial recommendations
        loadRecommendations(recommendationsList);
        
        return recommendationsBox;
    }
    
    private void configureRefreshButton() {
        if (refreshAllButton != null) {
            refreshAllButton.setOnAction(e -> {
                refreshAllButton.setDisable(true);
                refreshAllButton.setText("🔄 Refreshing...");
                
                // Refresh all data
                refreshStudentPointsFromDatabase();
                loadMenu();
                loadStudentOrders();
                
                // Find the rewards list and exchange rate label
                ListView<Reward> rewardsList = findRewardsList();
                Label exchangeRateLbl = findExchangeRateLabel();
                
                if (rewardsList != null) {
                    loadRewards(rewardsList);
                }
                if (exchangeRateLbl != null) {
                    loadExchangeRate(exchangeRateLbl);
                }
                
                // Refresh recommendations in center area
                refreshRecommendationsInCenter();
                
                // Re-enable button after a short delay
                new Thread(() -> {
                    try {
                        Thread.sleep(1500);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                    }
                    Platform.runLater(() -> {
                        refreshAllButton.setDisable(false);
                        refreshAllButton.setText("🔄 Refresh All");
                    });
                }).start();
            });
        }
    }
    
    private ListView<Reward> findRewardsList() {
        // Search for rewards list in the center area
        if (getCenter() != null && getCenter() instanceof VBox) {
            VBox center = (VBox) getCenter();
            for (Node node : center.getChildren()) {
                if (node instanceof VBox) {
                    VBox vbox = (VBox) node;
                    for (Node child : vbox.getChildren()) {
                        if (child instanceof ListView) {
                            ListView<?> listView = (ListView<?>) child;
                            if (listView.getItems().size() > 0 && listView.getItems().get(0) instanceof Reward) {
                                return (ListView<Reward>) listView;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private Label findExchangeRateLabel() {
        // Search for exchange rate label in the UI
        if (getLeft() != null && getLeft() instanceof VBox) {
            VBox left = (VBox) getLeft();
            for (Node node : left.getChildren()) {
                if (node instanceof VBox) {
                    VBox vbox = (VBox) node;
                    for (Node child : vbox.getChildren()) {
                        if (child instanceof Label) {
                            Label label = (Label) child;
                            if (label.getText() != null && label.getText().contains("Exchange Rate")) {
                                return label;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private void refreshRecommendationsInCenter() {
        // Find and refresh recommendations in the center area
        if (getCenter() != null && getCenter() instanceof VBox) {
            VBox center = (VBox) getCenter();
            for (Node node : center.getChildren()) {
                if (node instanceof VBox) {
                    VBox vbox = (VBox) node;
                    for (Node child : vbox.getChildren()) {
                        if (child instanceof ListView) {
                            ListView<?> listView = (ListView<?>) child;
                            if (listView.getItems().size() > 0 && listView.getItems().get(0) instanceof MenuItem) {
                                // Found recommendations list, refresh it
                                loadRecommendations((ListView<MenuItem>) listView);
                                return;
                            }
                        }
                    }
                }
            }
        }
    }
    
    private void loadRecommendations(ListView<MenuItem> recommendationsList) {
        try {
            List<MenuItem> allMenuItems = menuRepo.getMenu();
            List<MenuItem> personalizedRecommendations = recommendationSystem.getPersonalizedRecommendations(
                student.getStudentID(), allMenuItems, 5);
            
            recommendationsList.getItems().clear();
            recommendationsList.getItems().addAll(personalizedRecommendations);
            
            if (personalizedRecommendations.isEmpty()) {
                // Fallback to general recommendations
                List<MenuItem> generalRecommendations = recommendationSystem.getTopRecommendations(allMenuItems, 5);
                recommendationsList.getItems().addAll(generalRecommendations);
            }
            
        } catch (Exception e) {
            System.err.println("❌ Failed to load recommendations: " + e.getMessage());
            alert("Unable to load recommendations at this time.");
        }
    }
    
    private void showPointsHistory() {
        try {
            DatabaseLoyaltyRepository loyaltyRepo = (DatabaseLoyaltyRepository) loyalty;
            List<domain.LoyaltyTransaction> transactions = loyaltyRepo.getStudentTransactionHistory(student.getStudentID());
            
            if (transactions.isEmpty()) {
                alert("📊 Points History\n\nNo transactions found.\n\nYou haven't earned or spent any loyalty points yet.");
                return;
            }
            
            StringBuilder history = new StringBuilder();
            history.append("📊 Points History for ").append(student.getName()).append("\n");
            history.append("Current Balance: ").append(student.getLoyaltyPoints()).append(" points\n\n");
            history.append("Recent Transactions:\n");
            history.append("─────────────────\n\n");
            
            for (domain.LoyaltyTransaction transaction : transactions) {
                String sign = transaction.getPointsChanged() >= 0 ? "+" : "";
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss");
                String localTime = sdf.format(transaction.getCreatedAt());
                
                history.append("• ").append(sign).append(transaction.getPointsChanged()).append(" points\n");
                history.append("  ").append(transaction.getDescription()).append("\n");
                history.append("  ").append(localTime).append("\n\n");
            }
            
            alert(history.toString());
            
        } catch (Exception e) {
            alert("❌ Failed to load points history: " + e.getMessage());
        }
    }
    
    // ✅ Notification methods (SIMPLIFIED APPROACH)
    private void loadNotifications() {
        try {
            List<Map<String, Object>> notifications = notificationService.getStudentNotifications(student.getStudentID());
            
            // ✅ Check for new notifications using your suggested approach
            boolean hasNewNotifications = notificationService.hasNewNotifications(student.getStudentID());
            int newCount = notificationService.getNewNotificationsCount(student.getStudentID());
            
            // Update the UI
            if (notificationsList != null) {
                notificationsList.getItems().clear();
                notificationsList.getItems().addAll(notifications);
            }
            
            // Update unread count in UI if needed
            int unreadCount = notificationService.getUnreadNotificationsCount(student.getStudentID());
            // Silent operation - no console output
            
            // ✅ Show alert for new notifications (YOUR APPROACH)
            if (hasNewNotifications && newCount > 0) {
                // Get the latest notification message
                String latestMessage = notificationService.getLatestNotificationMessage(student.getStudentID());
                if (latestMessage != null) {
                    showNewNotificationAlert(newCount, latestMessage);
                } else {
                    showNewNotificationAlert(newCount, "New notification received");
                }
                
                // ✅ Update the last count AFTER showing the alert (your approach)
                notificationService.updateLastNotificationCount(student.getStudentID());
            }
            
        } catch (Exception e) {
            System.err.println("❌ Failed to load notifications: " + e.getMessage());
        }
    }
    
    private void markNotificationAsRead(Map<String, Object> notification) {
        try {
            int notificationId = (Integer) notification.get("notification_id");
            
            boolean success = notificationService.markNotificationAsRead(notificationId);
            if (success) {
                // Update the notification in the list
                notification.put("is_read", true);
                notificationsList.refresh();
                
                // Reload notifications to reflect changes
                loadNotifications();
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to mark notification as read: " + e.getMessage());
        }
    }
    
    private void startNotificationTimer() {
        // Cancel existing timer if any
        if (notificationTimer != null) {
            notificationTimer.cancel();
        }
        
        // Create new timer for auto-refresh every 30 seconds
        notificationTimer = new java.util.Timer();
        notificationTimer.scheduleAtFixedRate(new java.util.TimerTask() {
            @Override
            public void run() {
                // Use Platform.runLater to update UI from background thread
                Platform.runLater(() -> {
                    loadNotifications();
                    // Auto-refresh completed silently
                });
            }
        }, 30000, 30000); // 30 seconds initial delay, then every 30 seconds
        
        // Timer started silently
    }

    /**
     * ✅ Show alert for new notifications (ENHANCED with message)
     */
    private void showNewNotificationAlert(int newCount, String latestMessage) {
        Platform.runLater(() -> {
            // ✅ Play notification sound (optional - will work if system supports it)
            try {
                java.awt.Toolkit.getDefaultToolkit().beep();
            } catch (Exception e) {
                // Sound not supported, continue without it
            }
            
            // ✅ Show visual indicator
            if (newNotificationIndicator != null) {
                newNotificationIndicator.setVisible(true);
                newNotificationIndicator.setManaged(true);
                
                // Hide indicator after 10 seconds
                new java.util.Timer().schedule(new java.util.TimerTask() {
                    @Override
                    public void run() {
                        Platform.runLater(() -> {
                            if (newNotificationIndicator != null) {
                                newNotificationIndicator.setVisible(false);
                                newNotificationIndicator.setManaged(false);
                            }
                        });
                    }
                }, 10000); // 10 seconds
            }
            
            try {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("🔔 New Notifications!");
                alert.setHeaderText("You have " + newCount + " new notification" + (newCount > 1 ? "s" : ""));
                
                // ✅ Show the actual notification message
                String contentText = "Latest message:\n" + latestMessage + "\n\nCheck the notifications panel on the right to view all your messages.";
                alert.setContentText(contentText);
                
                // Custom styling for notification alert
                alert.getDialogPane().setStyle("-fx-background-color: #E3F2FD;");
                
                // Show the alert
                alert.showAndWait();
                
            } catch (Exception e) {
                System.err.println("❌ ERROR creating/showing alert: " + e.getMessage());
            }
        });
    }



    private void alert(String m) { 
        new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); 
    }
    
    private void loadExchangeRate(Label exchangeRateLbl) {
        try {
            DatabaseLoyaltyRepository loyaltyRepo = (DatabaseLoyaltyRepository) loyalty;
            int exchangeRate = loyaltyRepo.getExchangeRate();
            double egpPerPoint = loyaltyRepo.getEGPPerPoint();
            
            // Update cached exchange rate
            cachedExchangeRate = exchangeRate;
            
            exchangeRateLbl.setText("Exchange Rate: " + exchangeRate + " points = 1 EGP\n" +
                                   "EGP per Point: " + String.format("%.4f", egpPerPoint) + " EGP\n" +
                                   "(Min: " + exchangeRate + " points)");
        } catch (Exception e) {
            exchangeRateLbl.setText("Exchange Rate: Error loading");
            System.err.println("❌ Failed to load exchange rate: " + e.getMessage());
        }
    }
    
    private void updateDiscountPreview(int points, Label previewLabel) {
        try {
            // Cache exchange rate to avoid repeated database calls
            if (cachedExchangeRate == -1) {
                DatabaseLoyaltyRepository loyaltyRepo = (DatabaseLoyaltyRepository) loyalty;
                cachedExchangeRate = loyaltyRepo.getExchangeRate();
            }
            
            int exchangeRate = cachedExchangeRate;
            
            if (points >= exchangeRate) {
                double discount = (double) points / exchangeRate;
                previewLabel.setText("Preview: " + points + " points = " + String.format("%.2f", discount) + " EGP discount");
                previewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #4CAF50; -fx-font-style: italic;");
            } else {
                previewLabel.setText("Preview: " + points + " points = Need at least " + exchangeRate + " points for discount");
                previewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #f44336; -fx-font-style: italic;");
            }
        } catch (Exception e) {
            previewLabel.setText("Preview: Error calculating discount");
            previewLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #f44336; -fx-font-style: italic;");
        }
    }
    
    private void exchangePoints(int pointsToExchange) {
        if (pointsToExchange <= 0) {
            alert("Please enter a valid number of points to exchange.");
            return;
        }
        
        if (student.getLoyaltyPoints() < pointsToExchange) {
            alert("Not enough points! You have " + student.getLoyaltyPoints() + " points, but trying to exchange " + pointsToExchange + " points.");
            return;
        }
        
        // Check if there are items in cart
        if (cartItems.isEmpty()) {
            alert("❌ No items in cart!\n\nPlease add items to your cart before exchanging points for a discount.");
            return;
        }
        
        // Calculate discount amount and validate minimum exchange
        try {
            DatabaseLoyaltyRepository loyaltyRepo = (DatabaseLoyaltyRepository) loyalty;
            int exchangeRate = loyaltyRepo.getExchangeRate();
            
            // Check if points to exchange meets minimum requirement (at least 1 EGP worth)
            if (pointsToExchange < exchangeRate) {
                alert("❌ Exchange failed!\n\n" +
                      "Points to exchange must be at least " + exchangeRate + " points to get 1 EGP.\n" +
                      "You tried to exchange: " + pointsToExchange + " points\n" +
                      "Current rate: " + exchangeRate + " points = 1 EGP");
                return;
            }
            
            double discountAmount = (double) pointsToExchange / exchangeRate;
            
            // Show confirmation dialog
            boolean confirmed = showConfirmDialog(
                "Confirm Points Exchange for Discount",
                "Exchange " + pointsToExchange + " points for " + String.format("%.2f", discountAmount) + " EGP discount?",
                "Current rate: " + exchangeRate + " points = 1 EGP\n\n" +
                "This discount will be applied to your current order.\n" +
                "This action cannot be undone."
            );
            
            if (confirmed) {
                // Perform the exchange for discount
                double actualDiscount = loyaltyRepo.exchangePointsForOrderDiscount(student, pointsToExchange);
                if (actualDiscount > 0) {
                    // Apply discount to current order
                    currentOrderDiscount += actualDiscount;
                    updateDiscountDisplay();
                    
                    alert("✅ Points exchanged for discount!\n\n" +
                          "Exchanged: " + pointsToExchange + " points\n" +
                          "Discount: " + String.format("%.2f", actualDiscount) + " EGP\n" +
                          "Total Discount: " + String.format("%.2f", currentOrderDiscount) + " EGP\n\n" +
                          "Your new balance: " + student.getLoyaltyPoints() + " points\n\n" +
                          "The discount will be applied when you place your order!");
                    
                    // Refresh the points display
                    refreshStudentPointsFromDatabase();
                } else {
                    alert("❌ Exchange failed. Please try again.");
                }
            }
        } catch (Exception e) {
            alert("❌ Error during exchange: " + e.getMessage());
            System.err.println("❌ Exchange error: " + e.getMessage());
        }
    }
}