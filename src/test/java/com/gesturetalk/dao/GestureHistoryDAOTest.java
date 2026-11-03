package com.gesturetalk.dao;

import com.gesturetalk.model.GestureAnalysis;
import com.gesturetalk.model.GestureHistory;
import com.gesturetalk.model.GestureType;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.List;
import java.util.Optional;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GestureHistoryDAO JDBC Integration Tests (H2 MySQL Mode)")
class GestureHistoryDAOTest {

    private static JdbcDataSource dataSource;
    private GestureHistoryDAO dao;

    @BeforeAll
    static void initSchema() throws SQLException {
        dataSource = new JdbcDataSource();
        dataSource.setURL("jdbc:h2:mem:gesturetalk_history_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        dataSource.setUser("sa");
        dataSource.setPassword("");

        try (Connection conn = dataSource.getConnection();
             Statement stmt = conn.createStatement()) {
            stmt.execute("CREATE TABLE IF NOT EXISTS gesture_history (" +
                    "  id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY," +
                    "  gesture_code VARCHAR(50) NOT NULL," +
                    "  mapped_message VARCHAR(255) NOT NULL," +
                    "  explanation TEXT," +
                    "  confidence DECIMAL(4,3) NOT NULL DEFAULT 0.000," +
                    "  source_type VARCHAR(20) NOT NULL DEFAULT 'WEBCAM'," +
                    "  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP" +
                    ")");
        }
    }

    @BeforeEach
    void setUp() throws SQLException {
        dao = new GestureHistoryDAO(dataSource);
        dao.deleteAll();
    }

    @Test
    @DisplayName("Should successfully insert a gesture history record and return a positive generated ID")
    void testInsert() throws SQLException {
        GestureHistory record = new GestureHistory(
                "THUMBS_UP",
                "Yes, that's correct.",
                "Hand with thumb pointing upwards.",
                0.965,
                "WEBCAM"
        );

        long generatedId = dao.insert(record);
        assertTrue(generatedId > 0, "Generated ID must be positive");

        assertEquals(1, dao.count());

        Optional<GestureHistory> retrievedOpt = dao.findById(generatedId);
        assertTrue(retrievedOpt.isPresent());
        GestureHistory retrieved = retrievedOpt.get();

        assertEquals(generatedId, retrieved.getId());
        assertEquals("THUMBS_UP", retrieved.getGestureCode());
        assertEquals("Yes, that's correct.", retrieved.getMappedMessage());
        assertEquals("Hand with thumb pointing upwards.", retrieved.getExplanation());
        assertEquals(0.965, retrieved.getConfidence(), 0.001);
        assertEquals("WEBCAM", retrieved.getSourceType());
        assertNotNull(retrieved.getCreatedAt());
    }

    @Test
    @DisplayName("Should retrieve recent records in descending order up to specified limit")
    void testFindRecent() throws SQLException {
        for (int i = 1; i <= 25; i++) {
            dao.insert(new GestureHistory(
                    "OPEN_PALM",
                    "Please stop or wait.",
                    "Frame sample " + i,
                    0.80 + (i * 0.005),
                    "WEBCAM"
            ));
        }

        assertEquals(25, dao.count());

        // Default display is latest 20 records
        List<GestureHistory> recent20 = dao.findRecent(20);
        assertEquals(20, recent20.size());

        // Verify descending order: first item should have higher ID than subsequent items
        assertTrue(recent20.get(0).getId() > recent20.get(1).getId());
        assertTrue(recent20.get(0).getExplanation().contains("Frame sample 25"));
    }

    @Test
    @DisplayName("Should accurately filter records by gesture type")
    void testFindByGesture() throws SQLException {
        dao.insert(new GestureHistory("THUMBS_UP", "Yes, that's correct.", "Ex 1", 0.95, "WEBCAM"));
        dao.insert(new GestureHistory("THUMBS_DOWN", "No, that's incorrect.", "Ex 2", 0.91, "UPLOAD"));
        dao.insert(new GestureHistory("THUMBS_UP", "Yes, that's correct.", "Ex 3", 0.88, "WEBCAM"));
        dao.insert(new GestureHistory("V_SIGN", "Peace and greetings.", "Ex 4", 0.99, "WEBCAM"));
        dao.insert(new GestureHistory("UNKNOWN", "Gesture not recognized.", "Ex 5", 0.35, "UPLOAD"));

        assertEquals(5, dao.count());

        List<GestureHistory> thumbsUpList = dao.findByGesture("THUMBS_UP", 20);
        assertEquals(2, thumbsUpList.size());
        assertTrue(thumbsUpList.stream().allMatch(r -> "THUMBS_UP".equals(r.getGestureCode())));

        List<GestureHistory> vSignList = dao.findByGesture("V_SIGN", 20);
        assertEquals(1, vSignList.size());
        assertEquals("V_SIGN", vSignList.get(0).getGestureCode());

        List<GestureHistory> unknownList = dao.findByGesture("UNKNOWN", 20);
        assertEquals(1, unknownList.size());
        assertEquals("UNKNOWN", unknownList.get(0).getGestureCode());

        // 'ALL' or empty filter should return all records
        List<GestureHistory> allList = dao.findByGesture("ALL", 20);
        assertEquals(5, allList.size());
    }

    @Test
    @DisplayName("Should purge all records on deleteAll and return deleted row count")
    void testDeleteAll() throws SQLException {
        dao.insert(new GestureHistory("THUMBS_UP", "Yes, that's correct.", "Ex 1", 0.95, "WEBCAM"));
        dao.insert(new GestureHistory("INDEX_POINTING", "Please look there.", "Ex 2", 0.85, "WEBCAM"));
        assertEquals(2, dao.count());

        int deletedCount = dao.deleteAll();
        assertEquals(2, deletedCount);
        assertEquals(0, dao.count());

        List<GestureHistory> remaining = dao.findRecent(20);
        assertTrue(remaining.isEmpty());
    }

    @Test
    @DisplayName("Should convert GestureAnalysis domain entity to GestureHistory entity cleanly")
    void testFromAnalysisConversion() throws SQLException {
        GestureAnalysis analysis = new GestureAnalysis(
                GestureType.V_SIGN,
                "Peace and greetings.",
                0.975,
                "Two extended digits in peace formation.",
                "WEBCAM",
                true,
                true
        );

        GestureHistory history = GestureHistory.fromAnalysis(analysis);
        assertEquals("V_SIGN", history.getGestureCode());
        assertEquals("Peace and greetings.", history.getMappedMessage());
        assertEquals("Two extended digits in peace formation.", history.getExplanation());
        assertEquals(0.975, history.getConfidence(), 0.001);
        assertEquals("WEBCAM", history.getSourceType());

        long id = dao.insert(history);
        assertTrue(id > 0);
    }
}

