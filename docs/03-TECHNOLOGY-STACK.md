# GestureTalk AI — Technology Stack & Dependencies

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-03 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Technical Specification |
| **Source of Truth** | `pom.xml` (Project Root) |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Technology Stack Overview

GestureTalk AI utilizes a modern, robust, and standards-compliant technology stack specifically chosen to fulfill academic Advanced Internet Programming requirements while harnessing modern multimodal artificial intelligence. 

The application rejects heavyweight monolithic application frameworks (such as Spring Boot) and unvetted micro-frameworks (such as Python FastAPI/Flask) in favor of **pure Jakarta EE 10 standards** running on **Apache Tomcat 10.1** with **Java 21 LTS**.

---

## 2. Definitive Technology Specification Table

The versions listed below are directly sourced from the project's root [`pom.xml`](../pom.xml):

```
┌──────────────────────────────┬──────────────────┬─────────────────────────────┬────────────────────────────────────────────────────────┬──────────────────────┐
│ Technology / Library         │ Configured Ver.  │ Architectural Role          │ Technical Justification / Selection Rational          │ Component Usage      │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Java Platform (JDK)          │ 21 LTS           │ Core Language & Runtime     │ Modern LTS release offering virtual threads, enhanced  │ Backend Core, Model, │
│                              │ (<release>21)    │                             │ pattern matching, records, and strong memory safety.   │ Service, Controller  │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Apache Tomcat                │ 10.1.x           │ Servlet Container / Server  │ Industry-standard reference implementation supporting   │ Web Runtime Host,    │
│                              │ (Jakarta EE 10)  │                             │ the Jakarta Servlet 6.0 and JSP 3.1 specifications.    │ HTTP Request Handler │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Jakarta Servlet API          │ 6.0.0            │ HTTP Controller Base        │ Pure Jakarta EE 10 standard (`jakarta.servlet.*`).     │ Controller Layer     │
│ (`jakarta.servlet-api`)      │ (scope: provided)│                             │ Lightweight, zero overhead, highly transparent.        │ (`*Servlet.java`)    │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Jakarta JSP API              │ 3.1.0            │ Server-Side View Generation │ Standards-compliant templating for dynamic view        │ Presentation Layer   │
│ (`jakarta.servlet.jsp-api`)  │ (scope: provided)│                             │ rendering and clean separation of HTML markup.         │ (`*.jsp`)            │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Jakarta JSTL (API & GlassFish│ API: 3.0.0       │ JSP Standard Tag Library    │ Standard declarative tags for iterating audit records, │ View Rendering       │
│ Implementation)              │ Impl: 3.0.1      │                             │ conditional rendering, and formatting on JSP views.    │ (`*.jsp`)            │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Google GenAI Java SDK        │ 0.1.1            │ Multimodal Computer Vision  │ Official Google SDK (`com.google.genai:google-genai`)  │ Service Layer        │
│ (`google-genai`)             │                  │ Integration                 │ for idiomatic Java integration with Gemini models.     │ (`GeminiService`)    │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ MySQL Database               │ 8.0 / 8.4        │ Relational Database         │ ACID-compliant enterprise RDBMS for reliable audit     │ Database Tier        │
│                              │                  │                             │ logging, relational data integrity, and analytics.     │ (`gesturetalk_db`)   │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ MySQL Connector/J            │ 8.4.0            │ JDBC Driver                 │ Official Type 4 JDBC driver providing secure, high-    │ Data Access Layer    │
│ (`mysql-connector-j`)        │                  │                             │ performance communication between Java and MySQL.      │ (`DatabaseConnection`)│
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ HikariCP                     │ 5.1.0            │ JDBC Connection Pool        │ Ultra-fast, zero-overhead production connection pool   │ Data Access Layer    │
│ (`HikariCP`)                 │                  │                             │ preventing socket starvation and managing DB health.   │ (`DatabaseConnection`)│
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Google Gson                  │ 2.11.0           │ JSON Serialization Engine   │ High-performance Java object ↔ JSON parsing for API    │ Service & Controller │
│ (`gson`)                     │                  │                             │ requests, responses, and Gemini schema mapping.        │ (`JsonUtils`)        │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ SLF4J & Logback              │ SLF4J: 2.0.16    │ Application Logging         │ Standard enterprise logging facade and high-speed      │ Entire Application   │
│ (`logback-classic`)          │ Logback: 1.5.12  │                             │ backend with fine-grained log-level control.           │                      │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ JUnit 5                      │ 5.11.3           │ Unit Testing Framework      │ Modern Java unit testing framework for test-driven     │ Test Tier            │
│ (`junit-jupiter`)            │ (scope: test)    │                             │ verification of services, DAOs, and utilities.         │ (`*Test.java`)       │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Mockito                      │ 5.14.2           │ Mocking Framework           │ Enables isolated unit testing of controllers and       │ Test Tier            │
│ (`mockito-core`, `jupiter`)  │ (scope: test)    │                             │ services without triggering real network/DB calls.     │ (`*Test.java`)       │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ H2 Database Engine           │ 2.3.232          │ In-Memory Test Database     │ Enables automated Maven unit and integration tests     │ Test Tier            │
│ (`h2`)                       │ (scope: test)    │                             │ to run without requiring a live MySQL server running.  │ (`*DAOTest.java`)    │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Bootstrap                    │ 5.3.x            │ Responsive CSS Framework    │ Clean, accessible, modern UI component framework with  │ Client UI Layout     │
│                              │ (CDN)            │                             │ responsive grid, modals, badges, and utility classes.  │ (`index.jsp`)        │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Vanilla JavaScript (ES6+)    │ ECMAScript 2022+ │ Browser Functionality       │ Client camera capture, API fetch invocations, and      │ Client Web Browser   │
│                              │ Native           │                             │ event handling without bloated third-party frameworks. │ (`app.js`)           │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ HTML5 MediaDevices API       │ W3C Standard     │ Webcam Hardware Interface   │ Standardized browser access to device cameras via      │ Client Web Browser   │
│                              │ Native           │                             │ `navigator.mediaDevices.getUserMedia()`.               │ (`webcam.js`)        │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ HTML5 Canvas API             │ W3C Standard     │ In-Memory Frame Rasterizer  │ Captures instant frame snapshots from `<video>` and    │ Client Web Browser   │
│                              │ Native           │                             │ converts pixel buffers to JPEG Blobs for upload.       │ (`webcam.js`)        │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Web Speech API               │ W3C Standard     │ Text-to-Speech Engine       │ Client-side vocalization via `SpeechSynthesis` without │ Client Web Browser   │
│ (`SpeechSynthesis`)          │ Native           │                             │ external TTS API latency or billing overhead.          │ (`tts.js`)           │
├──────────────────────────────┼──────────────────┼─────────────────────────────┼────────────────────────────────────────────────────────┼──────────────────────┤
│ Apache Maven                 │ 3.9.x            │ Build Automation & Package  │ Standard Java build tool managing dependencies,        │ Build & Deployment   │
│                              │ Standard         │ compilation, testing, and standard WAR packaging.      │ (`pom.xml`)          │
└──────────────────────────────┴──────────────────┴─────────────────────────────┴────────────────────────────────────────────────────────┴──────────────────────┘
```

---

## 3. In-Depth Component Analysis & Selection Rationale

### 3.1 Why Core Java 21, Servlets, and JSP instead of Spring Boot
In enterprise academic computing, students and analysts must understand the raw mechanisms of HTTP transactions, servlet life cycles (`init`, `service`, `destroy`), session management, and manual inversion of control. 
* **Spring Boot Overhead**: A typical Spring Boot application introduces 40+ transitive dependencies, hides request dispatching behind reflection magic, and consumes 300MB+ of heap memory on startup.
* **Jakarta Servlets**: Operating directly with `jakarta.servlet.http.HttpServlet` provides total transparency, minimal memory footprint (<30MB heap), instant container startup times, and direct compliance with the Java Community Process (JCP) enterprise standards.

### 3.2 Why Official Google GenAI Java SDK (`com.google.genai`)
Prior to the release of `com.google.genai:google-genai`, developers were forced to either manually construct fragile `java.net.http.HttpClient` REST payloads or import monolithic Google Cloud Vertex AI client libraries designed for massive GCP architectures.
* The official GenAI SDK provides an idiomatic Java client (`Client client = new Client();` or `Client.builder().apiKey(key).build();`).
* It natively supports in-memory image parts (`Part.fromBytes(imageBytes, "image/jpeg")`), structured output enforcement, and zero-disk streaming.

### 3.3 Why JDBC and HikariCP instead of Hibernate / JPA
Object-Relational Mapping (ORM) frameworks like Hibernate introduce severe complexity, lazy-loading bugs, and hidden N+1 query performance traps for simple audit logging applications.
* GestureTalk AI's persistence needs are focused on fast, predictable insertions and chronological queries.
* Raw JDBC `PreparedStatement` combined with **HikariCP** connection pooling delivers microsecond-level query execution, crystal-clear SQL parameterization, and zero ORM overhead.

### 3.4 Why Browser SpeechSynthesis instead of Server-Side TTS APIs
Many architectures route text-to-speech through external cloud providers (e.g., Google Cloud Text-to-Speech or Amazon Polly), returning an MP3 audio stream to the client.
* **Network & Cost Waste**: Routing audio bytes back over HTTP adds 300–800ms of latency and consumes billable cloud quotas.
* **Zero-Latency Native Voice**: Modern browsers (Chrome, Edge, Safari, Firefox) bundle rich, high-quality local neural and system voices accessible instantly through `window.speechSynthesis.speak()`. This yields zero bandwidth consumption, zero added cost, and immediate audio articulation.

---

## 4. Build Configuration & Dependency Alignment

Inspection of the project configuration confirms:
* **Source/Target**: Java 21 (`<maven.compiler.release>21</maven.compiler.release>`)
* **Packaging**: `war` (`<packaging>war</packaging>`)
* **Provided Dependencies**: `jakarta.servlet-api` and `jakarta.servlet.jsp-api` are correctly scoped as `<scope>provided</scope>` so they are supplied by Apache Tomcat at runtime rather than bundled in `WEB-INF/lib`.
* **Testing Capabilities**: `h2` database engine is included in `<scope>test</scope>` to enable self-contained Maven build verification even on environments where MySQL is not yet configured.

---

## 5. Document Cross-References

* System architecture layers: [04-SYSTEM-ARCHITECTURE.md](04-SYSTEM-ARCHITECTURE.md)
* Setup and environment installation: [16-SETUP-AND-DEPLOYMENT.md](16-SETUP-AND-DEPLOYMENT.md)
* Architecture decision records: [18-TECHNICAL-DECISIONS.md](18-TECHNICAL-DECISIONS.md)

