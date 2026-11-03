# GestureTalk AI — Limitations & Future Scope

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-17 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Strategic Analysis, Critical Evaluation & Product Roadmap |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Objective Assessment of Current Release (v1.0)

While GestureTalk AI delivers an efficient, secure, and privacy-preserving bridge for assistive communication, an honest academic and architectural evaluation requires documenting its technical boundaries and operating constraints.

---

## 2. Inherent System Limitations

```
┌─────────────────────────────────┬────────────────────────────────────────────────────────────────────────────┐
│ System Dimension                │ Nature of Architectural Limitation                                         │
├─────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
│ Vocabulary Breadth              │ Restricted strictly to 5 predefined static gestures: THUMBS_UP,            │
│                                 │ THUMBS_DOWN, OPEN_PALM, V_SIGN, and INDEX_POINTING.                       │
├─────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
│ Optical & Environmental Quality │ Classification accuracy degrades under severe underexposure (<10 lux),     │
│                                 │ heavy motion blur, or when hands are positioned beyond 2 meters from lens. │
├─────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
│ Probabilistic AI Error Rate     │ As with all Large Multimodal Models, Gemini Vision can occasionally        │
│                                 │ misclassify ambiguous angles or hallucinate hand configurations.           │
├─────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
│ Mandatory Internet Connectivity │ Because Gemini inference executes via Google's cloud endpoints, local      │
│                                 │ offline operation is impossible in the v1 architecture.                    │
├─────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
│ Browser Speech Synthesis Variance│ Auditory quality, accent naturalness, and voice options depend entirely    │
│                                 │ on the host OS and browser engine (e.g., Windows SAPI vs. Apple AVVoice).  │
├─────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
│ Non-Sign-Language Boundary      │ Does not translate natural sign languages (ASL/BSL/ISL), which require     │
│                                 │ dynamic temporal syntax, facial expressions, and spatial movement.         │
├─────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
│ Discrete Snapshot Processing    │ Operates on discrete snapshots triggered by the user; does not perform     │
│                                 │ real-time continuous 30 FPS video stream inference over WebSockets.        │
├─────────────────────────────────┼────────────────────────────────────────────────────────────────────────────┤
│ Platform Form Factor            │ Packaged as a responsive web application; lacks native mobile wrappers     │
│                                 │ (iOS/Android native APKs) or native background system services.            │
└─────────────────────────────────┴────────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Future Scope & Roadmap Horizons

```
                     ┌────────────────────────────────────────────────────────┐
                     │ HORIZON 1: Near-Term Enhancements (v1.1 - v1.2)        │
                     │ • Temporal smoothing & continuous video tracking       │
                     │ • Vocabulary expansion to 15 essential gestures        │
                     │ • Multilingual text-to-speech output (10+ languages)   │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │ HORIZON 2: Advanced Interaction (v2.0)                 │
                     │ • Custom gesture-to-message mapping builder            │
                     │ • Sentence chaining & conversational dialogue builder  │
                     │ • Interactive gesture training & calibration mode      │
                     └───────────────────────────┬────────────────────────────┘
                                                 │
                                                 ▼
                     ┌────────────────────────────────────────────────────────┐
                     │ HORIZON 3: Edge Computing & Offline Mode (v3.0)        │
                     │ • On-device MediaPipe / ONNX hand landmarking          │
                     │ • Complete offline recognition capability              │
                     │ • Native Progressive Web App (PWA) offline packaging   │
                     └────────────────────────────────────────────────────────┘
```

---

## 4. Detailed Future Architectural Upgrades

### 4.1 Continuous Real-Time Gesture Tracking (WebSocket / WebRTC)
In future revisions, the discrete "Capture" button will be complemented by a continuous tracking mode. Video frames will stream at 5–10 FPS over full-duplex WebSockets to a dedicated Java servlet worker pool. A temporal state debouncer will ensure a gesture is held steadily for 500 milliseconds before firing a single vocalization event, preventing repetitive audio stuttering.

### 4.2 Expanded Semantic Vocabulary
Future releases will introduce additional high-value communicative gestures:
* `FIST_BUMP`: Solidarity / Agreement
* `HAND_OVER_HEART`: Gratitude / "Thank you"
* `OK_SIGN`: Perfection / "I am okay"
* `TIME_OUT` (T-shape two hands): Need assistance / Urgent pause
* `CUP_TO_MOUTH`: Thirst / "Need water"

### 4.3 Multilingual Speech Synthesis
Leveraging the browser SpeechSynthesis engine's native multi-language voice dictionaries, the UI will feature a language selector allowing mapped messages to be spoken in Spanish, French, German, Hindi, or Japanese based on user preference.

### 4.4 User-Customizable Gesture-to-Message Mapping
A planned dynamic mapping manager will allow users to customize their phrases (e.g., mapping `OPEN_PALM` to *"Please call the doctor"* instead of *"Please stop or wait"*), persisting custom mappings per session in MySQL.

### 4.5 Hybrid Edge/Cloud Architecture
To mitigate cloud latency and enable offline operation during internet outages, future versions can evaluate hybrid inference: running lightweight on-device hand landmarking (e.g., ONNX Runtime in Java or MediaPipe in browser) for basic gestures, falling back to Gemini Vision for complex or ambiguous postures.

---

## 5. Document Cross-References

* Core problem and objectives: [01-PROBLEM-STATEMENT-AND-OBJECTIVES.md](01-PROBLEM-STATEMENT-AND-OBJECTIVES.md)
* Architectural decisions: [18-TECHNICAL-DECISIONS.md](18-TECHNICAL-DECISIONS.md)
* Implementation phases: [15-IMPLEMENTATION-ROADMAP.md](15-IMPLEMENTATION-ROADMAP.md)

