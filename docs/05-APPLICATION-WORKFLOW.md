# GestureTalk AI — Application Workflow

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-05 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Operational & Functional Workflow |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |
| **Companion Diagrams** | [diagrams/application-workflow.md](diagrams/application-workflow.md) • [diagrams/sequence-diagram.md](diagrams/sequence-diagram.md) |

---

## 1. Overview of Operational Workflows

GestureTalk AI supports two primary user interaction flows:
1. **Live Webcam Capture Workflow**: Real-time video preview with instant frame extraction.
2. **Static Image Upload Workflow**: Manual file selection and drag-and-drop for pre-recorded photos.

Both operational workflows converge on the same secure server-side recognition pipeline, where in-memory validation, Gemini classification, Java message mapping, database persistence, and browser-side speech synthesis occur sequentially.

---

## 2. Workflow A: Live Webcam Acquisition & Recognition

```
[User Browser]                      [Apache Tomcat / Java]                    [Google Gemini API]
      │                                       │                                        │
      │ 1. Request Camera Permission          │                                        │
      │────(navigator.mediaDevices)───────────┼────────────────────────────────────────┤
      │                                       │                                        │
      │ 2. Stream to <video>                  │                                        │
      │────(Live Viewfinder 30 FPS)───────────┼────────────────────────────────────────┤
      │                                       │                                        │
      │ 3. Click "Capture Gesture"            │                                        │
      │    [Draw to <canvas> & Blob]          │                                        │
      │                                       │                                        │
      │ 4. HTTP POST /api/recognize           │                                        │
      │────(Multipart: image/jpeg blob)──────>│                                        │
      │                                       │ 5. Validate Image (size, magic bytes)  │
      │                                       │                                        │
      │                                       │ 6. Part.fromBytes() + Prompt           │
      │                                       │────(HTTPS POST /generateContent)──────>│
      │                                       │                                        │
      │                                       │ 7. Return JSON Classification          │
      │                                       │<───({ gesture, confidence, reason })───│
      │                                       │                                        │
      │                                       │ 8. Validate Gesture & Map to Message   │
      │                                       │ 9. Insert into MySQL (gesture_history) │
      │ 10. HTTP 200 JSON Response            │                                        │
      │<───({ gesture, message, audioText })──│                                        │
      │                                       │                                        │
      │ 11. Render Result Card on UI          │                                        │
      │ 12. window.speechSynthesis.speak()    │                                        │
      ▼                                       ▼                                        ▼
```

### Detailed Step-by-Step Breakdown:
1. **User Activation**: The user navigates to the GestureTalk AI web interface (`index.jsp`) and clicks the **"Start Camera"** button.
2. **Permission & Streaming**: The browser requests video permission. Upon approval, the stream is attached to the on-screen `<video>` element, providing real-time visual feedback so the user can frame their hand properly.
3. **Capture Trigger**: The user positions their hand in front of the lens in one of the five target postures and clicks **"Capture Gesture"**.
4. **Canvas Rasterization**: The JavaScript controller captures the current video frame into an off-screen `<canvas>` at its native aspect ratio and invokes `canvas.toBlob(callback, 'image/jpeg', 0.90)`.
5. **Multipart Transmission**: The blob is appended to a `FormData` object with the field name `image` and `sourceType=WEBCAM` and dispatched via `fetch('/api/recognize', { method: 'POST', body: formData })`.
6. **Server Receipt & In-Memory Reading**: Tomcat routes the request to `GestureRecognitionServlet`. The servlet reads the file stream directly into a memory buffer (`byte[]`). No temporary files are written to the server disk.
7. **Security & Header Validation**: `ImageValidationService` inspects the byte buffer to confirm non-empty payload, size <= 5 MB, and valid JPEG/PNG magic numbers.
8. **AI Inference**: `GeminiVisionService` packages the image bytes into `Part.fromBytes()` and dispatches the classification request using `com.google.genai.Client`.
9. **JSON Parsing & Gesture Validation**: Gemini returns structured JSON. The backend extracts the `gesture` string and safely matches it against the Java `GestureType` enum.
10. **Deterministic Message Mapping**: `GestureMappingService` retrieves the authoritative spoken message associated with the recognized enum.
11. **Persistence**: `RecognitionLogDAO` records the transaction details in MySQL `gesture_history`.
12. **Client Presentation & Vocalization**: The JSON response arrives at the browser. The UI updates with the gesture label, confidence badge, and explanatory note. The browser immediately triggers `window.speechSynthesis.speak()` to vocalize the text through the user's speakers or headphones.

---

## 3. Workflow B: Static Image Upload Workflow

1. **File Selection**: The user drags an existing photo into the drop zone or selects a file through the native file browser.
2. **Client-Side Pre-Validation**: JavaScript checks that the file size is under 5 MB and the extension is `.jpg`, `.jpeg`, `.png`, or `.webp`. A thumbnail preview renders immediately on screen.
3. **Upload Trigger**: The user clicks **"Analyze Uploaded Image"**.
4. **Transmission**: `FormData` containing the file and `sourceType=UPLOAD` is dispatched to `POST /api/recognize`.
5. **Processing Pipeline**: Steps 6 through 12 from Workflow A execute identically, ensuring consistent validation, AI inference, and database logging.

---

## 4. The UNKNOWN Gesture Workflow

When an image contains ambiguous hand configurations, no hand, multiple conflicting hands, or postures outside the five defined categories, the system executes the deterministic **UNKNOWN Workflow**:

```
                       [Captured Frame]
                              │
                              ▼
                   [Gemini Vision Engine]
                              │
               Does the image clearly match one of:
         THUMBS_UP, THUMBS_DOWN, OPEN_PALM, V_SIGN, INDEX_POINTING?
                              │
                   ┌──────────┴──────────┐
                  YES                    NO (or confidence < 0.60)
                   │                     │
                   ▼                     ▼
          [Valid Gesture Code]       [UNKNOWN]
                   │                     │
                   ▼                     ▼
      Specific Spoken Sentence    "Gesture not recognized. Please try again."
```

* **Backend Action**: The server identifies the `UNKNOWN` token, assigns confidence, records the reason in the audit log, and maps it to the polite instructional sentence: *"Gesture not recognized. Please try again."*
* **Frontend Presentation**: The UI renders a warning alert (yellow/grey badge), explains the ambiguity, and vocalizes the instructional sentence to prompt the user to re-attempt the gesture with better framing.

---

## 5. Error Paths & Handling Protocols

```
┌─────────────────────────────────┬───────────────────────────────────┬──────────────────────────────────────────────┐
│ Error Condition                 │ Detection Point                   │ System Recovery & User Message               │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ Camera Permission Denied        │ Browser MediaDevices API          │ Alert banner: "Camera access was denied.     │
│                                 │ (navigator.mediaDevices)          │ Please enable permissions or use upload."    │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ Image Size Exceeds 5 MB         │ Client JS & Server Servlet Guard  │ HTTP 400 Bad Request: "Uploaded image        │
│                                 │ (ImageValidationService)          │ exceeds the maximum allowable limit of 5MB." │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ Corrupted / Non-Image File      │ Server Magic Bytes Validation     │ HTTP 415 Unsupported Media: "Uploaded file   │
│                                 │ (ImageValidationService)          │ is not a valid JPEG, PNG, or WebP image."    │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ Gemini API Rate Limit / Quota   │ Server GeminiVisionService        │ HTTP 503 Service Unavailable: "AI service is │
│ (HTTP 429 / RESOURCE_EXHAUSTED) │ (Google GenAI Client Exception)   │ busy. Please retry in a few moments."        │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ Missing GOOGLE_API_KEY          │ Server Startup / Request Time     │ HTTP 500 Internal Server Error: "Server AI   │
│                                 │ (AppConfig / GeminiVisionService) │ service is unconfigured. Contact admin."     │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ Database Connection Offline     │ Data Access Layer                 │ Non-blocking: Server logs DB error, returns  │
│                                 │ (RecognitionLogDAO)               │ recognition to user, warns DB unrecorded.    │
├─────────────────────────────────┼───────────────────────────────────┼──────────────────────────────────────────────┤
│ SpeechSynthesis Unavailable     │ Browser JavaScript                │ Fallback: Display textual notification       │
│                                 │ (window.speechSynthesis check)    │ stating audio synthesis is unsupported.      │
└─────────────────────────────────┴───────────────────────────────────┴──────────────────────────────────────────────┘
```

---

## 6. Document Cross-References

* Visual Sequence Diagram: [diagrams/sequence-diagram.md](diagrams/sequence-diagram.md)
* Visual Workflow Flowchart: [diagrams/application-workflow.md](diagrams/application-workflow.md)
* API payload structure: [09-API-ENDPOINT-DESIGN.md](09-API-ENDPOINT-DESIGN.md)
* Comprehensive error matrix: [13-ERROR-HANDLING-AND-VALIDATION.md](13-ERROR-HANDLING-AND-VALIDATION.md)

