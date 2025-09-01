package ui;

import infrastructure.DatabaseUserRepository;
import infrastructure.DatabaseStaffRepository;
import domain.Student;
import domain.Staff;
import domain.IStudent;
import utils.PasswordEncryption;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class LoginRegisterView extends BorderPane {
    private final ToggleGroup authToggle = new ToggleGroup();
    private final TextField idField = new TextField();
    private final PasswordField passField = new PasswordField();
    private final TextField nameField = new TextField();
    private final ComboBox<String> roleBox = new ComboBox<>();

    private final DatabaseUserRepository userRepo = new DatabaseUserRepository();
    private final DatabaseStaffRepository staffRepo = new DatabaseStaffRepository();

    public LoginRegisterView(Stage stage) {
        setPadding(new Insets(24));
        setupStudentView(stage);
    }
    
    private void setupStudentView(Stage stage) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);

        Label title = new Label("Student Portal");
        title.getStyleClass().add("title");

        HBox authRow = new HBox(12);
        RadioButton login = new RadioButton("Login");
        RadioButton register = new RadioButton("Register");
        login.setToggleGroup(authToggle);
        register.setToggleGroup(authToggle);
        login.setSelected(true);
        authRow.getChildren().addAll(login, register);

        idField.setPromptText("Student ID");
        passField.setPromptText("Password");
        nameField.setPromptText("Full Name (register only)");
        nameField.managedProperty().bind(register.selectedProperty());
        nameField.visibleProperty().bind(register.selectedProperty());

        Button submit = new Button("Continue");
        submit.setOnAction(e -> handleStudent(stage, authToggle.getSelectedToggle() == register));

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.setAlignment(Pos.CENTER);
        
        // Create labels that will be shown/hidden based on register mode
        Label authLabel = new Label("Authentication:");
        Label idLabel = new Label("Student ID:");
        Label passLabel = new Label("Password:");
        Label nameLabel = new Label("Name:");
        
        // Hide name label during login
        nameLabel.managedProperty().bind(register.selectedProperty());
        nameLabel.visibleProperty().bind(register.selectedProperty());
        
        form.addRow(0, authLabel, authRow);
        form.addRow(1, idLabel, idField);
        form.addRow(2, passLabel, passField);
        form.addRow(3, nameLabel, nameField);

        VBox wrap = new VBox(16, title, form, submit);
        wrap.setPadding(new Insets(8));
        wrap.setAlignment(Pos.CENTER);

        // Add staff button at the bottom
        Button staffButton = new Button("Staff/Admin Login");
        staffButton.setOnAction(e -> setupStaffView(stage));
        staffButton.setStyle("-fx-font-size: 10px; -fx-padding: 5px;");
        
        VBox mainContent = new VBox(16, wrap, staffButton);
        mainContent.setAlignment(Pos.CENTER);
        
        setCenter(new StackPane(mainContent));
    }
    
    private void setupStaffView(Stage stage) {
        VBox card = new VBox(14);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);

        Label title = new Label("Staff Portal");
        title.getStyleClass().add("title");

        HBox authRow = new HBox(12);
        RadioButton login = new RadioButton("Login");
        RadioButton register = new RadioButton("Register");
        login.setToggleGroup(authToggle);
        register.setToggleGroup(authToggle);
        login.setSelected(true);
        authRow.getChildren().addAll(login, register);

        // Role selection only needed for registration, not login
        roleBox.getItems().addAll("Staff", "Admin");
        roleBox.getSelectionModel().select("Staff");
        roleBox.managedProperty().bind(register.selectedProperty());
        roleBox.visibleProperty().bind(register.selectedProperty());

        idField.setPromptText("Staff ID");
        passField.setPromptText("Password");
        nameField.setPromptText("Full Name (register only)");
        nameField.managedProperty().bind(register.selectedProperty());
        nameField.visibleProperty().bind(register.selectedProperty());

        Button submit = new Button("Continue");
        submit.setOnAction(e -> handleStaff(stage, authToggle.getSelectedToggle() == register, roleBox.getValue().equals("Admin")));

        GridPane form = new GridPane();
        form.setHgap(10); form.setVgap(10);
        form.setAlignment(Pos.CENTER);
        
        // Create labels that will be shown/hidden based on register mode
        Label authLabel = new Label("Authentication:");
        Label roleLabel = new Label("Role:");
        Label idLabel = new Label("Staff ID:");
        Label passLabel = new Label("Password:");
        Label nameLabel = new Label("Name:");
        
        // Hide role and name labels during login
        roleLabel.managedProperty().bind(register.selectedProperty());
        roleLabel.visibleProperty().bind(register.selectedProperty());
        nameLabel.managedProperty().bind(register.selectedProperty());
        nameLabel.visibleProperty().bind(register.selectedProperty());
        
        form.addRow(0, authLabel, authRow);
        form.addRow(1, roleLabel, roleBox);
        form.addRow(2, idLabel, idField);
        form.addRow(3, passLabel, passField);
        form.addRow(4, nameLabel, nameField);

        VBox wrap = new VBox(16, title, form, submit);
        wrap.setPadding(new Insets(8));
        wrap.setAlignment(Pos.CENTER);

        // Add back to student button at the bottom
        Button studentButton = new Button("Back to Student Portal");
        studentButton.setOnAction(e -> setupStudentView(stage));
        studentButton.setStyle("-fx-font-size: 10px; -fx-padding: 5px;");
        
        VBox mainContent = new VBox(16, wrap, studentButton);
        mainContent.setAlignment(Pos.CENTER);
        
        setCenter(new StackPane(mainContent));
    }

    private void handleStudent(Stage stage, boolean isRegister) {
        String id = idField.getText().trim();
        String pass = passField.getText().trim();
        
        if (id.isEmpty() || pass.isEmpty()) {
            show("Enter Student ID and Password.");
            return;
        }
        
        if (isRegister && nameField.getText().isEmpty()) {
            show("Enter full name for registration.");
            return;
        }
        
        if (isRegister) {
            Student existing = userRepo.findById(id);
            if (existing != null) { show("You are already registered."); return; }
            
            // Validate password security
            if (!utils.PasswordEncryption.isPasswordSecure(pass)) {
                show("❌ Password must be at least 6 characters and contain both letters and numbers!");
                return;
            }
            
            Student s = new Student(nameField.getText().trim(), id, pass);
            userRepo.save(s);
            show("✅ Registered successfully.");
        } else {
            // For login, verify password using the new secure method
            Student existing = userRepo.findById(id);
            if (existing == null) { show("Student not found."); return; }
            
            if (userRepo.verifyPassword(id, pass)) {
                StudentDashboardView view = new StudentDashboardView(stage, existing);
                stage.getScene().setRoot(view);
            } else {
                show("Invalid credentials.");
            }
        }
    }

    private void handleStaff(Stage stage, boolean isRegister, boolean asAdmin) {
        String id = idField.getText().trim();
        String pass = passField.getText().trim();
        
        if (id.isEmpty() || pass.isEmpty()) {
            show("Enter Staff ID and Password.");
            return;
        }
        
        if (isRegister && nameField.getText().isEmpty()) {
            show("Enter full name for registration.");
            return;
        }
        
        if (isRegister) {
            if (staffRepo.findById(id) != null) { show("You are already registered."); return; }
            
            // Validate password security
            if (!utils.PasswordEncryption.isPasswordSecure(pass)) {
                show("❌ Password must be at least 6 characters and contain both letters and numbers!");
                return;
            }
            
            boolean ok = staffRepo.registerStaff(id, nameField.getText().trim(), pass, asAdmin);
            if (!ok) { show("Registration failed."); return; }
            show("✅ Registered successfully.");
        } else {
            Staff s = staffRepo.login(id, pass);
            if (s == null) { show("Invalid credentials."); return; }
            if (s.isAdmin()) {
                stage.getScene().setRoot(new AdminDashboardView(stage, s));
            } else {
                stage.getScene().setRoot(new StaffDashboardView(stage, s));
            }
        }
    }

    private void show(String msg) { new Alert(Alert.AlertType.INFORMATION, msg).showAndWait(); }
}