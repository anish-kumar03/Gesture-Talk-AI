# GestureTalk AI — Functional & Non-Functional Requirements

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-02 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Requirements Specification |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Requirements Categorization Framework

Requirements are categorized according to the **MoSCoW** prioritization framework:
* **Must Have (M)**: Non-negotiable core functionality required for initial release and academic compliance.
* **Should Have (S)**: High-priority features that significantly improve usability and diagnostic capability.
* **Future Enhancement (F)**: Capabilities scheduled for post-v1 expansion.

---

## 2. Functional Requirements (FR)

```
┌──────────┬─────────────────────────────┬──────────┬────────────────────────────────────────────────────────┐
│ Req ID   │ Requirement Title           │ Priority │ Detailed Specification                                 │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-01    │ Webcam Access & Streaming   │ Must     │ The client interface must request user permission via  │
│          │                             │          │ `navigator.mediaDevices.getUserMedia` to access the    │
│          │                             │          │ local camera and render an uninterrupted live video    │
│          │                             │          │ stream into an HTML5 `<video>` element.                │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-02    │ Instant Frame Capture       │ Must     │ Upon user click of "Capture Gesture", the frontend     │
│          │                             │          │ must freeze the active video frame, render it onto an  │
│          │                             │          │ off-screen HTML5 `<canvas>`, and convert it into a     │
│          │                             │          │ standard JPEG/PNG Blob object.                         │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-03    │ Manual Image Upload         │ Must     │ The system must provide a drag-and-drop and file       │
│          │                             │          │ selector interface enabling users to upload an image   │
│          │                             │          │ file directly from local disk (JPEG, PNG, or WebP).    │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-04    │ Image Validation & Guard    │ Must     │ The Java Servlet backend must validate incoming image  │
│          │                             │          │ payloads: ensure payload is non-empty, file size does  │
│          │                             │          │ not exceed 5 MB, and MIME type corresponds to valid    │
│          │                             │          │ image signatures (magic bytes check).                  │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-05    │ Gemini Vision Integration   │ Must     │ The Java backend must invoke the Google GenAI SDK      │
│          │                             │          │ (`com.google.genai`) with the image byte array and a   │
│          │                             │          │ strict prompt to classify the visual gesture.          │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-06    │ Strict Gesture Vocabulary   │ Must     │ The system must classify the image into exactly one of │
│          │ Classification              │          │ five supported static tokens: `THUMBS_UP`,             │
│          │                             │          │ `THUMBS_DOWN`, `OPEN_PALM`, `V_SIGN`, `INDEX_POINTING`,│
│          │                             │          │ or classify it as `UNKNOWN`.                           │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-07    │ Unknown Gesture Detection   │ Must     │ If the image contains no visible hand, multiple        │
│          │                             │          │ conflicting hands, blurred hands, or unsupported       │
│          │                             │          │ hand signs, the system must assign `UNKNOWN`.          │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-08    │ Deterministic Text Mapping  │ Must     │ The Java service layer must deterministically map      │
│          │                             │          │ recognized tokens to fixed natural sentences:          │
│          │                             │          │ • THUMBS_UP → "Yes, that's correct."                   │
│          │                             │          │ • THUMBS_DOWN → "No, that's incorrect."               │
│          │                             │          │ • OPEN_PALM → "Please stop or wait."                   │
│          │                             │          │ • V_SIGN → "Peace and greetings."                      │
│          │                             │          │ • INDEX_POINTING → "Please look there."                │
│          │                             │          │ • UNKNOWN → "Gesture not recognized. Please try again."│
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-09    │ Browser Speech Synthesis    │ Must     │ The client application must invoke the native browser  │
│          │                             │          │ `window.speechSynthesis.speak()` API to audibly        │
│          │                             │          │ articulate the mapped sentence upon receiving response.│
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-10    │ Speech Audio Controls       │ Should   │ The UI must allow users to configure speech parameters:│
│          │                             │          │ speech rate (speed), pitch, and voice selection from   │
│          │                             │          │ the client system's installed speech voices.           │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-11    │ Recognition Audit Logging   │ Must     │ Successful and recognized gesture requests must be     │
│          │                             │          │ persisted into the MySQL database (`gesture_history`), │
│          │                             │          │ storing gesture code, mapped message, source type,     │
│          │                             │          │ confidence estimate, and timestamp.                    │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-12    │ History Querying & Filter   │ Should   │ The system must expose `GET /api/history` supporting   │
│          │                             │          │ chronological retrieval and optional query parameter   │
│          │                             │          │ filtering by gesture code (e.g., `?gesture=THUMBS_UP`).│
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-13    │ History Purge / Clear       │ Should   │ The system must provide a `DELETE /api/history`        │
│          │                             │          │ endpoint allowing users to reset session history logs. │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-14    │ System Health Check         │ Should   │ The system must expose `GET /api/health` returning JSON│
│          │                             │          │ verifying database connectivity and API key detection. │
├──────────┼─────────────────────────────┼──────────┼────────────────────────────────────────────────────────┤
│ FR-15    │ Continuous Stream Tracking  │ Future   │ WebSocket-based continuous frame analysis with temporal│
│          │                             │          │ gesture smoothing and gesture debouncing.              │
└──────────┴─────────────────────────────┴──────────┴────────────────────────────────────────────────────────┘
```

---

## 3. Non-Functional Requirements (NFR)

### 3.1 Performance (NFR-01)
* **Response Latency**: End-to-end processing (from image transmission to UI display and audio start) must complete in under **2.5 seconds** under standard broadband conditions (dominated by Gemini API network inference).
* **Zero Client Lag**: Video streaming and canvas image extraction must run at 30–60 FPS locally without degrading browser responsiveness.
* **Database Connection Overhead**: HikariCP connection retrieval must execute in under **10 milliseconds** using pre-initialized pool threads.

### 3.2 Reliability & Fault Tolerance (NFR-02)
* **Graceful Degradation**: If the Gemini API experiences network timeouts or rate limits (HTTP 429), the application must return a structured JSON error without crashing or exposing raw stack traces.
* **Database Resiliency**: If MySQL is temporarily offline, image recognition must proceed and return results, recording a graceful warning in server logs rather than failing the client's gesture recognition.

### 3.3 Usability & Human Factors (NFR-03)
* **Accessibility**: Clean, high-contrast user interface with large clickable buttons suitable for users with motor coordination challenges.
* **Visual Status Indicators**: Explicit loading spinners, progress messages ("Analyzing gesture with Gemini..."), and visual color badges for recognized gestures (e.g., green for positive gestures, amber for caution, grey for unknown).
* **Reference Guide**: Permanent on-screen reference cards illustrating the five supported hand gestures so users know exact expected postures.

### 3.4 Security (NFR-04)
* **Server-Side Secret Isolation**: The `GOOGLE_API_KEY` must never be transmitted to, stored in, or referenced by client-side code, headers, or HTML.
* **SQL Injection Prevention**: 100% of SQL queries must utilize parameterized JDBC `PreparedStatement` instances. No string concatenation in SQL queries.
* **Input Sanitization**: All incoming multipart file names, sizes, and MIME headers must be validated and sanitized to prevent path traversal or upload exploits.

### 3.5 Privacy & Data Protection (NFR-05)
* **Ephemeral Image Processing**: Images received by the server must be held in volatile memory (`byte[]`) solely for the duration of the Gemini API call and garbage collected immediately.
* **Zero Disk Footprint**: Images must **never** be saved to the server file system, cloud storage buckets, or database tables.
* **No Facial Data Storage**: Only categorical gesture tokens, confidence scores, and timestamps are retained in database audit logs.

### 3.6 Maintainability & Code Quality (NFR-06)
* **Standards Compliance**: Strict Jakarta EE 10 specifications using `jakarta.servlet.*` imports.
* **Modular Layering**: Clear architectural separation: Controller (`*Servlet`), Service (`*Service`), Data Access (`*DAO`), Model (`*Model`/`*Record`), and Utility (`*Util`).
* **Zero Complex Frameworks**: Absence of heavyweight container frameworks (e.g., Spring) ensures transparency, low memory footprint, and high educational clarity.

### 3.7 Browser & Platform Compatibility (NFR-07)
* Supported Browsers: Google Chrome 90+, Microsoft Edge 90+, Mozilla Firefox 88+, Apple Safari 14+.
* Required Client APIs: HTML5 MediaDevices (`getUserMedia`), HTML5 Canvas API, Fetch API, and Web Speech API (`SpeechSynthesis`).

### 3.8 Scalability Considerations (NFR-08)
* Stateless Servlet Architecture: Controllers maintain zero in-memory session state for image analysis, enabling load-balanced deployment across multiple Tomcat nodes if needed in future releases.

---

## 4. Requirement Verification Matrix

```
┌─────────┬──────────────────────────┬─────────────────────────────┬──────────────────────────┐
│ Req ID  │ Verification Method      │ Verification Criterion      │ Phase Verified           │
├─────────┼──────────────────────────┼─────────────────────────────┼──────────────────────────┤
│ FR-01/02│ Manual / Browser Test    │ Video renders, snapshot OK  │ Phase 2 (Frontend)       │
│ FR-03/04│ Unit & Integration Test  │ Rejects >5MB, validates PNG │ Phase 1 & 2              │
│ FR-05/06│ AI Mock & Live Unit Test │ Returns 5 tokens + UNKNOWN  │ Phase 3 (AI Service)     │
│ FR-08/09│ Functional Testing       │ Audio speaks exact phrase   │ Phase 4 (Speech Mapping) │
│ FR-11/12│ DAO Integration Test     │ SQL insert & select verified│ Phase 5 (Database)       │
│ NFR-04  │ Static Code Analysis     │ Zero client API key leaks   │ Phase 6 (Security Audit) │
│ NFR-05  │ Filesystem Audit         │ 0 temp image files written  │ Phase 6 (Privacy Audit)  │
└─────────┴──────────────────────────┴─────────────────────────────┴──────────────────────────┘
```

---

## 5. Document Cross-References

* For technology components fulfilling these requirements: [03-TECHNOLOGY-STACK.md](03-TECHNOLOGY-STACK.md)
* For architectural layering: [04-SYSTEM-ARCHITECTURE.md](04-SYSTEM-ARCHITECTURE.md)
* For validation details and error definitions: [13-ERROR-HANDLING-AND-VALIDATION.md](13-ERROR-HANDLING-AND-VALIDATION.md)

