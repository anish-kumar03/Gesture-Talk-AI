# GestureTalk AI — User Interface & Screen Specifications

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-06 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | UI/UX & Interaction Specification |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. UI Design Philosophy & Visual Language

The user interface of GestureTalk AI is built upon **Bootstrap 5.3**, utilizing a clean, high-contrast, modern visual palette that prioritizes accessibility and immediate visual feedback. 

### Key Design Pillars:
* **Accessibility First**: Large tactile buttons, high-contrast color badges, clear typography, and ARIA labels for screen readers.
* **Responsive Layout**: Adapts smoothly across desktop monitors, laptops, tablets, and mobile screens.
* **Zero Jargon**: Intuitive icons and simple terminology so users with varied technological literacy can operate the system effortlessly.
* **Unified Single-Page Workspace**: Core capture, recognition, speech controls, and recent history reside within an integrated workspace to minimize page reloading.

---

## 2. Screen & Section Breakdown

```
┌───────────────────────────────────────────────────────────────────────────┐
│ NAVIGATION BAR: Brand Logo • Live Status • Speech Audio Toggle • Docs    │
├───────────────────────────────────────────────────────────────────────────┤
│ HERO HEADER: Headline • Brief Intro • Quick Start CTA                     │
├─────────────────────────────────────┬─────────────────────────────────────┤
│ RECOGNITION WORKSPACE (Left Column) │ RESULTS & FEEDBACK (Right Column)   │
│ • Tabs: [Webcam Mode | Upload Mode] │ • Gesture Status Card               │
│ • Live <video> Viewfinder / Canvas  │ • Large Icon & Text Badge           │
│ • Camera Controls (Start/Stop/Snap) │ • Spoken Phrase Display             │
│ • File Dropzone (Drag-and-Drop)     │ • Confidence Meter & Reason         │
│ • Image Preview Container           │ • "Replay Speech" Button            │
├─────────────────────────────────────┴─────────────────────────────────────┤
│ SPEECH SYNTHESIS CONTROLS BAR: Voice Dropdown • Pitch • Rate • Volume     │
├───────────────────────────────────────────────────────────────────────────┤
│ GESTURE REFERENCE CHEAT SHEET: 5 Cards with Hand Diagrams & Output Text   │
├───────────────────────────────────────────────────────────────────────────┤
│ RECOGNITION AUDIT HISTORY: Filterable Table • Badges • Clear History CTA │
├───────────────────────────────────────────────────────────────────────────┤
│ FOOTER: Academic Metadata • Jakarta EE / Tomcat 10.1 • Project Links      │
└───────────────────────────────────────────────────────────────────────────┘
```

---

## 3. Comprehensive Section Specifications

### 3.1 Navigation Bar & Header
* **Purpose**: Provides persistent branding, system health indicators, and global audio settings.
* **UI Components**:
  * Brand icon and title: *"GestureTalk AI"*.
  * System Health Pill: Green badge ("AI & DB Online") or Yellow/Red badge if services are degraded.
  * Audio Mute Toggle: Switch to disable/enable speech synthesis.
  * Navigation links: *Workspace*, *Gesture Guide*, *Audit History*, *About*.
* **User Interactions**: Clicking links smoothly scrolls to designated page anchors. Toggling audio silences `SpeechSynthesis`.
* **Backend Integration**: Polling `GET /api/health` on initial load.

### 3.2 Hero Header Section
* **Purpose**: Explains the core purpose of the application in two sentences to orient new users.
* **UI Components**: Catchy title, subtitle emphasizing assistive technology, and dual quick-action buttons: *"Start Camera"* and *"Upload Image"*.

### 3.3 Recognition Workspace — Webcam Interface
* **Purpose**: Houses the live video viewfinder for continuous visual framing and instant snapshot capture.
* **UI Components**:
  * 16:9 responsive `<video>` element with rounded borders and subtle drop shadow.
  * Off-screen `<canvas>` for rasterization.
  * Control Toolbar:
    * **Start Camera**: Activates `navigator.mediaDevices.getUserMedia`.
    * **Stop Camera**: Terminates active video tracks to release hardware lock.
    * **Capture Gesture** (Primary Action Button): Takes snapshot, freezes frame, and initiates analysis.
  * Live status overlay ("Camera Ready", "Requesting Permission...", "Camera Offline").
* **Backend Integration**: Dispatches snapshot blob via `POST /api/recognize` as `sourceType=WEBCAM`.

### 3.4 Recognition Workspace — Image Upload Interface
* **Purpose**: Provides manual image submission for devices without cameras or pre-recorded evaluation images.
* **UI Components**:
  * Drag-and-drop dashed boundary zone with cloud upload icon.
  * Native `<input type="file" accept="image/jpeg,image/png,image/webp">` trigger.
  * Image Preview Card: Displays selected image with dimension and file-size readout.
  * **Analyze Image** button with loading spinner integration.
* **Backend Integration**: Dispatches file via `POST /api/recognize` as `sourceType=UPLOAD`.

### 3.5 Recognition Result Panel
* **Purpose**: Real-time display of the AI classification and mapped spoken message.
* **UI Components**:
  * **Gesture Badge**: Color-coded pill (`success` for `THUMBS_UP`, `danger` for `THUMBS_DOWN`, `primary` for `OPEN_PALM`, `info` for `V_SIGN`, `warning` for `INDEX_POINTING`, `secondary` for `UNKNOWN`).
  * **Vocalized Sentence Display**: Large, legible display of the exact spoken message.
  * **Confidence Gauge**: Progress bar indicating Gemini's estimated model confidence percentage.
  * **AI Analysis Note**: Collapsible accordion explaining the model's visual reasoning.
  * **Speak Again Button**: Re-triggers browser `SpeechSynthesis` for the current message.
* **Expected Output**: Instant display upon receiving `HTTP 200` response from the backend servlet.

### 3.6 Speech Synthesis Control Bar
* **Purpose**: Gives users granular control over auditory feedback characteristics.
* **UI Components**:
  * **Voice Selector**: Populated dynamically from `window.speechSynthesis.getVoices()`.
  * **Rate Slider**: Range `0.5x` to `2.0x` (default `1.0x`).
  * **Pitch Slider**: Range `0.5` to `1.5` (default `1.0`).
  * **Volume Slider**: Range `0%` to `100%` (default `100%`).
  * **Test Voice** button: Speaks *"GestureTalk voice ready."*

### 3.7 Gesture Reference Cheat Sheet
* **Purpose**: Displays the exact visual configuration of all five supported gestures to prevent user confusion.
* **UI Components**: Grid of five visual cards, each containing:
  1. Hand sign graphic / icon.
  2. Gesture code name (e.g., `OPEN_PALM`).
  3. Clear visual description of finger positions.
  4. The exact sentence that will be spoken upon recognition.

### 3.8 Recognition Audit History Section
* **Purpose**: Displays a chronological table of past recognitions recorded during the current session.
* **UI Components**:
  * Filter Dropdown: *All Gestures*, *Thumbs Up*, *Thumbs Down*, *Open Palm*, *V Sign*, *Index Pointing*, *Unknown*.
  * Action Button: **"Clear History"** (triggers confirmation modal).
  * Responsive Data Table:
    * Columns: *ID*, *Timestamp*, *Source*, *Gesture Code*, *Spoken Message*, *Confidence*, *Actions*.
    * Row Action: Miniature audio speaker icon to re-speak any historical message.
* **Backend Integration**: Populated via `GET /api/history` and cleared via `DELETE /api/history`.

### 3.9 About & Academic Metadata Section
* **Purpose**: Documents the system's academic context, architecture, technologies, and non-sign-language disclaimer.
* **UI Components**: Academic author information, technology stack summary badges, and prominent disclaimer banner.

---

## 4. UI State Transitions & Feedback Matrix

```
┌──────────────────────────┬─────────────────────────────┬──────────────────────────┬──────────────────────────────┐
│ Application State        │ Visual UI Indicator         │ Audio Behavior           │ Action Controls State        │
├──────────────────────────┼─────────────────────────────┼──────────────────────────┼──────────────────────────────┤
│ Initial Idle             │ Camera offline, cards empty │ Silent                   │ "Start Camera" enabled       │
│ Camera Streaming         │ Live 30FPS viewfinder active│ Silent                   │ "Capture" enabled            │
│ Analyzing Frame          │ Overlay spinner: "Analyzing"│ Silent                   │ All buttons disabled         │
│ Successful Recognition   │ Colored badge + text banner │ SpeechSynthesis speaks   │ All buttons re-enabled       │
│ Unknown Gesture          │ Muted grey/yellow badge     │ Speaks retry instruction │ Prompt user to adjust hand   │
│ Network / API Failure    │ Red Bootstrap toast alert   │ SpeechSynthesis speaks   │ Displays descriptive error   │
└──────────────────────────┴─────────────────────────────┴──────────────────────────┴──────────────────────────────┘
```

---

## 5. Document Cross-References

* Operational workflow execution: [05-APPLICATION-WORKFLOW.md](05-APPLICATION-WORKFLOW.md)
* Backend API endpoints backing the UI: [09-API-ENDPOINT-DESIGN.md](09-API-ENDPOINT-DESIGN.md)
* Frontend component files and assets: [11-PROJECT-STRUCTURE.md](11-PROJECT-STRUCTURE.md)

