package com.gesturetalk.config;

import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

/**
 * Adapter delegating to {@link DBConnection}.
 * Provides backwards compatibility with documentation references to DatabaseConnection.
 */
public class DatabaseConnection {

    private DatabaseConnection() {
    }

    public static DataSource getDataSource() {
        return DBConnection.getDataSource();
    }

    public static Connection getConnection() throws SQLException {
        return DBConnection.getConnection();
    }

    public static void setDataSource(DataSource ds) {
        DBConnection.setDataSource(ds);
    }

    public static void resetDataSource() {
        DBConnection.resetDataSource();
    }

    public static boolean testConnection() {
        return DBConnection.testConnection();
    }

    public static void shutdown() {
        DBConnection.shutdown();
    }
}

