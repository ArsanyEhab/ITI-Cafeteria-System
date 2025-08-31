package infrastructure;

import java.sql.*;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Robust Database Connection Manager
 * Provides automatic reconnection and connection health monitoring
 */
public class DatabaseRepository {
    private static Connection con;
    private static boolean connectionAttempted = false;
    private static long lastConnectionTime = 0;
    private static final long CONNECTION_TIMEOUT = 300000; // 5 minutes
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    private static final String URL = "jdbc:mysql://cafeteria-system-cafeteria-system.k.aivencloud.com:14411/defaultdb?sslmode=require";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_3QRmLrF1K5jfZ_qfPsn";

    // Static initializer to establish connection when class is loaded
    static {
        initializeConnection();
        // Start connection health monitoring
        startConnectionMonitoring();
    }

    private static void initializeConnection() {
        if (connectionAttempted && con != null && isConnectionValid()) {
            return;
        }
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            con = DriverManager.getConnection(URL, USER, PASSWORD);
            lastConnectionTime = System.currentTimeMillis();
            System.out.println("✅ Connected to the database!");
        } catch (ClassNotFoundException e) {
            System.out.println("⚠️ MySQL JDBC Driver not found - Running in offline mode");
            System.out.println("   (Add mysql-connector-java to classpath for database features)");
        } catch (SQLException e) {
            System.out.println("⚠️ Database connection failed - Running in offline mode");
            System.out.println("   Reason: " + e.getMessage());
        }
    }

    public static boolean isConnectionValid() {
        if (con == null) return false;
        try {
            return con.isValid(5); // 5 second timeout
        } catch (SQLException e) {
            return false;
        }
    }

    private static void startConnectionMonitoring() {
        scheduler.scheduleAtFixedRate(() -> {
            try {
                if (con != null && !isConnectionValid()) {
                    System.out.println("⚠️ Database connection lost, attempting to reconnect...");
                    reconnect();
                }
            } catch (Exception e) {
                System.err.println("❌ Connection monitoring error: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS); // Check every 30 seconds
    }

    private static void reconnect() {
        try {
            if (con != null) {
                try {
                    con.close();
                } catch (SQLException e) {
                    // Ignore close errors
                }
            }
            con = null;
            connectionAttempted = false;
            initializeConnection();
        } catch (Exception e) {
            System.err.println("❌ Reconnection failed: " + e.getMessage());
        }
    }

    public static Connection getConnection() {
        // Check if connection is valid and not too old
        if (con == null || !isConnectionValid() || 
            (System.currentTimeMillis() - lastConnectionTime) > CONNECTION_TIMEOUT) {
            System.out.println("🔄 Refreshing database connection...");
            reconnect();
        }
        return con;
    }

    public static Connection createNewConnection() {
        try {
            Connection newCon = DriverManager.getConnection(URL, USER, PASSWORD);
            // Set connection properties for better reliability
            newCon.setAutoCommit(true);
            newCon.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            return newCon;
        } catch (SQLException e) {
            System.err.println("❌ Failed to create new connection: " + e.getMessage());
            return null;
        }
    }

    /**
     * Get a connection with automatic retry
     */
    public static Connection getReliableConnection() {
        Connection connection = getConnection();
        if (connection == null || !isConnectionValid()) {
            reconnect();
            connection = getConnection();
        }
        return connection;
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

    /**
     * Shutdown the connection manager
     */
    public static void shutdown() {
        closeConnection();
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
    }
}