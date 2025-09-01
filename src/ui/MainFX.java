package ui;

import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import infrastructure.DatabaseRepository;

public class MainFX extends Application {

    @Override
    public void start(Stage stage) {
        // Initialize database connection pool at app startup
        try {
            System.out.println("🚀 Initializing database connection pool...");
            DatabaseRepository.initializeConnectionPool();
            System.out.println("✅ Database connection pool ready!");
            System.out.println("📊 " + DatabaseRepository.getPoolStatus());
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize database: " + e.getMessage());
            // Continue with app startup even if DB fails
        }
        
        LoginRegisterView root = new LoginRegisterView(stage);
        Scene scene = new Scene(root, 900, 600);
        // Load the CSS stylesheet
        scene.getStylesheets().add(getClass().getResource("/app.css").toExternalForm());
        stage.setTitle("Cafeteria System - Student Portal");
        stage.setScene(scene);
        stage.show();
        
        // Handle application shutdown
        stage.setOnCloseRequest(event -> {
            System.out.println("🔄 Shutting down application...");
            try {
                // Close database connections gracefully
                DatabaseRepository.shutdownPool();
                System.out.println("✅ Database connections closed gracefully");
            } catch (Exception e) {
                System.err.println("❌ Error during shutdown: " + e.getMessage());
            }
        });
    }

    public static void main(String[] args) {
        launch();
    }
}