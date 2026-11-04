# GestureTalk AI — System Architecture & Design

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-04 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Architecture & Detailed Design |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |
| **Companion Diagram** | [diagrams/system-architecture.md](diagrams/system-architecture.md) |

---

## 1. Architectural Style & Philosophy

GestureTalk AI adopts a **Six-Tier Layered Architecture** implementing the classical **Model-View-Controller (MVC)** architectural pattern within a **Jakarta EE 10** container environment. 

The architecture strictly adheres to the following core software engineering tenets:
* **Separation of Concerns (SoC)**: Controllers handle HTTP routing and validation; services encapsulate pure business and AI workflows; DAOs isolate SQL execution; Models represent pure immutable state; Views handle DOM layout.
* **Loose Coupling & High Cohesion**: Components communicate via well-defined interfaces and typed Data Transfer Objects (DTOs), preventing cross-layer leakages (e.g., DAOs never process HTTP requests; Servlets never execute SQL).
* **Zero-Disk In-Memory Processing**: Media streams and image buffers transit the JVM heap as volatile byte arrays without touching persistent disk storage.
* **Server-Side Secret Encapsulation**: Cloud credentials and sensitive environment variables are completely inaccessible from the Presentation Layer.

---

## 2. Six-Tier Architectural Layering

```
┌─────────────────────────────────────────────────────────────────────────────┐
│ 1. PRESENTATION LAYER (Client Browser)                                      │
│    • HTML5 / JSP (index.jsp, history.jsp)                                   │
│    • Bootstrap 5 Responsive Grid & UI Components                            │
│    • JavaScript Client Modules (webcam.js, upload.js, tts.js, app.js)       │
│    • Native Browser APIs: MediaDevices, Canvas, SpeechSynthesis             │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ HTTP Requests (Multipart / JSON)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 2. CONTROLLER LAYER (Jakarta Servlets)                                      │
│    • GestureRecognitionServlet (/api/recognize)                             │
│    • HistoryServlet (/api/history)                                          │
│    • HealthServlet (/api/health)                                            │
│    • CharacterEncodingFilter & Global Exception Mapping                     │
└──────────────────────────────────────┬──────────────────────────────────────┘
                                       │ Method Calls (DTOs, Records)
                                       ▼
┌─────────────────────────────────────────────────────────────────────────────┐
│ 3. SERVICE LAYER (Java Business Logic)                                      │
│    • RecognitionService (Orchestrator of recognition pipeline)              │
│    • GestureMappingService (Deterministic mapping of gesture → message)     │
│    • ImageValidationService (MIME, Magic Bytes, Dimensions, Size limits)    │
└──────────────────────┬───────────────────────────────────────┬──────────────┘
                       │                                       │
                       ▼                                       ▼
┌─────────────────────────────────────────┐ ┌─────────────────────────────────┐
│ 4. AI INTEGRATION LAYER (Google GenAI)  │ │ 5. DATA ACCESS LAYER (JDBC DAO) │
│    • GeminiVisionService                │ │    • RecognitionLogDAO          │
│    • Google GenAI Client (0.1.1)        │ │    • DatabaseConnectionPool     │
│    • Prompt Templates & JSON Parser     │ │      (HikariCP DataSource)      │
│    • Strict Fallback & Timeout Handler  │ │    • PreparedStatements         │
└──────────────────────┬──────────────────┘ └─────────────────┬───────────────┘
                       │ HTTPS (TLS 1.3)                      │ TCP / JDBC (Port 3306)
                       ▼                                      ▼
┌─────────────────────────────────────────┐ ┌─────────────────────────────────┐
│ EXTERNAL AI PROVIDER                    │ │ 6. DATABASE LAYER (RDBMS)       │
│ • Google Gemini Developer API           │ │ • MySQL 8.0+ Server             │
│ • Model: gemini-2.5-flash               │ │ • Database: gesturetalk_db      │
│ • Multimodal Vision Endpoint            │ │ • Table: gesture_history        │
└─────────────────────────────────────────┘ └─────────────────────────────────┘
```

---

## 3. Layer Responsibilities & Component Directory

### Layer 1: Presentation Layer
* **Role**: Provides the human-facing interface, displays live camera feedback, accepts drag-and-drop file inputs, presents recognition cards, and executes local speech synthesis.
* **Components**:
  * `index.jsp`: Single-page layout housing the webcam feed, canvas capture trigger, upload drop zone, result display card, and gesture cheat sheet.
  * `webcam.js`: Manages `navigator.mediaDevices.getUserMedia()`, video stream binding, and canvas image extraction into Blob format.
  * `upload.js`: Handles file drag-and-drop, client-side preview rendering, and file size pre-checks.
  * `tts.js`: Interfaces with `window.speechSynthesis` to trigger audible voice articulation, managing pitch, speech rate, and language voice selection.
  * `app.js`: Coordinates UI states (loading spinners, error badges, result cards).

### Layer 2: Controller Layer (Jakarta Servlets)
* **Role**: Acts as the HTTP entry point. Parses incoming multipart requests, handles CORS and character encoding, coordinates service execution, and transforms service outcomes into HTTP status codes and JSON payloads.
* **Components**:
  * `GestureRecognitionServlet`: Mapped to `POST /api/recognize`. Accepts `multipart/form-data` containing image bytes and source mode (`WEBCAM` or `UPLOAD`). Returns structured recognition JSON.
  * `HistoryServlet`: Mapped to `GET /api/history` and `DELETE /api/history`. Retrieves audit logs with optional filtering or purges audit history.
  * `HealthServlet`: Mapped to `GET /api/health`. Emits server status, database connection availability, and API key presence.

### Layer 3: Service Layer (Java Domain Logic)
* **Role**: Implements the pure, deterministic business logic of the system. Completely decoupled from HTTP request/response objects.
* **Components**:
  * `RecognitionService`: High-level workflow orchestrator. Accepts image bytes, calls `ImageValidationService`, invokes `GeminiVisionService`, passes raw token to `GestureMappingService`, and triggers async logging via `RecognitionLogDAO`.
  * `GestureMappingService`: Provides deterministic mapping between recognized `GestureType` enum values and their natural language sentences. Enforces that the spoken text is hardcoded in Java logic, preventing generative AI hallucinations.
  * `ImageValidationService`: Validates image binary integrity, rejects corrupted buffers, enforces the 5 MB ceiling, and verifies image magic numbers (JPEG `FF D8 FF`, PNG `89 50 4E 47`, WebP `52 49 46 46`).

### Layer 4: AI Integration Layer (GenAI SDK)
* **Role**: Encapsulates all communication with the Google Gemini Multimodal API.
* **Components**:
  * `GeminiVisionService`: Configures the `Client` using `System.getenv("GOOGLE_API_KEY")`. Formulates the multimodal prompt using `Content.fromParts(Part.fromText(...), Part.fromBytes(...))`.
  * `GeminiResponseParser`: Parses the raw JSON response emitted by Gemini, extracts the gesture string, normalizes it, and maps it to a valid `GestureType` or defaults to `UNKNOWN`.

### Layer 5: Data Access Layer (DAO & Pool)
* **Role**: Manages database lifecycle, connection pooling, and parameterized SQL transactions.
* **Components**:
  * `DatabaseConnection`: Singleton managing the HikariCP `HikariDataSource`. Configures pool size, connection timeouts, and leak detection thresholds.
  * `RecognitionLogDAO`: Encapsulates SQL queries (`INSERT`, `SELECT`, `DELETE`) on the `gesture_history` table using strictly parameterized `PreparedStatement` instances.

### Layer 6: Database Layer
* **Role**: Durable relational store ensuring ACID compliance for recognition audit logging.
* **Components**:
  * MySQL 8.0+ running `gesturetalk_db` with `gesture_history` table indexed on `created_at` and `gesture_code`.

---

## 4. End-to-End Request-Response Lifecycle

```
[User Action: Capture Frame / Upload File]
              │
              ▼
1. Browser JS encapsulates image as JPEG/PNG Blob in FormData.
              │
              ▼
2. Fetch API sends POST /api/recognize (multipart/form-data) to Tomcat.
              │
              ▼
3. GestureRecognitionServlet receives HttpServletRequest.
              │
              ▼
4. Servlet extracts byte[] from Part "image" (never saved to disk).
              │
              ▼
5. ImageValidationService checks magic bytes and size <= 5MB.
              │
              ▼
6. GeminiVisionService sends Part.fromBytes() + prompt to Google Gemini.
              │
              ▼
7. Gemini analyzes image and returns structured JSON { gesture, confidence, explanation }.
              │
              ▼
8. GeminiResponseParser normalizes result to GestureType enum (or UNKNOWN).
              │
              ▼
9. GestureMappingService maps GestureType to predefined spoken sentence.
              │
              ▼
10. RecognitionLogDAO inserts audit record into MySQL (gesture_history).
              │
              ▼
11. Servlet writes JSON response to HttpServletResponse (HTTP 200).
              │
              ▼
12. Browser JS parses JSON, updates UI result card, and calls SpeechSynthesis.speak().
```

---

## 5. Architectural Quality Attributes

```
┌─────────────────────────┬──────────────────────────────────────────────────────────────────┐
│ Architectural Attribute │ Implementation Technique                                         │
├─────────────────────────┼──────────────────────────────────────────────────────────────────┤
│ Zero Secret Leakage     │ GOOGLE_API_KEY read only in GeminiVisionService via env variable.│
│ Privacy by Design       │ Images processed in volatile RAM; never written to filesystem.   │
│ Deterministic Safety    │ Spoken messages defined in Java code, eliminating AI drift.      │
│ Low Memory Overhead     │ Absence of Spring/Hibernate limits base heap usage to < 64MB.    │
│ SQL Injection Immunity  │ 100% of SQL statements executed via JDBC PreparedStatement.      │
│ High Connection Reuse   │ HikariCP pool maintains warm MySQL connections, avoiding latency.│
└─────────────────────────┴──────────────────────────────────────────────────────────────────┘
```

---

## 6. Document Cross-References

* Visual architecture diagrams: [diagrams/system-architecture.md](diagrams/system-architecture.md)
* Detailed step-by-step workflow: [05-APPLICATION-WORKFLOW.md](05-APPLICATION-WORKFLOW.md)
* API endpoint specifications: [09-API-ENDPOINT-DESIGN.md](09-API-ENDPOINT-DESIGN.md)
* Database schema and DAO details: [10-DATABASE-DESIGN.md](10-DATABASE-DESIGN.md)

