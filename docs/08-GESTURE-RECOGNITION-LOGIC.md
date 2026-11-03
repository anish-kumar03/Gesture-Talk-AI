# GestureTalk AI — Gesture Recognition & Mapping Logic

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-08 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Domain Logic & Computer Vision Semantics |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Domain Conceptual Separation

To maintain architectural clarity and academic rigor, GestureTalk AI strictly delineates four distinct conceptual stages in its recognition pipeline:

```
┌─────────────────────────┐
│ 1. Image Recognition    │ Computer vision detection of hand presence, bounding envelope,
│                         │ and anatomical features from pixel arrays.
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 2. Gesture              │ Categorization of observed hand posture into a symbolic token
│    Classification       │ (e.g., THUMBS_UP, OPEN_PALM, or UNKNOWN).
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 3. Confidence Threshold │ Numerical validation ensuring model self-assessment meets or
│    Validation           │ exceeds the empirical safety barrier (>= 0.60).
└───────────┬─────────────┘
            ▼
┌─────────────────────────┐
│ 4. Gesture-to-Message   │ Pure Java deterministic mapping binding the verified token to an
│    Mapping              │ immutable, grammatically correct spoken sentence.
└─────────────────────────┘
```

---

## 2. Why Java Performs Deterministic Gesture-to-Message Mapping

A common design flaw in naive generative AI applications is instructing the LLM to generate both the classification and the conversational sentence in free-form natural language. 

### Why Free-Form LLM Output is Rejected in GestureTalk AI:
1. **Hallucination & Semantic Drift**: An LLM asked to speak for the user might unpredictably generate *"Looks like you're giving me a thumbs up buddy!"* or *"Affirmative!"* instead of the standard *"Yes, that's correct."*
2. **Inconsistent Auditory Experience**: For assistive communication, predictability and consistency are vital. Non-verbal users must have absolute confidence in the exact phrase their device will vocalize.
3. **Safety & Compliance**: Restricting speech to hardcoded Java constants ensures that no offensive, unverified, or erratic text can ever be emitted by the SpeechSynthesis engine.
4. **Separation of Capabilities**: Gemini acts strictly as a **visual classifier**, while Java acts as the **authoritative semantic controller**.

---

## 3. Supported Gesture Vocabulary Catalog

The initial release recognizes exactly **five predefined static hand gestures** plus an explicit **UNKNOWN** state:

### 3.1 `THUMBS_UP`
* **Visual Posture**: A closed fist with all four fingers curled tightly into the palm, with the thumb extended vertically upward.
* **Recognition Conditions**: Thumb must point generally upward (within ±45° of vertical). Palm can face laterally or toward camera.
* **Deterministic Output Sentence**: `"Yes, that's correct."`
* **Ambiguous Cases**: Thumb angled horizontally (risk of confusion with pointing); hand tilted downward.

### 3.2 `THUMBS_DOWN`
* **Visual Posture**: A closed fist with all four fingers curled into the palm, with the thumb extended vertically downward.
* **Recognition Conditions**: Thumb must point generally downward (within ±45° of downward vertical).
* **Deterministic Output Sentence**: `"No, that's incorrect."`
* **Ambiguous Cases**: Inverted fist with loose fingers or thumb tucked behind knuckles.

### 3.3 `OPEN_PALM`
* **Visual Posture**: An open, flat hand with all five fingers (thumb through pinky) fully extended and visible, facing toward the camera lens.
* **Recognition Conditions**: Palm surface visible; fingers distinctly separated or held together flatly in a "stop" or "halt" posture.
* **Deterministic Output Sentence**: `"Please stop or wait."`
* **Ambiguous Cases**: Side-profile of hand (edge-on); fingers curled into a claw; hand angled sharply away from optical plane.

### 3.4 `V_SIGN`
* **Visual Posture**: The index and middle fingers are extended upward in a distinct "V" shape, while the ring finger, little finger, and thumb are curled into the palm.
* **Recognition Conditions**: Separation between index and middle fingertips must be clearly distinguishable; palm can face forward or backward.
* **Deterministic Output Sentence**: `"Peace and greetings."`
* **Ambiguous Cases**: Index and middle fingers pressed tightly together (looks like two-finger pointing); three fingers extended (scout sign).

### 3.5 `INDEX_POINTING`
* **Visual Posture**: The index finger is fully extended in a pointing direction, while the thumb, middle, ring, and little fingers are curled into the palm.
* **Recognition Conditions**: Single prominent extended digit (index). Orientation may point upward, forward, left, or right.
* **Deterministic Output Sentence**: `"Please look there."`
* **Ambiguous Cases**: Middle finger accidentally extended; thumb extended alongside index (L-shape).

### 3.6 `UNKNOWN`
* **Visual Posture**: Any posture not definitively conforming to the five specifications above.
* **Trigger Conditions**:
  * No hand visible in the frame.
  * Hand severely cropped, blurred, or occluded by objects.
  * Multiple hands displaying contradictory signs.
  * Supported gesture presented at an unresolvable optical angle.
  * Model self-assessed confidence `< 0.60`.
* **Deterministic Output Sentence**: `"Gesture not recognized. Please try again."`

---

## 4. Comprehensive Gesture Specification Matrix

```
┌────────────────┬──────────────────────────┬──────────────────────┬────────────────────────────────────────────┬─────────────┐
│ Gesture Code   │ Visual Finger Config     │ Target Meaning       │ Deterministic Java Output Message          │ Confidence  │
│                │                          │                      │                                            │ Threshold   │
├────────────────┼──────────────────────────┼──────────────────────┼────────────────────────────────────────────┼─────────────┤
│ THUMBS_UP      │ Fist closed, thumb up    │ Affirmation / Yes    │ "Yes, that's correct."                     │ >= 0.60     │
├────────────────┼──────────────────────────┼──────────────────────┼────────────────────────────────────────────┼─────────────┤
│ THUMBS_DOWN    │ Fist closed, thumb down  │ Negation / No        │ "No, that's incorrect."                    │ >= 0.60     │
├────────────────┼──────────────────────────┼──────────────────────┼────────────────────────────────────────────┼─────────────┤
│ OPEN_PALM      │ 5 fingers flat, extended │ Stop / Wait / Hold   │ "Please stop or wait."                     │ >= 0.60     │
├────────────────┼──────────────────────────┼──────────────────────┼────────────────────────────────────────────┼─────────────┤
│ V_SIGN         │ Index + Middle V-shape   │ Peace / Greetings    │ "Peace and greetings."                     │ >= 0.60     │
├────────────────┼──────────────────────────┼──────────────────────┼────────────────────────────────────────────┼─────────────┤
│ INDEX_POINTING │ Index extended, rest in  │ Directional / Look   │ "Please look there."                       │ >= 0.60     │
├────────────────┼──────────────────────────┼──────────────────────┼────────────────────────────────────────────┼─────────────┤
│ UNKNOWN        │ Ambiguous or unsupported │ Unclear / Retry      │ "Gesture not recognized. Please try again."│ < 0.60 or   │
│                │                          │                      │                                            │ Unrecognized│
└────────────────┴──────────────────────────┴──────────────────────┴────────────────────────────────────────────┴─────────────┘
```

---

## 5. Java Implementation Model: `GestureType` Enum

The core domain model encapsulates gesture definitions and deterministic mappings within a strongly typed Java `enum`:

```java
package com.gesturetalk.model;

public enum GestureType {
    THUMBS_UP("Yes, that's correct.", "Thumbs Up"),
    THUMBS_DOWN("No, that's incorrect.", "Thumbs Down"),
    OPEN_PALM("Please stop or wait.", "Open Palm"),
    V_SIGN("Peace and greetings.", "V Sign"),
    INDEX_POINTING("Please look there.", "Index Pointing"),
    UNKNOWN("Gesture not recognized. Please try again.", "Unknown Gesture");

    private final String defaultMessage;
    private final String displayName;

    GestureType(String defaultMessage, String displayName) {
        this.defaultMessage = defaultMessage;
        this.displayName = displayName;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static GestureType fromString(String code) {
        if (code == null) {
            return UNKNOWN;
        }
        try {
            return GestureType.valueOf(code.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
```

---

## 6. Document Cross-References

* Prompt engineering and Gemini interaction: [07-GEMINI-AI-INTEGRATION.md](07-GEMINI-AI-INTEGRATION.md)
* Java Service layer architecture: [04-SYSTEM-ARCHITECTURE.md](04-SYSTEM-ARCHITECTURE.md)
* Testing and accuracy evaluation matrix: [14-TESTING-AND-QUALITY-ASSURANCE.md](14-TESTING-AND-QUALITY-ASSURANCE.md)

