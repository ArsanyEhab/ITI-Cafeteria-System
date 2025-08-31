package ui;

import domain.MenuItem;
import domain.Staff;
import domain.Reward;

import infrastructure.MenuOperationsRepository;
import infrastructure.DatabaseLoyaltyRepository;
import services.PointsPerEGPReward;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import java.util.Optional;


public class AdminDashboardView extends BorderPane {
    private final Staff admin;
    private final MenuOperationsRepository menuRepo = new MenuOperationsRepository();
    private final DatabaseLoyaltyRepository loyaltyRepo = new DatabaseLoyaltyRepository(new PointsPerEGPReward());
    private final ObservableList<MenuItem> menuData = FXCollections.observableArrayList();
    private final ObservableList<Reward> rewardsData = FXCollections.observableArrayList();
    private final ListView<MenuItem> list = new ListView<>(menuData);
    private final ListView<Reward> rewardsList = new ListView<>(rewardsData);

    public AdminDashboardView(Stage stage, Staff a) {
        this.admin = a;
        setPadding(new Insets(16));

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Button logout = new Button("Logout");
        logout.setOnAction(e -> stage.getScene().setRoot(new LoginRegisterView(stage)));
        Label title = new Label("Admin - Menu Manager");
        title.getStyleClass().add("title");
        top.getChildren().addAll(title, new Label(" | Admin: " + admin.getName()), logout);
        setTop(top);

        list.setCellFactory(l -> new ListCell<>() {
            @Override protected void updateItem(MenuItem m, boolean empty) {
                super.updateItem(m, empty);
                if (empty || m == null) setText(null);
                else setText("#"+m.getId()+" - "+m.getName()+" - "+(int)m.getPrice()+" EGP");
            }
        });

        TextField name = new TextField(); name.setPromptText("Name");
        TextField desc = new TextField(); desc.setPromptText("Description");
        TextField price = new TextField(); price.setPromptText("Price");
        TextField cat = new TextField(); cat.setPromptText("Category");
        Button add = new Button("Add Item");
        add.setOnAction(e -> {
            try {
                double p = Double.parseDouble(price.getText());
                menuRepo.addMenuItem(name.getText(), desc.getText(), p, cat.getText());
                load();
            } catch (Exception ex) { info("Enter a valid price."); }
        });

        Button remove = new Button("Remove Selected");
        remove.setOnAction(e -> {
            MenuItem sel = list.getSelectionModel().getSelectedItem();
            if (sel == null) { info("Select an item."); return; }
            menuRepo.removeMenuItem(sel.getId());
            load();
        });

        // Menu management section
        VBox menuSection = new VBox(10);
        menuSection.getStyleClass().add("card");
        menuSection.setPadding(new Insets(12));
        menuSection.getChildren().addAll(
            new Label("Menu Management"),
            list,
            new HBox(10, name, desc, price, cat, add, remove)
        );
        
        // Rewards management section
        VBox rewardsSection = new VBox(10);
        rewardsSection.getStyleClass().add("card");
        rewardsSection.setPadding(new Insets(12));
        
        rewardsList.setCellFactory(l -> new ListCell<>() {
            @Override protected void updateItem(Reward r, boolean empty) {
                super.updateItem(r, empty);
                if (empty || r == null) setText(null);
                else setText("#" + r.getRewardId() + " - " + r.getRewardName() + " - " + r.getPointsRequired() + " pts → Item ID: " + r.getRewardedItemId());
            }
        });
        
        // Add click handler to show reward details
        rewardsList.setOnMouseClicked(e -> {
            Reward selected = rewardsList.getSelectionModel().getSelectedItem();
            if (selected != null) {
                showRewardDetails(selected);
            }
        });
        
        TextField rewardName = new TextField(); rewardName.setPromptText("Reward Name");
        TextField pointsRequired = new TextField(); pointsRequired.setPromptText("Points Required");
        
        // Dropdown for selecting menu items
        ComboBox<MenuItem> menuItemCombo = new ComboBox<>(menuData);
        menuItemCombo.setPromptText("Select Menu Item");
        menuItemCombo.setCellFactory(l -> new ListCell<>() {
            @Override protected void updateItem(MenuItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) setText(null);
                else setText("#" + item.getId() + " - " + item.getName() + " (" + (int)item.getPrice() + " EGP)");
            }
        });
        menuItemCombo.setButtonCell(menuItemCombo.getCellFactory().call(null));
        
        Button addReward = new Button("Add Reward");
        addReward.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addReward.setOnAction(e -> {
            // Validate inputs
            if (rewardName.getText().trim().isEmpty()) {
                info("Please enter a reward name.");
                return;
            }
            
            if (pointsRequired.getText().trim().isEmpty()) {
                info("Please enter the points required.");
                return;
            }
            
            try {
                int points = Integer.parseInt(pointsRequired.getText());
                if (points <= 0) {
                    info("Points required must be a positive number.");
                    return;
                }
                
                MenuItem selectedItem = menuItemCombo.getValue();
                if (selectedItem == null) {
                    info("Please select a menu item for the reward.");
                    return;
                }
                
                // Confirm before adding
                boolean confirmed = showConfirmDialog(
                    "Confirm New Reward",
                    "Add new reward?",
                    "Name: " + rewardName.getText().trim() + "\n" +
                    "Points Required: " + points + "\n" +
                    "Rewarded Item: " + selectedItem.getName() + " (ID: " + selectedItem.getId() + ")"
                );
                
                if (confirmed) {
                    addRewardToDatabase(rewardName.getText().trim(), points, selectedItem.getId());
                    loadRewards();
                    clearRewardFields(rewardName, pointsRequired);
                    menuItemCombo.setValue(null);
                    info("✅ Reward added successfully!");
                }
            } catch (NumberFormatException ex) { 
                info("Please enter a valid number for points required."); 
            }
        });
        
        Button removeReward = new Button("Remove Selected");
        removeReward.setStyle("-fx-background-color: #f44336; -fx-text-fill: white;");
        removeReward.setOnAction(e -> {
            Reward sel = rewardsList.getSelectionModel().getSelectedItem();
            if (sel == null) { 
                info("Please select a reward to remove."); 
                return; 
            }
            
            // Confirm before removing
            boolean confirmed = showConfirmDialog(
                "Confirm Reward Removal",
                "Remove this reward?",
                "Name: " + sel.getRewardName() + "\n" +
                "Points Required: " + sel.getPointsRequired() + "\n" +
                "Rewarded Item ID: " + sel.getRewardedItemId() + "\n\n" +
                "⚠️ This action cannot be undone!"
            );
            
            if (confirmed) {
                removeRewardFromDatabase(sel.getRewardId());
                loadRewards();
                info("✅ Reward removed successfully!");
            }
        });
        
        Button clearFields = new Button("Clear Fields");
        clearFields.setStyle("-fx-background-color: #9E9E9E; -fx-text-fill: white;");
        clearFields.setOnAction(e -> {
            clearRewardFields(rewardName, pointsRequired);
            menuItemCombo.setValue(null);
        });
        
        rewardsSection.getChildren().addAll(
            new Label("Rewards Management"),
            rewardsList,
            new HBox(10, rewardName, pointsRequired, menuItemCombo, addReward, removeReward, clearFields)
        );
        
                // Exchange points management section
        VBox exchangeSection = new VBox(10);
        exchangeSection.getStyleClass().add("card");
        exchangeSection.setPadding(new Insets(12));

        Label currentRateLbl = new Label("Current Rate: Loading...");
        currentRateLbl.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");

        TextField newRateField = new TextField();
        newRateField.setPromptText("New points per EGP (e.g., 50)");
        newRateField.setPrefWidth(200);

        Button updateRateBtn = new Button("Update Exchange Rate");
        updateRateBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        updateRateBtn.setOnAction(e -> {
            try {
                int newRate = Integer.parseInt(newRateField.getText());
                if (newRate <= 0) {
                    info("Please enter a positive number.");
                    return;
                }
                updateExchangeRate(newRate, currentRateLbl);
                newRateField.clear();
            } catch (NumberFormatException ex) {
                info("Please enter a valid number.");
            }
        });

        // Load current rate
        loadCurrentExchangeRate(currentRateLbl);

        exchangeSection.getChildren().addAll(
            new Label("Exchange Points Management"),
            currentRateLbl,
            new HBox(10, newRateField, updateRateBtn)
        );
        
        // Main layout with tabs
        TabPane tabPane = new TabPane();
        
        Tab menuTab = new Tab("Menu Management", menuSection);
        menuTab.setClosable(false);
        
        Tab rewardsTab = new Tab("Rewards Management", rewardsSection);
        rewardsTab.setClosable(false);
        
        Tab exchangeTab = new Tab("Exchange Points", exchangeSection);
        exchangeTab.setClosable(false);
        
        tabPane.getTabs().addAll(menuTab, rewardsTab, exchangeTab);
        
        setCenter(tabPane);
        load();
        loadRewards();
    }

    private void load() {
        menuData.clear();
        menuData.addAll(menuRepo.getMenu());
    }
    
    private void loadRewards() {
        rewardsData.clear();
        rewardsData.addAll(loyaltyRepo.getAllRewards());
    }
    
    private void addRewardToDatabase(String rewardName, int pointsRequired, int rewardedItemId) {
        String sql = "INSERT INTO rewards (reward_name, points_required, rewarded_item_id) VALUES (?, ?, ?)";
        try (var con = loyaltyRepo.getConnection()) {
            if (con == null) {
                info("Database connection failed.");
                return;
            }
            try (var pstmt = con.prepareStatement(sql)) {
                pstmt.setString(1, rewardName);
                pstmt.setInt(2, pointsRequired);
                pstmt.setInt(3, rewardedItemId);
                pstmt.executeUpdate();
                info("Reward added successfully!");
            }
        } catch (Exception e) {
            info("Failed to add reward: " + e.getMessage());
        }
    }
    
    private void removeRewardFromDatabase(int rewardId) {
        String sql = "DELETE FROM rewards WHERE reward_id = ?";
        try (var con = loyaltyRepo.getConnection()) {
            if (con == null) {
                info("Database connection failed.");
                return;
            }
            try (var pstmt = con.prepareStatement(sql)) {
                pstmt.setInt(1, rewardId);
                int rowsAffected = pstmt.executeUpdate();
                if (rowsAffected > 0) {
                    info("Reward removed successfully!");
                } else {
                    info("Reward not found.");
                }
            }
        } catch (Exception e) {
            info("Failed to remove reward: " + e.getMessage());
        }
    }
    
    private void clearRewardFields(TextField... fields) {
        for (TextField field : fields) {
            field.clear();
        }
    }
    
    private void loadCurrentExchangeRate(Label rateLabel) {
        try {
            int currentRate = loyaltyRepo.getExchangeRate();
            double egpPerPoint = loyaltyRepo.getEGPPerPoint();
            rateLabel.setText("Current Rate: " + currentRate + " points = 1 EGP\n" +
                            "EGP per Point: " + String.format("%.4f", egpPerPoint) + " EGP");
        } catch (Exception e) {
            rateLabel.setText("Current Rate: Error loading");
            System.err.println("❌ Failed to load exchange rate: " + e.getMessage());
        }
    }
    
    private void updateExchangeRate(int newRate, Label rateLabel) {
        try {
            boolean success = loyaltyRepo.updateExchangeRate(newRate);
            if (success) {
                double egpPerPoint = 1.0 / newRate;
                info("✅ Exchange rate updated successfully!\n\n" +
                     "New rate: " + newRate + " points = 1 EGP\n" +
                     "EGP per point: " + String.format("%.4f", egpPerPoint) + " EGP");
                loadCurrentExchangeRate(rateLabel);
            } else {
                info("❌ Failed to update exchange rate. Please try again.");
            }
        } catch (Exception e) {
            info("❌ Error updating exchange rate: " + e.getMessage());
            System.err.println("❌ Exchange rate update error: " + e.getMessage());
        }
    }
    


    private void info(String m) { new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    
    private boolean showConfirmDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    private void showRewardDetails(Reward reward) {
        try {
            // Try to get the actual menu item details
            MenuItem menuItem = null;
            try {
                int rewardedItemId = reward.getRewardedItemId();
                for (MenuItem item : menuData) {
                    if (item.getId() == rewardedItemId) {
                        menuItem = item;
                        break;
                    }
                }
            } catch (NumberFormatException e) {
                // Handle case where rewarded_item is not a valid integer
            }
            
            StringBuilder details = new StringBuilder();
            details.append("Reward Details:\n\n");
            details.append("ID: ").append(reward.getRewardId()).append("\n");
            details.append("Name: ").append(reward.getRewardName()).append("\n");
            details.append("Points Required: ").append(reward.getPointsRequired()).append("\n");
            details.append("Rewarded Item ID: ").append(reward.getRewardedItemId()).append("\n");
            
            if (menuItem != null) {
                details.append("Item Name: ").append(menuItem.getName()).append("\n");
                details.append("Item Price: ").append((int)menuItem.getPrice()).append(" EGP\n");
                details.append("Item Category: ").append(menuItem.getCategory());
            } else {
                details.append("Item Details: Could not find menu item with ID " + reward.getRewardedItemId());
            }
            
            info(details.toString());
        } catch (Exception e) {
            info("Error loading reward details: " + e.getMessage());
        }
    }
}