# GestureTalk AI — Sequence Diagrams

| Diagram Document | Value |
| :--- | :--- |
| **Document ID** | DIAG-03 |
| **Project Name** | GestureTalk AI |
| **Classification** | Interaction & Protocol Diagrams (Mermaid) |
| **Parent Document** | [../05-APPLICATION-WORKFLOW.md](../05-APPLICATION-WORKFLOW.md) |

---

## 1. Webcam-to-Gemini Real-Time Recognition Sequence

```mermaid
sequenceDiagram
    autonumber
    actor User as User
    participant Browser as Browser Client (JS)
    participant HW as Webcam Hardware
    participant Servlet as GestureRecognitionServlet
    participant Validation as ImageValidationService
    participant GeminiSvc as GeminiVisionService
    participant CloudAI as Google Gemini API
    participant Mapper as GestureMappingService
    participant DAO as RecognitionLogDAO
    participant DB as MySQL Database
    participant Speech as SpeechSynthesis Engine

    %% Initialization & Capture
    User->>Browser: Click "Start Camera"
    Browser->>HW: navigator.mediaDevices.getUserMedia({video: true})
    HW-->>Browser: Stream video frames to <video>
    User->>Browser: Perform Gesture & Click "Capture Gesture"
    Browser->>Browser: Draw frame to <canvas>, toBlob('image/jpeg')

    %% HTTP Dispatch
    Browser->>Servlet: POST /api/recognize (multipart/form-data: image blob)
    activate Servlet

    %% In-Memory Validation
    Servlet->>Servlet: Read Part stream into volatile byte[] (RAM only)
    Servlet->>Validation: validateImage(byte[])
    activate Validation
    Validation-->>Servlet: Validation Passed (JPEG magic bytes verified, size <= 5MB)
    deactivate Validation

    %% AI Multimodal Inference
    Servlet->>GeminiSvc: classifyGesture(byte[], "image/jpeg")
    activate GeminiSvc
    GeminiSvc->>GeminiSvc: Create Part.fromBytes(byte[]) & structured prompt
    GeminiSvc->>CloudAI: client.models.generateContent("gemini-2.5-flash", content)
    activate CloudAI
    CloudAI-->>GeminiSvc: GenerateContentResponse (JSON string)
    deactivate CloudAI
    GeminiSvc->>GeminiSvc: Parse JSON & apply confidence threshold (>= 0.60)
    GeminiSvc-->>Servlet: GestureResult(THUMBS_UP, 0.95, explanation)
    deactivate GeminiSvc

    %% Deterministic Mapping
    Servlet->>Mapper: getSpokenSentence(GestureType.THUMBS_UP)
    activate Mapper
    Mapper-->>Servlet: "Yes, that's correct." (Java Constant)
    deactivate Mapper

    %% Database Persistence
    Servlet->>DAO: insert(RecognitionLog)
    activate DAO
    DAO->>DB: INSERT INTO gesture_history (...) VALUES (?, ?, ...)
    activate DB
    DB-->>DAO: Generated Key ID: 101
    deactivate DB
    DAO-->>Servlet: Return loggedId: 101
    deactivate DAO

    %% Client Response & Audio Articulation
    Servlet-->>Browser: HTTP 200 OK (JSON: { gesture: "THUMBS_UP", spokenMessage: "...", audioText: "..." })
    deactivate Servlet

    Browser->>Browser: Update UI Result Card (Badge, Confidence Bar, Reason)
    Browser->>Speech: window.speechSynthesis.speak(Utterance)
    activate Speech
    Speech-->>User: Device Speakers Vocalize: "Yes, that's correct."
    deactivate Speech
```

---

## 2. Static Image Upload Sequence Diagram

```mermaid
sequenceDiagram
    autonumber
    actor User as User
    participant Browser as Browser Client (JS)
    participant Servlet as GestureRecognitionServlet
    participant Validation as ImageValidationService
    participant GeminiSvc as GeminiVisionService
    participant CloudAI as Google Gemini API
    participant Mapper as GestureMappingService
    participant DAO as RecognitionLogDAO
    participant DB as MySQL Database
    participant Speech as SpeechSynthesis Engine

    %% File Selection
    User->>Browser: Select or Drag & Drop File (e.g. gesture.png)
    Browser->>Browser: Verify client file size <= 5MB & render thumbnail preview
    User->>Browser: Click "Analyze Uploaded Image"

    %% HTTP Dispatch
    Browser->>Servlet: POST /api/recognize (multipart: file, sourceType="UPLOAD")
    activate Servlet

    %% In-Memory Read & Validation
    Servlet->>Servlet: Read InputStream into byte[] buffer
    Servlet->>Validation: validateImage(byte[])
    activate Validation
    alt File Size > 5MB or Corrupt Header
        Validation-->>Servlet: throw InvalidImageException
        Servlet-->>Browser: HTTP 413 or 415 (JSON Error)
        Browser-->>User: Show Alert: "Invalid image format or file too large."
    else Valid File Header
        Validation-->>Servlet: Validation Passed
    end
    deactivate Validation

    %% AI Vision Call
    Servlet->>GeminiSvc: classifyGesture(byte[], "image/png")
    activate GeminiSvc
    GeminiSvc->>CloudAI: client.models.generateContent(...)
    activate CloudAI
    CloudAI-->>GeminiSvc: Return JSON: { gesture: "OPEN_PALM", confidence: 0.92 }
    deactivate CloudAI
    GeminiSvc-->>Servlet: GestureResult(OPEN_PALM, 0.92, "Flat open palm facing camera")
    deactivate GeminiSvc

    %% Deterministic Mapping & Logging
    Servlet->>Mapper: getSpokenSentence(GestureType.OPEN_PALM)
    activate Mapper
    Mapper-->>Servlet: "Please stop or wait."
    deactivate Mapper

    Servlet->>DAO: insert(RecognitionLog)
    activate DAO
    DAO->>DB: INSERT INTO gesture_history (...)
    activate DB
    DB-->>DAO: Row Inserted (id: 102)
    deactivate DB
    DAO-->>Servlet: Return 102
    deactivate DAO

    %% Response Delivery
    Servlet-->>Browser: HTTP 200 OK (JSON)
    deactivate Servlet
    Browser->>Browser: Render "Open Palm" Badge and Spoken Message
    Browser->>Speech: window.speechSynthesis.speak("Please stop or wait.")
    activate Speech
    Speech-->>User: Speaks: "Please stop or wait."
    deactivate Speech
```

---

## 3. Document Cross-References

* Workflow documentation: [../05-APPLICATION-WORKFLOW.md](../05-APPLICATION-WORKFLOW.md)
* API endpoint specifications: [../09-API-ENDPOINT-DESIGN.md](../09-API-ENDPOINT-DESIGN.md)
* Visual system architecture: [system-architecture.md](system-architecture.md)

