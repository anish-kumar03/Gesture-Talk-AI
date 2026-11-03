package com.gesturetalk.dao;

import com.gesturetalk.config.DBConnection;
import com.gesturetalk.model.GestureHistory;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Data Access Object (DAO) for the gesture_history database table.
 * Strictly adheres to JDBC best practices: PreparedStatements, try-with-resources,
 * parameter sanitization, and structured exception handling.
 */
public class GestureHistoryDAO implements HistoryDAO {

    private static final Logger logger = LoggerFactory.getLogger(GestureHistoryDAO.class);

    private static final String INSERT_SQL = 
            "INSERT INTO gesture_history (gesture_code, mapped_message, explanation, confidence, source_type) " +
            "VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_RECENT_SQL = 
            "SELECT id, gesture_code, mapped_message, explanation, confidence, source_type, created_at " +
            "FROM gesture_history ORDER BY id DESC LIMIT ?";

    private static final String SELECT_BY_GESTURE_SQL = 
            "SELECT id, gesture_code, mapped_message, explanation, confidence, source_type, created_at " +
            "FROM gesture_history WHERE gesture_code = ? ORDER BY id DESC LIMIT ?";

    private static final String SELECT_BY_ID_SQL = 
            "SELECT id, gesture_code, mapped_message, explanation, confidence, source_type, created_at " +
            "FROM gesture_history WHERE id = ?";

    private static final String DELETE_ALL_SQL = 
            "DELETE FROM gesture_history";

    private static final String COUNT_SQL = 
            "SELECT COUNT(*) FROM gesture_history";

    private final DataSource dataSource;

    /**
     * Default constructor utilizing the application's central DBConnection DataSource.
     */
    public GestureHistoryDAO() {
        this(DBConnection.getDataSource());
    }

    /**
     * Constructor allowing custom DataSource injection (ideal for testing with H2 or mock DataSources).
     *
     * @param dataSource custom JDBC DataSource
     */
    public GestureHistoryDAO(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    /**
     * Inserts a new gesture recognition record into the database.
     *
     * @param history record to insert
     * @return generated primary key ID
     * @throws SQLException if a database access error occurs
     */
    public long insert(GestureHistory history) throws SQLException {
        if (history == null) {
            throw new IllegalArgumentException("GestureHistory entity cannot be null.");
        }

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {

            ps.setString(1, truncate(history.getGestureCode(), 50));
            ps.setString(2, truncate(history.getMappedMessage(), 255));
            ps.setString(3, history.getExplanation());
            ps.setBigDecimal(4, BigDecimal.valueOf(history.getConfidence()));
            ps.setString(5, truncate(history.getSourceType(), 20));

            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new SQLException("Inserting gesture history record failed, no rows affected.");
            }

            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    long generatedId = rs.getLong(1);
                    logger.debug("Successfully inserted gesture history record with id: {}", generatedId);
                    return generatedId;
                } else {
                    throw new SQLException("Inserting gesture history record failed, no generated ID returned.");
                }
            }
        } catch (SQLException ex) {
            logger.error("Database error while inserting gesture history record: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Retrieves the latest N recognition records, ordered by id descending (most recent first).
     *
     * @param limit maximum number of records to return (minimum 1, maximum 100)
     * @return unmodifiable list of matching records
     * @throws SQLException if a database access error occurs
     */
    public List<GestureHistory> findRecent(int limit) throws SQLException {
        int safeLimit = Math.clamp(limit, 1, 100);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_RECENT_SQL)) {

            ps.setInt(1, safeLimit);

            try (ResultSet rs = ps.executeQuery()) {
                List<GestureHistory> records = new ArrayList<>();
                while (rs.next()) {
                    records.add(mapRow(rs));
                }
                return Collections.unmodifiableList(records);
            }
        } catch (SQLException ex) {
            logger.error("Database error while retrieving recent gesture history: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Retrieves the latest N records filtered by a specific gesture code.
     *
     * @param gestureCode gesture code to filter by (e.g. THUMBS_UP, UNKNOWN)
     * @param limit maximum number of records to return
     * @return unmodifiable list of matching records
     * @throws SQLException if a database access error occurs
     */
    public List<GestureHistory> findByGesture(String gestureCode, int limit) throws SQLException {
        if (gestureCode == null || gestureCode.isBlank() || "ALL".equalsIgnoreCase(gestureCode.trim())) {
            return findRecent(limit);
        }

        int safeLimit = Math.clamp(limit, 1, 100);

        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_GESTURE_SQL)) {

            ps.setString(1, gestureCode.trim().toUpperCase());
            ps.setInt(2, safeLimit);

            try (ResultSet rs = ps.executeQuery()) {
                List<GestureHistory> records = new ArrayList<>();
                while (rs.next()) {
                    records.add(mapRow(rs));
                }
                return Collections.unmodifiableList(records);
            }
        } catch (SQLException ex) {
            logger.error("Database error while filtering gesture history by [{}]: {}", gestureCode, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Retrieves a single record by its primary key ID.
     *
     * @param id primary key ID
     * @return Optional containing the record if found, or empty Optional
     * @throws SQLException if a database access error occurs
     */
    public Optional<GestureHistory> findById(long id) throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(SELECT_BY_ID_SQL)) {

            ps.setLong(1, id);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(mapRow(rs));
                }
            }
            return Optional.empty();
        } catch (SQLException ex) {
            logger.error("Database error while finding gesture record by id [{}]: {}", id, ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Purges all records from the gesture_history table.
     *
     * @return number of deleted records
     * @throws SQLException if a database access error occurs
     */
    public int deleteAll() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(DELETE_ALL_SQL)) {

            int deletedCount = ps.executeUpdate();
            logger.info("Purged {} records from gesture_history table.", deletedCount);
            return deletedCount;
        } catch (SQLException ex) {
            logger.error("Database error while clearing gesture history: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    /**
     * Returns the total count of records stored in the gesture_history table.
     *
     * @return total record count
     * @throws SQLException if a database access error occurs
     */
    public int count() throws SQLException {
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(COUNT_SQL);
             ResultSet rs = ps.executeQuery()) {

            if (rs.next()) {
                return rs.getInt(1);
            }
            return 0;
        } catch (SQLException ex) {
            logger.error("Database error while counting gesture records: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    private GestureHistory mapRow(ResultSet rs) throws SQLException {
        long id = rs.getLong("id");
        String gestureCode = rs.getString("gesture_code");
        String mappedMessage = rs.getString("mapped_message");
        String explanation = rs.getString("explanation");
        double confidence = rs.getBigDecimal("confidence") != null 
                ? rs.getBigDecimal("confidence").doubleValue() 
                : 0.0;
        String sourceType = rs.getString("source_type");
        Timestamp createdAt = rs.getTimestamp("created_at");

        return new GestureHistory(id, gestureCode, mappedMessage, explanation, confidence, sourceType, createdAt);
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength);
    }
}
