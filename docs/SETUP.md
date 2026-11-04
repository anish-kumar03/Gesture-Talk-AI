# GestureTalk AI — Setup, Installation & Deployment Guide

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | SETUP-01 |
| **Project Name** | GestureTalk AI |
| **Version** | 1.0.0 (Production Release) |
| **Classification** | Operations, Deployment & Environment Setup Guide |
| **Target Runtime** | Apache Tomcat 10.1+ (Jakarta EE 10) on Java 21 LTS |
| **Target Database** | MySQL 8.0+ / 8.4 LTS |

---

## 1. System Requirements & Prerequisites

Ensure the target host satisfies the following minimum software requirements:

| Component | Minimum Version | Recommended Version | Verification Command |
| :--- | :--- | :--- | :--- |
| **Java Development Kit (JDK)** | Java 17 | Java 21 LTS (or JDK 25) | `java -version` |
| **Apache Maven** | 3.8.0 | 3.9.6+ | `mvn -v` |
| **Apache Tomcat** | 10.1.0 (Jakarta EE 10) | 10.1.20+ | `catalina version` |
| **MySQL Server** | 8.0.0 | 8.0+ / 8.4 LTS | `mysql --version` |
| **Web Browser** | Chrome 90+, Edge 90+, Firefox 85+, Safari 14.1+ | Latest Chrome or Edge | Supports WebRTC & SpeechSynthesis |

> [!IMPORTANT]
> **Tomcat 10.1+ is mandatory**. Do not use Apache Tomcat 9 or earlier. Tomcat 10.1 provides Jakarta EE 10 servlet specifications (`jakarta.servlet.*`), which are required by the application.

---

## 2. Environment Variables Configuration

GestureTalk AI keeps all secrets, credentials, and tuning parameters outside the compiled codebase. Configure the following environment variables on your deployment server:

### Variable Reference

| Variable Name | Required | Default Value | Description |
| :--- | :---: | :--- | :--- |
| `GEMINI_API_KEY` | **Yes** | *None* | Google Gemini API key obtained from [Google AI Studio](https://aistudio.google.com/). |
| `GEMINI_MODEL` | No | `gemini-2.5-flash` | Multimodal model name (e.g. `gemini-2.5-flash`, `gemini-1.5-flash`). |
| `CONFIDENCE_THRESHOLD` | No | `0.75` | Minimum classification confidence ($0.0$ to $1.0$) required to accept a gesture. |
| `DB_URL` | No | `jdbc:mysql://localhost:3306/gesturetalk_db?...` | JDBC connection URL for MySQL. |
| `DB_USER` | No | `root` | MySQL username. |
| `DB_PASSWORD` | No | `root` | MySQL user password. |
| `DB_POOL_SIZE` | No | `10` | HikariCP maximum connection pool size. |

---

### Setting Variables by Operating System

#### Windows (PowerShell)
```powershell
$env:GEMINI_API_KEY="AIzaSyYourSecretKeyHere"
$env:DB_USER="root"
$env:DB_PASSWORD="your_password"
$env:DB_URL="jdbc:mysql://localhost:3306/gesturetalk_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

#### Windows (Command Prompt - CMD)
```cmd
set GEMINI_API_KEY=AIzaSyYourSecretKeyHere
set DB_USER=root
set DB_PASSWORD=your_password
set DB_URL=jdbc:mysql://localhost:3306/gesturetalk_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC
```

#### Linux / macOS (Bash or Zsh)
```bash
export GEMINI_API_KEY="AIzaSyYourSecretKeyHere"
export DB_USER="root"
export DB_PASSWORD="your_password"
export DB_URL="jdbc:mysql://localhost:3306/gesturetalk_db?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
```

---

## 3. Database Schema Initialization

1. Start your local or remote MySQL service.
2. Execute the provided DDL initialization script located at [`src/main/resources/db/schema.sql`](file:///d:/MY%20PROJECTS/GestureTalk%20AI/src/main/resources/db/schema.sql):

```bash
mysql -u root -p < src/main/resources/db/schema.sql
```

Alternatively, open MySQL Workbench or your preferred SQL editor and execute:
```sql
CREATE DATABASE IF NOT EXISTS `gesturetalk_db`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `gesturetalk_db`;

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
```

---

## 4. Building and Packaging the Application

In the root directory of the project, run:

### Step 1: Run the Automated Verification Suite
```bash
mvn clean test
```
*Confirms all 87 unit, security, DAO, and empirical accuracy benchmark tests pass with 0 errors.*

### Step 2: Package the Deployable Web Application Archive (WAR)
```bash
mvn clean package
```
This generates the deployable artifact:
```
target/gesturetalk-ai.war
```

---

## 5. Apache Tomcat 10.1 Deployment

### Method A: Deploying to Local Standalone Tomcat

1. Locate your Apache Tomcat 10.1 installation directory (referred to as `$CATALINA_HOME`).
2. Configure Tomcat environment variables:
   * **Windows**: Create or edit `$CATALINA_HOME/bin/setenv.bat`:
     ```cmd
     set "GEMINI_API_KEY=AIzaSyYourSecretKeyHere"
     set "DB_USER=root"
     set "DB_PASSWORD=your_password"
     ```
   * **Linux/macOS**: Create or edit `$CATALINA_HOME/bin/setenv.sh`:
     ```bash
     export GEMINI_API_KEY="AIzaSyYourSecretKeyHere"
     export DB_USER="root"
     export DB_PASSWORD="your_password"
     ```
     Ensure execution permissions: `chmod +x $CATALINA_HOME/bin/setenv.sh`
3. Copy the compiled WAR file to Tomcat's `webapps` folder:
   * **Windows**: `copy target\gesturetalk-ai.war %CATALINA_HOME%\webapps\`
   * **Linux/macOS**: `cp target/gesturetalk-ai.war $CATALINA_HOME/webapps/`
4. Start Apache Tomcat:
   * **Windows**: `%CATALINA_HOME%\bin\startup.bat`
   * **Linux/macOS**: `$CATALINA_HOME/bin/startup.sh`
5. Access the application in your browser:
   ```
   http://localhost:8080/gesturetalk-ai/
   ```

---

## 6. End-to-End Operational Verification Checklist

1. **Open Application**: Navigate to `http://localhost:8080/gesturetalk-ai/`.
2. **Camera Initialization**: Click **"Start Camera"** and allow browser permissions when prompted. The video stream will activate.
3. **Capture Gesture**: Present an unambiguous hand posture (e.g. `THUMBS_UP`) and click **"Capture Gesture"**.
4. **Validation & Recognition**:
   * The webcam frame is sent to `/api/recognize`.
   * The server validates binary magic bytes.
   * Google Gemini classifies the posture.
   * Server validates confidence against the $0.75$ threshold.
   * Server deterministically maps the gesture to `"Yes, that's correct."`.
5. **Speech Playback**:
   * The browser synthesizes speech: *"Yes, that's correct."*.
   * Click **Replay** or adjust playback speed as desired.
6. **Persistence Audit**:
   * Scroll to the **Recognition History** table.
   * Confirm the new record appears with timestamp, confidence, and source type (`WEBCAM`).
   * Test gesture category filtering and the "Clear All History" feature.

---

## 7. Troubleshooting & FAQ

### Problem: Browser displays "Camera Access Blocked"
* **Solution**: Click the camera icon in your browser's URL address bar, select **"Always allow"**, and refresh the page. Alternatively, click the **"Camera Guide"** button in the application navbar for step-by-step instructions.

### Problem: HTTP 500 with message `GEMINI_API_KEY is not configured`
* **Solution**: Ensure `GEMINI_API_KEY` is exported in the shell running Tomcat or configured in `$CATALINA_HOME/bin/setenv.bat` (or `setenv.sh`). Restart Tomcat after changing environment variables.

### Problem: HTTP 503 with message `AI_RATE_LIMIT`
* **Solution**: You have exceeded the free tier quota for the Gemini API. Wait 15–30 seconds and retry, or configure a paid quota in Google Cloud Console.

### Problem: Database connection errors (`Communications link failure`)
* **Solution**: Verify that MySQL is running on port 3306 and that the credentials in `DB_USER` and `DB_PASSWORD` have permission to access `gesturetalk_db`.
