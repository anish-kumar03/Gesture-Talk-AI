package com.gesturetalk.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Properties;
import javax.sql.DataSource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Manages JDBC database connectivity and HikariCP connection pooling for GestureTalk AI.
 * <p>
 * Configuration resolution priority:
 * 1. Explicitly injected custom DataSource (primarily for unit/integration testing with H2)
 * 2. System Environment Variables (DB_URL, DB_USER, DB_PASSWORD, DB_DRIVER)
 * 3. db.properties configuration file in classpath
 * 4. Built-in defaults for local MySQL deployment
 */
public class DBConnection {

    private static final Logger logger = LoggerFactory.getLogger(DBConnection.class);

    // Environment variable names
    public static final String ENV_DB_URL = "DB_URL";
    public static final String ENV_DB_USER = "DB_USER";
    public static final String ENV_DB_PASSWORD = "DB_PASSWORD";
    public static final String ENV_DB_DRIVER = "DB_DRIVER";

    // Default configuration values
    public static final String DEFAULT_URL = 
            "jdbc:mysql://localhost:3306/gesturetalk_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    public static final String DEFAULT_USER = "root";
    public static final String DEFAULT_PASSWORD = "";
    public static final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    private static volatile DataSource customDataSource = null;
    private static volatile HikariDataSource defaultDataSource = null;

    private DBConnection() {
        // Utility / Configuration class; prevent direct instantiation
    }

    /**
     * Retrieves the active DataSource. If a custom DataSource was injected (e.g. for testing),
     * it will be returned. Otherwise, initializes and returns the HikariCP connection pool.
     *
     * @return active DataSource instance
     */
    public static DataSource getDataSource() {
        if (customDataSource != null) {
            return customDataSource;
        }

        if (defaultDataSource == null) {
            synchronized (DBConnection.class) {
                if (defaultDataSource == null) {
                    defaultDataSource = initializeHikariDataSource();
                }
            }
        }
        return defaultDataSource;
    }

    /**
     * Obtains a connection from the connection pool.
     * Callers MUST close the returned connection via try-with-resources.
     *
     * @return an open JDBC Connection
     * @throws SQLException if a database access error occurs
     */
    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }

    /**
     * Injects a custom DataSource (e.g. H2 in-memory DataSource for integration tests).
     *
     * @param dataSource custom DataSource or null to revert to default
     */
    public static synchronized void setDataSource(DataSource dataSource) {
        customDataSource = dataSource;
    }

    /**
     * Resets the custom DataSource, reverting to the default HikariCP pool.
     */
    public static synchronized void resetDataSource() {
        customDataSource = null;
    }

    /**
     * Tests connectivity to the database by attempting to borrow and validate a connection.
     *
     * @return true if database responds within 3 seconds, false otherwise
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed() && conn.isValid(3);
        } catch (Exception ex) {
            logger.warn("Database connectivity test failed: {}", ex.getMessage());
            return false;
        }
    }

    /**
     * Shuts down the HikariCP connection pool and releases all underlying resources.
     */
    public static synchronized void shutdown() {
        if (defaultDataSource != null && !defaultDataSource.isClosed()) {
            logger.info("Closing HikariCP connection pool...");
            defaultDataSource.close();
            defaultDataSource = null;
        }
    }

    private static HikariDataSource initializeHikariDataSource() {
        Properties fileProps = loadPropertiesFile();

        String url = getEnvOrProp(ENV_DB_URL, fileProps, "db.url", DEFAULT_URL);
        String user = getEnvOrProp(ENV_DB_USER, fileProps, "db.user", DEFAULT_USER);
        String password = getEnvOrProp(ENV_DB_PASSWORD, fileProps, "db.password", DEFAULT_PASSWORD);
        String driver = getEnvOrProp(ENV_DB_DRIVER, fileProps, "db.driver", DEFAULT_DRIVER);

        HikariConfig config = new HikariConfig();
        config.setPoolName("GestureTalk-HikariPool");
        config.setJdbcUrl(url);
        config.setUsername(user);
        config.setPassword(password);
        config.setDriverClassName(driver);

        // Pool tuning parameters
        config.setMaximumPoolSize(getIntProp(fileProps, "db.pool.maxSize", 10));
        config.setMinimumIdle(getIntProp(fileProps, "db.pool.minIdle", 2));
        config.setConnectionTimeout(getIntProp(fileProps, "db.pool.connectionTimeout", 3000));
        config.setIdleTimeout(getIntProp(fileProps, "db.pool.idleTimeout", 600000));

        // Reliability settings
        config.setConnectionTestQuery("SELECT 1");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        logger.info("Initializing HikariCP pool for JDBC URL: {}", url);
        return new HikariDataSource(config);
    }

    private static Properties loadPropertiesFile() {
        Properties props = new Properties();
        try (InputStream is = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (is != null) {
                props.load(is);
            }
        } catch (Exception ex) {
            logger.debug("Optional db.properties not found or unreadable: {}", ex.getMessage());
        }
        return props;
    }

    private static String getEnvOrProp(String envKey, Properties props, String propKey, String fallback) {
        String envVal = System.getenv(envKey);
        if (envVal != null && !envVal.isBlank()) {
            return envVal.trim();
        }
        String propVal = props.getProperty(propKey);
        if (propVal != null && !propVal.isBlank()) {
            return propVal.trim();
        }
        return fallback;
    }

    private static int getIntProp(Properties props, String key, int defaultValue) {
        try {
            String val = props.getProperty(key);
            if (val != null && !val.isBlank()) {
                return Integer.parseInt(val.trim());
            }
        } catch (NumberFormatException ignored) {
            // fall back
        }
        return defaultValue;
    }
}
