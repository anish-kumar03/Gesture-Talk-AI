# GestureTalk AI — Database Architecture & Specification

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DB-01 |
| **Project Name** | GestureTalk AI |
| **Version** | 1.0.0 (Production Release) |
| **Classification** | Relational Database & Persistence Specification |
| **Target Engine** | MySQL 8.0+ / 8.4 LTS (Compatible with H2 MySQL Mode) |

---

## 1. Persistence Overview & Privacy Philosophy

The persistence layer of **GestureTalk AI** is designed specifically for **telemetry auditing, usage tracking, and quality verification**.

### Core Constraints & Guarantees:
* **Zero Image Retention**: Raw image bytes, base64 strings, or temporary file paths are **never** persisted to the database. Only classified gesture codes and telemetry metadata are retained.
* **No Unnecessary Entities**: The schema excludes extraneous user authentication tables, sessions, or enterprise access control lists.
* **100% PreparedStatements**: All SQL interactions use parameterized JDBC `PreparedStatement` instances. No string concatenation is permitted.
* **High-Performance Connection Pooling**: Database connections are managed via **HikariCP** (`HikariDataSource`), minimizing latency and connection overhead.

---

## 2. Database Schema DDL

```sql
-- =============================================================================
-- GestureTalk AI Database Schema
-- Database Name: gesturetalk_db
-- Target: MySQL 8.0+ / 8.4 LTS
-- =============================================================================

CREATE DATABASE IF NOT EXISTS `gesturetalk_db`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `gesturetalk_db`;

-- Core Audit & Recognition Log Table
CREATE TABLE IF NOT EXISTS `gesture_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT,
  `gesture_code` VARCHAR(50) NOT NULL,
  `mapped_message` VARCHAR(255) NOT NULL,
  `explanation` TEXT NULL,
  `confidence` DECIMAL(4,3) NOT NULL DEFAULT 0.000,
  `source_type` VARCHAR(20) NOT NULL DEFAULT 'WEBCAM',
  `created_at` TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- Performance Indexes for Sorting & Filtering
CREATE INDEX `idx_gesture_code` ON `gesture_history` (`gesture_code`);
CREATE INDEX `idx_created_at` ON `gesture_history` (`created_at`);
```

---

## 3. Data Dictionary: `gesture_history`

| Column | Data Type | Nullable | Default | Description |
| :--- | :--- | :--- | :--- | :--- |
| **`id`** | `BIGINT` | No | `AUTO_INCREMENT` | Synthetic primary key uniquely identifying each audit log record. |
| **`gesture_code`** | `VARCHAR(50)` | No | — | Standardized gesture token: `THUMBS_UP`, `THUMBS_DOWN`, `OPEN_PALM`, `V_SIGN`, `INDEX_POINTING`, or `UNKNOWN`. Indexed. |
| **`mapped_message`** | `VARCHAR(255)` | No | — | The immutable user-facing sentence mapped by deterministic Java logic. |
| **`explanation`** | `TEXT` | Yes | `NULL` | Model-generated diagnostic reasoning explaining the observed hand posture. |
| **`confidence`** | `DECIMAL(4,3)` | No | `0.000` | Model self-estimated certainty score ranging between `0.000` and `1.000`. |
| **`source_type`** | `VARCHAR(20)` | No | `'WEBCAM'` | Origin of image payload: `'WEBCAM'` (live viewfinder frame) or `'UPLOAD'` (manual file). |
| **`created_at`** | `TIMESTAMP` | No | `CURRENT_TIMESTAMP` | Server-assigned timestamp marking record creation. Indexed for fast descending queries. |

---

## 4. JDBC Data Access Layer Architecture

### 4.1 Connection Management via HikariCP ([`DBConnection.java`](file:///d:/MY%20PROJECTS/GestureTalk%20AI/src/main/java/com/gesturetalk/config/DBConnection.java))
```java
HikariConfig config = new HikariConfig();
config.setPoolName("GestureTalk-HikariPool");
config.setJdbcUrl(url);
config.setUsername(user);
config.setPassword(password);
config.setDriverClassName(driver);
config.setMaximumPoolSize(10);
config.setMinimumIdle(2);
config.setConnectionTimeout(3000); // 3-second timeout
config.setIdleTimeout(600000);
```

### 4.2 Data Access Interface & DAO Implementation
The data layer is abstracted through [`HistoryDAO.java`](file:///d:/MY%20PROJECTS/GestureTalk%20AI/src/main/java/com/gesturetalk/dao/HistoryDAO.java) and implemented by [`GestureHistoryDAO.java`](file:///d:/MY%20PROJECTS/GestureTalk%20AI/src/main/java/com/gesturetalk/dao/GestureHistoryDAO.java):

* **`long insert(GestureHistory history)`**: Parameterized insert retrieving generated primary key.
* **`List<GestureHistory> findRecent(int limit)`**: Retrieves recent logs (`ORDER BY id DESC LIMIT ?`), clamped between 1 and 100.
* **`List<GestureHistory> findByGesture(String gestureCode, int limit)`**: Filters by specific gesture token (`WHERE gesture_code = ? ORDER BY id DESC LIMIT ?`).
* **`Optional<GestureHistory> findById(long id)`**: Single record lookup.
* **`int deleteAll()`**: Purges table records upon user confirmation.
* **`int count()`**: Returns total persisted records count.

---

## 5. Configuration Resolution & Environment Variables

Credentials are read with the following precedence:
1. Injected custom `DataSource` (used in automated tests).
2. System Environment Variables:
   - **`DB_URL`**: JDBC connection string (Default: `jdbc:mysql://localhost:3306/gesturetalk_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC`)
   - **`DB_USER`**: Database username (Default: `root`)
   - **`DB_PASSWORD`**: Database password (Default: `""`)
   - **`DB_DRIVER`**: JDBC Driver class (Default: `com.mysql.cj.jdbc.Driver`)
3. Classpath resource: `src/main/resources/db.properties`.
4. Built-in local fallback defaults.

---

## 6. Testing Strategy: In-Memory H2

Automated tests do **not** require an active MySQL server. Tests in [`GestureHistoryDAOTest.java`](file:///d:/MY%20PROJECTS/GestureTalk%20AI/src/test/java/com/gesturetalk/dao/GestureHistoryDAOTest.java) and [`DBConnectionTest.java`](file:///d:/MY%20PROJECTS/GestureTalk%20AI/src/test/java/com/gesturetalk/config/DBConnectionTest.java) use an embedded **H2 Database** configured in MySQL compatibility mode:
```text
jdbc:h2:mem:gesturetalk_history_test;MODE=MySQL;DB_CLOSE_DELAY=-1
```
This enables full JDBC verification and schema execution inside Maven (`mvn clean test`) with zero external service dependencies.
