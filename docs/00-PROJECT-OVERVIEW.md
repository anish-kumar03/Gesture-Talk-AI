# GestureTalk AI — Project Overview

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-00 |
| **Project Name** | GestureTalk AI |
| **Subtitle** | AI-Powered Hand Gesture Recognition and Communication Assistant |
| **Document Version** | 1.0.0 |
| **Classification** | Technical & Academic Architecture Documentation |
| **Target Framework** | Jakarta EE 10 / Apache Tomcat 10.1 / Java 21 LTS |

---

## 1. Executive Summary

**GestureTalk AI** is an advanced, AI-powered assistive communication web application designed to bridge the accessibility gap for non-verbal individuals, persons with speech impairments, and users requiring touchless, gesture-driven machine interaction. By combining modern computer vision via the **Google Gemini Multimodal API** (interfaced through the official **Google GenAI Java SDK**) with a robust, enterprise-grade **Jakarta EE MVC backend** running on **Apache Tomcat 10.1**, the system captures webcam feeds or uploaded static images, classifies predefined static hand gestures with high fidelity, maps them to contextual natural language sentences, and vocalizes the resulting message in real time using the browser-native **SpeechSynthesis API**.

Unlike typical modern web prototypes built on bloated full-stack JavaScript or Python-centric micro-frameworks (such as FastAPI or Flask), GestureTalk AI is intentionally engineered as an **academic Advanced Internet Programming showcase** where **Java serves as the primary backend, domain model, and business logic language**. The architecture strictly maintains separation of concerns across Model-View-Controller (MVC) layers, enforces server-side credential isolation, executes zero-disk-footprint in-memory image processing for privacy, and persists recognition analytics into a **MySQL** relational database via raw **JDBC PreparedStatements** and high-throughput **HikariCP** connection pooling.

---

## 2. Project Identification & Domain

* **Project Title**: GestureTalk AI
* **Academic Category**: Advanced Internet Programming / Applied Artificial Intelligence / Web Engineering
* **Domain**: Assistive Technology, Human-Computer Interaction (HCI), Computer Vision, Multimodal AI
* **Primary Target Environment**: On-premise or cloud-hosted Java Servlet Container (Apache Tomcat 10.1+, Java 21 LTS, MySQL 8.0+)
* **Client Requirements**: Standard HTML5-compliant web browser supporting the WebRTC MediaDevices API, Canvas API, and Web Speech API (Chrome, Edge, Firefox, Safari)

---

## 3. Core Technologies & Architecture Synopsis

The application strictly implements an enterprise MVC pattern without relying on Spring Boot, maintaining complete control over Jakarta Servlet lifecycles and HTTP exchanges.

```
                    ┌───────────────────────────────────────────────┐
                    │               CLIENT BROWSER                  │
                    │  HTML5 + Bootstrap 5 + Vanilla JavaScript     │
                    │  [Webcam / Canvas Capture / SpeechSynthesis]  │
                    └───────────────────────┬───────────────────────┘
                                            │ Multipart HTTP POST
                                            ▼
                    ┌───────────────────────────────────────────────┐
                    │             JAKARTA EE BACKEND                │
                    │   Apache Tomcat 10.1  •  Java 21 LTS          │
                    │  ┌─────────────────────────────────────────┐  │
                    │  │ Controller Layer: Jakarta Servlets      │  │
                    │  ├─────────────────────────────────────────┤  │
                    │  │ Service Layer: Vision & Mapping Logic   │  │
                    │  ├─────────────────────────────────────────┤  │
                    │  │ Data Access: JDBC (PreparedStatements)  │  │
                    │  └────────────────────┬────────────────────┘  │
                    └───────────────────────┼───────────────────────┘
                                            │
                    ┌───────────────────────┴───────────────────────┐
                    │                                               │
                    ▼                                               ▼
     ┌─────────────────────────────┐                 ┌─────────────────────────────┐
     │      GOOGLE GEMINI API      │                 │      MYSQL 8.0 DATABASE     │
     │  Official GenAI Java SDK    │                 │  HikariCP Connection Pool   │
     │  Multimodal Vision Engine   │                 │  Audit & Recognition Logs   │
     └─────────────────────────────┘                 └─────────────────────────────┘
```

---

## 4. Main AI Capability

GestureTalk AI leverages Google's Gemini multimodal models (specifically `gemini-2.5-flash`) through the official `com.google.genai:google-genai` Java SDK. The AI integration performs zero-shot and few-shot visual classification on single-frame static hand gestures transmitted directly as in-memory byte arrays. 

### Supported Vocabulary (Initial Release)

The initial version of GestureTalk AI strictly recognizes **five predefined static hand gestures** plus an explicit **UNKNOWN** fallback state:

| Gesture Identifier | Visual Gesture Description | Deterministic Output Sentence |
| :--- | :--- | :--- |
| `THUMBS_UP` | Closed fist with thumb pointing upwards | *"Yes, that's correct."* |
| `THUMBS_DOWN` | Closed fist with thumb pointing downwards | *"No, that's incorrect."* |
| `OPEN_PALM` | Flat open hand, all five fingers extended facing camera | *"Please stop or wait."* |
| `V_SIGN` | Index and middle fingers extended in a V-shape, other fingers folded | *"Peace and greetings."* |
| `INDEX_POINTING` | Index finger extended pointing toward a direction, other fingers folded | *"Please look there."* |
| `UNKNOWN` | Ambiguous, unsupported, partially occluded, or missing hand gesture | *"Gesture not recognized. Please try again."* |

---

## 5. Intended Users & Personas

1. **Non-Verbal & Speech-Impaired Individuals**:
   * Users who cannot vocalize spoken sentences or find vocal communication strenuous can use quick, intuitive hand signs in front of a laptop or mobile webcam to convey essential communicative intents.
2. **Post-Operative & Intensive Care Patients**:
   * Patients temporarily unable to speak due to intubation, throat trauma, or medical interventions can communicate basic affirmations, denials, and alerts to medical staff.
3. **Noisy / Quiet Workplace Operators**:
   * Users working in extreme acoustic environments (e.g., factories, broadcast studios, libraries) where verbal communication is impossible or prohibited.
4. **Academic Evaluators & Software Engineers**:
   * Students, educators, and enterprise Java engineers seeking a clear, standards-compliant reference implementation of Jakarta EE 10, modern Google GenAI Java SDK, and native HTML5 client APIs.

---

## 6. Expected Outcomes

* **Zero-Exposition of Credentials**: Strict architectural enforcement guaranteeing that the `GEMINI_API_KEY` never touches client-side code, cookies, or browser storage.
* **Privacy-by-Design Compliance**: No captured webcam frames or uploaded images are written to persistent storage (disk, cloud bucket, or database). Frames exist solely in volatile JVM memory during the HTTP request lifecycle.
* **Deterministic Semantic Mapping**: Separation of probabilistic visual classification (handled by Gemini) from deterministic text mapping (handled by Java business logic), eliminating AI hallucinations in spoken output.
* **Platform Independence**: Client execution on any device with a modern browser without requiring native application installation, browser extensions, or specialized neural compute hardware.

---

## 7. Scope & Boundaries of Initial Release

### In-Scope
* Web camera integration via HTML5 MediaDevices API.
* Manual image upload via Drag-and-Drop or File Picker (JPEG, PNG, WebP).
* Real-time image validation (size limits, MIME types, byte signature checks).
* Server-side communication with Gemini API using Google GenAI Java SDK.
* Deterministic Java mapping of the five supported gestures.
* Client-side speech synthesis via browser SpeechSynthesis API.
* Audit and recognition logging into MySQL using JDBC `PreparedStatement`.
* Historical query endpoint for audit and analytics inspection.

### Explicitly Out-of-Scope (Non-Goals)
* Full Sign Language Translation (ASL, BSL, ISL) requiring continuous spatial syntax, facial expressions, and dynamic motion tracking.
* Continuous real-time video stream inference (30 FPS video parsing) over WebSockets/WebRTC (v1 operates on discrete snapshot requests).
* User authentication, user profiles, multi-tenancy, and complex role-based access control (RBAC) to maintain architectural focus.
* Permanent archiving or cloud storage of user webcam photographs.

---

## 8. Document Cross-References

* For detailed problem analysis and justification: [01-PROBLEM-STATEMENT-AND-OBJECTIVES.md](01-PROBLEM-STATEMENT-AND-OBJECTIVES.md)
* For formal requirements specifications: [02-FUNCTIONAL-AND-NONFUNCTIONAL-REQUIREMENTS.md](02-FUNCTIONAL-AND-NONFUNCTIONAL-REQUIREMENTS.md)
* For dependency versions and runtime prerequisites: [03-TECHNOLOGY-STACK.md](03-TECHNOLOGY-STACK.md)
* For architectural layering and data flows: [04-SYSTEM-ARCHITECTURE.md](04-SYSTEM-ARCHITECTURE.md)

