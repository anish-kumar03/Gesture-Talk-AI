# GestureTalk AI — Project Directory Structure & Package Architecture

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-11 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Codebase Layout & Structural Specification |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Project Directory Tree Layout

The directory layout adheres strictly to standard Maven WAR packaging rules.

> [!NOTE]
> **Implementation Status Indicator**:
> * `[CONFIGURED]` : File exists in current project baseline (`pom.xml`, documentation).
> * `[PLANNED]` : Target Java class, resource, or view planned for implementation in subsequent phases.

```
GestureTalk AI/
│
├── pom.xml                                      [CONFIGURED] Maven build configuration
├── docs/                                        [CONFIGURED] Complete architecture documentation
│   ├── 00-PROJECT-OVERVIEW.md                   [CONFIGURED]
│   ├── ...                                      [CONFIGURED]
│   └── diagrams/                                [CONFIGURED]
│
├── src/
│   ├── main/
│   │   ├── java/
│   │   │   └── com/
│   │   │       └── gesturetalk/
│   │   │           ├── config/                  [PLANNED] Runtime configuration & connection pooling
│   │   │           │   ├── AppConfig.java       [PLANNED] Environment & property loader
│   │   │           │   └── DatabaseConnection.java [PLANNED] HikariCP JDBC DataSource manager
│   │   │           │
│   │   │           ├── controller/              [PLANNED] Jakarta Servlet HTTP endpoints
│   │   │           │   ├── HomeServlet.java     [PLANNED] Routes /home and root URL to index.jsp
│   │   │           │   ├── GestureRecognitionServlet.java [PLANNED] POST /api/recognize multipart controller
│   │   │           │   ├── HistoryServlet.java  [PLANNED] GET/DELETE /api/history audit controller
│   │   │           │   └── HealthServlet.java   [PLANNED] GET /api/health monitoring controller
│   │   │           │
│   │   │           ├── service/                 [PLANNED] Business logic & AI orchestration
│   │   │           │   ├── RecognitionService.java [PLANNED] High-level recognition orchestrator
│   │   │           │   ├── GeminiVisionService.java [PLANNED] Google GenAI SDK multimodal wrapper
│   │   │           │   ├── GestureMappingService.java [PLANNED] Deterministic gesture-to-speech mapping
│   │   │           │   └── ImageValidationService.java [PLANNED] File size, format & magic bytes validator
│   │   │           │
│   │   │           ├── model/                   [PLANNED] Domain entities & DTO records
│   │   │           │   ├── GestureType.java     [PLANNED] Enum of 5 gestures + UNKNOWN with speech text
│   │   │           │   ├── GestureResult.java   [PLANNED] DTO holding classification outcome
│   │   │           │   ├── RecognitionLog.java  [PLANNED] Entity representing database audit record
│   │   │           │   └── HealthStatus.java    [PLANNED] DTO representing system readiness
│   │   │           │
│   │   │           ├── dao/                     [PLANNED] Data access layer (JDBC)
│   │   │           │   └── RecognitionLogDAO.java [PLANNED] Parameterized SQL operations on gesture_history
│   │   │           │
│   │   │           ├── util/                    [PLANNED] Cross-cutting utilities
│   │   │           │   ├── JsonUtils.java       [PLANNED] Google Gson serialization helpers
│   │   │           │   └── MagicNumberValidator.java [PLANNED] Binary header inspection utility
│   │   │           │
│   │   │           └── exception/               [PLANNED] Domain-specific exceptions
│   │   │               ├── GestureRecognitionException.java [PLANNED] Root application exception
│   │   │               ├── InvalidImageException.java [PLANNED] Bad payload / corrupted byte stream
│   │   │               └── AIServiceException.java [PLANNED] Gemini quota, timeout or network fault
│   │   │
│   │   ├── resources/
│   │   │   ├── logback.xml                      [PLANNED] Logback logging configuration
│   │   │   └── db/
│   │   │       └── schema.sql                   [PLANNED] MySQL DDL schema and initial seed data
│   │   │
│   │   └── webapp/                              [PLANNED] Web application resources & presentation
│   │       ├── WEB-INF/
│   │       │   └── web.xml                      [PLANNED] Jakarta Servlet 6.0 deployment descriptor
│   │       │
│   │       ├── index.jsp                        [PLANNED] Main single-page application view
│   │       │
│   │       └── static/
│   │           ├── css/
│   │           │   └── style.css                [PLANNED] Custom UI stylesheet, dark accents & animations
│   │           ├── js/
│   │           │   ├── webcam.js                [PLANNED] MediaDevices video streaming & canvas snapshot
│   │           │   ├── upload.js                [PLANNED] Drag-and-drop file preview & validation
│   │           │   ├── tts.js                   [PLANNED] Browser SpeechSynthesis vocalization wrapper
│   │           │   ├── history.js               [PLANNED] Audit log table renderer & filter logic
│   │           │   └── app.js                   [PLANNED] Central client controller & event binder
│   │           └── img/
│   │               └── gestures/                [PLANNED] Visual reference icons for supported gestures
│   │
│   └── test/
│       ├── java/
│       │   └── com/
│       │       └── gesturetalk/
│       │           ├── config/
│       │           │   └── AppConfigTest.java   [PLANNED] Environment loading & fallback tests
│       │           ├── model/
│       │           │   └── GestureTypeTest.java [PLANNED] Enum parsing & speech text verification
│       │           ├── service/
│       │           │   ├── GestureMappingServiceTest.java [PLANNED] Deterministic mapping verification
│       │           │   └── ImageValidationServiceTest.java [PLANNED] File size & magic bytes unit tests
│       │           └── dao/
│       │               └── RecognitionLogDAOTest.java [PLANNED] In-memory H2 database integration tests
│       └── resources/
│           └── test-schema.sql                  [PLANNED] H2 database test schema
```

---

## 2. Package Responsibilities & Architectural Constraints

```
┌──────────────────────────┬───────────────────────────────────────────────────────────────────┐
│ Package Name             │ Package Responsibilities & Enforcement Rules                      │
├──────────────────────────┼───────────────────────────────────────────────────────────────────┤
│ `com.gesturetalk.config`  │ Manages application configuration (`AppConfig`), reads environment│
│                          │ variables, and maintains the singleton HikariCP DataSource.       │
│                          │ Rules: Never import presentation or servlet packages.             │
├──────────────────────────┼───────────────────────────────────────────────────────────────────┤
│ `com.gesturetalk.model`   │ Pure domain records, enums, and data transfer objects (DTOs).     │
│                          │ Rules: Must remain immutable; no database or network dependencies.│
├──────────────────────────┼───────────────────────────────────────────────────────────────────┤
│ `com.gesturetalk.dao`     │ Database access objects utilizing JDBC `PreparedStatement`.       │
│                          │ Rules: Strictly parameterized SQL; no HTTP or Servlet awareness.  │
├──────────────────────────┼───────────────────────────────────────────────────────────────────┤
│ `com.gesturetalk.service`│ Encapsulates all business logic, image validation, Gemini SDK     │
│                          │ interaction, and deterministic text mapping.                      │
│                          │ Rules: Agnostic to HTTP; testable via standard JUnit 5.          │
├──────────────────────────┼───────────────────────────────────────────────────────────────────┤
│ `com.gesturetalk.control`│ Jakarta Servlets handling HTTP routing, multipart extraction,     │
│                          │ header validation, and JSON serialization.                        │
│                          │ Rules: Thin controllers; delegates immediately to service layer.  │
├──────────────────────────┼───────────────────────────────────────────────────────────────────┤
│ `com.gesturetalk.util`   │ Pure static utility functions for JSON formatting and byte checks.│
├──────────────────────────┼───────────────────────────────────────────────────────────────────┤
│ `com.gesturetalk.except` │ Custom unchecked and checked domain exceptions for predictable    │
│                          │ error mapping.                                                    │
└──────────────────────────┴───────────────────────────────────────────────────────────────────┘
```

---

## 3. Webapp Layout & Static Asset Management

* **`src/main/webapp/WEB-INF/web.xml`**: Defines servlet mappings, session timeout thresholds (30 minutes), and UTF-8 `CharacterEncodingFilter`.
* **`src/main/webapp/index.jsp`**: Primary user-facing view, compiling Bootstrap 5, custom styles, video canvases, and UI panels.
* **`src/main/webapp/static/`**: Cleanly organized into `css/`, `js/`, and `img/`.

---

## 4. Document Cross-References

* System architecture layers: [04-SYSTEM-ARCHITECTURE.md](04-SYSTEM-ARCHITECTURE.md)
* Implementation phases: [15-IMPLEMENTATION-ROADMAP.md](15-IMPLEMENTATION-ROADMAP.md)
* Setup and compilation: [16-SETUP-AND-DEPLOYMENT.md](16-SETUP-AND-DEPLOYMENT.md)

