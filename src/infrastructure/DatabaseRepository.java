package infrastructure;

import java.sql.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantLock;

/**
 * Optimized Database Connection Manager with Connection Pooling
 * Provides fast, reliable database access with connection reuse
 */
public class DatabaseRepository {
    // Connection pool configuration
    private static final int INITIAL_POOL_SIZE = 15;
    private static final int MAX_POOL_SIZE = 50;
    private static final int MAX_TIMEOUT = 30000; // 30 seconds
    private static final int VALIDATION_TIMEOUT = 5; // 5 seconds
    
    // Connection pool
    private static final BlockingQueue<Connection> connectionPool = new LinkedBlockingQueue<>(MAX_POOL_SIZE);
    private static final AtomicInteger activeConnections = new AtomicInteger(0);
    private static final ReentrantLock poolLock = new ReentrantLock();
    
    // Database configuration
    private static final String URL = "jdbc:mysql://cafeteria-system-cafeteria-system.k.aivencloud.com:14411/defaultdb?sslmode=require&useSSL=true&serverTimezone=UTC&allowPublicKeyRetrieval=true";
    private static final String USER = "avnadmin";
    private static final String PASSWORD = "AVNS_3QRmLrF1K5jfZ_qfPsn";
    
    // Connection monitoring
    private static final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(2);
    private static volatile boolean isInitialized = false;
    
    // Static initializer
    static {
        initializeConnectionPool();
        startConnectionMonitoring();
    }
    
    /**
     * Initialize the connection pool with initial connections
     */
    public static void initializeConnectionPool() {
        if (isInitialized) return;
        
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Create initial connections
            for (int i = 0; i < INITIAL_POOL_SIZE; i++) {
                Connection conn = createNewConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                }
            }
            
            isInitialized = true;
            System.out.println("✅ Database connection pool initialized with " + connectionPool.size() + " connections");
            
        } catch (ClassNotFoundException e) {
            System.err.println("❌ MySQL JDBC Driver not found: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("❌ Failed to initialize connection pool: " + e.getMessage());
        }
    }
    
    /**
     * Create a new database connection with optimized settings
     */
    private static Connection createNewConnection() {
        try {
            Connection conn = DriverManager.getConnection(URL, USER, PASSWORD);
            
            // Optimize connection settings for performance
            conn.setAutoCommit(true);
            conn.setTransactionIsolation(Connection.TRANSACTION_READ_COMMITTED);
            conn.setNetworkTimeout(Executors.newSingleThreadExecutor(), MAX_TIMEOUT);
            
            return conn;
        } catch (SQLException e) {
            System.err.println("❌ Failed to create connection: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Get a connection from the pool (FAST ACCESS)
     */
    public static Connection getConnection() {
        if (!isInitialized) {
            initializeConnectionPool();
        }
        
        try {
            // Try to get connection from pool with timeout
            Connection conn = connectionPool.poll(100, TimeUnit.MILLISECONDS);
            
            if (conn != null && isConnectionValid(conn)) {
                activeConnections.incrementAndGet();
                return conn;
            }
            
            // If no valid connection in pool, create new one if under limit
            if (activeConnections.get() < MAX_POOL_SIZE) {
                conn = createNewConnection();
                if (conn != null) {
                    activeConnections.incrementAndGet();
                    return conn;
                }
            }
            
            // Wait for a connection to become available
            conn = connectionPool.take();
            activeConnections.incrementAndGet();
            return conn;
            
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return createNewConnection(); // Fallback
        }
    }
    
    /**
     * Check if the connection pool is ready
     */
    public static boolean isPoolReady() {
        return isInitialized && !connectionPool.isEmpty();
    }
    
    /**
     * Get pool status information
     */
    public static String getPoolStatus() {
        if (!isInitialized) {
            return "Pool not initialized";
        }
        return String.format("Pool ready: %d available, %d active connections", 
                           connectionPool.size(), activeConnections.get());
    }
    
    /**
     * Return a connection to the pool
     */
    public static void returnConnection(Connection conn) {
        if (conn == null) return;
        
        try {
            if (isConnectionValid(conn)) {
                // Reset connection state
                if (conn.getAutoCommit() == false) {
                    conn.rollback();
                    conn.setAutoCommit(true);
                }
                
                // Return to pool
                connectionPool.offer(conn);
            } else {
                // Connection is invalid, close it
                conn.close();
            }
        } catch (SQLException e) {
            try {
                conn.close();
            } catch (SQLException closeEx) {
                // Ignore close errors
            }
        } finally {
            activeConnections.decrementAndGet();
        }
    }
    
    /**
     * Check if a connection is valid
     */
    private static boolean isConnectionValid(Connection conn) {
        if (conn == null) return false;
        try {
            return conn.isValid(VALIDATION_TIMEOUT);
        } catch (SQLException e) {
            return false;
        }
    }
    

    
    /**
     * Start connection monitoring and pool maintenance
     */
    private static void startConnectionMonitoring() {
        // Monitor pool health every 30 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                maintainPool();
            } catch (Exception e) {
                System.err.println("❌ Pool maintenance error: " + e.getMessage());
            }
        }, 30, 30, TimeUnit.SECONDS);
        
        // Validate connections every 60 seconds
        scheduler.scheduleAtFixedRate(() -> {
            try {
                validatePoolConnections();
            } catch (Exception e) {
                System.err.println("❌ Connection validation error: " + e.getMessage());
            }
        }, 60, 60, TimeUnit.SECONDS);
    }
    
    /**
     * Maintain optimal pool size
     */
    private static void maintainPool() {
        poolLock.lock();
        try {
            int currentSize = connectionPool.size();
            int targetSize = Math.max(INITIAL_POOL_SIZE, activeConnections.get() + 2);
            
            // Add connections if pool is too small
            while (connectionPool.size() < targetSize && connectionPool.size() < MAX_POOL_SIZE) {
                Connection conn = createNewConnection();
                if (conn != null) {
                    connectionPool.offer(conn);
                } else {
                    break; // Stop if we can't create more connections
                }
            }
            
            // Remove excess connections if pool is too large
            while (connectionPool.size() > targetSize && connectionPool.size() > INITIAL_POOL_SIZE) {
                Connection conn = connectionPool.poll();
                if (conn != null) {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        // Ignore close errors
                    }
                }
            }
        } finally {
            poolLock.unlock();
        }
    }
    
    /**
     * Gracefully shutdown the connection pool
     */
    public static void shutdownPool() {
        System.out.println("🔄 Shutting down connection pool...");
        
        // Stop the scheduler
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
        
        // Close all connections in the pool
        poolLock.lock();
        try {
            Connection conn;
            int closedCount = 0;
            while ((conn = connectionPool.poll()) != null) {
                try {
                    conn.close();
                    closedCount++;
                } catch (SQLException e) {
                    // Ignore close errors
                }
            }
            System.out.println("✅ Closed " + closedCount + " database connections");
            isInitialized = false;
        } finally {
            poolLock.unlock();
        }
    }
    
    /**
     * Validate and clean up invalid connections
     */
    private static void validatePoolConnections() {
        poolLock.lock();
        try {
            int initialSize = connectionPool.size();
            BlockingQueue<Connection> validConnections = new LinkedBlockingQueue<>();
            
            // Validate each connection
            Connection conn;
            while ((conn = connectionPool.poll()) != null) {
                if (isConnectionValid(conn)) {
                    validConnections.offer(conn);
                } else {
                    try {
                        conn.close();
                    } catch (SQLException e) {
                        // Ignore close errors
                    }
                }
            }
            
            // Replace pool with valid connections
            connectionPool.addAll(validConnections);
            
            int removed = initialSize - connectionPool.size();
            if (removed > 0) {
                System.out.println("🔄 Removed " + removed + " invalid connections from pool");
            }
        } finally {
            poolLock.unlock();
        }
    }
}