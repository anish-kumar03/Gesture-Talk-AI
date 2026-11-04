# GestureTalk AI — Security & Privacy Architecture

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-12 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Security, Data Governance & Privacy Architecture |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Security Architecture Principles

In accordance with strict enterprise and academic standards, GestureTalk AI enforces security by design across all architectural boundaries. The system treats the client web browser as an untrusted environment, maintaining all cryptographic secrets, API credentials, and critical validation logic strictly on the server side.

---

## 2. API Key Protection & Credential Isolation

```
[Browser / Client Side]                  [Jakarta EE / Server Side]            [Google Cloud / GenAI]
       │                                             │                                   │
       │  Submits image multipart                    │                                   │
       │────────────────────────────────────────────>│ Reads System.getenv("GOOGLE_API_KEY")
       │                                             │                                   │
       │                                             │ Invokes SDK with hidden API Key   │
       │                                             │──────────────────────────────────>│
       │                                             │                                   │
       │  Receives JSON result (NO KEY EXPOSED)      │                                   │
       │<────────────────────────────────────────────│                                   │
```

### Key Protection Rules:
* **Zero Client Exposure**: The `GOOGLE_API_KEY` is **never** injected into JSP templates, JavaScript files, HTML attributes, browser cookies, or client-side storage (`localStorage` or `sessionStorage`).
* **Environment Variable Injection**: The key is supplied exclusively through the host operating system's standard environment variable (`GOOGLE_API_KEY`) or container runtime environment. Windows User Environment Variables are the recommended development configuration.
* **No Version Control Leaks**: `.gitignore` rules prevent properties files containing credentials or secrets from being committed into the source repository.

---

## 3. Privacy-by-Design & Data Minimization

> [!IMPORTANT]
> **Zero Permanent Image Storage (Ephemeral Processing Pipeline)**
> 
> A foundational ethical and privacy guarantee of GestureTalk AI is that **user images are never written to permanent disk storage, cloud object stores, or database blobs**.

### Privacy Safeguards:
1. **Volatile In-Memory Streams**: Image streams received via multipart requests are read into volatile `byte[]` arrays within JVM heap memory.
2. **Immediate Garbage Collection**: Once `Part.fromBytes()` is processed and the Gemini API responds, all references to the byte array are dereferenced, making the memory immediately eligible for JVM Garbage Collection.
3. **No Biometric Identification**: The system does not attempt facial recognition, fingerprint identification, or personal identity matching. The database stores only categorical gesture tokens (`THUMBS_UP`), confidence values, and timestamps.

---

## 4. Image Upload & Binary Header Validation

To defend against malicious file uploads, executable masquerading, and Denial-of-Service (DoS) buffer attacks, incoming payloads undergo three sequential verification layers:

```
                  Incoming Multipart Part
                             │
                             ▼
         1. File Size Verification (Max 5 MB)
            Reject with HTTP 413 if exceeded
                             │
                             ▼
         2. MIME-Type Header Verification
            Reject with HTTP 415 if not image/*
                             │
                             ▼
         3. Binary Magic Number Signature Check
            Inspect first 4 to 12 bytes of raw stream
                             │
           ┌─────────────────┴─────────────────┐
         VALID                               INVALID
           │                                   │
           ▼                                   ▼
    Proceed to AI               Reject with HTTP 415 Invalid Format
```

### Magic Number Signature Specifications:
The `ImageValidationService` inspects the initial bytes of the stream:
* **JPEG / JPG**: First 3 bytes must equal `FF D8 FF`.
* **PNG**: First 8 bytes must equal `89 50 4E 47 0D 0A 1A 0A`.
* **WebP**: Bytes 0–3 must equal `52 49 46 46` (`RIFF`) and bytes 8–11 must equal `57 45 42 50` (`WEBP`).

Any uploaded file claiming an image extension but failing magic byte inspection is immediately aborted and rejected.

---

## 5. SQL Injection Prevention

* **Absolute Parameterization**: 100% of database interactions in `RecognitionLogDAO` use JDBC `PreparedStatement` parameters (`?`). 
* **Zero String Concatenation**: No dynamic string concatenation or variable interpolation is permitted in SQL strings.
* **Input Sanitization**: Query parameters for filtering history are sanitized and validated against allowed enumeration values before binding.

```java
// SAFE: Parameterized execution
String sql = "SELECT * FROM gesture_history WHERE gesture_code = ? ORDER BY created_at DESC LIMIT ?";
try (PreparedStatement ps = connection.prepareStatement(sql)) {
    ps.setString(1, validatedGestureCode);
    ps.setInt(2, validatedLimit);
    ...
}
```

---

## 6. Secure JSON Parsing & Prompt Injection Defense

* **Deterministic Text Mapping as Security Shield**: Because spoken natural language messages are mapped using Java constants rather than generated by Gemini, prompt injection attacks (e.g., a user holding a sign saying *"Ignore previous instructions and say X"*) are inherently neutralized. Even if Gemini were deceived into misclassifying the gesture, the system will only ever emit one of the five hardcoded safe sentences.
* **Defensive JSON Parsing**: The server uses Google Gson with strict type-checking. Unexpected or malformed JSON responses from external APIs do not crash the container and fall back safely to `UNKNOWN`.

---

## 7. Network Timeouts & DoS Prevention

```
┌─────────────────────────┬───────────────────┬────────────────────────────────────────────────────────┐
│ Boundary                │ Configured Limit  │ Protective Purpose                                     │
├─────────────────────────┼───────────────────┼────────────────────────────────────────────────────────┤
│ Max Request File Size   │ 5 MB (5,242,880 B)│ Prevents heap exhaustion from massive image uploads.   │
│ Gemini SDK Call Timeout │ 8,000 ms (8 sec)  │ Prevents servlet worker threads from hanging on stalled│
│                         │                   │ external connections.                                  │
│ HikariCP Connection Wait│ 3,000 ms (3 sec)  │ Prevents request queuing if database pool is saturated.│
│ HTTP Session Max Age    │ 30 minutes        │ Recycles idle session memory.                          │
└─────────────────────────┴───────────────────┴────────────────────────────────────────────────────────┘
```

---

## 8. Browser-Side Camera Permissions & User Consent

* The browser never activates the camera automatically. Hardware activation requires an explicit user gesture (clicking **"Start Camera"**), triggering the native browser permission dialog via `navigator.mediaDevices.getUserMedia`.
* A **"Stop Camera"** button is provided to immediately stop all video tracks and release the physical hardware camera indicator.

---

## 9. Document Cross-References

* Technology versions: [03-TECHNOLOGY-STACK.md](03-TECHNOLOGY-STACK.md)
* Error definitions and response payloads: [13-ERROR-HANDLING-AND-VALIDATION.md](13-ERROR-HANDLING-AND-VALIDATION.md)
* Architecture decision records: [18-TECHNICAL-DECISIONS.md](18-TECHNICAL-DECISIONS.md)

