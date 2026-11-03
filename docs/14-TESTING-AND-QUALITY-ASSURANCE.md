# GestureTalk AI — Testing & Quality Assurance Architecture

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-14 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Quality Assurance, Verification & Test Protocol |
| **Status** | Planned Test Strategy (Pre-Execution Baseline) |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Quality Assurance Strategy & Test Pyramid

To achieve academic and enterprise reliability, GestureTalk AI employs a multi-tiered test architecture spanning unit tests, component mock tests, in-memory integration tests, and planned computer vision accuracy evaluations:

```
                      ┌────────────────────────┐
                      │    Manual E2E & UI     │  (Browser camera, SpeechSynthesis,
                      │   Validation Testing   │   responsiveness)
                      ├────────────────────────┤
                      │   In-Memory Database   │  (HikariCP, H2 test schema,
                      │    Integration Tests   │   PreparedStatement lifecycle)
                      ├────────────────────────┤
                      │  AI Service Mock Unit  │  (Mockito mocks of GenAI Client,
                      │         Tests          │   JSON parser, threshold checks)
                      ├────────────────────────┤
                      │ Pure Domain Unit Tests │  (GestureType, MagicNumberValidator,
                      │     (JUnit 5 Engine)   │   deterministic speech mappings)
                      └────────────────────────┘
```

---

## 2. Testing Levels & Component Scope

### 2.1 Pure Domain & Utility Unit Tests (JUnit 5)
* **Scope**: Executes against Java classes without dependencies on networks, databases, or servlet containers.
* **Key Targets**:
  * `GestureTypeTest`: Verifies enum parsing, case-insensitivity, and fallback to `UNKNOWN` on unrecognized strings.
  * `ImageValidationServiceTest`: Verifies that magic byte checking accurately distinguishes JPEG, PNG, and WebP from malicious executable binaries.
  * `JsonUtilsTest`: Validates serialization and error JSON formatting.

### 2.2 AI Service & Response Parser Unit Tests (Mockito)
* **Scope**: Validates prompt formulation, response parsing, markdown stripping, and confidence threshold enforcement without incurring billable Gemini API tokens.
* **Technique**: Mocks `com.google.genai.Client` and `GenerateContentResponse` to return pre-recorded JSON payloads simulating high-confidence, low-confidence, malformed, and quota-exceeded responses.

### 2.3 Database Layer Integration Tests (H2 Database)
* **Scope**: Confirms that `RecognitionLogDAO` executes valid SQL syntax and handles connection edge cases.
* **Technique**: Leverages the embedded **H2 Database Engine** (`com.h2database:h2:2.3.232`, configured with `<scope>test</scope>` in `pom.xml`). H2 runs in MySQL compatibility mode (`jdbc:h2:mem:testdb;MODE=MySQL`), allowing continuous Maven build verification (`mvn test`) without requiring an active MySQL daemon.

### 2.4 Browser-Level Functional & Speech Tests
* **Scope**: Validates client-side hardware interactions.
* **Technique**: Browser-based evaluation verifying camera stream acquisition, off-screen canvas snapshot generation, multipart payload composition, and audio vocalization across Google Chrome, Microsoft Edge, Mozilla Firefox, and Apple Safari.

---

## 3. Comprehensive Master Test Case Catalog

```
┌────────┬──────────────────────┬────────────────────────────────┬────────────────────────────────────────────┬──────────┐
│ Test ID│ Test Scenario        │ Test Input / Action            │ Expected Result                            │ Priority │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-01  │ Enum String Mapping  │ Input string "thumbs_up"       │ Returns GestureType.THUMBS_UP              │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-02  │ Enum Unknown Fallback│ Input string "WAVING_HAND"     │ Returns GestureType.UNKNOWN                │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-03  │ Null Gesture String  │ Input null                     │ Returns GestureType.UNKNOWN                │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-04  │ Image Magic Byte JPG │ Byte array starting FF D8 FF   │ ImageValidationService accepts format      │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-05  │ Image Magic Byte PNG │ Byte array starting 89 50 4E 47│ ImageValidationService accepts format      │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-06  │ Corrupted Binary File│ Byte array starting 4D 5A (EXE)│ Throws InvalidImageException (HTTP 415)    │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-07  │ Image Size Exceeded  │ Byte array of 6.2 MB           │ Throws InvalidImageException (HTTP 413)    │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-08  │ Empty Image Payload  │ Empty 0-byte array             │ Throws InvalidImageException (HTTP 400)    │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-09  │ AI JSON Sanitization │ JSON wrapped in ```json ```    │ Strips markdown, parses JSON correctly     │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-10  │ Low Confidence Guard │ Gesture THUMBS_UP, conf 0.45   │ Coerces result to UNKNOWN (conf < 0.60)    │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-11  │ AI Rate Limit (429)  │ Mock client throws 429 error   │ Throws AIServiceException (HTTP 503)       │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-12  │ Missing API Key      │ Clear GEMINI_API_KEY env       │ Throws IllegalStateException on init       │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-13  │ DB Insert Log (H2)   │ Valid RecognitionLog record    │ Generates non-null primary key ID          │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-14  │ DB Filter Log (H2)   │ Query records by THUMBS_UP     │ Returns only matching records              │ Medium   │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-15  │ DB Purge History (H2)│ Trigger DELETE /api/history    │ Clears table; row count returns 0          │ Medium   │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-16  │ Web Camera Stream    │ Click "Start Camera" in Chrome │ Stream renders in <video> at 30 FPS        │ High     │
├────────┼──────────────────────┼────────────────────────────────┼────────────────────────────────────────────┼──────────┤
│ TC-17  │ Web Speech Synthesis │ Trigger vocalization in Edge   │ Browser speaks "Yes, that's correct."      │ High     │
└────────┴──────────────────────┴────────────────────────────────┴────────────────────────────────────────────┴──────────┘
```

---

## 4. Planned Evaluation Dataset & Accuracy Benchmark

To evaluate the empirical classification accuracy of the system across diverse environmental conditions, the project plans a standardized evaluation dataset:

### 4.1 Evaluation Dataset Structure (~120 Photographic Samples)
```
┌────────────────────┬──────────────┬──────────────────────────────────────────────────────────────────┐
│ Gesture Category   │ Sample Count │ Planned Variation Characteristics                                │
├────────────────────┼──────────────┼──────────────────────────────────────────────────────────────────┤
│ `THUMBS_UP`        │ 20 photos    │ Diverse skin tones, left/right hand, frontal & angled postures.  │
├────────────────────┼──────────────┼──────────────────────────────────────────────────────────────────┤
│ `THUMBS_DOWN`      │ 20 photos    │ High/low ambient lighting, plain vs. cluttered home backgrounds. │
├────────────────────┼──────────────┼──────────────────────────────────────────────────────────────────┤
│ `OPEN_PALM`        │ 20 photos    │ Fingers spread vs. fingers joined, distances: 0.5m, 1.0m, 1.5m.  │
├────────────────────┼──────────────┼──────────────────────────────────────────────────────────────────┤
│ `V_SIGN`           │ 20 photos    │ Palm facing forward, palm facing inward (British variant test).  │
├────────────────────┼──────────────┼──────────────────────────────────────────────────────────────────┤
│ `INDEX_POINTING`   │ 20 photos    │ Pointing up, pointing left, pointing right, pointing to lens.    │
├────────────────────┼──────────────┼──────────────────────────────────────────────────────────────────┤
│ `UNKNOWN` Samples  │ 20 photos    │ Empty room (no hand), waving hand, fist, two hands, coffee cup.  │
├────────────────────┼──────────────┼──────────────────────────────────────────────────────────────────┤
│ **TOTAL SAMPLES**  │ **120 photos**│ Comprehensive benchmark suite.                                   │
└────────────────────┴──────────────┴──────────────────────────────────────────────────────────────────┘
```

### 4.2 Accuracy Calculation Metric

The classification accuracy will be formally calculated as:

$$\text{Classification Accuracy (\%)} = \left( \frac{\text{Total Correct Classifications}}{\text{Total Evaluation Samples}} \right) \times 100$$

Where a classification is deemed **Correct** if and only if:
1. Ground Truth matches the predicted `gestureCode`.
2. Model confidence is $\ge 0.60$ for known gestures, or correctly assigns `UNKNOWN` for ambiguous/unsupported images.

> [!IMPORTANT]
> **No Fabricated Results**: Because the project is currently in the architectural documentation phase, no test execution results or accuracy percentages are claimed as passed or verified. All performance and accuracy benchmarks will be generated and documented during the Quality Assurance phase (Phase 6).

---

## 5. Document Cross-References

* Gesture classification rules: [08-GESTURE-RECOGNITION-LOGIC.md](08-GESTURE-RECOGNITION-LOGIC.md)
* Error definitions: [13-ERROR-HANDLING-AND-VALIDATION.md](13-ERROR-HANDLING-AND-VALIDATION.md)
* Implementation schedule: [15-IMPLEMENTATION-ROADMAP.md](15-IMPLEMENTATION-ROADMAP.md)

