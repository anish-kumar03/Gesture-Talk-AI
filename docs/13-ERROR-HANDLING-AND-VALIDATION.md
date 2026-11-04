# GestureTalk AI — Error Handling & Validation Architecture

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-13 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Reliability, Fault Tolerance & Exception Architecture |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Error Handling Philosophy

GestureTalk AI implements a **defense-in-depth error handling strategy** across all client and server boundaries. The core objective is **graceful degradation**: unexpected network interruptions, API timeouts, or unclassifiable images must never result in blank screens, uncaught 500 stack traces, or silent application freezes.

---

## 2. Standardized Error Response Contract

All backend errors emitted across the `/api/*` endpoints conform to a uniform, machine-readable JSON structure:

```json
{
  "success": false,
  "errorCode": "ERROR_CATEGORY_CODE",
  "message": "Human-readable description suitable for display or logging.",
  "timestamp": "2026-11-03T14:30:00Z"
}
```

---

## 3. Comprehensive Error Catalog & Resolution Matrix

```
┌───────────────────────┬────────────────────────────┬─────────────┬────────────────────────────────────────────────────────┐
│ Error Category        │ Error Code Identifier      │ HTTP Status │ System Resolution & User Feedback                      │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Camera Permission     │ CLIENT_CAMERA_DENIED       │ N/A (Client)│ Display alert: "Camera permission denied. Allow camera │
│                       │                            │             │ access in browser settings or use manual upload."      │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Missing Camera Device │ CLIENT_NO_CAMERA           │ N/A (Client)│ Display alert: "No webcam detected on this device.     │
│                       │                            │             │ Please use the image upload option."                   │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Empty Request Payload │ EMPTY_IMAGE_PAYLOAD        │ 400 Bad Req │ Return JSON error: "No image file detected in request. │
│                       │                            │             │ Please capture or select an image."                    │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Payload Size Exceeded │ FILE_TOO_LARGE             │ 413 Too Lrg │ Return JSON error: "Uploaded image exceeds the 5MB     │
│                       │                            │             │ limit. Please upload a smaller photo."                 │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Corrupt / Bad Format  │ INVALID_IMAGE_SIGNATURE    │ 415 Unsupp. │ Return JSON error: "File binary header does not match  │
│                       │                            │             │ valid JPEG, PNG, or WebP signatures."                  │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ No Hand in Image      │ NO_HAND_DETECTED           │ 200 OK      │ Categorized as UNKNOWN: Speaks "Gesture not recognized.│
│                       │                            │             │ Please ensure your hand is visible to the camera."     │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Ambiguous Hand Sign   │ AMBIGUOUS_GESTURE          │ 200 OK      │ Categorized as UNKNOWN: Speaks "Gesture not recognized.│
│                       │                            │             │ Please try again."                                     │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Low Confidence (<0.60)│ LOW_CONFIDENCE_FALLBACK    │ 200 OK      │ Coerced to UNKNOWN: Speaks "Gesture not recognized.    │
│                       │                            │             │ Please frame your hand clearly."                       │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Missing API Key       │ AI_CONFIG_ERROR            │ 500 Int Err │ Return JSON error: "Server AI configuration error.     │
│                       │                            │             │ Contact administrator to set GOOGLE_API_KEY."         │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Gemini Quota Exceeded │ AI_RATE_LIMIT              │ 503 Unavail │ Return JSON error: "AI service quota reached.          │
│ (HTTP 429)            │                            │             │ Please wait 5 seconds before retrying."                │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Gemini API Timeout    │ AI_TIMEOUT                 │ 504 Gateway │ Return JSON error: "AI service timed out (>8s).        │
│                       │                            │             │ Please retry with a lighter image."                    │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Database Offline      │ DATABASE_UNAVAILABLE       │ 200 OK (Deg)│ Log warning to server logs; return gesture result to   │
│                       │                            │             │ user without saving to history. Non-blocking.          │
├───────────────────────┼────────────────────────────┼─────────────┼────────────────────────────────────────────────────────┤
│ Speech API Unsupported│ CLIENT_TTS_UNAVAILABLE     │ N/A (Client)│ Display toast: "Browser SpeechSynthesis not supported. │
│                       │                            │             │ Text output will be displayed only."                   │
└───────────────────────┴────────────────────────────┴─────────────┴────────────────────────────────────────────────────────┘
```

---

## 4. Server-Side Exception Hierarchy

The application establishes a clear Java exception hierarchy to cleanly map service exceptions to HTTP status codes:

```
java.lang.Throwable
  └── java.lang.Exception
        └── java.lang.RuntimeException
              └── com.gesturetalk.exception.GestureRecognitionException (Root)
                    ├── com.gesturetalk.exception.InvalidImageException (HTTP 400 / 413 / 415)
                    ├── com.gesturetalk.exception.AIServiceException (HTTP 502 / 503 / 504)
                    └── com.gesturetalk.exception.DatabaseAccessException (Logged internally)
```

### 4.1 Global Servlet Exception Handling Pattern

```java
@Override
protected void doPost(HttpServletRequest request, HttpServletResponse response) 
        throws ServletException, IOException {
    response.setContentType("application/json");
    response.setCharacterEncoding("UTF-8");

    try {
        // Execute recognition pipeline
        RecognitionResponse result = recognitionService.processRecognition(request);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtils.toJson(result));
    } catch (InvalidImageException ex) {
        response.setStatus(ex.getHttpStatus());
        response.getWriter().write(JsonUtils.errorJson(ex.getErrorCode(), ex.getMessage()));
    } catch (AIServiceException ex) {
        response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        response.getWriter().write(JsonUtils.errorJson("AI_SERVICE_UNAVAILABLE", ex.getMessage()));
    } catch (Exception ex) {
        logger.error("Unhandled servlet error", ex);
        response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        response.getWriter().write(JsonUtils.errorJson("INTERNAL_SERVER_ERROR", "An unexpected error occurred."));
    }
}
```

---

## 5. Non-Blocking Database Graceful Degradation

If MySQL encounters connection dropouts, table locks, or server restarts, **the primary user-facing gesture recognition flow must not fail**.

```
                           AI Classification Success
                                       │
                                       ▼
                       Map to Spoken Message (In-Memory)
                                       │
                                       ▼
                       Attempt MySQL Database Insert
                                       │
                         ┌─────────────┴─────────────┐
                     SUCCESS                       FAILURE
                         │                             │
                         ▼                             ▼
                 Log Saved to DB             Catch SQLException
                         │                   Log warning to server logger
                         │                   Proceed to deliver audio to user
                         └─────────────┬───────────────┘
                                       │
                                       ▼
                     Deliver Spoken Output to User (HTTP 200)
```

---

## 6. Document Cross-References

* Security and sanitization: [12-SECURITY-AND-PRIVACY.md](12-SECURITY-AND-PRIVACY.md)
* API response schemas: [09-API-ENDPOINT-DESIGN.md](09-API-ENDPOINT-DESIGN.md)
* Testing and verification: [14-TESTING-AND-QUALITY-ASSURANCE.md](14-TESTING-AND-QUALITY-ASSURANCE.md)

