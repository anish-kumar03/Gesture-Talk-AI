# GestureTalk AI — System Architecture Diagrams

| Diagram Document | Value |
| :--- | :--- |
| **Document ID** | DIAG-01 |
| **Project Name** | GestureTalk AI |
| **Classification** | Architectural Diagrams (Mermaid) |
| **Parent Document** | [../04-SYSTEM-ARCHITECTURE.md](../04-SYSTEM-ARCHITECTURE.md) |

---

## 1. Layered System Architecture Diagram

```mermaid
flowchart TD
    subgraph Client["1. Presentation Layer (Browser Client)"]
        UI["Web View (index.jsp / Bootstrap 5)"]
        Cam["Webcam Module (webcam.js / MediaDevices)"]
        Canvas["Snapshot Rasterizer (HTML5 Canvas)"]
        Upload["File Drag & Drop (upload.js)"]
        TTS["Speech Engine (tts.js / SpeechSynthesis)"]
    end

    subgraph Controller["2. Controller Layer (Jakarta Servlets)"]
        RecogServlet["GestureRecognitionServlet (/api/recognize)"]
        HistServlet["HistoryServlet (/api/history)"]
        HealthServlet["HealthServlet (/api/health)"]
        Filter["CharacterEncodingFilter (UTF-8)"]
    end

    subgraph Service["3. Service Layer (Java Business Logic)"]
        RecogSvc["RecognitionService (Pipeline Orchestrator)"]
        ValidSvc["ImageValidationService (Magic Bytes & Size)"]
        MapSvc["GestureMappingService (Deterministic Speech Rules)"]
    end

    subgraph AI["4. AI Integration Layer (Google GenAI)"]
        GeminiSvc["GeminiVisionService"]
        GenAIClient["Google GenAI SDK Client (v0.1.1)"]
        Parser["GeminiResponseParser (Gson Parser)"]
    end

    subgraph Data["5. Data Access Layer (JDBC & Pool)"]
        Pool["HikariCP DataSource Pool"]
        DAO["RecognitionLogDAO (PreparedStatements)"]
    end

    subgraph External["External Services & Database"]
        CloudAI["Google Gemini Developer API (gemini-2.5-flash)"]
        MySQL[("MySQL 8.0 Server (gesturetalk_db)")]
    end

    %% Interactions
    Cam -->|"Render stream"| UI
    UI -->|"Trigger capture"| Canvas
    Canvas -->|"Multipart POST Blob"| RecogServlet
    Upload -->|"Multipart POST File"| RecogServlet
    UI -->|"GET / DELETE"| HistServlet
    UI -->|"GET Health"| HealthServlet

    RecogServlet --> RecogSvc
    HistServlet --> DAO
    HealthServlet --> Pool

    RecogSvc --> ValidSvc
    RecogSvc --> GeminiSvc
    RecogSvc --> MapSvc
    RecogSvc --> DAO

    GeminiSvc --> GenAIClient
    GenAIClient -->|"HTTPS TLS 1.3 (Part.fromBytes)"| CloudAI
    CloudAI -->|"JSON Classification"| GenAIClient
    GenAIClient --> Parser
    Parser --> GeminiSvc

    DAO --> Pool
    Pool -->|"JDBC TCP Port 3306"| MySQL

    MapSvc -->|"Spoken sentence"| RecogSvc
    RecogSvc -->|"JSON Response"| RecogServlet
    RecogServlet -->|"JSON Payload"| UI
    UI -->|"Speak phrase"| TTS
```

---

## 2. MVC Component Relationship Diagram

```mermaid
classDiagram
    class GestureRecognitionServlet {
        +doPost(request, response)
        -extractImageBytes(part) byte[]
    }

    class RecognitionService {
        +processRecognition(imageBytes, sourceType) RecognitionResponse
    }

    class ImageValidationService {
        +validateImage(imageBytes) void
        -checkMagicBytes(imageBytes) boolean
    }

    class GeminiVisionService {
        +classifyGesture(imageBytes, mimeType) GestureResult
        -buildPrompt() String
    }

    class GestureMappingService {
        +getSpokenSentence(gestureType) String
    }

    class RecognitionLogDAO {
        +insert(log) long
        +findRecent(limit) List~RecognitionLog~
        +findByGesture(code, limit) List~RecognitionLog~
        +deleteAll() int
    }

    class GestureType {
        <<enumeration>>
        THUMBS_UP
        THUMBS_DOWN
        OPEN_PALM
        V_SIGN
        INDEX_POINTING
        UNKNOWN
        +getDefaultMessage() String
        +getDisplayName() String
        +fromString(code) GestureType
    }

    class GestureResult {
        -GestureType gestureType
        -double confidence
        -String explanation
        +getGestureType() GestureType
        +getConfidence() double
        +getExplanation() String
    }

    class RecognitionLog {
        -long id
        -String gestureCode
        -String mappedMessage
        -String explanation
        -double confidence
        -String sourceType
        -Timestamp createdAt
    }

    %% Relationships
    GestureRecognitionServlet --> RecognitionService : delegates
    RecognitionService --> ImageValidationService : validates
    RecognitionService --> GeminiVisionService : classifies
    RecognitionService --> GestureMappingService : maps
    RecognitionService --> RecognitionLogDAO : records audit
    GeminiVisionService ..> GestureResult : produces
    GestureResult --> GestureType : references
    GestureMappingService ..> GestureType : evaluates
    RecognitionLogDAO ..> RecognitionLog : persists
```

---

## 3. Document Cross-References

* High-level architectural specifications: [../04-SYSTEM-ARCHITECTURE.md](../04-SYSTEM-ARCHITECTURE.md)
* Application workflows: [application-workflow.md](application-workflow.md)
* Sequence diagrams: [sequence-diagram.md](sequence-diagram.md)

