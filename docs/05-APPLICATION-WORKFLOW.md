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

GestureTalk AI supports three operational flows:
1. **Live Real-Time Webcam Recognition Workflow (Primary)**: Continuous client-side video inference loop running on Google MediaPipe WebAssembly with skeletal landmark tracking, consecutive-frame prediction smoothing, and instant speech articulation.
2. **Snapshot Image Capture Workflow (Secondary / Optional)**: In-memory single-frame snapshot sent to Google Gemini Vision for deep multimodal diagnostic inspection.
3. **Static Image Upload Workflow**: Manual file selection and drag-and-drop for pre-recorded photos analyzed via the Java servlet pipeline.

---

## 2. Workflow A: Real-Time MediaPipe Webcam Recognition (Primary)

```
[User Browser & MediaPipe WASM]               [Apache Tomcat / Jakarta EE]             [MySQL Database]
       │                                                    │                                  │
       │ 1. Start Camera (30+ FPS)                          │                                  │
       │    requestAnimationFrame Inference Loop            │                                  │
       │                                                    │                                  │
       │ 2. Detect 21 Hand Landmarks & Draw Overlay         │                                  │
       │    Recognize Gesture (e.g. Thumb_Up: 0.94)         │                                  │
       │                                                    │                                  │
       │ 3. Apply Prediction Smoothing                      │                                  │
       │    (4 Consecutive Frames >= 0.70 Confidence)       │                                  │
       │    (Enforce Cooldown & Duplicate Suppression)      │                                  │
       │                                                    │                                  │
       │ 4. HTTP POST /api/gesture-result                   │                                  │
       │────({ gestureCode: "THUMBS_UP", confidence: 0.94 })>                                  │
       │                                                    │ 5. Validate Enum & Confidence    │
       │                                                    │ 6. Map Speech (MessageMapper)    │
       │                                                    │ 7. Insert into gesture_history ──>
       │                                                    │                                  │
       │ 8. HTTP 200 JSON Response                          │                                  │
       │<───({ gestureCode, mappedMessage, autoSpeak: true })                                  │
       │                                                    │                                  │
       │ 9. Render Result & Live HUD                        │                                  │
       │ 10. window.speechSynthesis.speak()                 │                                  │
       ▼                                                    ▼                                  ▼
```

### Detailed Step-by-Step Breakdown:
1. **User Activation**: The user navigates to `index.jsp` and clicks **"Start Camera"**.
2. **WebAssembly Initialization**: `@mediapipe/tasks-vision` loads locally from `/static/vendor/mediapipe/` (with CDN fallback) and initializes the `gesture_recognizer.task` model.
3. **Continuous Tracking Loop**: `requestAnimationFrame` feeds frames into `recognizer.recognizeForVideo(video, timestamp)`.
4. **Skeletal Landmark Overlay**: 21 3D hand landmarks are drawn dynamically onto `#landmarkCanvas` in mirrored alignment with the user's hand.
5. **Smoothing & Stability Verification**: Before triggering speech, the system requires 4 consecutive identical frames satisfying the $\ge 0.70$ confidence threshold, preventing single-frame jitter or false triggers.
6. **Backend Persistence & Speech Authorization**: The confirmed token is posted to `/api/gesture-result`. Java validates the payload, applies deterministic sentence mapping, logs the record to MySQL, and authorizes vocalization.
7. **Immediate Vocalization**: The browser articulates the sentence using the Web SpeechSynthesis API. Re-triggering the same gesture is debounced for 2.5 seconds to prevent audio spamming while the hand is held stationary.
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

