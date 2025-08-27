package infrastructure;

import java.sql.*;

/**
 * Simple Database Connection Manager
 * Provides static access to database connection for all repositories
 */
public class DatabaseRepository {
    private static Connection con;
    private static boolean connectionAttempted = false;

    private static final String URL = "jdbc:mysql://cafeteria-system-cafeteria-system.k.aivencloud.com:14411/defaultdb?sslmode=require";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_3QRmLrF1K5jfZ_qfPsn";

    // Static initializer to establish connection when class is loaded
    static {
        initializeConnection();
    }

    private static void initializeConnection() {
        if (connectionAttempted) {
            return;
        }
        connectionAttempted = true;
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASSWORD);
            System.out.println("✅ Connected to the database!");
        } catch (ClassNotFoundException e) {
            System.out.println("⚠️ MySQL JDBC Driver not found - Running in offline mode");
            System.out.println("   (Add mysql-connector-java to classpath for database features)");
        } catch (SQLException e) {
            System.out.println("⚠️ Database connection failed - Running in offline mode");
            System.out.println("   Reason: " + e.getMessage());
        }
    }


    public static Connection getConnection() {
        if (con == null) {
            initializeConnection();
        }
        return con;
    }


    public static Connection createNewConnection() {
        try {
            return DriverManager.getConnection(URL, USER, PASSWORD);
        } catch (SQLException e) {
            return null;
        }
    }

    /**
     * Close the main database connection
     */
    public static void closeConnection() {
        if (con != null) {
            try {
                con.close();
                con = null;
                System.out.println("✅ Database connection closed!");
            } catch (SQLException e) {
                System.err.println("❌ Error closing database connection: " + e.getMessage());
            }
        }
    }
}