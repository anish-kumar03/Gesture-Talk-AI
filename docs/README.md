# GestureTalk AI — Technical Architecture Documentation Index

Welcome to the central technical documentation repository for **GestureTalk AI** — an AI-powered hand gesture recognition and communication assistant engineered for academic and enterprise evaluation.

---

## 1. Documentation Structure & Master Index

The documentation suite is structured into numbered, topic-specific architectural specifications:

```
docs/
├── README.md (This Index)
├── 00-PROJECT-OVERVIEW.md
├── 01-PROBLEM-STATEMENT-AND-OBJECTIVES.md
├── 02-FUNCTIONAL-AND-NONFUNCTIONAL-REQUIREMENTS.md
├── 03-TECHNOLOGY-STACK.md
├── 04-SYSTEM-ARCHITECTURE.md
├── 05-APPLICATION-WORKFLOW.md
├── 06-USER-INTERFACE-AND-SCREENS.md
├── 07-GEMINI-AI-INTEGRATION.md
├── 08-GESTURE-RECOGNITION-LOGIC.md
├── 09-API-ENDPOINT-DESIGN.md
├── 10-DATABASE-DESIGN.md
├── 11-PROJECT-STRUCTURE.md
├── 12-SECURITY-AND-PRIVACY.md
├── 13-ERROR-HANDLING-AND-VALIDATION.md
├── 14-TESTING-AND-QUALITY-ASSURANCE.md
├── 15-IMPLEMENTATION-ROADMAP.md
├── 16-SETUP-AND-DEPLOYMENT.md
├── 17-PROJECT-LIMITATIONS-AND-FUTURE-SCOPE.md
├── 18-TECHNICAL-DECISIONS.md
└── diagrams/
    ├── system-architecture.md
    ├── application-workflow.md
    ├── sequence-diagram.md
    └── database-er-diagram.md
```

---

## 2. Master Navigation Matrix

| Doc ID | Document Title | Primary Content Summary |
| :---: | :--- | :--- |
| **[00](00-PROJECT-OVERVIEW.md)** | **[Project Overview](00-PROJECT-OVERVIEW.md)** | Executive summary, domain, target environment, and core capabilities. |
| **[01](01-PROBLEM-STATEMENT-AND-OBJECTIVES.md)** | **[Problem Statement & Objectives](01-PROBLEM-STATEMENT-AND-OBJECTIVES.md)** | Communicative barriers, non-verbal accessibility, and prototype boundaries. |
| **[02](02-FUNCTIONAL-AND-NONFUNCTIONAL-REQUIREMENTS.md)** | **[Requirements Specification](02-FUNCTIONAL-AND-NONFUNCTIONAL-REQUIREMENTS.md)** | Numbered functional & non-functional requirements categorized via MoSCoW. |
| **[03](03-TECHNOLOGY-STACK.md)** | **[Technology Stack & Dependencies](03-TECHNOLOGY-STACK.md)** | Detailed inventory of Java 21, Jakarta EE 10, Tomcat 10.1, GenAI SDK, and MySQL. |
| **[04](04-SYSTEM-ARCHITECTURE.md)** | **[System Architecture & Design](04-SYSTEM-ARCHITECTURE.md)** | Six-tier layered architecture, component responsibilities, and MVC patterns. |
| **[05](05-APPLICATION-WORKFLOW.md)** | **[Application Workflow](05-APPLICATION-WORKFLOW.md)** | Step-by-step journeys for live webcam streaming, uploads, and UNKNOWN gestures. |
| **[06](06-USER-INTERFACE-AND-SCREENS.md)** | **[User Interface & Screens](06-USER-INTERFACE-AND-SCREENS.md)** | Layout specifications for workspace, viewfinder, result panels, and speech bars. |
| **[07](07-GEMINI-AI-INTEGRATION.md)** | **[Gemini AI Integration](07-GEMINI-AI-INTEGRATION.md)** | GenAI Java SDK (`google-genai`), in-memory `Part.fromBytes()`, and JSON schemas. |
| **[08](08-GESTURE-RECOGNITION-LOGIC.md)** | **[Gesture Recognition & Mapping Logic](08-GESTURE-RECOGNITION-LOGIC.md)** | Visual definitions of the 5 supported signs and deterministic Java mappings. |
| **[09](09-API-ENDPOINT-DESIGN.md)** | **[API Endpoint Design & Contracts](09-API-ENDPOINT-DESIGN.md)** | HTTP specifications for `/api/recognize`, `/api/history`, and `/api/health`. |
| **[10](10-DATABASE-DESIGN.md)** | **[Database Design & Data Access](10-DATABASE-DESIGN.md)** | MySQL DDL schema, HikariCP connection pooling, and `PreparedStatement` DAOs. |
| **[11](11-PROJECT-STRUCTURE.md)** | **[Project Directory Structure](11-PROJECT-STRUCTURE.md)** | Standard Maven WAR layout, package responsibilities, and planned Java classes. |
| **[12](12-SECURITY-AND-PRIVACY.md)** | **[Security & Privacy Architecture](12-SECURITY-AND-PRIVACY.md)** | Server secret isolation, magic byte checks, zero-disk ephemeral processing. |
| **[13](13-ERROR-HANDLING-AND-VALIDATION.md)** | **[Error Handling & Validation](13-ERROR-HANDLING-AND-VALIDATION.md)** | Comprehensive error catalog, exception hierarchy, and fallback mechanisms. |
| **[14](14-TESTING-AND-QUALITY-ASSURANCE.md)** | **[Testing & Quality Assurance](14-TESTING-AND-QUALITY-ASSURANCE.md)** | Test pyramid, master test cases, and planned 120-image accuracy benchmark. |
| **[15](15-IMPLEMENTATION-ROADMAP.md)** | **[Phased Implementation Roadmap](15-IMPLEMENTATION-ROADMAP.md)** | Sequential phases (Phase 0 through Phase 7) with strict verification gates. |
| **[16](16-SETUP-AND-DEPLOYMENT.md)** | **[Setup & Deployment Guide](16-SETUP-AND-DEPLOYMENT.md)** | Step-by-step setup for Java 21, MySQL, Tomcat 10.1, and Windows PowerShell. |
| **[17](17-PROJECT-LIMITATIONS-AND-FUTURE-SCOPE.md)** | **[Limitations & Future Scope](17-PROJECT-LIMITATIONS-AND-FUTURE-SCOPE.md)** | Critical analysis of v1 boundaries and 3-horizon future roadmap. |
| **[18](18-TECHNICAL-DECISIONS.md)** | **[Architecture Decision Records (ADR)](18-TECHNICAL-DECISIONS.md)** | Justifications for Servlets, Gemini Vision, Browser TTS, and zero disk writes. |

---

## 3. Visual Architecture & Diagram Catalog

All architectural diagrams are rendered using standard GitHub-flavored **Mermaid** syntax:

* **[diagrams/system-architecture.md](diagrams/system-architecture.md)**: 
  * Layered Six-Tier Architecture Diagram.
  * MVC Class & Component Relationship Diagram.
* **[diagrams/application-workflow.md](diagrams/application-workflow.md)**: 
  * End-to-End Processing Workflow Flowchart.
  * Decision Logic for Gesture Classification & Confidence Fallback.
* **[diagrams/sequence-diagram.md](diagrams/sequence-diagram.md)**: 
  * Live Webcam-to-Gemini Real-Time Recognition Sequence.
  * Static Image Upload Sequence.
* **[diagrams/database-er-diagram.md](diagrams/database-er-diagram.md)**: 
  * Entity-Relationship (ER) Diagram for `gesturetalk_db`.
  * Table Indexing and Physical Storage Architecture.

---

## 4. Governance & Architectural Adherence Checklist

* [x] **Primary Language**: 100% Core Java 21 LTS backend.
* [x] **No Spring Boot / No Node.js / No Python**: Pure Jakarta EE 10 Servlets on Apache Tomcat 10.1+.
* [x] **Modern GenAI SDK**: Official `com.google.genai:google-genai` Java SDK integrated on the server side.
* [x] **Zero Secret Leakage**: `GEMINI_API_KEY` accessed exclusively through server environment variables.
* [x] **Privacy Preserved**: Ephemeral in-memory image processing; zero permanent disk or database storage.
* [x] **Deterministic Audio**: Natural speech mapped via immutable Java constants; vocalized via browser SpeechSynthesis.
* [x] **Relational Persistence**: MySQL 8.0 audit logging via HikariCP connection pool and JDBC `PreparedStatement`.

