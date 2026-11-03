# GestureTalk AI — Architecture Decision Records (ADR)

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-18 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Architectural Governance & Decision Log |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Overview & ADR Governance

This document records the foundational architectural decisions made during the design of GestureTalk AI. Each Architecture Decision Record (ADR) captures the problem context, alternatives evaluated, the final decision, and the associated trade-offs.

---

## 2. Decision Log Summary Table

```
┌────────┬────────────────────────────────────────────────────────────────────────┬──────────┐
│ ADR ID │ Title                                                                  │ Status   │
├────────┼────────────────────────────────────────────────────────────────────────┼──────────┤
│ ADR-01 │ Adopt Pure Jakarta Servlets & JSP instead of Spring Boot               │ Accepted │
├────────┼────────────────────────────────────────────────────────────────────────┼──────────┤
│ ADR-02 │ Utilize Google Gemini Vision via Official GenAI Java SDK               │ Accepted │
├────────┼────────────────────────────────────────────────────────────────────────┼──────────┤
│ ADR-03 │ Use Browser SpeechSynthesis API instead of Server-Side TTS APIs        │ Accepted │
├────────┼────────────────────────────────────────────────────────────────────────┼──────────┤
│ ADR-04 │ Enforce Ephemeral In-Memory Image Processing (Zero Disk Storage)       │ Accepted │
├────────┼────────────────────────────────────────────────────────────────────────┼──────────┤
│ ADR-05 │ Enforce Deterministic Java Message Mapping instead of Generative Speech│ Accepted │
├────────┼────────────────────────────────────────────────────────────────────────┼──────────┤
│ ADR-06 │ Restrict Database Scope to MySQL Audit Logging via Raw JDBC & HikariCP │ Accepted │
├────────┼────────────────────────────────────────────────────────────────────────┼──────────┤
│ ADR-07 │ Standardize on Java 21 LTS with Maven WAR Packaging for Tomcat 10.1+   │ Accepted │
└────────┴────────────────────────────────────────────────────────────────────────┴──────────┘
```

---

## 3. Detailed Architecture Decision Records

### ADR-01: Adopt Pure Jakarta Servlets & JSP instead of Spring Boot
* **Context**: Academic Advanced Internet Programming specifications require demonstration of foundational Java web architectures, HTTP transaction lifecycles, and container dispatching without relying on heavy opinionated frameworks.
* **Alternatives Considered**: Spring Boot 3.x, Quarkus, Micronaut, Dropwizard.
* **Decision**: Implement the backend using Jakarta Servlet 6.0 (`jakarta.servlet.*`) and Jakarta Server Pages (JSP 3.1) deployed as a standard WAR on Apache Tomcat 10.1+.
* **Advantages**:
  * Complete transparency: exact control over request routing, multipart stream parsing, and error status mapping.
  * Rapid startup (< 1.5 seconds) and minimal heap memory footprint (< 40 MB).
  * Direct alignment with enterprise Jakarta EE specifications.
* **Trade-Offs**: Requires manual wiring of service dependencies and explicit servlet mappings in `web.xml` rather than annotation-driven autowiring.

---

### ADR-02: Utilize Google Gemini Vision via Official GenAI Java SDK
* **Context**: The application requires high-accuracy visual classification of hand postures across diverse lighting, backgrounds, and skin tones.
* **Alternatives Considered**: Locally trained CNN (TensorFlow/PyTorch/OpenCV), YOLOv8, Google MediaPipe, Vertex AI Heavy GCP SDK, raw `HttpClient` REST calls.
* **Decision**: Interface with Google's `gemini-2.5-flash` multimodal model using the official `com.google.genai:google-genai` Java SDK (version `0.1.1`).
* **Advantages**:
  * Superior zero-shot accuracy across environmental variations without local model training overhead.
  * Idiomatic Java SDK with native in-memory byte array support (`Part.fromBytes()`).
  * Structured JSON schema output enforcement.
* **Trade-Offs**: Requires active outbound HTTPS connectivity to Google's API endpoints; subject to network latency and cloud API quota limitations.

---

### ADR-03: Use Browser SpeechSynthesis API instead of Server-Side TTS APIs
* **Context**: The system must convert recognized gestures into audible speech for the user or nearby listeners.
* **Alternatives Considered**: Google Cloud Text-to-Speech API, Amazon Polly, ElevenLabs API, eSpeak server process.
* **Decision**: Delegate speech generation directly to the client browser's native Web Speech API (`window.speechSynthesis`).
* **Advantages**:
  * Zero server-side bandwidth: audio waveform is generated locally on the client device.
  * Zero additional cloud billing or external API quota consumption.
  * Instant vocalization with zero round-trip audio file streaming latency.
  * Leverages high-quality neural voices already installed on modern OS platforms (Windows, macOS, Android, iOS).
* **Trade-Offs**: Speech timbre and available voice accents vary across different operating systems and browser engines.

---

### ADR-04: Enforce Ephemeral In-Memory Image Processing (Zero Disk Storage)
* **Context**: Processing webcam snapshots and user photos introduces serious personal privacy and data security risks if images are saved on the server.
* **Alternatives Considered**: Saving images to a temporary server directory (`/tmp`), persisting images in AWS S3 / GCS buckets, storing base64 blobs in MySQL.
* **Decision**: All uploaded and captured images are processed solely in volatile JVM memory (`byte[]`) and never written to permanent disk storage.
* **Advantages**:
  * Guarantees absolute user privacy: user photographs cannot be stolen via server directory traversal or database breaches.
  * Eliminates disk cleanup cron jobs, inode exhaustion, and storage cost.
  * Full compliance with GDPR and privacy-by-design standards.
* **Trade-Offs**: Historical recognition records retain only categorical metadata (gesture code, timestamp), preventing visual post-mortem re-verification of the original photograph.

---

### ADR-05: Enforce Deterministic Java Message Mapping instead of Generative Speech
* **Context**: Hand gestures must be translated into clear, spoken sentences. Generative LLMs can generate varied conversational text.
* **Alternatives Considered**: Letting Gemini directly generate the spoken response sentence in its prompt response.
* **Decision**: Gemini classifies the image into a rigid token (`THUMBS_UP`, `THUMBS_DOWN`, `OPEN_PALM`, `V_SIGN`, `INDEX_POINTING`, or `UNKNOWN`). Java business logic (`GestureType` enum / `GestureMappingService`) deterministically binds each token to an immutable natural language sentence.
* **Advantages**:
  * 100% deterministic speech: eliminates hallucinations, inappropriate language, and tone drift.
  * Non-verbal users have complete confidence in the exact sentence their system will vocalize.
  * Neutralizes prompt injection attacks.
* **Trade-Offs**: Less conversational flexibility in the initial release (sentences are fixed constants).

---

### ADR-06: Restrict Database Scope to MySQL Audit Logging via Raw JDBC & HikariCP
* **Context**: The system needs to log recognition transactions for audit and monitoring. Full-blown enterprise features (e.g., user authentication, RBAC) would introduce unnecessary complexity.
* **Alternatives Considered**: Hibernate/JPA with Spring Data, MongoDB, SQLite, full multi-tenant schema with users/roles/sessions.
* **Decision**: Deploy a single focused MySQL table (`gesture_history`), interacted with via pure JDBC `PreparedStatement` and the high-performance **HikariCP** connection pool.
* **Advantages**:
  * Microsecond connection acquisition and zero ORM overhead.
  * Elimination of N+1 queries, lazy loading exceptions, and complex entity states.
  * 100% protection against SQL injection via parameterized execution.
* **Trade-Offs**: Requires manual SQL writing and `ResultSet` mapping in Java DAO classes.

---

### ADR-07: Standardize on Java 21 LTS with Maven WAR Packaging for Tomcat 10.1+
* **Context**: Aligning build tooling with modern LTS Java standards and Jakarta EE specifications.
* **Alternatives Considered**: Java 17, Java 11, executable JAR packaging, Gradle build system.
* **Decision**: Standardize on **Java 21 LTS** with Maven WAR packaging targeting **Apache Tomcat 10.1+** (Jakarta EE 10, Servlet 6.0).
* **Advantages**:
  * Java 21 provides modern language features (records, pattern matching, virtual threads).
  * WAR packaging is the standard enterprise format for Jakarta servlet containers.
  * Apache Tomcat 10.1 is the industry reference container for modern Jakarta EE 10 workloads.
* **Trade-Offs**: Requires target server to run Tomcat 10.1 or higher (older Tomcat 9 deployments using `javax.servlet` are incompatible).

---

## 4. Document Cross-References

* High-level architectural layout: [04-SYSTEM-ARCHITECTURE.md](04-SYSTEM-ARCHITECTURE.md)
* Security implications of ADRs: [12-SECURITY-AND-PRIVACY.md](12-SECURITY-AND-PRIVACY.md)
* Deployment procedures: [16-SETUP-AND-DEPLOYMENT.md](16-SETUP-AND-DEPLOYMENT.md)

