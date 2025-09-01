package ui;

import domain.MenuItem;
import domain.Staff;
import domain.Reward;
import domain.Student;
import domain.Order;

import infrastructure.MenuOperationsRepository;
import infrastructure.DatabaseLoyaltyRepository;
import infrastructure.DatabaseUserRepository;
import infrastructure.DatabaseStaffRepository;
import infrastructure.DatabaseOrderRepository;
import infrastructure.DatabaseRepository;
import services.PointsPerEGPReward;
import services.ReportGeneratorImpl;
import contracts.IReportGenerator;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.control.Separator;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import javafx.application.Platform;
import java.util.Optional;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Date;
import java.util.stream.Collectors;
import java.text.SimpleDateFormat;
import java.util.TimeZone;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;


public class AdminDashboardView extends BorderPane {
    private final Staff admin;
    private final MenuOperationsRepository menuRepo;
    private final DatabaseLoyaltyRepository loyaltyRepo = new DatabaseLoyaltyRepository(new PointsPerEGPReward());
    private final DatabaseUserRepository userRepo = new DatabaseUserRepository();
    private final DatabaseStaffRepository staffRepo = new DatabaseStaffRepository();
    private final DatabaseOrderRepository orderRepo = new DatabaseOrderRepository();
    private final IReportGenerator reportGenerator = new ReportGeneratorImpl();
    private final ObservableList<MenuItem> menuData = FXCollections.observableArrayList();
    private final ObservableList<Reward> rewardsData = FXCollections.observableArrayList();
    private final ObservableList<Student> studentsData = FXCollections.observableArrayList();
    private final ObservableList<Staff> staffData = FXCollections.observableArrayList();
    private final ObservableList<Order> ordersData = FXCollections.observableArrayList();
    private final ListView<MenuItem> list = new ListView<>(menuData);
    private final ListView<Reward> rewardsList = new ListView<>(rewardsData);
    private final ListView<Student> studentsList = new ListView<>(studentsData);
    private final ListView<Staff> staffListView = new ListView<>(staffData);
    private final ListView<Order> ordersList = new ListView<>(ordersData);
    private ComboBox<String> categoryComboBox; // For category suggestions

    public AdminDashboardView(Stage stage, Staff a) {
        this.admin = a;
        this.menuRepo = new MenuOperationsRepository(a.getId());
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
        
        // Category ComboBox with suggestions
        categoryComboBox = new ComboBox<>();
        categoryComboBox.setPromptText("Category");
        categoryComboBox.setEditable(true); // Allow typing new categories
        categoryComboBox.setPrefWidth(150);
        categoryComboBox.setStyle("-fx-background-color: white; -fx-border-color: #ccc; -fx-border-radius: 3;");
        
        // Load existing categories
        List<String> existingCategories = menuRepo.getAllCategories();
        categoryComboBox.getItems().addAll(existingCategories);
        
        // Add a refresh button to update categories
        Button refreshCategories = new Button("🔄");
        refreshCategories.setTooltip(new Tooltip("Refresh categories"));
        refreshCategories.setOnAction(e -> {
            List<String> updatedCategories = menuRepo.getAllCategories();
            categoryComboBox.getItems().clear();
            categoryComboBox.getItems().addAll(updatedCategories);
        });
        Button add = new Button("Add Item");
        add.setOnAction(e -> {
            try {
                double p = Double.parseDouble(price.getText());
                String category = categoryComboBox.getValue() != null ? categoryComboBox.getValue() : categoryComboBox.getEditor().getText();
                if (category == null || category.trim().isEmpty()) {
                    info("Please select or enter a category.");
                    return;
                }
                menuRepo.addMenuItem(name.getText(), desc.getText(), p, category.trim());
                load();
                
                // Clear fields
                name.clear();
                desc.clear();
                price.clear();
                categoryComboBox.setValue(null);
                categoryComboBox.getEditor().clear();
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
            new HBox(10, name, desc, price, categoryComboBox, refreshCategories, add, remove)
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

        // Two separate fields for exchange rates
        VBox rateFields = new VBox(10);
        
        // Points per EGP field
        HBox pointsPerEGPBox = new HBox(10);
        Label pointsPerEGPLbl = new Label("Points per EGP:");
        pointsPerEGPLbl.setPrefWidth(120);
        TextField pointsPerEGPField = new TextField();
        pointsPerEGPField.setPromptText("e.g., 50");
        pointsPerEGPField.setPrefWidth(150);
        pointsPerEGPBox.getChildren().addAll(pointsPerEGPLbl, pointsPerEGPField);
        
        // EGP per Points field
        HBox egpPerPointsBox = new HBox(10);
        Label egpPerPointsLbl = new Label("EGP per Point:");
        egpPerPointsLbl.setPrefWidth(120);
        TextField egpPerPointsField = new TextField();
        egpPerPointsField.setPromptText("e.g., 0.02");
        egpPerPointsField.setPrefWidth(150);
        egpPerPointsBox.getChildren().addAll(egpPerPointsLbl, egpPerPointsField);
        
        rateFields.getChildren().addAll(pointsPerEGPBox, egpPerPointsBox);

        Button updateRateBtn = new Button("Update Exchange Rates");
        updateRateBtn.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        updateRateBtn.setOnAction(e -> {
            try {
                // Validate Points per EGP
                int pointsPerEGP = Integer.parseInt(pointsPerEGPField.getText());
                if (pointsPerEGP <= 0) {
                    info("Points per EGP must be a positive number.");
                    return;
                }
                
                // Validate EGP per Points
                double egpPerPoints = Double.parseDouble(egpPerPointsField.getText());
                if (egpPerPoints <= 0) {
                    info("EGP per Point must be a positive number.");
                    return;
                }
                
                updateExchangeRates(pointsPerEGP, egpPerPoints, currentRateLbl);
                pointsPerEGPField.clear();
                egpPerPointsField.clear();
            } catch (NumberFormatException ex) {
                info("Please enter valid numbers for both fields.");
            }
        });

        // Load current rate and populate fields
        loadCurrentExchangeRate(currentRateLbl, pointsPerEGPField, egpPerPointsField);

        exchangeSection.getChildren().addAll(
            new Label("Exchange Points Management"),
            currentRateLbl,
            rateFields,
            new HBox(10, updateRateBtn)
        );
        
        // Main layout with tabs
        TabPane tabPane = new TabPane();
        
        Tab menuTab = new Tab("Menu Management", menuSection);
        menuTab.setClosable(false);
        
        Tab rewardsTab = new Tab("Rewards Management", rewardsSection);
        rewardsTab.setClosable(false);
        
        Tab exchangeTab = new Tab("Exchange Points", exchangeSection);
        exchangeTab.setClosable(false);
        
        // Student Management Tab
        Tab studentTab = new Tab("Student Management", createStudentManagementSection());
        studentTab.setClosable(false);
        
        // Staff Management Tab
        Tab staffTab = new Tab("Staff Management", createStaffManagementSection());
        staffTab.setClosable(false);
        
        // Order Management Tab (same as staff dashboard)
        Tab orderTab = new Tab("Order Management", createOrderManagementSection());
        orderTab.setClosable(false);
        
        // Reports & Analytics Tab
        Tab reportsTab = new Tab("📊 Reports & Analytics", createReportsSection());
        reportsTab.setClosable(false);
        
        tabPane.getTabs().addAll(menuTab, rewardsTab, exchangeTab, studentTab, staffTab, orderTab, reportsTab);
        
        setCenter(tabPane);
        load();
        loadRewards();
        loadStudents();
        loadStaff();
        loadOrders();
    }

    private void load() {
        menuData.clear();
        menuData.addAll(menuRepo.getMenu());
        
        // Refresh categories in the ComboBox after loading menu
        refreshCategorySuggestions();
    }
    
    private void refreshCategorySuggestions() {
        // Refresh the category ComboBox with current categories
        if (categoryComboBox != null) {
            List<String> updatedCategories = menuRepo.getAllCategories();
            categoryComboBox.getItems().clear();
            categoryComboBox.getItems().addAll(updatedCategories);
        }
    }
    
    private void loadRewards() {
        rewardsData.clear();
        rewardsData.addAll(loyaltyRepo.getAllRewards());
    }
    
    private void loadStudents() {
        studentsData.clear();
        try {
            studentsData.addAll(userRepo.getAllStudents());
        } catch (Exception e) {
            System.err.println("❌ Failed to load students: " + e.getMessage());
        }
    }
    
    private void loadStaff() {
        staffData.clear();
        try {
            staffData.addAll(staffRepo.getAllStaff());
        } catch (Exception e) {
            System.err.println("❌ Failed to load staff: " + e.getMessage());
        }
    }
    
    private void loadOrders() {
        ordersData.clear();
        try {
            List<Order> allOrders = orderRepo.getAllOrders();
            for (Order o : allOrders) {
                // Show pending, preparing, and ready orders for admin to manage
                if ("pending".equalsIgnoreCase(o.getStatus()) || 
                    "preparing".equalsIgnoreCase(o.getStatus()) || 
                    "ready".equalsIgnoreCase(o.getStatus())) {
                    ordersData.add(o);
                }
            }
            
            if (ordersData.isEmpty()) {
                System.out.println("No pending, preparing, or ready orders at the moment.");
            }
        } catch (Exception e) {
            System.err.println("❌ Failed to load orders: " + e.getMessage());
        }
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
    
    private void loadCurrentExchangeRate(Label rateLabel, TextField pointsPerEGPField, TextField egpPerPointsField) {
        try {
            int currentRate = loyaltyRepo.getExchangeRate();
            double egpPerPoint = loyaltyRepo.getEGPPerPoint();
            rateLabel.setText("Current Rate: " + currentRate + " points = 1 EGP\n" +
                            "EGP per Point: " + String.format("%.4f", egpPerPoint) + " EGP");
            
            // Populate the text fields with current values (only if they're not null)
            if (pointsPerEGPField != null) {
                pointsPerEGPField.setText(String.valueOf(currentRate));
            }
            if (egpPerPointsField != null) {
                egpPerPointsField.setText(String.format("%.4f", egpPerPoint));
            }
        } catch (Exception e) {
            rateLabel.setText("Current Rate: Error loading");
            System.err.println("❌ Failed to load exchange rate: " + e.getMessage());
        }
    }
    
    private void updateExchangeRates(int pointsPerEGP, double egpPerPoints, Label rateLabel) {
        try {
            boolean success = loyaltyRepo.updateExchangeRates(pointsPerEGP, egpPerPoints);
            if (success) {
                info("✅ Exchange rates updated successfully!\n\n" +
                     "New rates:\n" +
                     "• " + pointsPerEGP + " points = 1 EGP\n" +
                     "• 1 point = " + String.format("%.4f", egpPerPoints) + " EGP");
                loadCurrentExchangeRate(rateLabel, null, null); // Refresh display only
            } else {
                info("❌ Failed to update exchange rates. Please try again.");
            }
        } catch (Exception e) {
            info("❌ Error updating exchange rates: " + e.getMessage());
            System.err.println("❌ Exchange rates update error: " + e.getMessage());
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
                loadCurrentExchangeRate(rateLabel, null, null);
            } else {
                info("❌ Failed to update exchange rate. Please try again.");
            }
        } catch (Exception e) {
            info("❌ Error updating exchange rate: " + e.getMessage());
            System.err.println("❌ Exchange rate update error: " + e.getMessage());
        }
    }
    


    private void searchStudent(String studentId) {
        if (studentId == null || studentId.trim().isEmpty()) {
            info("Please enter a student ID to search.");
            return;
        }
        
        try {
            Student student = userRepo.findById(studentId.trim());
            if (student != null) {
                // Select the student in the list
                studentsList.getSelectionModel().select(student);
                info("✅ Student found: " + student.getName());
            } else {
                info("❌ No student found with ID: " + studentId.trim());
            }
        } catch (Exception e) {
            error("Error searching for student: " + e.getMessage());
        }
    }
    
    private void searchStaff(String staffId) {
        if (staffId == null || staffId.trim().isEmpty()) {
            info("Please enter a staff ID to search.");
            return;
        }
        
        try {
            Staff staff = staffRepo.findById(staffId.trim());
            if (staff != null) {
                // Select the staff in the list
                staffListView.getSelectionModel().select(staff);
                info("✅ Staff found: " + staff.getName());
            } else {
                info("❌ No staff found with ID: " + staffId.trim());
            }
        } catch (Exception e) {
            error("Error searching for staff: " + e.getMessage());
        }
    }
    
    private void showStudentDetails(Student student, VBox content, HBox actionButtons) {
        content.getChildren().clear();
        actionButtons.getChildren().clear();
        
        // Student information
        Label idLabel = new Label("ID: " + student.getId());
        Label nameLabel = new Label("Name: " + student.getName());
        Label pointsLabel = new Label("Loyalty Points: " + String.valueOf(student.getLoyaltyPoints()));
        
        content.getChildren().addAll(idLabel, nameLabel, pointsLabel);
        
        // Action buttons
        Button editBtn = new Button("✏️ Edit");
        editBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        editBtn.setOnAction(e -> showEditStudentDialog(student));
        
        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> deleteStudent(student));
        
        Button resetPointsBtn = new Button("🔄 Reset Points");
        resetPointsBtn.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white;");
        resetPointsBtn.setOnAction(e -> resetStudentPoints(student));
        
        actionButtons.getChildren().addAll(editBtn, deleteBtn, resetPointsBtn);
    }
    
    private void showStaffDetails(Staff staff, VBox content, HBox actionButtons) {
        content.getChildren().clear();
        actionButtons.getChildren().clear();
        
        // Staff information
        Label idLabel = new Label("ID: " + staff.getId());
        Label nameLabel = new Label("Name: " + staff.getName());
        Label roleLabel = new Label("Role: " + staff.getRole());
        
        content.getChildren().addAll(idLabel, nameLabel, roleLabel);
        
        // Action buttons
        Button editBtn = new Button("✏️ Edit");
        editBtn.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white;");
        editBtn.setOnAction(e -> showEditStaffDialog(staff));
        
        Button deleteBtn = new Button("🗑️ Delete");
        deleteBtn.setStyle("-fx-background-color: #F44336; -fx-text-fill: white;");
        deleteBtn.setOnAction(e -> deleteStaff(staff));
        
        actionButtons.getChildren().addAll(editBtn, deleteBtn);
    }
    
    private void showAddStudentDialog() {
        // Create a simple dialog for adding students
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add New Student");
        dialog.setHeaderText("Enter student information");
        
        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create the custom content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Student Name");
        TextField idField = new TextField();
        idField.setPromptText("Student ID");
        TextField passwordField = new TextField();
        passwordField.setPromptText("Password");
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("ID:"), 0, 1);
        grid.add(idField, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passwordField, 1, 2);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the name field by default
        Platform.runLater(() -> nameField.requestFocus());
        
        // Convert the result to the entered data when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().trim().isEmpty() || idField.getText().trim().isEmpty() || passwordField.getText().trim().isEmpty()) {
                    info("All fields are required.");
                    return null;
                }
                return nameField.getText().trim() + "|" + idField.getText().trim() + "|" + passwordField.getText().trim();
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String[] parts = data.split("\\|");
            if (parts.length == 3) {
                try {
                    Student newStudent = new Student(parts[0], parts[1], parts[2]);
                    userRepo.save(newStudent);
                    info("✅ Student added successfully!");
                    loadStudents();
                } catch (Exception e) {
                    error("Failed to add student: " + e.getMessage());
                }
            }
        });
    }
    
    private void showAddStaffDialog() {
        // Create a simple dialog for adding staff
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Add New Staff");
        dialog.setHeaderText("Enter staff information");
        
        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create the custom content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField();
        nameField.setPromptText("Staff Name");
        TextField idField = new TextField();
        idField.setPromptText("Staff ID");
        TextField passwordField = new TextField();
        passwordField.setPromptText("Password");
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("staff", "admin");
        roleCombo.setValue("staff");
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("ID:"), 0, 1);
        grid.add(idField, 1, 1);
        grid.add(new Label("Password:"), 0, 2);
        grid.add(passwordField, 1, 2);
        grid.add(new Label("Role:"), 0, 3);
        grid.add(roleCombo, 1, 3);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the name field by default
        Platform.runLater(() -> nameField.requestFocus());
        
        // Convert the result to the entered data when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().trim().isEmpty() || idField.getText().trim().isEmpty() || passwordField.getText().trim().isEmpty()) {
                    info("All fields are required.");
                    return null;
                }
                return nameField.getText().trim() + "|" + idField.getText().trim() + "|" + passwordField.getText().trim() + "|" + roleCombo.getValue();
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String[] parts = data.split("\\|");
            if (parts.length == 4) {
                try {
                    boolean isAdmin = "admin".equalsIgnoreCase(parts[3]);
                    Staff newStaff = new Staff(parts[0], parts[1], parts[2], isAdmin);
                    staffRepo.save(newStaff);
                    info("✅ Staff added successfully!");
                    loadStaff();
                } catch (Exception e) {
                    error("Failed to add staff: " + e.getMessage());
                }
            }
        });
    }
    
    private void showEditStudentDialog(Student student) {
        // Create a simple dialog for editing students
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Student");
        dialog.setHeaderText("Edit student information");
        
        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create the custom content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField(student.getName());
        TextField pointsField = new TextField(String.valueOf(student.getLoyaltyPoints()));
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Loyalty Points:"), 0, 1);
        grid.add(pointsField, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the name field by default
        Platform.runLater(() -> nameField.requestFocus());
        
        // Convert the result to the entered data when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().trim().isEmpty() || pointsField.getText().trim().isEmpty()) {
                    info("All fields are required.");
                    return null;
                }
                try {
                    int points = Integer.parseInt(pointsField.getText().trim());
                    if (points < 0) {
                        info("Loyalty points cannot be negative.");
                        return null;
                    }
                    return nameField.getText().trim() + "|" + points;
                } catch (NumberFormatException e) {
                    info("Please enter a valid number for loyalty points.");
                    return null;
                }
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String[] parts = data.split("\\|");
            if (parts.length == 2) {
                try {
                    student.setName(parts[0]);
                    student.setLoyaltyPoints(Integer.parseInt(parts[1]));
                    userRepo.updateStudent(student);
                    info("✅ Student updated successfully!");
                    loadStudents();
                } catch (Exception e) {
                    error("Failed to update student: " + e.getMessage());
                }
            }
        });
    }
    
    private void showEditStaffDialog(Staff staff) {
        // Create a simple dialog for editing staff
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Edit Staff");
        dialog.setHeaderText("Edit staff information");
        
        // Set the button types
        ButtonType saveButtonType = new ButtonType("Save", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);
        
        // Create the custom content
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField nameField = new TextField(staff.getName());
        ComboBox<String> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll("staff", "admin");
        roleCombo.setValue(staff.getRole());
        
        grid.add(new Label("Name:"), 0, 0);
        grid.add(nameField, 1, 0);
        grid.add(new Label("Role:"), 0, 1);
        grid.add(roleCombo, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        // Request focus on the name field by default
        Platform.runLater(() -> nameField.requestFocus());
        
        // Convert the result to the entered data when the save button is clicked
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType) {
                if (nameField.getText().trim().isEmpty()) {
                    info("Name field is required.");
                    return null;
                }
                return nameField.getText().trim() + "|" + roleCombo.getValue();
            }
            return null;
        });
        
        Optional<String> result = dialog.showAndWait();
        result.ifPresent(data -> {
            String[] parts = data.split("\\|");
            if (parts.length == 2) {
                try {
                    staff.setName(parts[0]);
                    boolean isAdmin = "admin".equalsIgnoreCase(parts[1]);
                    staff.setRole(isAdmin);
                    staffRepo.updateStaff(staff);
                    info("✅ Staff updated successfully!");
                    loadStaff();
                } catch (Exception e) {
                    error("Failed to update staff: " + e.getMessage());
                }
            }
        });
    }
    
    private void deleteStudent(Student student) {
        boolean confirmed = showConfirmDialog(
            "Confirm Student Deletion",
            "Delete this student?",
            "Student: " + student.getName() + " (ID: " + student.getId() + ")\n\n" +
            "This action cannot be undone!"
        );
        
        if (confirmed) {
            try {
                userRepo.deleteStudent(student.getId());
                info("✅ Student deleted successfully!");
                loadStudents();
            } catch (Exception e) {
                error("Failed to delete student: " + e.getMessage());
            }
        }
    }
    
    private void deleteStaff(Staff staff) {
        boolean confirmed = showConfirmDialog(
            "Confirm Staff Deletion",
            "Delete this staff member?",
            "Staff: " + staff.getName() + " (ID: " + staff.getId() + ")\n\n" +
            "This action cannot be undone!"
        );
        
        if (confirmed) {
            try {
                staffRepo.deleteStaff(staff.getId());
                info("✅ Staff deleted successfully!");
                loadStaff();
            } catch (Exception e) {
                error("Failed to delete staff: " + e.getMessage());
            }
        }
    }
    
    private void resetStudentPoints(Student student) {
        boolean confirmed = showConfirmDialog(
            "Confirm Points Reset",
            "Reset loyalty points for this student?",
            "Student: " + student.getName() + " (ID: " + student.getId() + ")\n\n" +
            "Current points: " + student.getLoyaltyPoints() + "\n" +
            "This will set points to 0."
        );
        
        if (confirmed) {
            try {
                student.setLoyaltyPoints(0);
                userRepo.updateStudent(student);
                info("✅ Student loyalty points reset to 0!");
                loadStudents();
            } catch (Exception e) {
                error("Failed to reset student points: " + e.getMessage());
            }
        }
    }
    
    private void info(String m) { new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    
    private void error(String m) { new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
    
    private void showMenuLogs() {
        try {
            List<Map<String, Object>> logs = getMenuLogs();
            if (logs.isEmpty()) {
                info("No menu change logs found.");
                return;
            }
            
            StringBuilder logText = new StringBuilder();
            logText.append("📝 Menu Change Logs\n\n");
            
            for (Map<String, Object> log : logs) {
                String staffId = (String) log.get("staff_id");
                String action = (String) log.get("action");
                Integer menuItemId = (Integer) log.get("menu_item_id");
                String logTime = (String) log.get("log_time");
                
                logText.append(String.format("🕐 %s\n", logTime));
                logText.append(String.format("👤 Staff: %s\n", staffId));
                logText.append(String.format("🔧 Action: %s\n", action));
                logText.append(String.format("🍽️ Item ID: %d\n", menuItemId));
                logText.append("─".repeat(40) + "\n\n");
            }
            
            info(logText.toString());
        } catch (Exception e) {
            error("Failed to load menu logs: " + e.getMessage());
        }
    }
    
    private List<Map<String, Object>> getMenuLogs() {
        List<Map<String, Object>> logs = new ArrayList<>();
        String sql = "SELECT staff_id, action, menu_item_id, log_time FROM menu_logs ORDER BY log_time DESC LIMIT 50";
        
        try (Connection con = DatabaseRepository.getConnection()) {
            if (con == null) return logs;
            
            try (PreparedStatement pstmt = con.prepareStatement(sql);
                 ResultSet rs = pstmt.executeQuery()) {
                
                while (rs.next()) {
                    Map<String, Object> log = new HashMap<>();
                    log.put("staff_id", rs.getString("staff_id"));
                    log.put("action", rs.getString("action"));
                    log.put("menu_item_id", rs.getInt("menu_item_id"));
                    log.put("log_time", rs.getString("log_time"));
                    logs.add(log);
                }
            }
        } catch (SQLException e) {
            System.err.println("❌ Failed to get menu logs: " + e.getMessage());
        }
        
        return logs;
    }
    
    private boolean showConfirmDialog(String title, String header, String content) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle(title);
        alert.setHeaderText(header);
        alert.setContentText(content);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    private VBox createStudentManagementSection() {
        VBox studentSection = new VBox(10);
        studentSection.getStyleClass().add("card");
        studentSection.setPadding(new Insets(12));
        
        // Search section
        HBox searchBox = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("Enter Student ID to search");
        searchField.setPrefWidth(200);
        Button searchBtn = new Button("🔍 Search");
        searchBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        searchBtn.setOnAction(e -> searchStudent(searchField.getText()));
        searchBox.getChildren().addAll(searchField, searchBtn);
        
        // Students list
        studentsList.setCellFactory(l -> new ListCell<>() {
            @Override protected void updateItem(Student s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) setText(null);
                else setText("#" + s.getId() + " - " + s.getName() + " - " + s.getLoyaltyPoints() + " pts");
            }
        });
        
        // Student details panel
        VBox studentDetailsPanel = new VBox(10);
        studentDetailsPanel.getStyleClass().add("card");
        studentDetailsPanel.setPadding(new Insets(12));
        studentDetailsPanel.setVisible(false);
        
        Label studentDetailsTitle = new Label("Student Details");
        studentDetailsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        VBox studentDetailsContent = new VBox(8);
        studentDetailsContent.setVisible(false);
        
        // Student action buttons
        HBox studentActionButtons = new HBox(10);
        studentActionButtons.setVisible(false);
        
        // Student selection listener
        studentsList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showStudentDetails(newSelection, studentDetailsContent, studentActionButtons);
                studentDetailsPanel.setVisible(true);
                studentDetailsContent.setVisible(true);
                studentActionButtons.setVisible(true);
            } else {
                studentDetailsPanel.setVisible(false);
                studentDetailsContent.setVisible(false);
                studentActionButtons.setVisible(false);
            }
        });
        
        studentDetailsPanel.getChildren().addAll(studentDetailsTitle, studentDetailsContent, studentActionButtons);
        
        // Management buttons
        HBox managementButtons = new HBox(10);
        Button refreshStudents = new Button("🔄 Refresh");
        refreshStudents.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        refreshStudents.setOnAction(e -> loadStudents());
        
        Button addStudent = new Button("➕ Add Student");
        addStudent.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addStudent.setOnAction(e -> showAddStudentDialog());
        
        Button viewLogs = new Button("📝 View Logs");
        viewLogs.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white;");
        viewLogs.setOnAction(e -> showMenuLogs());
        
        managementButtons.getChildren().addAll(refreshStudents, addStudent, viewLogs);
        
        studentSection.getChildren().addAll(
            new Label("Student Management"),
            searchBox,
            studentsList,
            studentDetailsPanel,
            managementButtons
        );
        
        return studentSection;
    }
    
    private VBox createStaffManagementSection() {
        VBox staffSection = new VBox(10);
        staffSection.getStyleClass().add("card");
        staffSection.setPadding(new Insets(12));
        
        // Search section
        HBox searchBox = new HBox(10);
        TextField searchField = new TextField();
        searchField.setPromptText("Enter Staff ID to search");
        searchField.setPrefWidth(200);
        Button searchBtn = new Button("🔍 Search");
        searchBtn.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        searchBtn.setOnAction(e -> searchStaff(searchField.getText()));
        searchBox.getChildren().addAll(searchField, searchBtn);
        
        // Staff list
        staffListView.setCellFactory(l -> new ListCell<>() {
            @Override protected void updateItem(Staff s, boolean empty) {
                super.updateItem(s, empty);
                if (empty || s == null) setText(null);
                else setText("#" + s.getId() + " - " + s.getName() + " - " + s.getRole());
            }
        });
        
        // Staff details panel
        VBox staffDetailsPanel = new VBox(10);
        staffDetailsPanel.getStyleClass().add("card");
        staffDetailsPanel.setPadding(new Insets(12));
        staffDetailsPanel.setVisible(false);
        
        Label staffDetailsTitle = new Label("Staff Details");
        staffDetailsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        VBox staffDetailsContent = new VBox(8);
        staffDetailsContent.setVisible(false);
        
        // Staff action buttons
        HBox staffActionButtons = new HBox(10);
        staffActionButtons.setVisible(false);
        
        // Staff selection listener
        staffListView.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showStaffDetails(newSelection, staffDetailsContent, staffActionButtons);
                staffDetailsPanel.setVisible(true);
                staffDetailsContent.setVisible(true);
                staffActionButtons.setVisible(true);
            } else {
                staffDetailsPanel.setVisible(false);
                staffDetailsContent.setVisible(false);
                staffActionButtons.setVisible(false);
            }
        });
        
        staffDetailsPanel.getChildren().addAll(staffDetailsTitle, staffDetailsContent, staffActionButtons);
        
        // Management buttons
        HBox managementButtons = new HBox(10);
        Button refreshStaff = new Button("🔄 Refresh");
        refreshStaff.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        refreshStaff.setOnAction(e -> loadStaff());
        
        Button addStaff = new Button("➕ Add Staff");
        addStaff.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white;");
        addStaff.setOnAction(e -> showAddStaffDialog());
        
        managementButtons.getChildren().addAll(refreshStaff, addStaff);
        
        staffSection.getChildren().addAll(
            new Label("Staff Management"),
            searchBox,
            staffListView,
            staffDetailsPanel,
            managementButtons
        );
        
        return staffSection;
    }
    
    private VBox createOrderManagementSection() {
        VBox orderSection = new VBox(10);
        orderSection.getStyleClass().add("card");
        orderSection.setPadding(new Insets(12));
        
        // Orders list with enhanced formatting
        ordersList.setCellFactory(l -> new ListCell<>() {
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
        ordersList.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                showOrderDetails(newSelection);
            } else {
                hideOrderDetails();
            }
        });
        
        // Control buttons
        Button refresh = new Button("🔄 Refresh");
        refresh.setStyle("-fx-background-color: #2196F3; -fx-text-fill: white;");
        refresh.setOnAction(e -> loadOrders());
        
        // Order details panel setup
        VBox orderDetailsPanel = createOrderDetailsPanel();
        
        // Main layout
        VBox center = new VBox(15);
        center.getStyleClass().add("card");
        center.setPadding(new Insets(16));
        
        // Orders section
        VBox ordersSection = new VBox(10);
        Label ordersLabel = new Label("📋 Orders Queue (Pending → Preparing → Ready)");
        ordersLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        ordersSection.getChildren().addAll(ordersLabel, ordersList, refresh);
        
        center.getChildren().addAll(ordersSection, orderDetailsPanel);
        
        orderSection.getChildren().addAll(
            new Label("Order Management"),
            center
        );
        
        return orderSection;
    }
    
    private VBox createOrderDetailsPanel() {
        VBox orderDetailsPanel = new VBox(10);
        orderDetailsPanel.getStyleClass().add("card");
        orderDetailsPanel.setPadding(new Insets(16));
        orderDetailsPanel.setVisible(false);
        orderDetailsPanel.setManaged(false);
        
        Label orderDetailsTitle = new Label("Order Details");
        orderDetailsTitle.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        VBox orderDetailsContent = new VBox(8);
        orderDetailsContent.setVisible(false);
        
        HBox orderActionButtons = new HBox(10);
        orderActionButtons.setVisible(false);
        
        orderDetailsPanel.getChildren().addAll(orderDetailsTitle, orderDetailsContent, orderActionButtons);
        
        return orderDetailsPanel;
    }
    
    private void showOrderDetails(Order order) {
        VBox orderDetailsPanel = (VBox) ordersList.getScene().lookup("#orderDetailsPanel");
        if (orderDetailsPanel == null) return;
        
        VBox orderDetailsContent = (VBox) orderDetailsPanel.lookup("#orderDetailsContent");
        HBox orderActionButtons = (HBox) orderDetailsPanel.lookup("#orderActionButtons");
        
        if (orderDetailsContent == null || orderActionButtons == null) return;
        
        orderDetailsContent.getChildren().clear();
        orderActionButtons.getChildren().clear();
        
        // Order information
        Label orderIdLabel = new Label("Order ID: #" + String.valueOf(order.getOrderID()));
        orderIdLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Label statusLabel = new Label("Status: " + order.getStatus().toUpperCase());
        statusLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #666;");
        
        // Student Details Section
        Label studentHeaderLabel = new Label("👤 Student Information:");
        studentHeaderLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Label studentIdLabel = new Label("Student ID: " + String.valueOf(order.getStudentId()));
        studentIdLabel.setStyle("-fx-font-size: 12px;");
        
        // Try to get student details
        Student student = null;
        try {
            student = userRepo.findById(order.getStudentId());
        } catch (Exception e) {
            System.err.println("❌ Failed to fetch student details: " + e.getMessage());
        }
        
        Label studentNameLabel = new Label("Name: " + (student != null ? student.getName() : "Unknown"));
        studentNameLabel.setStyle("-fx-font-size: 12px;");
        
        Label studentPointsLabel = new Label("Loyalty Points: " + (student != null ? String.valueOf(student.getLoyaltyPoints()) : "Unknown"));
        studentPointsLabel.setStyle("-fx-font-size: 12px;");
        
        // Item Details - Show all items in the order
        List<MenuItem> items = order.getItems();
        if (items != null && !items.isEmpty()) {
            Label itemsHeaderLabel = new Label("🍽️ Order Items:");
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
        Label summaryHeaderLabel = new Label("📋 Order Summary:");
        summaryHeaderLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        int itemCount = items != null ? items.size() : 0;
        long uniqueItems = items != null ? items.stream().map(MenuItem::getName).distinct().count() : 0;
        
        Label quantityLabel = new Label("Total Items: " + String.valueOf(itemCount) + " (Unique: " + String.valueOf(uniqueItems) + ")");
        quantityLabel.setStyle("-fx-font-size: 12px;");
        
        Label priceLabel = new Label("Total Cost: " + String.format("%.0f", order.getTotalCost()) + " EGP");
        priceLabel.setStyle("-fx-font-size: 12px; -fx-font-weight: bold; -fx-text-fill: #2E7D32;");
        
        // Calculate potential loyalty points earned
        int pointsEarned = (int) (order.getTotalCost() * loyaltyRepo.getExchangeRate());
        Label pointsLabel = new Label("Points Earned: " + String.valueOf(pointsEarned) + " points");
        pointsLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #FF6F00;");
        
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
            orderIdLabel, statusLabel, 
            new Separator(), // Visual separator
            studentHeaderLabel, studentIdLabel, studentNameLabel, studentPointsLabel,
            new Separator(), // Visual separator
            summaryHeaderLabel, quantityLabel, priceLabel, pointsLabel
        );
        
        // Show action buttons based on current status
        updateOrderActionButtons(order.getStatus(), orderActionButtons, order);
        
        orderDetailsPanel.setVisible(true);
        orderDetailsPanel.setManaged(true);
        orderDetailsContent.setVisible(true);
        orderActionButtons.setVisible(true);
    }
    
    private void hideOrderDetails() {
        VBox orderDetailsPanel = (VBox) ordersList.getScene().lookup("#orderDetailsPanel");
        if (orderDetailsPanel != null) {
            orderDetailsPanel.setVisible(false);
            orderDetailsPanel.setManaged(false);
        }
    }
    
    private void updateOrderActionButtons(String currentStatus, HBox orderActionButtons, Order order) {
        orderActionButtons.getChildren().clear();
        
        if ("pending".equalsIgnoreCase(currentStatus)) {
            Button startPreparing = new Button("🔥 Start Preparing");
            startPreparing.setStyle("-fx-background-color: #FF9800; -fx-text-fill: white; -fx-font-weight: bold;");
            startPreparing.setOnAction(e -> {
                updateOrderStatus(order, "preparing");
            });
            
            Button cancelOrder = new Button("❌ Cancel Order");
            cancelOrder.setStyle("-fx-background-color: #F44336; -fx-text-fill: white; -fx-font-weight: bold;");
            cancelOrder.setOnAction(e -> {
                updateOrderStatus(order, "cancelled");
            });
            
            orderActionButtons.getChildren().addAll(startPreparing, cancelOrder);
        } else if ("preparing".equalsIgnoreCase(currentStatus)) {
            Button markReady = new Button("✅ Mark Ready");
            markReady.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
            markReady.setOnAction(e -> {
                updateOrderStatus(order, "ready");
            });
            
            orderActionButtons.getChildren().addAll(markReady);
        } else if ("ready".equalsIgnoreCase(currentStatus)) {
            Button markPickedUp = new Button("📦 Mark Picked Up");
            markPickedUp.setStyle("-fx-background-color: #9C27B0; -fx-text-fill: white; -fx-font-weight: bold;");
            markPickedUp.setOnAction(e -> {
                updateOrderStatus(order, "completed");
            });
            
            orderActionButtons.getChildren().addAll(markPickedUp);
        }
        
        orderActionButtons.setAlignment(Pos.CENTER);
    }
    
    private void updateOrderStatus(Order order, String newStatus) {
        try {
            orderRepo.updateOrderStatus(order.getOrderID(), newStatus);
            info("Order #" + order.getOrderID() + " status updated to: " + newStatus.toUpperCase());
            loadOrders();
            
            // Update the selected order details
            Order updatedOrder = ordersList.getSelectionModel().getSelectedItem();
            if (updatedOrder != null) {
                showOrderDetails(updatedOrder);
            }
        } catch (Exception e) {
            error("Failed to update order status: " + e.getMessage());
        }
    }
    
    // Get current admin staff ID for logging
    private String getCurrentAdminId() {
        return admin.getId();
    }
    
    private VBox createReportsSection() {
        VBox reportsSection = new VBox(20);
        reportsSection.getStyleClass().add("card");
        reportsSection.setPadding(new Insets(16));
        
        // Title
        Label titleLabel = new Label("📊 Business Analytics & Reports");
        titleLabel.setStyle("-fx-font-size: 20px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        // Date range selector
        HBox dateRangeBox = new HBox(10);
        dateRangeBox.setAlignment(Pos.CENTER_LEFT);
        
        Label dateRangeLabel = new Label("Date Range:");
        DatePicker startDate = new DatePicker();
        DatePicker endDate = new DatePicker();
        startDate.setValue(java.time.LocalDate.now().minusDays(30));
        endDate.setValue(java.time.LocalDate.now());
        
        Button generateReport = new Button("🔄 Generate Report");
        generateReport.setStyle("-fx-background-color: #4CAF50; -fx-text-fill: white; -fx-font-weight: bold;");
        
        dateRangeBox.getChildren().addAll(dateRangeLabel, startDate, new Label("to"), endDate, generateReport);
        
        // Create scrollable content area
        ScrollPane scrollPane = new ScrollPane();
        VBox contentArea = new VBox(20);
        contentArea.setPadding(new Insets(10));
        scrollPane.setContent(contentArea);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefHeight(600);
        
        // Generate report action
        generateReport.setOnAction(e -> {
            if (startDate.getValue() == null || endDate.getValue() == null) {
                info("Please select both start and end dates.");
                return;
            }
            
            generateReport.setDisable(true);
            generateReport.setText("🔄 Generating...");
            
            // Convert LocalDate to Date
            java.util.Date start = java.sql.Date.valueOf(startDate.getValue());
            java.util.Date end = java.sql.Date.valueOf(endDate.getValue());
            
            // Generate all reports
            generateAllReports(contentArea, start, end);
            
            // Re-enable button
            generateReport.setDisable(false);
            generateReport.setText("🔄 Generate Report");
        });
        
        reportsSection.getChildren().addAll(titleLabel, dateRangeBox, scrollPane);
        return reportsSection;
    }
    
    private void generateAllReports(VBox contentArea, java.util.Date startDate, java.util.Date endDate) {
        contentArea.getChildren().clear();
        
        // Sales Analytics
        VBox salesAnalytics = createSalesAnalyticsCard(startDate, endDate);
        contentArea.getChildren().add(salesAnalytics);
        
        // Top Selling Items
        VBox topItems = createTopSellingItemsCard();
        contentArea.getChildren().add(topItems);
        
        // Category Sales
        VBox categorySales = createCategorySalesCard(startDate, endDate);
        contentArea.getChildren().add(categorySales);
        
        // Order Status Distribution
        VBox orderStatus = createOrderStatusCard();
        contentArea.getChildren().add(orderStatus);
        
        // Customer Analytics
        VBox customerAnalytics = createCustomerAnalyticsCard();
        contentArea.getChildren().add(customerAnalytics);
        
        // Revenue Trends
        VBox revenueTrends = createRevenueTrendsCard(startDate, endDate);
        contentArea.getChildren().add(revenueTrends);
        
        // Loyalty Program Analytics
        VBox loyaltyAnalytics = createLoyaltyAnalyticsCard();
        contentArea.getChildren().add(loyaltyAnalytics);
        
        // Recommendation System Analytics
        VBox recommendationAnalytics = createRecommendationAnalyticsCard();
        contentArea.getChildren().add(recommendationAnalytics);
    }
    
    private VBox createSalesAnalyticsCard(java.util.Date startDate, java.util.Date endDate) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        Label title = new Label("💰 Sales Analytics");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Map<String, Object> analytics = reportGenerator.getSalesAnalytics(startDate, endDate);
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        
        grid.add(new Label("Total Orders:"), 0, 0);
        grid.add(new Label(String.valueOf(analytics.get("total_orders"))), 1, 0);
        
        grid.add(new Label("Total Revenue:"), 0, 1);
        grid.add(new Label(String.format("%.2f EGP", analytics.get("total_revenue"))), 1, 1);
        
        grid.add(new Label("Average Order Value:"), 0, 2);
        grid.add(new Label(String.format("%.2f EGP", analytics.get("avg_order_value"))), 1, 2);
        
        grid.add(new Label("Unique Customers:"), 0, 3);
        grid.add(new Label(String.valueOf(analytics.get("unique_customers"))), 1, 3);
        
        grid.add(new Label("Total Discounts:"), 0, 4);
        grid.add(new Label(String.format("%.2f EGP", analytics.get("total_discounts"))), 1, 4);
        
        grid.add(new Label("Orders per Customer:"), 0, 5);
        grid.add(new Label(String.format("%.2f", analytics.get("orders_per_customer"))), 1, 5);
        
        card.getChildren().addAll(title, grid);
        return card;
    }
    
    private VBox createTopSellingItemsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        Label title = new Label("🏆 Top Selling Items");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        List<Map<String, Object>> topItems = reportGenerator.getTopSellingItems(10);
        
        VBox itemsList = new VBox(5);
        for (int i = 0; i < topItems.size(); i++) {
            Map<String, Object> item = topItems.get(i);
            HBox itemRow = new HBox(10);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            
            Label rank = new Label("#" + (i + 1));
            rank.setStyle("-fx-font-weight: bold; -fx-text-fill: #FF6F00;");
            rank.setPrefWidth(30);
            
            Label name = new Label((String) item.get("name"));
            name.setPrefWidth(150);
            
            Label category = new Label((String) item.get("category"));
            category.setPrefWidth(100);
            
            Label quantity = new Label("Qty: " + item.get("total_quantity"));
            quantity.setPrefWidth(80);
            
            Label revenue = new Label(String.format("%.2f EGP", item.get("total_revenue")));
            revenue.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32;");
            
            itemRow.getChildren().addAll(rank, name, category, quantity, revenue);
            itemsList.getChildren().add(itemRow);
        }
        
        card.getChildren().addAll(title, itemsList);
        return card;
    }
    
    private VBox createCategorySalesCard(java.util.Date startDate, java.util.Date endDate) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        Label title = new Label("📊 Category Sales");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Map<String, Double> categorySales = reportGenerator.getCategorySales(startDate, endDate);
        
        VBox categoriesList = new VBox(5);
        for (Map.Entry<String, Double> entry : categorySales.entrySet()) {
            HBox categoryRow = new HBox(10);
            categoryRow.setAlignment(Pos.CENTER_LEFT);
            
            Label category = new Label(entry.getKey());
            category.setPrefWidth(150);
            
            Label revenue = new Label(String.format("%.2f EGP", entry.getValue()));
            revenue.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32;");
            
            categoryRow.getChildren().addAll(category, revenue);
            categoriesList.getChildren().add(categoryRow);
        }
        
        card.getChildren().addAll(title, categoriesList);
        return card;
    }
    
    private VBox createOrderStatusCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        Label title = new Label("📋 Order Status Distribution");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Map<String, Integer> statusDistribution = reportGenerator.getOrderStatusDistribution();
        
        VBox statusList = new VBox(5);
        for (Map.Entry<String, Integer> entry : statusDistribution.entrySet()) {
            HBox statusRow = new HBox(10);
            statusRow.setAlignment(Pos.CENTER_LEFT);
            
            Label status = new Label(entry.getKey().toUpperCase());
            status.setPrefWidth(100);
            
            Label count = new Label(String.valueOf(entry.getValue()));
            count.setStyle("-fx-font-weight: bold;");
            
            statusRow.getChildren().addAll(status, count);
            statusList.getChildren().add(statusRow);
        }
        
        card.getChildren().addAll(title, statusList);
        return card;
    }
    
    private VBox createCustomerAnalyticsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        Label title = new Label("👥 Customer Analytics");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Map<String, Object> customerAnalytics = reportGenerator.getCustomerAnalytics();
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        
        grid.add(new Label("Total Customers:"), 0, 0);
        grid.add(new Label(String.valueOf(customerAnalytics.get("total_customers"))), 1, 0);
        
        grid.add(new Label("Active (30 days):"), 0, 1);
        grid.add(new Label(String.valueOf(customerAnalytics.get("active_customers_30d"))), 1, 1);
        
        grid.add(new Label("Active (7 days):"), 0, 2);
        grid.add(new Label(String.valueOf(customerAnalytics.get("active_customers_7d"))), 1, 2);
        
        grid.add(new Label("Avg Order Value:"), 0, 3);
        grid.add(new Label(String.format("%.2f EGP", customerAnalytics.get("avg_order_value"))), 1, 3);
        
        grid.add(new Label("Max Order Value:"), 0, 4);
        grid.add(new Label(String.format("%.2f EGP", customerAnalytics.get("max_order_value"))), 1, 4);
        
        card.getChildren().addAll(title, grid);
        return card;
    }
    
    private VBox createRevenueTrendsCard(java.util.Date startDate, java.util.Date endDate) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        Label title = new Label("📈 Revenue Trends");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Map<String, Object> trends = reportGenerator.getRevenueTrends(startDate, endDate);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> dailyTrends = (List<Map<String, Object>>) trends.get("daily_trends");
        
        VBox trendsList = new VBox(5);
        if (dailyTrends != null && !dailyTrends.isEmpty()) {
            for (Map<String, Object> dayData : dailyTrends) {
                HBox dayRow = new HBox(10);
                dayRow.setAlignment(Pos.CENTER_LEFT);
                
                Label date = new Label(dayData.get("date").toString());
                date.setPrefWidth(120);
                
                Label orders = new Label("Orders: " + dayData.get("orders"));
                orders.setPrefWidth(100);
                
                Label revenue = new Label(String.format("%.2f EGP", dayData.get("revenue")));
                revenue.setStyle("-fx-font-weight: bold; -fx-text-fill: #2E7D32;");
                
                dayRow.getChildren().addAll(date, orders, revenue);
                trendsList.getChildren().add(dayRow);
            }
        } else {
            trendsList.getChildren().add(new Label("No data available for the selected date range."));
        }
        
        card.getChildren().addAll(title, trendsList);
        return card;
    }
    
    private VBox createLoyaltyAnalyticsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        Label title = new Label("🎁 Loyalty Program Analytics");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        Map<String, Object> loyaltyAnalytics = reportGenerator.getLoyaltyProgramAnalytics();
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        
        grid.add(new Label("Total Rewards:"), 0, 0);
        grid.add(new Label(String.valueOf(loyaltyAnalytics.get("total_rewards"))), 1, 0);
        
        grid.add(new Label("Redeemed Rewards:"), 0, 1);
        grid.add(new Label(String.valueOf(loyaltyAnalytics.get("redeemed_rewards"))), 1, 1);
        
        grid.add(new Label("Available Rewards:"), 0, 2);
        grid.add(new Label(String.valueOf(loyaltyAnalytics.get("available_rewards"))), 1, 2);
        
        grid.add(new Label("Avg Points Required:"), 0, 3);
        grid.add(new Label(String.format("%.1f", loyaltyAnalytics.get("avg_points_required"))), 1, 3);
        
        grid.add(new Label("Redemption Rate:"), 0, 4);
        grid.add(new Label(String.format("%.1f%%", loyaltyAnalytics.get("redemption_rate"))), 1, 4);
        
        card.getChildren().addAll(title, grid);
        return card;
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
    
    private VBox createRecommendationAnalyticsCard() {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(16));
        card.setStyle("-fx-background-color: #f8f9fa; -fx-border-color: #dee2e6; -fx-border-radius: 8; -fx-background-radius: 8;");
        
        Label title = new Label("🎯 Recommendation System Analytics");
        title.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        // Get recommendation system stats
        services.RecommendationSystem recommendationSystem = new services.RecommendationSystem();
        Map<String, Object> stats = recommendationSystem.getRecommendationStats();
        List<Map<String, Object>> topItems = recommendationSystem.getTopPerformingItems(menuData, 5);
        
        GridPane grid = new GridPane();
        grid.setHgap(20);
        grid.setVgap(10);
        
        grid.add(new Label("Total Interactions:"), 0, 0);
        grid.add(new Label(String.valueOf(stats.get("total_plays"))), 1, 0);
        
        grid.add(new Label("Unique Items:"), 0, 1);
        grid.add(new Label(String.valueOf(stats.get("unique_items"))), 1, 1);
        
        grid.add(new Label("Total Rewards:"), 0, 2);
        grid.add(new Label(String.format("%.1f", stats.get("total_rewards"))), 1, 2);
        
        grid.add(new Label("Avg Reward per Item:"), 0, 3);
        grid.add(new Label(String.format("%.2f", stats.get("average_reward_per_item"))), 1, 3);
        
        // Top performing items
        Label topItemsLabel = new Label("🏆 Top Performing Items (UCB Score):");
        topItemsLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #1976D2;");
        
        VBox topItemsList = new VBox(5);
        for (int i = 0; i < topItems.size(); i++) {
            Map<String, Object> itemData = topItems.get(i);
            MenuItem item = (MenuItem) itemData.get("item");
            double ucbValue = (Double) itemData.get("ucb_value");
            int playCount = (Integer) itemData.get("play_count");
            double totalReward = (Double) itemData.get("total_reward");
            
            HBox itemRow = new HBox(10);
            itemRow.setAlignment(Pos.CENTER_LEFT);
            
            Label rank = new Label("#" + (i + 1));
            rank.setStyle("-fx-font-weight: bold; -fx-text-fill: #FF6F00;");
            rank.setPrefWidth(30);
            
            Label name = new Label(item.getName());
            name.setPrefWidth(120);
            
            Label ucb = new Label(String.format("UCB: %.2f", ucbValue));
            ucb.setPrefWidth(80);
            
            Label plays = new Label("Plays: " + playCount);
            plays.setPrefWidth(70);
            
            Label reward = new Label(String.format("Reward: %.1f", totalReward));
            reward.setStyle("-fx-text-fill: #2E7D32;");
            
            itemRow.getChildren().addAll(rank, name, ucb, plays, reward);
            topItemsList.getChildren().add(itemRow);
        }
        
        card.getChildren().addAll(title, grid, topItemsLabel, topItemsList);
        return card;
    }
}