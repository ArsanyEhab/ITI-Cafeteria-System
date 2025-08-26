package infrastructure;
import domain.Order;
import domain.MenuItem;
import domain.Student;

import java.sql.*;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class DatabaseRepository {
    private Connection con;

    // JDBC connection details (example for Aiven MySQL)
    private static final String URL = "jdbc:mysql://cafeteria-system-cafeteria-system.k.aivencloud.com:14411/defaultdb?sslmode=require";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_3QRmLrF1K5jfZ_qfPsn";

    public DatabaseRepository() {
        try {
            // Optional: load JDBC driver (needed for older Java versions)
            Class.forName("com.mysql.cj.jdbc.Driver");

            // Establish connection
            this.con = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to the database!");

        } catch (ClassNotFoundException e) {
            System.err.println("❌ JDBC Driver not found: " + e.getMessage());
        } catch (SQLException e) {
            System.err.println("❌ Database connection failed: " + e.getMessage());
        }
    }

    // Getter to use the connection elsewhere
    public Connection getConnection() {
        return this.con;
    }
    
    // Close the connection
    public void closeConnection() {
        if (con != null) {
            try {
                con.close();
                System.out.println("✅ Database connection closed!");
            } catch (SQLException e) {
                System.err.println("❌ Error closing database connection: " + e.getMessage());
            }
        }
    }
}
