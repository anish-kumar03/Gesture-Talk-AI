# GestureTalk AI — Gemini AI Integration

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-07 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | AI Integration & Machine Learning Engineering |
| **Target SDK** | Official Google GenAI Java SDK (`com.google.genai:google-genai:0.1.1`) |
| **Target Model** | `gemini-2.5-flash` |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Rationale for Selecting Google Gemini Vision

Traditional computer vision approaches for hand gesture recognition (such as Haar cascades or custom-trained Convolutional Neural Networks) require extensive training datasets, struggle under varying skin tones and diverse lighting conditions, and demand dedicated GPU hosting infrastructure.

**Google Gemini** (`gemini-2.5-flash`) offers distinct architectural advantages:
1. **Multimodal Native Understanding**: Ingests raw image pixel buffers alongside textual instructions in a single inference call.
2. **Environmental Invariance**: Robustly classifies hand postures regardless of background clutter, varying ambient illumination, skin pigmentation, or camera noise.
3. **Structured JSON Output**: Gemini natively adheres to strict schema constraints, eliminating messy regex extraction from unstructured text.
4. **Official Java SDK Support**: With `com.google.genai:google-genai`, enterprise Java applications interface natively with Gemini models without manual HTTP boilerplate.

---

## 2. Official Google GenAI Java SDK Integration

The project integrates the official Google GenAI Java SDK configured in `pom.xml`:

```xml
<dependency>
    <groupId>com.google.genai</groupId>
    <artifactId>google-genai</artifactId>
    <version>0.1.1</version>
</dependency>
```

### 2.1 Server-Side Authentication & Secret Isolation
The SDK initializes an authenticated `Client` instance by reading the `GEMINI_API_KEY` environment variable. The API key is strictly maintained within the server process and is **never** accessible to the client browser:

```java
package com.gesturetalk.service;

import com.google.genai.Client;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class GeminiVisionService {
    private static final Logger logger = LoggerFactory.getLogger(GeminiVisionService.class);
    private static final String MODEL_NAME = "gemini-2.5-flash";
    private final Client client;

    public GeminiVisionService() {
        String apiKey = System.getenv("GEMINI_API_KEY");
        if (apiKey == null || apiKey.trim().isEmpty()) {
            apiKey = System.getenv("GOOGLE_API_KEY");
        }
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("CRITICAL: GEMINI_API_KEY environment variable is not configured.");
            throw new IllegalStateException("GEMINI_API_KEY is not set in the environment.");
        }
        // Initialize official GenAI SDK client
        this.client = Client.builder().apiKey(apiKey).build();
    }
}
```

---

## 3. Multimodal In-Memory Image Dispatching

To comply with the project's **zero-disk-footprint** privacy rule, uploaded images or webcam snapshots are read directly from the servlet `InputStream` into a volatile `byte[]` buffer. The SDK converts the raw byte array into an in-memory `Part` object:

```java
// Convert in-memory byte array directly into an SDK Part
Part imagePart = Part.fromBytes(imageBytes, mimeType);
Part instructionPart = Part.fromText(buildClassificationPrompt());

Content content = Content.fromParts(instructionPart, imagePart);
```

---

## 4. Prompt Engineering & Structured JSON Output

The prompt is engineered to enforce deterministic classification, eliminate conversational preamble, and restrict classifications to the five supported gesture codes or `UNKNOWN`.

### 4.1 System & Classification Prompt Definition

```
You are an expert computer vision assistant specializing in hand gesture recognition.
Analyze the provided image and classify the primary hand gesture shown.

You MUST choose EXACTLY ONE gesture code from this list:
1. "THUMBS_UP"       - A closed fist with the thumb pointed straight upward.
2. "THUMBS_DOWN"     - A closed fist with the thumb pointed straight downward.
3. "OPEN_PALM"       - An open hand with all five fingers extended and visible, palm facing camera.
4. "V_SIGN"          - Index and middle fingers extended in a V-shape; ring and pinky folded into palm.
5. "INDEX_POINTING"  - Index finger fully extended pointing in any direction; thumb and other fingers folded.
6. "UNKNOWN"         - If no hand is visible, multiple hands show conflicting gestures, the hand is blurred or occluded, or the gesture does not unambiguously match any of the five above.

Output strictly valid JSON with no markdown formatting, no backticks, and no explanation outside the JSON object:
{
  "gesture": "THUMBS_UP | THUMBS_DOWN | OPEN_PALM | V_SIGN | INDEX_POINTING | UNKNOWN",
  "confidence": 0.0 to 1.0,
  "explanation": "Brief 1-sentence description of observed hand posture."
}
```

### 4.2 Representative Gemini JSON Output Payloads

#### Example 1: Valid Gesture Output (`V_SIGN`)
```json
{
  "gesture": "V_SIGN",
  "confidence": 0.96,
  "explanation": "The index and middle fingers are extended forming a V-shape while the other fingers are curled into the palm."
}
```

#### Example 2: Ambiguous / Unsupported Gesture Output (`UNKNOWN`)
```json
{
  "gesture": "UNKNOWN",
  "confidence": 0.42,
  "explanation": "The hand appears partially out of frame with ambiguous finger orientation."
}
```

---

## 5. API Response Parsing & Java Validation Pipeline

The raw response string from `response.text()` is validated and sanitized before domain mapping:

```
                  Gemini Raw Response Text
                             │
                             ▼
              1. Remove Markdown Fences (```json)
                             │
                             ▼
              2. Parse JSON via Google Gson
                             │
                             ▼
            3. Extract Fields (gesture, confidence)
                             │
                             ▼
         4. Validate Against Supported GestureType Enum
                             │
               ┌─────────────┴─────────────┐
             VALID                       INVALID / UNKNOWN
               │                               │
               ▼                               ▼
    Return Valid GestureResult         Return UNKNOWN Token
```

### 5.1 JSON Deserialization Logic
```java
public GestureResult parseResponse(String rawJson) {
    String cleanJson = sanitizeJson(rawJson);
    JsonObject obj = JsonParser.parseString(cleanJson).getAsJsonObject();
    
    String gestureStr = obj.has("gesture") ? obj.get("gesture").getAsString().trim().toUpperCase() : "UNKNOWN";
    double confidence = obj.has("confidence") ? obj.get("confidence").getAsDouble() : 0.0;
    String explanation = obj.has("explanation") ? obj.get("explanation").getAsString() : "";

    GestureType type = GestureType.fromString(gestureStr);
    
    // Safety Guard: Force UNKNOWN if model confidence is below empirical threshold
    if (confidence < 0.60 && type != GestureType.UNKNOWN) {
        logger.warn("Low confidence ({}) for gesture {}. Falling back to UNKNOWN.", confidence, type);
        type = GestureType.UNKNOWN;
    }
    
    return new GestureResult(type, confidence, explanation);
}
```

---

## 6. Model Confidence Limitations & Fallbacks

> [!WARNING]
> **Model Confidence is an Estimate, Not a Calibrated Probability.**
> 
> The `confidence` score generated by generative Large Language Models represents an internal self-assessed certainty metric rather than a mathematically rigorous Bayesian probability. 
> 
> To safeguard against overconfident hallucinations:
> 1. The Java service layer enforces a minimum **confidence threshold of 0.60**. Any classification falling below 0.60 is automatically coerced to `UNKNOWN`.
> 2. The spoken message is **never** generated by Gemini; it is deterministically mapped by Java code (see [08-GESTURE-RECOGNITION-LOGIC.md](08-GESTURE-RECOGNITION-LOGIC.md)).

---

## 7. Rate Limits, Quotas & Error Handling

```
┌─────────────────────────────────┬────────────────────────────────────────────────────────────┐
│ Failure Mode                    │ Protective Mitigation in Java Service                      │
├─────────────────────────────────┼────────────────────────────────────────────────────────────┤
│ HTTP 429 Too Many Requests      │ Catch SDK exception; return HTTP 503 to client with        │
│ (Rate limit exceeded)           │ user-friendly "AI service busy, retry in 5s" message.      │
├─────────────────────────────────┼────────────────────────────────────────────────────────────┤
│ Network Timeout (>10 seconds)   │ Configure connection and read timeouts on client call;     │
│                                 │ fall back to UNKNOWN gesture with timeout error code.      │
├─────────────────────────────────┼────────────────────────────────────────────────────────────┤
│ Unparseable / Malformed JSON    │ Catch JsonSyntaxException; log malformed payload; return   │
│                                 │ UNKNOWN gesture result.                                    │
├─────────────────────────────────┼────────────────────────────────────────────────────────────┤
│ Safety / Content Filter Block   │ Inspect response finish reason; if BLOCKED, return         │
│ (e.g. SAFETY flag triggered)    │ UNKNOWN with explanation "Image could not be analyzed."    │
└─────────────────────────────────┴────────────────────────────────────────────────────────────┘
```

---

## 8. Document Cross-References

* Core gesture definitions: [08-GESTURE-RECOGNITION-LOGIC.md](08-GESTURE-RECOGNITION-LOGIC.md)
* Security and secret isolation: [12-SECURITY-AND-PRIVACY.md](12-SECURITY-AND-PRIVACY.md)
* Comprehensive error scenarios: [13-ERROR-HANDLING-AND-VALIDATION.md](13-ERROR-HANDLING-AND-VALIDATION.md)

