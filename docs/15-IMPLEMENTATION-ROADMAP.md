# GestureTalk AI — Phased Implementation Roadmap

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-15 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Project Management & Execution Roadmap |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Phased Development Methodology

To ensure architectural integrity, avoid scope creep, and enforce strict verification gates, GestureTalk AI is engineered across **eight structured, sequential phases (Phase 0 through Phase 7)**.

In strict compliance with project governance:
* No future phase is implemented without explicit user authorization.
* Maven compilation and unit tests are executed at each phase gate.
* Existing functioning code is preserved and protected from unnecessary modifications.

---

## 2. Phase-by-Phase Roadmap Matrix

```
┌─────────┬──────────────────────────────────────────┬────────────┬──────────────────────────────────────────┐
│ Phase   │ Phase Designation                        │ Status     │ Primary Architectural Deliverable        │
├─────────┼──────────────────────────────────────────┼────────────┼──────────────────────────────────────────┤
│ Phase 0 │ Project Initialization & Agent Setup     │ COMPLETED  │ Directory init, base pom.xml, guidelines │
├─────────┼──────────────────────────────────────────┼────────────┼──────────────────────────────────────────┤
│ Phase 1 │ Maven WAR Foundation & MVC Architecture  │ COMPLETED  │ Technical docs, directory layout, models │
├─────────┼──────────────────────────────────────────┼────────────┼──────────────────────────────────────────┤
│ Phase 2 │ Frontend, Webcam Capture & File Uploads  │ COMPLETED  │ JSP views, MediaDevices, Canvas, CSS/JS  │
├─────────┼──────────────────────────────────────────┼────────────┼──────────────────────────────────────────┤
│ Phase 3 │ Gemini Vision SDK & Classification       │ COMPLETED  │ Google GenAI SDK integration, prompts    │
├─────────┼──────────────────────────────────────────┼────────────┼──────────────────────────────────────────┤
│ Phase 4 │ Deterministic Mapping & SpeechSynthesis  │ COMPLETED  │ Java semantic mapping, Web Speech API    │
├─────────┼──────────────────────────────────────────┼────────────┼──────────────────────────────────────────┤
│ Phase 5 │ MySQL Persistence & JDBC Audit Logging   │ COMPLETED  │ MySQL schema, HikariCP, DAOs, history API│
├─────────┼──────────────────────────────────────────┼────────────┼──────────────────────────────────────────┤
│ Phase 6 │ Testing, Accuracy Evaluation & Security  │ COMPLETED  │ Master test suite, 30-sample benchmark   │
├─────────┼──────────────────────────────────────────┼────────────┼──────────────────────────────────────────┤
│ Phase 7 │ UI Polishing, Deployment & Final Audit   │ COMPLETED  │ Tomcat 10.1 WAR deployment, final review │
└─────────┴──────────────────────────────────────────┴────────────┴──────────────────────────────────────────┘
```

---

## 3. Comprehensive Phase Specifications

### Phase 0: Project Initialization & Permanent Instructions `[COMPLETED]`
* **Objective**: Initialize project directory, establish core architectural boundaries, configure baseline Maven build file, and inspect runtime tooling.
* **Deliverables**: Root workspace, initial `pom.xml`, runtime verification (Java 21, Maven 3.9, MySQL 8.0, Tomcat 10.1+).
* **Completion Criteria**: Root `pom.xml` configured with correct group, artifact, Java release 21, and packaging WAR.

---

### Phase 1: Java Maven Foundation, MVC Core & Documentation `[COMPLETED]`
* **Objective**: Generate complete, academic-level technical documentation in `docs/` and prepare foundation directory skeletons and core domain models.
* **Required Implementation**:
  * Complete 19-document technical architecture suite in `docs/` and `docs/diagrams/`.
  * Define core domain entities: `GestureType.java`, `GestureResult.java`, `HealthStatus.java`.
  * Establish deployment descriptor `web.xml` and environment configuration scaffolding `AppConfig.java`.
* **Important Files**: `docs/*.md`, `docs/diagrams/*.md`, `pom.xml`.
* **Dependencies**: `jakarta.servlet-api`, `gson`, `junit-jupiter`.
* **Expected Deliverables**: Comprehensive documentation directory, documentation index `README.md`, verified directory layout.
* **Completion Criteria**: All 19 documents and 4 diagrams verified on disk, Mermaid diagrams structurally valid, zero compilation errors.

---

### Phase 2: Frontend Interface, Webcam Capture & Image Upload `[COMPLETED]`
* **Objective**: Deliver the single-page client interface with real-time camera viewfinder and drag-and-drop file upload.
* **Required Implementation**:
  * HTML5/JSP structure in `index.jsp` utilizing Bootstrap 5.3.
  * Browser camera streaming module `webcam.js` using `navigator.mediaDevices.getUserMedia`.
  * Snapshot rasterizer using off-screen HTML5 `<canvas>` and `toBlob()`.
  * Drag-and-drop file upload controller `upload.js`.
  * Skeleton `GestureRecognitionServlet.java` receiving multipart payloads.
* **Important Files**: `src/main/webapp/index.jsp`, `src/main/webapp/static/js/webcam.js`, `src/main/webapp/static/js/upload.js`.
* **Completion Criteria**: Browser renders 30 FPS camera feed, captures JPEG blob, and successfully uploads multipart payload to Servlet.

---

### Phase 3: Gemini API Integration & Visual Classification `[COMPLETED]`
* **Objective**: Connect the Java backend to the Google Gemini Multimodal Vision API using the official Google GenAI Java SDK.
* **Required Implementation**:
  * `GeminiVisionService.java` using `com.google.genai.Client`.
  * Multimodal payload generation with `Part.fromBytes()` and structured classification prompt.
  * JSON parser extracting `gesture`, `confidence`, and `explanation`.
  * Confidence threshold enforcement (minimum 0.60, fallback to `UNKNOWN`).
* **Important Files**: `com/gesturetalk/service/GeminiVisionService.java`, `com/gesturetalk/util/JsonUtils.java`.
* **Dependencies**: `com.google.genai:google-genai:0.1.1`, `com.google.code.gson:gson:2.11.0`.
* **Completion Criteria**: Uploaded gesture images accurately return structured JSON classifications matching the 5 gestures or `UNKNOWN`.

---

### Phase 4: Deterministic Mapping & Speech Synthesis `[COMPLETED]`
* **Objective**: Map recognized gesture tokens to immutable natural language sentences in Java and vocalize them in the browser.
* **Required Implementation**:
  * `GestureMappingService.java` executing deterministic mapping.
  * Integration into `GestureRecognitionServlet` returning full JSON response.
  * Browser client speech module `tts.js` wrapping `window.speechSynthesis.speak()`.
  * Speech controls interface (voice selection, pitch, rate sliders).
* **Important Files**: `com/gesturetalk/service/GestureMappingService.java`, `src/main/webapp/static/js/tts.js`.
* **Completion Criteria**: End-to-end flow speaks "Yes, that's correct." when a thumbs-up gesture is submitted.

---

### Phase 5: MySQL Persistence & Recognition Audit History `[COMPLETED]`
* **Objective**: Persist audit logs of recognized gestures into MySQL and display them in a filterable table.
* **Required Implementation**:
  * Database schema setup (`schema.sql` creating `gesture_history`).
  * `DBConnection.java` configuring HikariCP DataSource.
  * `GestureHistoryDAO.java` executing parameterized INSERT, SELECT, and DELETE statements.
  * `GestureHistoryServlet.java` (`GET /api/history`, `DELETE /api/history`).
  * Frontend table renderer `history.js` with gesture filtering.
* **Important Files**: `com/gesturetalk/dao/GestureHistoryDAO.java`, `com/gesturetalk/controller/GestureHistoryServlet.java`.
* **Dependencies**: `com.mysql:mysql-connector-j:8.4.0`, `com.zaxxer:HikariCP:5.1.0`.
* **Completion Criteria**: Every successful recognition inserts a row in `gesture_history`; history table updates dynamically.

---

### Phase 6: Testing, Accuracy Evaluation & Security Hardening `[COMPLETED]`
* **Objective**: Perform comprehensive verification, security validation, and evaluate accuracy on the empirical benchmark.
* **Required Implementation**:
  * Unit tests with JUnit 5 and Mockito across all components (87 tests).
  * In-memory H2 database integration tests for `GestureHistoryDAO`.
  * Benchmark evaluation across 30 samples (supported, unsupported, blurred, lighting, boundary edge cases).
  * Verification of magic number binary header inspection and zero-disk in-memory streams.
  * Secrets redaction and duplicate in-flight click prevention.
* **Important Files**: `src/test/java/com/gesturetalk/**/*Test.java`.
* **Completion Criteria**: 100% of automated Maven tests pass; formal accuracy report compiled.

---

### Phase 7: UI Polishing, Deployment & Final Academic Audit `[COMPLETED]`
* **Objective**: Finalize UI polish, assemble production WAR package, deploy to Apache Tomcat 10.1, and finalize project documentation.
* **Required Implementation**:
  * Production WAR packaging (`mvn clean package`).
  * Deployment verification on Apache Tomcat 10.1.
  * Final audit of `README.md` and submission package.
* **Completion Criteria**: Application fully functional on Tomcat container with zero console warnings or errors.

---

### Phase 8: MediaPipe WebAssembly Real-Time Recognition Engine `[COMPLETED]`
* **Objective**: Transition the primary recognition engine to client-side Google MediaPipe Gesture Recognizer WebAssembly (`@mediapipe/tasks-vision`) to eliminate HTTP 503 model overload, eliminate network latency, and provide real-time 30+ FPS live webcam tracking.
* **Required Implementation**:
  * Local bundle and WASM model hosting (`gesture_recognizer.task`, `vision_bundle.mjs`, `wasm/` files) with automatic CDN fallback.
  * Real-time continuous inference loop via `requestAnimationFrame` with 21-point hand skeleton canvas overlay.
  * Five-stage client prediction smoothing engine (4 consecutive frames $\ge 0.70$, 2.5s cooldown, 1.0s transition, duplicate vocalization suppression).
  * Java backend endpoint `POST /api/gesture-result` in `GestureResultServlet.java` for enum validation, sentence mapping, and MySQL audit persistence.
  * 10 additional unit and integration tests (97 tests total, 100% passing).
  * Secondary optional Gemini Vision integration for deep multimodal diagnostics without creating a single point of failure.
* **Completion Criteria**: Live 30+ FPS webcam tracking with instant speech vocalization, complete offline local operation, and 100% passing automated test suite.

---

## 4. Document Cross-References

* High-level project objectives: [01-PROBLEM-STATEMENT-AND-OBJECTIVES.md](01-PROBLEM-STATEMENT-AND-OBJECTIVES.md)
* Setup and installation instructions: [16-SETUP-AND-DEPLOYMENT.md](16-SETUP-AND-DEPLOYMENT.md)
* Project directory structure: [11-PROJECT-STRUCTURE.md](11-PROJECT-STRUCTURE.md)

