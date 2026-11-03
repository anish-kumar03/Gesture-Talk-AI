package com.gesturetalk.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;
import org.h2.jdbcx.JdbcDataSource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("DBConnection Configuration and Pool Tests")
class DBConnectionTest {

    @AfterEach
    void tearDown() {
        DBConnection.resetDataSource();
    }

    @Test
    @DisplayName("Should expose expected default database configuration properties")
    void testDefaultConstants() {
        assertNotNull(DBConnection.DEFAULT_URL);
        assertTrue(DBConnection.DEFAULT_URL.contains("gesturetalk_db"));
        assertEquals("root", DBConnection.DEFAULT_USER);
        assertEquals("com.mysql.cj.jdbc.Driver", DBConnection.DEFAULT_DRIVER);
    }

    @Test
    @DisplayName("Should accept custom DataSource injection and borrow connection")
    void testCustomDataSourceInjection() throws SQLException {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:db_conn_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        h2DataSource.setUser("sa");
        h2DataSource.setPassword("");

        DBConnection.setDataSource(h2DataSource);
        assertSame(h2DataSource, DBConnection.getDataSource());

        try (Connection conn = DBConnection.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
            assertTrue(conn.isValid(1));
        }

        assertTrue(DBConnection.testConnection());
    }

    @Test
    @DisplayName("DatabaseConnection adapter should delegate faithfully to DBConnection")
    void testDatabaseConnectionAdapter() throws SQLException {
        JdbcDataSource h2DataSource = new JdbcDataSource();
        h2DataSource.setURL("jdbc:h2:mem:db_adapter_test;MODE=MySQL;DB_CLOSE_DELAY=-1");
        h2DataSource.setUser("sa");
        h2DataSource.setPassword("");

        DatabaseConnection.setDataSource(h2DataSource);
        assertSame(h2DataSource, DatabaseConnection.getDataSource());

        try (Connection conn = DatabaseConnection.getConnection()) {
            assertNotNull(conn);
            assertFalse(conn.isClosed());
        }

        assertTrue(DatabaseConnection.testConnection());

        DatabaseConnection.resetDataSource();
    }
}

