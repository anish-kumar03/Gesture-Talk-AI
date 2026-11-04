# GestureTalk AI — Developer & AI Agent Guidelines (AGENTS.md)

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | AGENT-CONFIG-01 |
| **Project Name** | GestureTalk AI |
| **Version** | 1.1.0 (MediaPipe Real-Time Architecture Release) |
| **Target Runtime** | Java 21 LTS / Jakarta EE 10 / Apache Tomcat 10.1+ / MySQL 8.0+ |
| **Primary Engine** | Google MediaPipe Gesture Recognizer for Web (`@mediapipe/tasks-vision:0.10.14`) |
| **Secondary Service**| Optional Google GenAI Java SDK (`com.google.genai:google-genai:1.80.0`) |
| **Preferred Model** | `gemini-3.8-flash` (configurable via `GEMINI_MODEL`, optional fallback) |

---

## 1. Project Overview & Architectural Principles

**GestureTalk AI** is an AI-powered assistive communication web application that translates static hand gestures into natural speech using Java 21 LTS, Jakarta EE 10 Servlets on Apache Tomcat 10.1, **Google MediaPipe Gesture Recognizer WebAssembly (Primary Real-Time Engine)**, the browser SpeechSynthesis API, and optional **Google Gemini Vision** for secondary multimodal diagnostic analysis.

### Core Architectural Rules for All Agents:
1. **Hybrid Architecture (Client-Side Real-Time + Java Backend)**:
   - **Primary Engine**: Real-time webcam tracking runs in the client browser using Google MediaPipe Gesture Recognizer (`@mediapipe/tasks-vision`) compiled to WebAssembly. Zero video frames are transmitted over external networks for webcam recognition, eliminating latency and cloud API quota bottlenecks.
   - **Java Backend Authority**: All gesture validation, deterministic natural language mapping, and database persistence are strictly enforced by Java Servlets (`GestureResultServlet`, `GestureRecognitionServlet`).
2. **Deterministic Natural Language Mapping**: Classifiers (MediaPipe or Gemini) act solely as visual classifiers returning structured gesture tokens. User-facing spoken sentences are resolved deterministically by [`GestureMessageMapper.java`](file:///d:/MY%20PROJECTS/GestureTalk%20AI/src/main/java/com/gesturetalk/service/GestureMessageMapper.java). The AI is never permitted to invent user-facing phrases.
3. **Resilience & Gemini Optionality**: The application operates fully even if Gemini Vision returns HTTP 503, has rate limits, or is unconfigured. MediaPipe runs locally without external API dependencies.
4. **In-Memory Ephemeral Data Flow**: Uploaded image bytes and webcam frames exist strictly in volatile memory (`byte[]` buffers). Zero image bytes are ever written to disk or stored in the database.
5. **Secret Isolation**: Optional cloud API keys (`GOOGLE_API_KEY`) are accessed strictly on the server side from environment variables. No secrets are ever exposed in client-side code (JSP, HTML, JavaScript) or committed to version control.
6. **No Code Modification Without Authorization**: Respect existing working functionality and do not modify application classes unless an actual defect correction is requested.

---

## 2. Standard Environment Configuration (`GOOGLE_API_KEY`)

### 2.1 The Single Standard Authentication Variable
* **`GOOGLE_API_KEY`** is the **single primary and authoritative environment variable** for authenticating with the official Google GenAI Java SDK (`com.google.genai:google-genai`).
* The SDK's underlying client automatically inspects and resolves `GOOGLE_API_KEY` from the host environment during client initialization (`Client.builder().build()`).
* Legacy references to `GEMINI_API_KEY` have been removed to eliminate ambiguity and adhere to official Google GenAI SDK standards.
* **Never hardcode the API key** in any Java class, test file, JSP, or documentation example.

### 2.2 Configurable Vision Model (`GEMINI_MODEL`)
* **`GEMINI_MODEL`** is a configurable environment variable allowing operations to specify the model ID.
* **Preferred Default**: `gemini-3.8-flash` (if available to the configured API key).
* **Model Availability Verification**:
  Do not silently assume a specific model tier is accessible to an API key. Developers can verify model accessibility using:
  ```powershell
  # PowerShell Model Availability Inspection
  Invoke-RestMethod -Uri "https://generativelanguage.googleapis.com/v1beta/models?key=$env:GOOGLE_API_KEY" | Select-Object -ExpandProperty models | Select-Object name, displayName, supportedGenerationMethods
  ```
  Or via `curl`:
  ```bash
  curl -s "https://generativelanguage.googleapis.com/v1beta/models?key=${GOOGLE_API_KEY}" | grep -E '"name":|"displayName":'
  ```
  If `gemini-3.8-flash` is not yet provisioned for a specific tier/key, operations can override the variable:
  ```powershell
  $env:GEMINI_MODEL="gemini-2.5-flash"
  ```

### 2.3 Complete Environment Variable Reference

| Variable Name | Status | Default | Description |
| :--- | :---: | :--- | :--- |
| **`GOOGLE_API_KEY`** | **Optional** (Primary MediaPipe works offline) | *None* | Google GenAI API key for secondary Gemini deep analysis. Real-time MediaPipe webcam recognition works 100% without it. |
| **`GEMINI_MODEL`** | Configurable | `gemini-3.8-flash` | Multimodal model ID for optional secondary deep analysis. |
| **`CONFIDENCE_THRESHOLD`**| Configurable | `0.70` | Minimum classification confidence ($0.0$ to $1.0$) required to accept a gesture (MediaPipe default 0.70, Gemini default 0.75). |
| **`DB_URL`** | Optional | `jdbc:mysql://localhost:3306/gesturetalk_db?...` | JDBC connection URL for MySQL. |
| **`DB_USER`** | Optional | `root` | MySQL username. |
| **`DB_PASSWORD`** | Optional | `root` | MySQL user password. |
| **`DB_POOL_SIZE`** | Optional | `10` | Maximum HikariCP pool connection count. |

---

## 3. Secret Security & Windows Configuration Guide

### 3.1 Recommended Method: Windows User Environment Variables
Storing secrets directly in the Windows User Profile Registry is the recommended development pattern. This ensures credentials are never stored in repository files and cannot be accidentally checked into version control:

```powershell
# Set permanent user environment variables across all terminal sessions and reboots
[System.Environment]::SetEnvironmentVariable('GOOGLE_API_KEY', 'YOUR_ACTUAL_GOOGLE_API_KEY', 'User')
[System.Environment]::SetEnvironmentVariable('GEMINI_MODEL', 'gemini-3.8-flash', 'User')
```
> [!IMPORTANT]
> Restart your PowerShell terminal, VS Code / IDE, or Apache Tomcat instance after updating User environment variables so the child processes inherit the updated values.

### 3.2 Temporary Session Setup (Testing Only)
```powershell
$env:GOOGLE_API_KEY="YOUR_ACTUAL_GOOGLE_API_KEY"
$env:GEMINI_MODEL="gemini-3.8-flash"
```

### 3.3 Verifying the Variable in PowerShell
```powershell
# Check current session value
$env:GOOGLE_API_KEY

# Check user registry persistent value
[System.Environment]::GetEnvironmentVariable('GOOGLE_API_KEY', 'User')

# Verify presence in PowerShell environment drive
Get-ChildItem Env:GOOGLE_API_KEY
```

### 3.4 Apache Tomcat 10.1 Windows Service / Script Setup
If deploying to a standalone Apache Tomcat 10.1 instance on Windows, configure environment variables in `%CATALINA_HOME%\bin\setenv.bat`:

```cmd
@echo off
rem ===========================================================================
rem Apache Tomcat External Environment Configuration
rem DO NOT commit this file to the Git repository.
rem setenv.bat must exist ONLY in the external %CATALINA_HOME%\bin directory.
rem ===========================================================================
set "GOOGLE_API_KEY=YOUR_ACTUAL_API_KEY_HERE"
set "GEMINI_MODEL=gemini-3.8-flash"
set "DB_USER=root"
set "DB_PASSWORD=YOUR_DB_PASSWORD_HERE"
```

> [!CAUTION]
> Never create `setenv.bat` inside the GestureTalk AI project folder. `%CATALINA_HOME%` is located outside the workspace. Project `.gitignore` rules explicitly enforce that `.env`, `setenv.bat`, `*.key`, and `credentials.json` are excluded from Git.

---

## 4. Java Implementation Pattern

Java components resolve credentials strictly through `System.getenv("GOOGLE_API_KEY")` or the SDK's built-in discovery:

```java
// Standard API key resolution pattern in GestureTalk AI
private static String resolveApiKey() {
    String key = System.getenv("GOOGLE_API_KEY");
    if (key != null && !key.trim().isEmpty()) {
        return key.trim();
    }
    return null;
}

// Configurable model resolution pattern
private static String resolveModel() {
    String model = System.getenv("GEMINI_MODEL");
    return (model != null && !model.isBlank()) ? model.trim() : "gemini-3.8-flash";
}
```

---

## 5. Official SDK Dependency Verification

The official Google Gen AI Java SDK dependency in `pom.xml` is configured to the latest stable release:

```xml
<dependency>
    <groupId>com.google.genai</groupId>
    <artifactId>google-genai</artifactId>
    <version>1.80.0</version>
</dependency>
```

* **Group ID**: `com.google.genai`
* **Artifact ID**: `google-genai`
* **Version**: `1.80.0`
* **Official Source**: [https://github.com/googleapis/java-genai](https://github.com/googleapis/java-genai)
* **Status**: Fully resolved and compatible with Java 21 LTS and all automated tests.

---

## 6. Security & Log Scrubbing Directives

1. **No Hardcoded Secrets**: Under no circumstance should a real API key or placeholder resembling an actual key be checked into source control or documentation.
2. **Log Redaction**: Error handlers and logging interceptors must scrub any string matching `(key|api[_-]?key)=([a-zA-Z0-9_\-]+)` before writing to logs.
3. **No Frontend Exposure**: The API key is strictly server-side. No API keys are passed in REST responses, HTTP headers to the browser, or cookies.
