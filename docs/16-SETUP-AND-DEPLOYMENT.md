# GestureTalk AI — Setup, Configuration & Deployment Guide

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-16 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | DevOps, System Administration & Deployment Runbook |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. System Prerequisites & Environment Matrix

Ensure the host workstation or deployment server meets the following minimum prerequisites:

```
┌──────────────────────────┬─────────────────────────────┬────────────────────────────────────────────────────────┐
│ Component                │ Minimum Required Version    │ Verification Command (PowerShell / Bash)               │
├──────────────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┤
│ Java Development Kit     │ Java 21 LTS (or 17 LTS)     │ `java -version`                                        │
├──────────────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┤
│ Apache Maven             │ 3.9.0 or higher             │ `mvn -version`                                         │
├──────────────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┤
│ MySQL Community Server   │ 8.0 or 8.4 LTS              │ `mysql --version`                                      │
├──────────────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┤
│ Apache Tomcat Container  │ 10.1.x or 11.0.x (Jakarta)  │ Check Tomcat `RELEASE-NOTES` in root folder            │
├──────────────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┤
│ Modern Web Browser       │ Chrome 90+, Edge 90+, FF 88+│ Verify browser supports WebRTC & SpeechSynthesis       │
├──────────────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┤
│ Google AI Studio Account │ Active Gemini API Key       │ Obtain key from https://aistudio.google.com/           │
└──────────────────────────┴─────────────────────────────┴────────────────────────────────────────────────────────┘
```

---

## 2. Environment Variables Configuration

The application requires specific environment variables configured on the host machine. These variables configure database credentials and the Gemini API key without hardcoding them into source files:

### Windows PowerShell Configuration:
```powershell
# Set Gemini API Key (Required for AI Vision)
[System.Environment]::SetEnvironmentVariable('GEMINI_API_KEY', 'your-actual-gemini-api-key-here', 'User')

# Optional Database Overrides (Defaults apply if not provided)
[System.Environment]::SetEnvironmentVariable('DB_URL', 'jdbc:mysql://localhost:3306/gesturetalk_db?useSSL=false&serverTimezone=UTC', 'User')
[System.Environment]::SetEnvironmentVariable('DB_USER', 'root', 'User')
[System.Environment]::SetEnvironmentVariable('DB_PASSWORD', 'YourPasswordHere', 'User')
```

> [!IMPORTANT]
> After setting user environment variables in Windows, restart your terminal or IDE so that the child processes inherit the updated environment variables.

---

## 3. Database Initialization (MySQL)

1. Verify that the MySQL service is running:
```powershell
Get-Service -Name *mysql*
```

2. Open the MySQL interactive shell as administrator:
```powershell
mysql -u root -p
```

3. Execute the schema script located at `src/main/resources/db/schema.sql` (or paste the following commands):
```sql
CREATE DATABASE IF NOT EXISTS `gesturetalk_db`
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE `gesturetalk_db`;

CREATE TABLE IF NOT EXISTS `gesture_history` (
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

## 4. Building the Project with Apache Maven

Navigate to the project root directory (containing `pom.xml`):

```powershell
cd "d:\MY PROJECTS\GestureTalk AI"
```

### 4.1 Running Automated Unit Tests
```powershell
mvn clean test
```
*Executes all JUnit 5 unit tests and H2 in-memory DAO tests.*

### 4.2 Packaging the Web Archive (WAR)
```powershell
mvn clean package
```
Upon a successful build, the deployable WAR artifact will be generated at:
```
target/gesturetalk-ai.war
```

---

## 5. Deployment to Apache Tomcat 10.1+

GestureTalk AI runs natively on any standard Jakarta EE 10 servlet container, such as **Apache Tomcat 10.1.x** (or Tomcat 11.0.x).

### Method A: Automated Deployment via Tomcat `webapps`
1. Locate your Apache Tomcat installation root directory (e.g., `C:\apache-tomcat-10.1.x` or `C:\apache-tomcat-11.0.24`).
2. Copy the generated WAR package into Tomcat's `webapps` folder:
```powershell
Copy-Item "target\gesturetalk-ai.war" "C:\apache-tomcat-11.0.24\webapps\" -Force
```
3. Start the Apache Tomcat server:
```powershell
& "C:\apache-tomcat-11.0.24\bin\startup.bat"
```
Tomcat will automatically extract `gesturetalk-ai.war` into a directory named `gesturetalk-ai/`.

4. Open your web browser and navigate to the application endpoint:
```
http://localhost:8080/gesturetalk-ai/
```

### Method B: Root Context Deployment
To run the application directly from the server root URL (`http://localhost:8080/`):
1. Rename `target\gesturetalk-ai.war` to `ROOT.war`.
2. Delete the existing `ROOT` folder in Tomcat's `webapps/` directory.
3. Place `ROOT.war` inside `webapps/` and restart Tomcat.

---

## 6. Verification & Post-Deployment Health Check

Once Tomcat starts up:
1. Verify the health status endpoint:
```powershell
Invoke-RestMethod -Uri "http://localhost:8080/gesturetalk-ai/api/health" -Method Get
```
Expected output:
```json
{
  "status": "UP",
  "environment": {
    "javaVersion": "21",
    "geminiConfigured": true,
    "databaseConnected": true
  }
}
```

2. Open the main web page in Google Chrome or Microsoft Edge.
3. Click **"Start Camera"** and grant camera permissions.
4. Position your hand in a `THUMBS_UP` posture and click **"Capture Gesture"**.
5. Observe the result card and listen for browser vocalization: *"Yes, that's correct."*

---

## 7. Troubleshooting & Diagnostic Runbook

```
┌─────────────────────────────────┬───────────────────────────────────┬──────────────────────────────────────────────┐
│ Observed Symptom                │ Root Cause Diagnosis              │ Corrective Remediation                       │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ "Cannot find symbol: jakarta.*" │ Incompatible servlet API version  │ Ensure Tomcat 10.1+ is used. Tomcat 9 uses   │
│ during build or deployment      │ or legacy javax.* imports.        │ javax.servlet, which is incompatible.        │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ HTTP 500: GEMINI_API_KEY is not │ Missing environment variable in   │ Set GEMINI_API_KEY in Windows environment and│
│ set in the environment          │ Tomcat's execution environment.   │ restart Tomcat from a fresh PowerShell shell.│
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ Communications link failure     │ MySQL service is stopped or port  │ Start MySQL service (`Start-Service MySQL80`)│
│ (SQLState: 08S01)               │ 3306 is blocked.                  │ and verify credentials in DB_PASSWORD.       │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ Browser says: "Camera blocked"  │ Browser security blocking camera  │ Camera access requires HTTPS or localhost.   │
│ or permission denied            │ on insecure origin.               │ Access via `http://localhost:8080/`.         │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ Speech is silent in browser     │ System volume muted or voice list │ Open speech controls in UI; click "Test Voice│
│                                 │ not yet loaded by browser.        │ " to force voice synthesis initialization.   │
└─────────────────────────────────┴───────────────────────────────────┴──────────────────────────────────────────────┘
```

---

## 8. Document Cross-References

* Technical architecture: [04-SYSTEM-ARCHITECTURE.md](04-SYSTEM-ARCHITECTURE.md)
* API specifications: [09-API-ENDPOINT-DESIGN.md](09-API-ENDPOINT-DESIGN.md)
* Error codes and recovery: [13-ERROR-HANDLING-AND-VALIDATION.md](13-ERROR-HANDLING-AND-VALIDATION.md)

