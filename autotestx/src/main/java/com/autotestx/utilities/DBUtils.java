package com.autotestx.utilities;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import java.sql.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Database utility for direct PostgreSQL validation.
 *
 * Usage:
 *   DBUtils.getInstance().querySingle("SELECT * FROM books WHERE id = ?", 1L)
 *   DBUtils.getInstance().verifyBookExists(bookId)
 *   DBUtils.getInstance().verifyOrderCreated(orderId)
 *
 * Real automation engineers validate API side-effects directly in the database.
 */
public class DBUtils {

    private static final Logger log = LogManager.getLogger(DBUtils.class);
    private static volatile DBUtils instance;
    private Connection connection;

    private DBUtils() {
        connect();
    }

    public static DBUtils getInstance() {
        if (instance == null) {
            synchronized (DBUtils.class) {
                if (instance == null) {
                    instance = new DBUtils();
                }
            }
        }
        return instance;
    }

    private void connect() {
        try {
            String url      = ConfigReader.get("db.url");
            String username = ConfigReader.get("db.username");
            String password = ConfigReader.get("db.password");
            Class.forName("org.postgresql.Driver");
            connection = DriverManager.getConnection(url, username, password);
            log.info("Database connected: {}", url);
        } catch (Exception e) {
            log.error("DB connection failed: {}", e.getMessage());
            throw new RuntimeException("Failed to connect to database", e);
        }
    }

    private Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            log.warn("DB connection lost, reconnecting...");
            connect();
        }
        return connection;
    }

    /**
     * Execute a SELECT and return all rows as List<Map<String, Object>>.
     */
    public List<Map<String, Object>> queryList(String sql, Object... params) {
        List<Map<String, Object>> results = new ArrayList<>();
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                ResultSetMetaData meta = rs.getMetaData();
                int colCount = meta.getColumnCount();
                while (rs.next()) {
                    Map<String, Object> row = new HashMap<>();
                    for (int i = 1; i <= colCount; i++) {
                        row.put(meta.getColumnName(i), rs.getObject(i));
                    }
                    results.add(row);
                }
            }
        } catch (SQLException e) {
            log.error("Query failed: {} | Error: {}", sql, e.getMessage());
            throw new RuntimeException("DB query error", e);
        }
        return results;
    }

    /**
     * Execute a SELECT and return the first row.
     */
    public Map<String, Object> querySingle(String sql, Object... params) {
        List<Map<String, Object>> results = queryList(sql, params);
        return results.isEmpty() ? null : results.get(0);
    }

    /**
     * Execute COUNT or scalar query.
     */
    public long queryCount(String sql, Object... params) {
        try (PreparedStatement ps = getConnection().prepareStatement(sql)) {
            setParams(ps, params);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getLong(1);
            }
        } catch (SQLException e) {
            throw new RuntimeException("DB count query error", e);
        }
        return 0;
    }

    // ── Domain-specific helpers ──────────────────────────────────────────────────

    public boolean verifyBookExists(Long bookId) {
        long count = queryCount("SELECT COUNT(*) FROM books WHERE id = ?", bookId);
        log.info("Book {} exists in DB: {}", bookId, count > 0);
        return count > 0;
    }

    public Map<String, Object> getBookFromDB(Long bookId) {
        return querySingle("SELECT * FROM books WHERE id = ?", bookId);
    }

    public boolean verifyUserExists(String email) {
        long count = queryCount("SELECT COUNT(*) FROM users WHERE email = ?", email);
        log.info("User {} exists in DB: {}", email, count > 0);
        return count > 0;
    }

    public boolean verifyOrderExists(Long orderId) {
        long count = queryCount("SELECT COUNT(*) FROM orders WHERE id = ?", orderId);
        log.info("Order {} exists in DB: {}", orderId, count > 0);
        return count > 0;
    }

    public String getOrderStatus(Long orderId) {
        Map<String, Object> row = querySingle("SELECT status FROM orders WHERE id = ?", orderId);
        return row != null ? (String) row.get("status") : null;
    }

    public boolean verifyReviewExists(Long userId, Long bookId) {
        long count = queryCount(
            "SELECT COUNT(*) FROM reviews WHERE user_id = ? AND book_id = ?", userId, bookId);
        return count > 0;
    }

    public int getBookStock(Long bookId) {
        Map<String, Object> row = querySingle("SELECT stock FROM books WHERE id = ?", bookId);
        return row != null ? ((Number) row.get("stock")).intValue() : -1;
    }

    public void closeConnection() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                log.info("DB connection closed");
            }
        } catch (SQLException e) {
            log.error("Failed to close DB connection", e);
        }
    }

    private void setParams(PreparedStatement ps, Object[] params) throws SQLException {
        for (int i = 0; i < params.length; i++) {
            ps.setObject(i + 1, params[i]);
        }
    }
}
