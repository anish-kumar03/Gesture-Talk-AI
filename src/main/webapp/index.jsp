<%@ page language="java" contentType="text/html; charset=UTF-8" pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>GestureTalk AI — Your Gestures. Your Voice.</title>
    
    <!-- Meta tags for SEO & Accessibility -->
    <meta name="description" content="AI-powered hand gesture recognition and communication assistant using Jakarta EE and Google Gemini Vision.">
    <meta name="theme-color" content="#070a12">

    <!-- Google Fonts: Plus Jakarta Sans -->
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=Plus+Jakarta+Sans:wght@400;500;600;700;800&display=swap" rel="stylesheet">

    <!-- Bootstrap 5.3 CSS & Bootstrap Icons CDN -->
    <link href="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/css/bootstrap.min.css" rel="stylesheet" integrity="sha384-QWTKZyjpPEjISv5WaRU9OFeRpok6YctnYmDr5pNlyT2bRjXh0JMhjY6hW+ALEwIH" crossorigin="anonymous">
    <link rel="stylesheet" href="https://cdn.jsdelivr.net/npm/bootstrap-icons@1.11.3/font/bootstrap-icons.min.css">

    <!-- Custom Dark AI Theme Stylesheet -->
    <link rel="stylesheet" href="${pageContext.request.contextPath}/static/css/style.css">
</head>
<body>

    <!-- 1. NAVIGATION BAR -->
    <nav class="navbar navbar-expand-lg gt-navbar sticky-top">
        <div class="container">
            <a class="gt-brand-logo" href="#">
                <div class="gt-logo-icon">
                    <i class="bi bi-hand-index-thumb-fill"></i>
                </div>
                <span>GestureTalk <span class="text-gradient-purple-blue">AI</span></span>
            </a>

            <div class="d-flex align-items-center gap-3 order-lg-3">
                <span class="gt-badge-live">
                    <span class="gt-pulse-dot"></span>
                    <span>v1.0.0 Production Release</span>
                </span>
                <button class="navbar-toggler border-0 text-white" type="button" data-bs-toggle="collapse" data-bs-target="#navContent" aria-label="Toggle navigation">
                    <i class="bi bi-list fs-2 text-white"></i>
                </button>
            </div>

            <div class="collapse navbar-collapse justify-content-center order-lg-2" id="navContent">
                <div class="navbar-nav gap-2 py-2 py-lg-0">
                    <a class="gt-nav-link active" href="#workspace"><i class="bi bi-camera-video me-1"></i> Workspace</a>
                    <a class="gt-nav-link" href="#gestures"><i class="bi bi-hand-index me-1"></i> Supported Gestures</a>
                    <a class="gt-nav-link" href="#history"><i class="bi bi-clock-history me-1"></i> Audit History</a>
                    <a class="gt-nav-link" href="#how-it-works"><i class="bi bi-diagram-3 me-1"></i> How It Works</a>
                    <a class="gt-nav-link" href="#about"><i class="bi bi-info-circle me-1"></i> About</a>
                </div>
            </div>
        </div>
    </nav>

    <!-- 2. HERO SECTION -->
    <header class="py-5 text-center position-relative">
        <div class="container py-3">
            <div class="row justify-content-center">
                <div class="col-lg-9 col-xl-8">
                    <span class="badge rounded-pill px-3 py-2 mb-3" style="background: rgba(139, 92, 246, 0.15); color: #c084fc; border: 1px solid rgba(139, 92, 246, 0.3);">
                        <i class="bi bi-cpu-fill me-1"></i> AI-Powered Assistive Technology
                    </span>
                    <h1 class="display-4 fw-extrabold mb-3">
                        Your Gestures. <span class="text-gradient-purple-blue">Your Voice.</span>
                    </h1>
                    <p class="lead text-secondary mb-4 mx-auto" style="max-width: 620px; color: #94a3b8 !important;">
                        Turn hand gestures into meaningful communication using AI. Position your hand in front of your camera or upload a photo to begin.
                    </p>
                    <div class="d-flex flex-wrap justify-content-center gap-3">
                        <a href="#workspace" class="gt-btn-primary text-decoration-none">
                            <i class="bi bi-camera-video-fill"></i> Open Workspace
                        </a>
                        <a href="#gestures" class="gt-btn-secondary text-decoration-none">
                            <i class="bi bi-grid-fill"></i> View 5 Signs
                        </a>
                    </div>
                </div>
            </div>
        </div>
    </header>

    <!-- 3. MAIN WORKSPACE SECTION -->
    <main class="container mb-5" id="workspace">
        <div class="row g-4">
            
            <!-- LEFT COLUMN: Input Workspace (Webcam & Upload) -->
            <div class="col-lg-7">
                <div class="gt-card h-100">
                    <div class="gt-card-header">
                        <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-webcam text-primary fs-5"></i>
                            <h5 class="mb-0">Capture Workspace</h5>
                        </div>
                        
                        <!-- Ingestion Mode Selector -->
                        <div class="gt-mode-pills">
                            <button type="button" class="gt-mode-btn active" id="modeBtnWebcam">
                                <i class="bi bi-camera-video"></i> Live Camera
                            </button>
                            <button type="button" class="gt-mode-btn" id="modeBtnUpload">
                                <i class="bi bi-upload"></i> Image Upload
                            </button>
                        </div>
                    </div>

                    <div class="gt-card-body">

                        <!-- SUBSECTION A: Live Webcam Interface -->
                        <div id="webcamSection">
                            <div class="gt-viewfinder-container mb-3">
                                <!-- Status Overlay Badge -->
                                <div class="gt-viewfinder-status" id="cameraStatusBadge">
                                    <span class="gt-pulse-dot"></span>
                                    <span id="cameraStatusText">Camera Inactive</span>
                                </div>

                                <!-- Real-Time HUD Overlay -->
                                <div class="gt-viewfinder-hud" id="cameraHud">
                                    <span class="badge bg-dark bg-opacity-75 border border-secondary text-info small" id="fpsBadge">
                                        <i class="bi bi-speedometer2 me-1"></i>-- FPS
                                    </span>
                                    <span class="badge bg-dark bg-opacity-75 border border-secondary text-light small" id="handDetectionBadge">
                                        <i class="bi bi-hand-index me-1"></i>No Hand
                                    </span>
                                </div>

                                <!-- Scanning Overlay Effect -->
                                <div class="gt-scanline" id="cameraScanline"></div>

                                <!-- Video Element -->
                                <video id="webcamVideo" class="gt-video" autoplay playsinline muted></video>

                                <!-- Landmark Tracking Canvas -->
                                <canvas id="landmarkCanvas" class="gt-landmark-canvas"></canvas>

                                <!-- Placeholder when camera is stopped -->
                                <div class="gt-viewfinder-placeholder" id="viewfinderPlaceholder">
                                    <i class="bi bi-camera-video-off"></i>
                                    <h6 class="text-white mb-2">Camera is currently stopped</h6>
                                    <p class="small text-muted mb-0">Click "Start Camera" below to grant permissions and begin live visual tracking.</p>
                                </div>
                            </div>

                            <!-- Camera Controls Toolbar -->
                            <div class="d-flex flex-wrap gap-2 justify-content-between align-items-center">
                                <div class="d-flex gap-2 align-items-center">
                                    <button type="button" class="gt-btn-primary" id="btnStartCamera" aria-label="Start Camera Feed">
                                        <i class="bi bi-play-fill fs-5"></i> Start Camera
                                    </button>
                                    <button type="button" class="gt-btn-danger" id="btnStopCamera" disabled aria-label="Stop Camera Feed">
                                        <i class="bi bi-stop-fill fs-5"></i> Stop Camera
                                    </button>
                                    <div class="form-check form-switch ms-2 d-none d-sm-inline-flex align-items-center gap-1 mb-0" title="Toggle Continuous Real-Time Recognition via MediaPipe">
                                        <input class="form-check-input" type="checkbox" role="switch" id="continuousRecognitionToggle" checked>
                                        <label class="form-check-label small text-info fw-semibold" for="continuousRecognitionToggle">
                                            Live AI
                                        </label>
                                    </div>
                                </div>

                                <div class="d-flex gap-2 align-items-center">
                                    <button type="button" class="btn btn-sm btn-outline-secondary text-info border-0" data-bs-toggle="modal" data-bs-target="#cameraPermissionModal" title="Camera Permissions and Troubleshooting Guide">
                                        <i class="bi bi-question-circle me-1"></i> Guide
                                    </button>
                                    <button type="button" class="gt-btn-secondary" id="btnCaptureGesture" disabled aria-label="Capture Gesture Frame" title="Capture static snapshot for deep analysis">
                                        <i class="bi bi-camera-fill text-info"></i> Snapshot
                                    </button>
                                </div>
                            </div>
                        </div>

                        <!-- SUBSECTION B: Manual File Upload Interface -->
                        <div id="uploadSection" class="d-none">
                            <input type="file" id="fileInput" class="d-none" accept="image/jpeg,image/png,image/webp">
                            
                            <div class="gt-dropzone mb-3" id="dropzoneArea">
                                <div class="gt-dropzone-icon">
                                    <i class="bi bi-cloud-arrow-up-fill"></i>
                                </div>
                                <h5 class="text-white mb-1">Drag and drop your gesture image here</h5>
                                <p class="text-muted small mb-3">Supports high-resolution JPEG, PNG, or WebP (up to 5 MB)</p>
                                <button type="button" class="gt-btn-secondary btn-sm" onclick="document.getElementById('fileInput').click()">
                                    <i class="bi bi-folder2-open me-1"></i> Browse Files
                                </button>
                            </div>
                        </div>

                        <!-- SUBSECTION C: Captured Frame Preview Card -->
                        <div id="previewContainer" class="d-none mt-4 pt-3 border-top" style="border-color: var(--gt-border-glass) !important;">
                            <div class="d-flex align-items-center justify-content-between mb-2">
                                <span class="small text-uppercase fw-bold text-gradient-cyan">
                                    <i class="bi bi-check-circle-fill me-1"></i> In-Memory Captured Frame
                                </span>
                                <button type="button" class="btn btn-sm btn-link text-danger p-0 text-decoration-none" id="btnClearPreview">
                                    <i class="bi bi-x-circle me-1"></i> Clear
                                </button>
                            </div>

                            <div class="row g-3 align-items-center">
                                <div class="col-sm-5">
                                    <div class="gt-preview-wrapper">
                                        <img id="previewImage" class="gt-preview-img" alt="Captured Frame Preview">
                                    </div>
                                </div>
                                <div class="col-sm-7">
                                    <div class="p-3 rounded-3" style="background: rgba(0,0,0,0.3); border: 1px solid var(--gt-border-glass);">
                                        <div class="small text-muted mb-1">Source: <span id="previewSource" class="text-white fw-semibold">Live Snapshot</span></div>
                                        <div class="small text-muted mb-1">Dimensions: <span id="previewDimensions" class="text-white fw-semibold">1280 × 720 px</span></div>
                                        <div class="small text-muted mb-2">Byte Size: <span id="previewSize" class="text-white fw-semibold">84.5 KB</span></div>
                                        <div class="d-flex align-items-center justify-content-between mb-3">
                                            <span class="badge bg-success-subtle text-success border border-success-subtle small">
                                                <i class="bi bi-shield-check me-1"></i> Verified In-Memory
                                            </span>
                                        </div>
                                        <button type="button" class="gt-btn-primary w-100" id="btnAnalyzeGesture">
                                            <i class="bi bi-cpu-fill text-warning"></i>
                                            <span id="analyzeBtnText">Analyze Gesture with Gemini</span>
                                            <span id="analyzeSpinner" class="spinner-border spinner-border-sm d-none" role="status" aria-hidden="true"></span>
                                        </button>
                                    </div>
                                </div>
                            </div>
                        </div>

                    </div>
                </div>
            </div>

            <!-- RIGHT COLUMN: Recognition Result & Feedback Panel -->
            <div class="col-lg-5">
                <div class="gt-card h-100 d-flex flex-column">
                    <div class="gt-card-header">
                        <div class="d-flex align-items-center gap-2">
                            <i class="bi bi-chat-square-quote text-info fs-5"></i>
                            <h5 class="mb-0">Recognition & Speech</h5>
                        </div>
                        <div class="d-flex align-items-center gap-2">
                            <span class="badge rounded-pill bg-dark border border-secondary text-info small px-2 py-1" id="engineStatusBadge" title="Real-time on-device client recognition">
                                <i class="bi bi-cpu-fill text-info me-1"></i>MediaPipe Engine
                            </span>
                            <span class="badge rounded-pill bg-dark border border-secondary text-muted small px-2 py-1 d-none d-md-inline-block" id="geminiStatusBadge" title="Multimodal deep diagnostic analysis">
                                <i class="bi bi-stars text-warning me-1"></i>Gemini: Ready
                            </span>
                        </div>
                    </div>

                    <div class="gt-card-body flex-grow-1 d-flex flex-column justify-content-between">
                        
                        <!-- Recognition Status & Detected Gesture -->
                        <div>
                            <div class="mb-3">
                                <label class="small text-uppercase text-muted fw-bold mb-2">Gesture State</label>
                                <div>
                                    <span class="gt-result-badge text-muted border-secondary" id="resultGestureBadge">
                                        <i class="bi bi-hourglass-split me-2"></i>AWAITING INPUT
                                    </span>
                                </div>
                            </div>

                            <div class="mb-4">
                                <label class="small text-uppercase text-muted fw-bold mb-2">Recognized Gesture</label>
                                <h4 class="text-white fw-bold mb-0" id="resultGestureName" aria-live="polite">No Gesture Captured Yet</h4>
                            </div>

                            <!-- Spoken Phrase Box -->
                            <div class="mb-4">
                                <label class="small text-uppercase text-muted fw-bold mb-2">Mapped Spoken Message</label>
                                <div class="gt-result-phrase" id="resultMessageText" aria-live="polite">
                                    Start the webcam or upload a photo to capture a hand gesture.
                                </div>
                            </div>

                            <!-- Confidence Meter -->
                            <div class="mb-4">
                                <div class="d-flex justify-content-between small text-muted mb-1">
                                    <span>Model Confidence</span>
                                    <span class="text-white fw-semibold" id="resultConfidenceVal">0%</span>
                                </div>
                                <div class="gt-confidence-bar-bg">
                                    <div class="gt-confidence-bar-fill" id="resultConfidenceBar" style="width: 0%;"></div>
                                </div>
                            </div>

                            <!-- AI Reasoning / Explanation -->
                            <div class="mb-4">
                                <label class="small text-uppercase text-muted fw-bold mb-1">System Diagnostic</label>
                                <p class="small text-muted mb-0" id="resultExplanationText" aria-live="polite">
                                    Awaiting image input from the camera or manual upload.
                                </p>
                            </div>

                            <!-- Diagnostic Guide / Error Notice -->
                            <div class="alert alert-dark d-none border-secondary py-2 px-3 small" id="devModeBanner" style="background: rgba(139, 92, 246, 0.08);">
                                <i class="bi bi-info-circle-fill text-warning me-1"></i>
                                <span id="devModeBannerText">Backend configuration guidance will appear here.</span>
                            </div>
                        </div>

                        <!-- Speech Synthesis Controls -->
                        <div class="pt-3 border-top" style="border-color: var(--gt-border-glass) !important;">
                            <div class="d-flex justify-content-between align-items-center mb-2">
                                <span class="small text-uppercase text-muted fw-bold">
                                    <i class="bi bi-volume-up-fill text-info me-1"></i> Speech Synthesis Controls
                                </span>
                                <!-- Speaking State Indicator -->
                                <span class="badge rounded-pill bg-dark border border-secondary text-muted small" id="speechStatusBadge" aria-live="polite">
                                    <i class="bi bi-soundwave me-1"></i> Idle
                                </span>
                            </div>

                            <div class="row g-2 mb-3">
                                <div class="col-12">
                                    <label class="small text-muted mb-1">Voice Selection:</label>
                                    <select class="form-select form-select-sm bg-dark text-white border-secondary" id="voiceSelect" aria-label="Select Voice">
                                        <option value="">Loading system speech voices...</option>
                                    </select>
                                </div>
                                <div class="col-6">
                                    <div class="d-flex justify-content-between small text-muted mb-1">
                                        <span>Speed (Rate):</span>
                                        <span id="rateValue" class="text-white fw-bold">1.0x</span>
                                    </div>
                                    <input type="range" class="form-range" id="voiceRate" min="0.5" max="2.0" step="0.1" value="1.0">
                                </div>
                                <div class="col-6">
                                    <div class="d-flex justify-content-between small text-muted mb-1">
                                        <span>Pitch:</span>
                                        <span id="pitchValue" class="text-white fw-bold">1.0</span>
                                    </div>
                                    <input type="range" class="form-range" id="voicePitch" min="0.5" max="1.5" step="0.1" value="1.0">
                                </div>
                            </div>

                            <!-- Audio Action Controls: Speak, Replay, Stop -->
                            <div class="btn-group w-100" role="group" aria-label="Speech Action Controls">
                                <button type="button" class="btn btn-sm btn-outline-info" id="btnSpeakSpeech" title="Speak the displayed phrase">
                                    <i class="bi bi-play-fill me-1"></i> Speak
                                </button>
                                <button type="button" class="btn btn-sm btn-outline-secondary text-light" id="btnReplaySpeech" title="Replay the last spoken phrase">
                                    <i class="bi bi-arrow-repeat me-1"></i> Replay
                                </button>
                                <button type="button" class="btn btn-sm btn-outline-danger" id="btnStopSpeech" title="Immediately cancel speech playback">
                                    <i class="bi bi-stop-fill me-1"></i> Stop
                                </button>
                            </div>
                        </div>

                    </div>
                </div>
            </div>

        </div>
    </main>

    <!-- 4. GESTURE REFERENCE SECTION -->
    <section class="container py-5" id="gestures">
        <div class="text-center mb-5">
            <span class="badge rounded-pill px-3 py-2 mb-2" style="background: rgba(6, 182, 212, 0.15); color: #38bdf8; border: 1px solid rgba(6, 182, 212, 0.3);">
                <i class="bi bi-collection me-1"></i> Controlled Vocabulary
            </span>
            <h2 class="fw-bold">Supported Hand Gestures</h2>
            <p class="text-muted mx-auto" style="max-width: 600px;">
                The initial release recognizes exactly five predefined static gestures. Each gesture deterministically triggers its designated natural sentence.
            </p>
        </div>

        <div class="row g-3 row-cols-1 row-cols-sm-2 row-cols-lg-5">
            
            <!-- Card 1: Thumbs Up -->
            <div class="col">
                <div class="gt-gesture-card">
                    <div class="gt-gesture-icon-wrap">
                        <i class="bi bi-hand-thumbs-up-fill"></i>
                    </div>
                    <h6 class="text-white fw-bold mb-1">THUMBS_UP</h6>
                    <p class="small text-muted mb-3">Closed fist with thumb extended straight upward</p>
                    <div class="p-2 rounded small fw-semibold" style="background: rgba(16, 185, 129, 0.15); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.3);">
                        "Yes, that's correct."
                    </div>
                </div>
            </div>

            <!-- Card 2: Thumbs Down -->
            <div class="col">
                <div class="gt-gesture-card">
                    <div class="gt-gesture-icon-wrap" style="color: #f87171; border-color: rgba(239, 68, 68, 0.2); background: rgba(239, 68, 68, 0.1);">
                        <i class="bi bi-hand-thumbs-down-fill"></i>
                    </div>
                    <h6 class="text-white fw-bold mb-1">THUMBS_DOWN</h6>
                    <p class="small text-muted mb-3">Closed fist with thumb extended straight downward</p>
                    <div class="p-2 rounded small fw-semibold" style="background: rgba(239, 68, 68, 0.15); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.3);">
                        "No, that's incorrect."
                    </div>
                </div>
            </div>

            <!-- Card 3: Open Palm -->
            <div class="col">
                <div class="gt-gesture-card">
                    <div class="gt-gesture-icon-wrap" style="color: #38bdf8; border-color: rgba(6, 182, 212, 0.2); background: rgba(6, 182, 212, 0.1);">
                        <i class="bi bi-hand-index-thumb"></i>
                    </div>
                    <h6 class="text-white fw-bold mb-1">OPEN_PALM</h6>
                    <p class="small text-muted mb-3">Open flat hand with all five fingers extended</p>
                    <div class="p-2 rounded small fw-semibold" style="background: rgba(6, 182, 212, 0.15); color: #38bdf8; border: 1px solid rgba(6, 182, 212, 0.3);">
                        "Please stop or wait."
                    </div>
                </div>
            </div>

            <!-- Card 4: V Sign -->
            <div class="col">
                <div class="gt-gesture-card">
                    <div class="gt-gesture-icon-wrap">
                        <i class="bi bi-peace"></i>
                    </div>
                    <h6 class="text-white fw-bold mb-1">V_SIGN</h6>
                    <p class="small text-muted mb-3">Index and middle fingers in a V; others folded</p>
                    <div class="p-2 rounded small fw-semibold" style="background: rgba(139, 92, 246, 0.15); color: #c084fc; border: 1px solid rgba(139, 92, 246, 0.3);">
                        "Peace and greetings."
                    </div>
                </div>
            </div>

            <!-- Card 5: Index Pointing -->
            <div class="col">
                <div class="gt-gesture-card">
                    <div class="gt-gesture-icon-wrap" style="color: #fbbf24; border-color: rgba(245, 158, 11, 0.2); background: rgba(245, 158, 11, 0.1);">
                        <i class="bi bi-hand-index-fill"></i>
                    </div>
                    <h6 class="text-white fw-bold mb-1">INDEX_POINTING</h6>
                    <p class="small text-muted mb-3">Index finger pointing straight upward; other fingers curled into palm</p>
                    <div class="p-2 rounded small fw-semibold" style="background: rgba(245, 158, 11, 0.15); color: #fbbf24; border: 1px solid rgba(245, 158, 11, 0.3);">
                        "Please look there."
                    </div>
                </div>
            </div>

        </div>

        <!-- Best Practices & Environmental Guidance Banner -->
        <div class="row mt-4">
            <div class="col-12">
                <div class="p-3 p-md-4 rounded-3" style="background: rgba(13, 20, 36, 0.7); border: 1px solid var(--gt-border-glass);">
                    <div class="d-flex align-items-center gap-2 mb-2">
                        <i class="bi bi-lightbulb-fill text-warning fs-5"></i>
                        <h6 class="text-white mb-0 fw-bold">Tips for Optimal Recognition Accuracy</h6>
                    </div>
                    <div class="row g-3 small text-muted pt-2">
                        <div class="col-md-3">
                            <strong class="text-white d-block mb-1"><i class="bi bi-arrows-fullscreen text-info me-1"></i> Framing & Distance</strong>
                            Position your hand between 0.5 and 1.5 meters from the webcam, centered within the frame.
                        </div>
                        <div class="col-md-3">
                            <strong class="text-white d-block mb-1"><i class="bi bi-brightness-high text-warning me-1"></i> Balanced Lighting</strong>
                            Ensure your palm and fingers are clearly illuminated; avoid harsh direct backlighting or deep shadows.
                        </div>
                        <div class="col-md-3">
                            <strong class="text-white d-block mb-1"><i class="bi bi-person-check text-primary me-1"></i> Single Hand Focus</strong>
                            Keep one hand prominently in the frame to prevent multi-hand ambiguity or conflicting signs.
                        </div>
                        <div class="col-md-3">
                            <strong class="text-white d-block mb-1"><i class="bi bi-hand-thumbs-up text-success me-1"></i> Steady Posture</strong>
                            Hold the hand stationary for half a second before capturing to eliminate motion blur.
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- 5. RECOGNITION AUDIT HISTORY SECTION -->
    <section class="container py-5" id="history">
        <div class="gt-card">
            <div class="gt-card-header flex-wrap gap-3">
                <div class="d-flex align-items-center gap-2">
                    <i class="bi bi-clock-history text-primary fs-5"></i>
                    <h5 class="mb-0">Recognition Audit History</h5>
                    <span class="badge rounded-pill bg-dark border border-secondary text-info ms-2" id="historyCountBadge">
                        0 records
                    </span>
                </div>

                <!-- Filter & Action Controls -->
                <div class="d-flex align-items-center gap-2 flex-wrap ms-auto">
                    <!-- Gesture Filter Selector -->
                    <div class="d-flex align-items-center gap-2">
                        <label for="historyGestureFilter" class="small text-muted mb-0">Filter:</label>
                        <select class="form-select form-select-sm bg-dark text-white border-secondary" id="historyGestureFilter" style="min-width: 155px;">
                            <option value="ALL" selected>All Gestures</option>
                            <option value="THUMBS_UP">Thumbs Up</option>
                            <option value="THUMBS_DOWN">Thumbs Down</option>
                            <option value="OPEN_PALM">Open Palm</option>
                            <option value="V_SIGN">V Sign</option>
                            <option value="INDEX_POINTING">Index Pointing</option>
                            <option value="UNKNOWN">Unknown</option>
                        </select>
                    </div>

                    <!-- Refresh Button -->
                    <button type="button" class="btn btn-sm btn-outline-secondary text-light" id="btnRefreshHistory" title="Refresh Latest Records">
                        <i class="bi bi-arrow-clockwise me-1"></i> Refresh
                    </button>

                    <!-- Clear History Button with Confirmation -->
                    <button type="button" class="btn btn-sm btn-outline-danger" id="btnClearHistory" title="Clear All History Records">
                        <i class="bi bi-trash3 me-1"></i> Clear History
                    </button>
                </div>
            </div>

            <div class="gt-card-body">
                <!-- Status & Error Alert -->
                <div id="historyStatusAlert" class="alert alert-info py-2 px-3 small mb-3 border-info d-none"></div>

                <!-- Loading Spinner -->
                <div id="historyLoadingSpinner" class="text-center py-4 d-none">
                    <div class="spinner-border text-primary spinner-border-sm me-2" role="status"></div>
                    <span class="text-muted small">Loading recognition history from MySQL...</span>
                </div>

                <!-- Empty State Indicator -->
                <div id="historyEmptyState" class="text-center py-5 d-none">
                    <i class="bi bi-journal-x fs-1 text-muted mb-3 d-block"></i>
                    <h6 class="text-white mb-2">No Recognition Records Found</h6>
                    <p class="text-muted small mb-0">Capture a live gesture with your webcam or upload a photo to start recording audit logs.</p>
                </div>

                <!-- History Records Table -->
                <div id="historyTableContainer" class="table-responsive">
                    <table class="table table-dark table-hover table-striped mb-0 align-middle">
                        <thead class="text-secondary small text-uppercase" style="border-bottom: 1px solid var(--gt-border-glass);">
                            <tr>
                                <th scope="col" style="width: 70px;">ID</th>
                                <th scope="col" style="width: 140px;">Gesture</th>
                                <th scope="col">Spoken Message</th>
                                <th scope="col" style="width: 150px;">Confidence</th>
                                <th scope="col" style="width: 110px;">Source</th>
                                <th scope="col">AI Explanation</th>
                                <th scope="col" style="width: 170px;">Logged At</th>
                            </tr>
                        </thead>
                        <tbody id="historyTableBody">
                            <!-- Injected dynamically by history.js -->
                        </tbody>
                    </table>
                </div>
            </div>
        </div>
    </section>

    <!-- 6. HOW IT WORKS SECTION -->
    <section class="container py-5" id="how-it-works">
        <div class="gt-card p-4 p-lg-5">
            <div class="row align-items-center g-4">
                <div class="col-lg-5">
                    <span class="badge rounded-pill px-3 py-2 mb-2" style="background: rgba(139, 92, 246, 0.15); color: #c084fc; border: 1px solid rgba(139, 92, 246, 0.3);">
                        Architecture Overview
                    </span>
                    <h3 class="fw-bold mb-3">How GestureTalk AI Operates</h3>
                    <p class="text-muted">
                        A privacy-first pipeline engineered in pure Jakarta EE and Java 21, eliminating client-side model weight bloat and guaranteeing zero permanent image retention.
                    </p>
                </div>
                <div class="col-lg-7">
                    <div class="d-flex flex-column gap-3">
                        <div class="d-flex gap-3 align-items-start">
                            <div class="badge rounded-circle p-2 bg-primary fs-6" style="width: 32px; height: 32px;">1</div>
                            <div>
                                <h6 class="text-white mb-1">In-Memory Frame Capture</h6>
                                <p class="small text-muted mb-0">The browser rasterizes the live video feed into a volatile JPEG Blob without touching local disk storage.</p>
                            </div>
                        </div>
                        <div class="d-flex gap-3 align-items-start">
                            <div class="badge rounded-circle p-2 bg-info fs-6" style="width: 32px; height: 32px;">2</div>
                            <div>
                                <h6 class="text-white mb-1">Secure Java Servlet Ingestion</h6>
                                <p class="small text-muted mb-0">The Jakarta Servlet validates magic bytes, size limits (<= 5MB), and feeds the in-memory stream directly to Gemini Vision.</p>
                            </div>
                        </div>
                        <div class="d-flex gap-3 align-items-start">
                            <div class="badge rounded-circle p-2 bg-purple fs-6" style="width: 32px; height: 32px; background-color: #8b5cf6;">3</div>
                            <div>
                                <h6 class="text-white mb-1">Deterministic Java Mapping & Speech</h6>
                                <p class="small text-muted mb-0">Java maps the classified token to fixed phrases, and browser SpeechSynthesis vocalizes the sentence instantly.</p>
                            </div>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- 6. ABOUT SECTION & DISCLAIMER -->
    <section class="container py-5" id="about">
        <div class="row justify-content-center text-center">
            <div class="col-lg-8">
                <h4 class="text-white fw-bold mb-3">About GestureTalk AI</h4>
                <p class="text-muted mb-4">
                    Developed as an academic Advanced Internet Programming demonstration showcasing pure Jakarta EE 10 architecture on Apache Tomcat 10.1, official Google GenAI Java SDK integration, and native browser SpeechSynthesis.
                </p>
                <div class="alert alert-dark border-secondary py-3 text-start small">
                    <div class="d-flex gap-2">
                        <i class="bi bi-shield-exclamation text-warning fs-5"></i>
                        <div>
                            <strong class="text-white">Academic Prototype Disclaimer:</strong>
                            <span class="text-muted">
                                GestureTalk AI recognizes discrete static hand gestures mapped to predefined sentences. It is <strong>not</strong> a full sign-language translation system (e.g. ASL, ISL) and does not translate continuous spatial discourse or dynamic facial syntax.
                            </span>
                        </div>
                    </div>
                </div>
            </div>
        </div>
    </section>

    <!-- 7. FOOTER -->
    <footer class="gt-footer">
        <div class="container text-center">
            <p class="mb-2 text-muted">
                <strong>GestureTalk AI</strong> &bull; Advanced Internet Programming Architecture Showcase
            </p>
            <p class="small text-secondary mb-0">
                Engineered with Java 21 LTS &bull; Jakarta EE 10 &bull; Apache Tomcat 10.1 &bull; Google GenAI Java SDK
            </p>
        </div>
    </footer>

    <!-- 8. TOAST NOTIFICATION CONTAINER -->
    <div class="gt-toast-container">
        <div id="appToast" class="toast gt-toast" role="alert" aria-live="assertive" aria-atomic="true">
            <div class="toast-header bg-dark text-white border-bottom border-secondary" id="toastHeader">
                <i class="bi bi-info-circle-fill text-info me-2"></i>
                <strong class="me-auto" id="toastTitle">Notification</strong>
                <small class="text-muted">Just now</small>
                <button type="button" class="btn-close btn-close-white" data-bs-dismiss="toast" aria-label="Close"></button>
            </div>
            <div class="toast-body small" id="toastBody">
                Notification message.
            </div>
        </div>
    </div>

    <!-- 9. CAMERA PERMISSION & TROUBLESHOOTING MODAL -->
    <div class="modal fade" id="cameraPermissionModal" tabindex="-1" aria-labelledby="cameraPermissionModalLabel" aria-hidden="true">
        <div class="modal-dialog modal-dialog-centered modal-lg">
            <div class="modal-content gt-card text-white border-secondary">
                <div class="modal-header border-secondary">
                    <h5 class="modal-title d-flex align-items-center gap-2" id="cameraPermissionModalLabel">
                        <i class="bi bi-camera-video text-primary"></i> Camera Permission &amp; Troubleshooting Guide
                    </h5>
                    <button type="button" class="btn-close btn-close-white" data-bs-dismiss="modal" aria-label="Close"></button>
                </div>
                <div class="modal-body small text-muted">
                    <p class="text-white mb-3">If your browser is blocking camera access or showing an error, follow the instructions for your environment:</p>
                    
                    <div class="row g-3">
                        <div class="col-md-6">
                            <div class="p-3 rounded h-100" style="background: rgba(0,0,0,0.3); border: 1px solid var(--gt-border-glass);">
                                <strong class="text-white d-block mb-2"><i class="bi bi-browser-chrome text-warning me-1"></i> Google Chrome &amp; Microsoft Edge</strong>
                                <ol class="ps-3 mb-0">
                                    <li>Click the <strong>tune / padlock icon</strong> on the left of the address bar.</li>
                                    <li>Ensure <strong>Camera</strong> is set to <strong>Allow</strong>.</li>
                                    <li>Reload the page to start the live video feed.</li>
                                </ol>
                            </div>
                        </div>
                        
                        <div class="col-md-6">
                            <div class="p-3 rounded h-100" style="background: rgba(0,0,0,0.3); border: 1px solid var(--gt-border-glass);">
                                <strong class="text-white d-block mb-2"><i class="bi bi-apple text-info me-1"></i> Apple Safari (macOS &amp; iOS)</strong>
                                <ol class="ps-3 mb-0">
                                    <li>On macOS: Open <strong>Safari &gt; Settings &gt; Websites &gt; Camera</strong> and choose <strong>Allow</strong>.</li>
                                    <li>On iOS: Open <strong>Settings &gt; Safari &gt; Camera</strong> and select <strong>Ask</strong> or <strong>Allow</strong>.</li>
                                </ol>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="p-3 rounded h-100" style="background: rgba(0,0,0,0.3); border: 1px solid var(--gt-border-glass);">
                                <strong class="text-white d-block mb-2"><i class="bi bi-browser-firefox text-danger me-1"></i> Mozilla Firefox</strong>
                                <ol class="ps-3 mb-0">
                                    <li>Click the camera icon with red slash next to the URL.</li>
                                    <li>Click <strong>Clear Blocked Status</strong> (X).</li>
                                    <li>Click <strong>Start Camera</strong> again and grant permission.</li>
                                </ol>
                            </div>
                        </div>

                        <div class="col-md-6">
                            <div class="p-3 rounded h-100" style="background: rgba(0,0,0,0.3); border: 1px solid var(--gt-border-glass);">
                                <strong class="text-white d-block mb-2"><i class="bi bi-exclamation-triangle text-warning me-1"></i> Device Locked by Another App</strong>
                                <p class="mb-0">Webcams can only be accessed by one application at a time. Close Zoom, Microsoft Teams, Discord, or any other browser window that might be using the camera.</p>
                            </div>
                        </div>
                    </div>
                </div>
                <div class="modal-footer border-secondary">
                    <button type="button" class="btn btn-sm btn-outline-secondary text-light" data-bs-dismiss="modal">Close</button>
                </div>
            </div>
        </div>
    </div>

    <!-- Bootstrap 5.3 Bundle JS -->
    <script src="https://cdn.jsdelivr.net/npm/bootstrap@5.3.3/dist/js/bootstrap.bundle.min.js" integrity="sha384-YvpcrYf0tY3lHB60NNkmXc5s9fDVZLESaAA55NDzOxhy9GkcIdslK1eN7N6jIeHz" crossorigin="anonymous"></script>

    <!-- Application JavaScript Modules -->
    <script src="${pageContext.request.contextPath}/static/js/webcam.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/upload.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/tts.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/history.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/mediapipe-recognizer.js"></script>
    <script src="${pageContext.request.contextPath}/static/js/app.js"></script>

</body>
</html>

