package ui;

import contracts.IOrderRepository;
import domain.Order;
import domain.Staff;
import infrastructure.DatabaseOrderRepository;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;

public class StaffDashboardView extends BorderPane {
    private final Staff staff;
    private final IOrderRepository orderRepo = new DatabaseOrderRepository();
    private final ObservableList<Order> orders = FXCollections.observableArrayList();
    private final ListView<Order> list = new ListView<>(orders);

    public StaffDashboardView(Stage stage, Staff s) {
        this.staff = s;
        setPadding(new Insets(16));

        HBox top = new HBox(12);
        top.setAlignment(Pos.CENTER_LEFT);
        Button logout = new Button("Logout");
        logout.setOnAction(e -> stage.getScene().setRoot(new LoginRegisterView(stage)));
        Label title = new Label("Order Management - Pending & Preparing Orders");
        title.getStyleClass().add("title");
        top.getChildren().addAll(title, new Label(" | Staff: " + staff.getName()), logout);
        setTop(top);

        list.setCellFactory(l -> new ListCell<>() {
            @Override protected void updateItem(Order o, boolean empty) {
                super.updateItem(o, empty);
                if (empty || o == null) { setText(null); }
                else setText("#"+o.getOrderID()+" - "+o.getStudentId()+" - "+o.getItemName()+" - "+o.getStatus());
            }
        });

        Button refresh = new Button("Refresh");
        refresh.setOnAction(e -> load());
        Button startPreparing = new Button("Start Preparing");
        Button markReady = new Button("Mark Ready");

        startPreparing.setOnAction(e -> {
            Order sel = list.getSelectionModel().getSelectedItem();
            if (sel == null) { info("Select an order."); return; }
            orderRepo.updateOrderStatus(sel.getOrderID(), "preparing");
            info("Order moved to preparing; student will be notified.");
            load();
        });

        markReady.setOnAction(e -> {
            Order sel = list.getSelectionModel().getSelectedItem();
            if (sel == null) { info("Select an order."); return; }
            orderRepo.updateOrderStatus(sel.getOrderID(), "ready");
            info("Order marked as ready; student will be notified.");
            load();
        });

        VBox center = new VBox(10, list, new HBox(10, refresh, startPreparing, markReady));
        center.getStyleClass().add("card");
        center.setPadding(new Insets(12));
        setCenter(center);
        load();
    }

    private void load() {
        orders.clear();
        for (Order o : orderRepo.getAllOrders()) {
            // Show pending and preparing orders for staff to manage
            if ("pending".equalsIgnoreCase(o.getStatus()) || "preparing".equalsIgnoreCase(o.getStatus())) {
                orders.add(o);
            }
        }
    }

    private void info(String m) { new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
}