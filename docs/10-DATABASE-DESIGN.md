# GestureTalk AI — Database Design & Data Access Architecture

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-10 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Relational Database & Data Access Specification |
| **Target RDBMS** | MySQL 8.0+ / 8.4 LTS |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |
| **Companion Diagram** | [diagrams/database-er-diagram.md](diagrams/database-er-diagram.md) |

---

## 1. Database Philosophy & Architectural Constraints

The persistence architecture for GestureTalk AI is strictly scoped to meet audit, monitoring, and verification requirements. In strict accordance with the project guidelines:
* **No Unnecessary Entities**: The schema intentionally excludes superfluous tables (such as user accounts, roles, permissions, or system configurations).
* **Zero Image Persistence**: Raw images, base64 strings, or file paths are **never** stored in the database. Only symbolic gesture metadata and classification telemetry are retained.
* **100% PreparedStatements**: All database queries are executed via parameterized JDBC `PreparedStatement` instances.

---

## 2. Database & Schema Definition

* **Database Name**: `gesturetalk_db`
* **Character Set**: `utf8mb4`
* **Collation**: `utf8mb4_unicode_ci`
* **Storage Engine**: `InnoDB`

### 2.1 Complete SQL DDL Script

```sql
-- =============================================================================
-- GestureTalk AI Database Schema
-- Target: MySQL 8.0 / 8.4 LTS
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `gesturetalk_db`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `gesturetalk_db`;

-- Drop table if upgrading
DROP TABLE IF EXISTS `gesture_history`;

-- Core Audit & Recognition Log Table
CREATE TABLE `gesture_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `gesture_code` VARCHAR(32) NOT NULL,
  `mapped_message` VARCHAR(255) NOT NULL,
  `explanation` TEXT NULL,
  `confidence` DECIMAL(4,3) NOT NULL DEFAULT 0.000,
  `source_type` ENUM('WEBCAM', 'UPLOAD') NOT NULL DEFAULT 'WEBCAM',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  INDEX `idx_gesture_code` (`gesture_code`),
  INDEX `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
```

---

## 3. Data Dictionary: `gesture_history`

```
┌────────────────┬──────────────┬──────────┬───────────────┬────────────────────────────────────────────────────────┐
│ Field Name     │ Data Type    │ Nullable │ Default Value │ Functional Description & Constraints                   │
├────────────────┼──────────────┼──────────┼───────────────┼────────────────────────────────────────────────────────┤
│ id             │ BIGINT       │ No       │ AUTO_INCREMENT│ Synthetic primary key uniquely identifying the log.    │
├────────────────┼──────────────┼──────────┼───────────────┼────────────────────────────────────────────────────────┤
│ gesture_code   │ VARCHAR(32)  │ No       │ None          │ Standardized gesture token: 'THUMBS_UP', 'THUMBS_DOWN',│
│                │              │          │               │ 'OPEN_PALM', 'V_SIGN', 'INDEX_POINTING', or 'UNKNOWN'. │
├────────────────┼──────────────┼──────────┼───────────────┼────────────────────────────────────────────────────────┤
│ mapped_message │ VARCHAR(255) │ No       │ None          │ Deterministic spoken text mapped by Java logic.        │
├────────────────┼──────────────┼──────────┼───────────────┼────────────────────────────────────────────────────────┤
│ explanation    │ TEXT         │ Yes      │ NULL          │ Qualitative reasoning provided by Gemini Vision.       │
├────────────────┼──────────────┼──────────┼───────────────┼────────────────────────────────────────────────────────┤
│ confidence     │ DECIMAL(4,3) │ No       │ 0.000         │ Model self-assessed confidence score (0.000 to 1.000). │
├────────────────┼──────────────┼──────────┼───────────────┼────────────────────────────────────────────────────────┤
│ source_type    │ ENUM(...)    │ No       │ 'WEBCAM'      │ Ingestion origin: 'WEBCAM' (stream) or 'UPLOAD' (file).│
├────────────────┼──────────────┼──────────┼───────────────┼────────────────────────────────────────────────────────┤
│ created_at     │ TIMESTAMP    │ No       │ CURRENT_TIME  │ UTC timestamp when the record was inserted. Indexed.   │
└────────────────┴──────────────┴──────────┴───────────────┴────────────────────────────────────────────────────────┘
```

---

## 4. Sample Audit Data Records

```
┌────┬──────────────┬─────────────────────────────┬───────────────────────────────────────┬────────────┬─────────────┬─────────────────────┐
│ id │ gesture_code │ mapped_message              │ explanation                           │ confidence │ source_type │ created_at          │
├────┼──────────────┼─────────────────────────────┼───────────────────────────────────────┼────────────┼─────────────┼─────────────────────┤
│ 1  │ THUMBS_UP    │ Yes, that's correct.        │ Closed fist with upright thumb.       │ 0.965      │ WEBCAM      │ 2026-11-03 14:00:12 │
│ 2  │ OPEN_PALM    │ Please stop or wait.        │ Open flat hand with 5 visible digits. │ 0.920      │ WEBCAM      │ 2026-11-03 14:01:05 │
│ 3  │ UNKNOWN      │ Gesture not recognized. ... │ Hand blurry and partially occluded.   │ 0.350      │ UPLOAD      │ 2026-11-03 14:02:40 │
│ 4  │ V_SIGN       │ Peace and greetings.        │ Index and middle fingers extended.    │ 0.981      │ WEBCAM      │ 2026-11-03 14:05:18 │
└────┴──────────────┴─────────────────────────────┴───────────────────────────────────────┴────────────┴─────────────┴─────────────────────┘
```

---

## 5. JDBC Data Access Layer Architecture

### 5.1 Connection Pooling via HikariCP
To eliminate the severe overhead of opening fresh TCP and TLS handshakes to MySQL on every recognition request, the application uses **HikariCP** (`com.zaxxer.HikariCP:5.1.0`).

```java
package com.gesturetalk.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import javax.sql.DataSource;

public class DatabaseConnection {
    private static volatile HikariDataSource dataSource;

    public static DataSource getDataSource() {
        if (dataSource == null) {
            synchronized (DatabaseConnection.class) {
                if (dataSource == null) {
                    HikariConfig config = new HikariConfig();
                    String jdbcUrl = System.getenv("DB_URL") != null ? 
                        System.getenv("DB_URL") : "jdbc:mysql://localhost:3306/gesturetalk_db?useSSL=false&serverTimezone=UTC";
                    String user = System.getenv("DB_USER") != null ? System.getenv("DB_USER") : "root";
                    String password = System.getenv("DB_PASSWORD") != null ? System.getenv("DB_PASSWORD") : "";

                    config.setJdbcUrl(jdbcUrl);
                    config.setUsername(user);
                    config.setPassword(password);
                    config.setMaximumPoolSize(10);
                    config.setMinimumIdle(2);
                    config.setConnectionTimeout(3000); // 3 seconds timeout
                    config.setIdleTimeout(600000);
                    
                    dataSource = new HikariDataSource(config);
                }
            }
        }
        return dataSource;
    }

    public static Connection getConnection() throws SQLException {
        return getDataSource().getConnection();
    }
}
```

### 5.2 DAO Responsibilities: `RecognitionLogDAO`
The DAO handles all SQL statements via strict `PreparedStatement` parameters:

```java
package com.gesturetalk.dao;

import com.gesturetalk.config.DatabaseConnection;
import com.gesturetalk.model.RecognitionLog;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RecognitionLogDAO {

    private static final String INSERT_SQL = 
        "INSERT INTO gesture_history (gesture_code, mapped_message, explanation, confidence, source_type) " +
        "VALUES (?, ?, ?, ?, ?)";

    private static final String SELECT_RECENT_SQL = 
        "SELECT id, gesture_code, mapped_message, explanation, confidence, source_type, created_at " +
        "FROM gesture_history ORDER BY created_at DESC LIMIT ?";

    private static final String FILTER_BY_GESTURE_SQL = 
        "SELECT id, gesture_code, mapped_message, explanation, confidence, source_type, created_at " +
        "FROM gesture_history WHERE gesture_code = ? ORDER BY created_at DESC LIMIT ?";

    private static final String DELETE_ALL_SQL = 
        "DELETE FROM gesture_history";

    public long insert(RecognitionLog log) throws SQLException {
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(INSERT_SQL, Statement.RETURN_GENERATED_KEYS)) {
            
            ps.setString(1, log.getGestureCode());
            ps.setString(2, log.getMappedMessage());
            ps.setString(3, log.getExplanation());
            ps.setBigDecimal(4, java.math.BigDecimal.valueOf(log.getConfidence()));
            ps.setString(5, log.getSourceType());
            
            ps.executeUpdate();
            try (ResultSet rs = ps.getGeneratedKeys()) {
                if (rs.next()) {
                    return rs.getLong(1);
                }
            }
        }
        return -1;
    }
}
```

---

## 6. Document Cross-References

* Visual ER Diagram: [diagrams/database-er-diagram.md](diagrams/database-er-diagram.md)
* API endpoints querying this table: [09-API-ENDPOINT-DESIGN.md](09-API-ENDPOINT-DESIGN.md)
* Security and SQL injection prevention: [12-SECURITY-AND-PRIVACY.md](12-SECURITY-AND-PRIVACY.md)

