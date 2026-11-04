# GestureTalk AI

> **An AI-powered hand gesture recognition and communication assistant web application.**

[![Java 21](https://img.shields.io/badge/Java-21%20LTS-orange.svg)](https://www.oracle.com/java/)
[![Jakarta EE 10](https://img.shields.io/badge/Jakarta%20EE-10-blue.svg)](https://jakarta.ee/)
[![Apache Tomcat](https://img.shields.io/badge/Tomcat-10.1%2B-yellow.svg)](https://tomcat.apache.org/)
[![Google Gemini API](https://img.shields.io/badge/Gemini%20API-Multimodal%20Vision-purple.svg)](https://ai.google.dev/)
[![MySQL](https://img.shields.io/badge/MySQL-8.0%2B-00758F.svg)](https://www.mysql.com/)
[![Tests Passing](https://img.shields.io/badge/Tests-87%20Passed%20(100%25)-success.svg)](docs/TESTING.md)
[![Release](https://img.shields.io/badge/Release-v1.0.0%20Production-green.svg)](docs/15-IMPLEMENTATION-ROADMAP.md)

---

## 📖 Overview

**GestureTalk AI** is an advanced assistive communication web platform designed for non-verbal individuals, post-operative patients, and users in noise-restricted or sterile environments. The application captures webcam frames or static image uploads, classifies predefined static hand gestures via the **Google Gemini Multimodal Vision API** using the official **Google GenAI Java SDK**, maps recognized gestures deterministically to meaningful natural language sentences using Java business logic, and articulates them audibly in the browser via the **Web SpeechSynthesis API**.

The project is built as an **academic Advanced Internet Programming showcase** emphasizing:
* **Java 21 LTS** as the primary backend and domain logic language.
* **Jakarta EE 10 (Servlets 6.0 & JSP 3.1)** on **Apache Tomcat 10.1+** (no Spring Boot).
* **Zero Disk Storage**: In-memory ephemeral image processing (`Part.fromBytes()`) ensuring absolute user privacy.
* **Credential Isolation**: Gemini API keys are isolated on the server and never exposed to the client.
* **Relational Persistence**: MySQL audit logging via HikariCP connection pooling and parameterized JDBC `PreparedStatement` queries.

---

## ✋ Supported Gestures & Mapped Speech

The initial release recognizes exactly **five predefined static gestures** and provides an explicit **UNKNOWN** fallback:

| Gesture Token | Posture Description | Deterministic Output Sentence |
| :--- | :--- | :--- |
| `THUMBS_UP` | Closed fist, thumb straight up | *"Yes, that's correct."* |
| `THUMBS_DOWN` | Closed fist, thumb straight down | *"No, that's incorrect."* |
| `OPEN_PALM` | Flat open hand, 5 digits extended | *"Please stop or wait."* |
| `V_SIGN` | Index and middle fingers extended in a V | *"Peace and greetings."* |
| `INDEX_POINTING` | Single index finger pointing | *"Please look there."* |
| `UNKNOWN` | Ambiguous, missing or unsupported gesture | *"Gesture not recognized. Please try again."* |

---

## 🏗 System Architecture

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

## 📚 Core System Documentation

Comprehensive architecture, integration, database, testing, and operations documentation is maintained in the [`docs/`](docs/) directory:

| Document | Description |
| :--- | :--- |
| **[Architecture Specification](docs/ARCHITECTURE.md)** | End-to-end MVC architecture, Jakarta Servlets, sequence flows, and component interactions. |
| **[AI Integration Specification](docs/AI_INTEGRATION.md)** | Google GenAI Java SDK, Gemini 2.5 Flash, structured prompts, and 0.75 confidence thresholding. |
| **[Database Architecture](docs/DATABASE.md)** | MySQL 8.0+ schema, HikariCP connection pooling, PreparedStatement audit logging, and H2 testing. |
| **[Testing & QA Specification](docs/TESTING.md)** | 87 automated tests (100% pass), security boundaries, and 30-sample empirical accuracy benchmark. |
| **[Setup & Deployment Guide](docs/SETUP.md)** | Step-by-step installation, environment variables, database initialization, and Tomcat 10.1 WAR deployment. |

### Supplementary Architectural Documents

| Document | Description |
| :--- | :--- |
| [00. Project Overview](docs/00-PROJECT-OVERVIEW.md) | Executive summary, domain, capabilities, and target environments. |
| [01. Problem Statement & Objectives](docs/01-PROBLEM-STATEMENT-AND-OBJECTIVES.md) | Accessibility challenges and prototype vs. full sign-language boundary. |
| [02. Requirements Specification](docs/02-FUNCTIONAL-AND-NONFUNCTIONAL-REQUIREMENTS.md) | Numbered functional & non-functional requirements categorized via MoSCoW. |
| [03. Technology Stack](docs/03-TECHNOLOGY-STACK.md) | Verified dependency versions and architectural selection rationale. |
| [04. System Architecture](docs/04-SYSTEM-ARCHITECTURE.md) | Six-tier layered architecture, component responsibilities, and MVC design. |
| [05. Application Workflow](docs/05-APPLICATION-WORKFLOW.md) | Webcam capture, image upload, and fallback workflows. |
| [06. UI & Screen Specifications](docs/06-USER-INTERFACE-AND-SCREENS.md) | Detailed specifications for the responsive workspace and speech controls. |
| [07. Gemini AI Integration](docs/07-GEMINI-AI-INTEGRATION.md) | Official GenAI Java SDK integration, prompt design, and JSON schemas. |
| [08. Gesture Recognition Logic](docs/08-GESTURE-RECOGNITION-LOGIC.md) | Visual definitions of the 5 supported signs and deterministic Java mappings. |
| [09. API Endpoint Design](docs/09-API-ENDPOINT-DESIGN.md) | Planned HTTP contracts for `/api/recognize`, `/api/history`, and `/api/health`. |
| [10. Database Design](docs/10-DATABASE-DESIGN.md) | MySQL schema (`gesturetalk_db`), field dictionaries, and HikariCP DAOs. |
| [11. Project Directory Structure](docs/11-PROJECT-STRUCTURE.md) | Standard Maven WAR layout and package responsibilities. |
| [12. Security & Privacy](docs/12-SECURITY-AND-PRIVACY.md) | API key isolation, magic byte checks, and zero-disk ephemeral processing. |
| [13. Error Handling & Validation](docs/13-ERROR-HANDLING-AND-VALIDATION.md) | Error code catalog, exception hierarchy, and fallback mechanisms. |
| [14. Testing & Quality Assurance](docs/14-TESTING-AND-QUALITY-ASSURANCE.md) | Master test catalog and planned 120-image accuracy evaluation benchmark. |
| [15. Phased Implementation Roadmap](docs/15-IMPLEMENTATION-ROADMAP.md) | Phase 0 through Phase 7 sequential implementation roadmap. |
| [16. Setup & Deployment Guide](docs/16-SETUP-AND-DEPLOYMENT.md) | Runbook for Java 21, MySQL, Tomcat 10.1, and Windows PowerShell. |
| [17. Limitations & Future Scope](docs/17-PROJECT-LIMITATIONS-AND-FUTURE-SCOPE.md) | Critical analysis of v1 boundaries and 3-horizon product roadmap. |
| [18. Technical Decisions (ADR)](docs/18-TECHNICAL-DECISIONS.md) | Architecture Decision Records for all fundamental architectural choices. |
| [Mermaid Diagrams](docs/diagrams/) | Visual diagrams for architecture, workflows, sequences, and ER model. |

---

## 🛠 Getting Started

### Prerequisites
* **Java 21 LTS**
* **Apache Maven 3.9+**
* **MySQL 8.0+**
* **Apache Tomcat 10.1+**
* **Google AI Studio API Key** (`GOOGLE_API_KEY`)

### Quick Setup
1. Clone the repository:
   ```bash
   git clone https://github.com/anish-kumar03/Gesture-Talk-AI.git
   cd Gesture-Talk-AI
   ```
2. Configure your API key using Windows User Environment Variables (Recommended — never stored in files):
   ```powershell
   [System.Environment]::SetEnvironmentVariable('GOOGLE_API_KEY', 'your_api_key_from_google_ai_studio', 'User')
   ```
3. (Optional) Configure the preferred vision model (defaults to `gemini-3.8-flash`):
   ```powershell
   [System.Environment]::SetEnvironmentVariable('GEMINI_MODEL', 'gemini-3.8-flash', 'User')
   ```
   *(To verify which models are active for your API key, query `https://generativelanguage.googleapis.com/v1beta/models?key=$env:GOOGLE_API_KEY`)*
4. Initialize the database schema:
   ```bash
   mysql -u root -p < src/main/resources/db/schema.sql
   ```
4. Build the WAR package:
   ```bash
   mvn clean package
   ```
5. Deploy `target/gesturetalk-ai.war` to Apache Tomcat's `webapps/` folder.

---

## 📄 License & Academic Attribution
Developed as an Advanced Internet Programming project adhering to strict academic integrity, zero-disk user privacy, and pure Jakarta EE software architecture.

