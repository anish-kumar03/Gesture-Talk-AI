/**
 * GestureTalk AI — Google MediaPipe Gesture Recognizer Integration
 * Real-time client-side hand tracking and gesture recognition running on WebAssembly.
 * Fully supports offline local hosting with automatic CDN fallback.
 */

// 21 Hand Landmark skeletal connections for drawing
const HAND_CONNECTIONS = [
    // Palm base
    [0, 1], [0, 5], [5, 9], [9, 13], [13, 17], [0, 17],
    // Thumb
    [1, 2], [2, 3], [3, 4],
    // Index finger
    [5, 6], [6, 7], [7, 8],
    // Middle finger
    [9, 10], [10, 11], [11, 12],
    // Ring finger
    [13, 14], [14, 15], [15, 16],
    // Pinky finger
    [17, 18], [18, 19], [19, 20]
];

class MediaPipeGestureRecognizer {
    constructor(options = {}) {
        this.options = Object.assign({
            consecutiveFramesThreshold: 4,  // Frames required to confirm a stable gesture
            confidenceThreshold: 0.70,     // Minimum per-frame confidence score
            cooldownIntervalMs: 2500,       // Debounce cooldown before re-speaking SAME gesture
            transitionIntervalMs: 1000,    // Transition window before speaking DIFFERENT gesture
            noHandResetFrames: 4,          // Consecutive frames without hand before resetting state
            basePath: ''
        }, options);

        this.recognizer = null;
        this.isReady = false;
        this.isInitializing = false;
        this.isRunning = false;
        this.initError = null;

        // Stability & smoothing state
        this.candidateGesture = null;
        this.candidateCount = 0;
        this.candidateScores = [];
        this.lastSpokenGesture = null;
        this.lastSpokenTime = 0;
        this.noHandCount = 0;

        // FPS calculation
        this.lastFrameTime = performance.now();
        this.frameCount = 0;
        this.currentFps = 0;
        this.lastFpsUpdate = performance.now();

        // Animation frame handle
        this.animFrameId = null;
        this.videoElement = null;
        this.canvasElement = null;
        this.canvasCtx = null;

        // Event callbacks
        this.onGestureConfirmed = null; // ({ gestureCode, confidence, sourceType }) => {}
        this.onLiveUpdate = null;       // ({ handDetected, rawGesture, mappedGesture, confidence, fps, landmarks }) => {}
        this.onNoHand = null;           // () => {}
        this.onError = null;            // (error) => {}
    }

    /**
     * Resolves the application base path for loading local static assets.
     */
    _resolveBasePath() {
        if (this.options.basePath) {
            return this.options.basePath;
        }
        let path = window.location.pathname;
        if (path.endsWith('.jsp') || path.endsWith('.html')) {
            path = path.substring(0, path.lastIndexOf('/'));
        }
        if (path.endsWith('/')) {
            path = path.substring(0, path.length - 1);
        }
        return path;
    }

    /**
     * Initializes the MediaPipe Tasks Vision FilesetResolver and GestureRecognizer.
     * Tries local assets first; falls back to jsdelivr CDN if needed.
     */
    async initialize() {
        if (this.isReady) return true;
        if (this.isInitializing) return false;

        this.isInitializing = true;
        this.initError = null;
        const basePath = this._resolveBasePath();

        try {
            console.info('[MediaPipe] Initializing Gesture Recognizer...');

            // Step 1: Import vision bundle module (local first, fallback to CDN)
            let visionModule;
            const localBundleUrl = `${basePath}/static/vendor/mediapipe/vision_bundle.mjs`;
            const cdnBundleUrl = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14/vision_bundle.mjs';

            try {
                visionModule = await import(localBundleUrl);
                console.info('[MediaPipe] Loaded vision_bundle.mjs from local storage.');
            } catch (bundleErr) {
                console.warn('[MediaPipe] Local bundle load failed, attempting CDN fallback:', bundleErr);
                visionModule = await import(cdnBundleUrl);
                console.info('[MediaPipe] Loaded vision_bundle.mjs from CDN.');
            }

            const { FilesetResolver, GestureRecognizer } = visionModule;

            // Step 2: Initialize FilesetResolver for WASM runtime (local first, fallback to CDN)
            let vision;
            const localWasmPath = `${basePath}/static/vendor/mediapipe/wasm`;
            const cdnWasmPath = 'https://cdn.jsdelivr.net/npm/@mediapipe/tasks-vision@0.10.14/wasm';

            try {
                vision = await FilesetResolver.forVisionTasks(localWasmPath);
                console.info('[MediaPipe] Resolved WASM files from local directory.');
            } catch (wasmErr) {
                console.warn('[MediaPipe] Local WASM resolution failed, falling back to CDN:', wasmErr);
                vision = await FilesetResolver.forVisionTasks(cdnWasmPath);
                console.info('[MediaPipe] Resolved WASM files from CDN.');
            }

            // Step 3: Create GestureRecognizer instance with model asset
            const modelPath = `${basePath}/static/models/gesture_recognizer.task`;

            try {
                // Try GPU delegate first for smooth 30+ FPS
                this.recognizer = await GestureRecognizer.createFromOptions(vision, {
                    baseOptions: {
                        modelAssetPath: modelPath,
                        delegate: 'GPU'
                    },
                    runningMode: 'VIDEO',
                    numHands: 1
                });
                console.info('[MediaPipe] Gesture Recognizer initialized successfully using GPU delegate.');
            } catch (gpuErr) {
                console.warn('[MediaPipe] GPU delegate failed, falling back to CPU delegate:', gpuErr);
                this.recognizer = await GestureRecognizer.createFromOptions(vision, {
                    baseOptions: {
                        modelAssetPath: modelPath,
                        delegate: 'CPU'
                    },
                    runningMode: 'VIDEO',
                    numHands: 1
                });
                console.info('[MediaPipe] Gesture Recognizer initialized successfully using CPU delegate.');
            }

            this.isReady = true;
            this.isInitializing = false;
            return true;

        } catch (error) {
            this.isInitializing = false;
            this.isReady = false;
            this.initError = error;
            console.error('[MediaPipe] Initialization failed:', error);
            if (typeof this.onError === 'function') {
                this.onError(error);
            }
            throw error;
        }
    }

    /**
     * Maps MediaPipe built-in gesture categories to GestureTalk AI controlled vocabulary.
     * MediaPipe built-ins: Thumb_Up, Thumb_Down, Open_Palm, Victory, Pointing_Up, Closed_Fist, ILoveYou, None.
     */
    mapGestureCategory(categoryName) {
        if (!categoryName) return 'UNKNOWN';

        switch (categoryName) {
            case 'Thumb_Up':
                return 'THUMBS_UP';
            case 'Thumb_Down':
                return 'THUMBS_DOWN';
            case 'Open_Palm':
                return 'OPEN_PALM';
            case 'Victory':
                return 'V_SIGN';
            case 'Pointing_Up':
                return 'INDEX_POINTING';
            case 'Closed_Fist':
            case 'ILoveYou':
            case 'None':
            default:
                return 'UNKNOWN';
        }
    }

    /**
     * Starts continuous real-time inference loop on the webcam video element.
     */
    start(videoElement, canvasElement) {
        if (!this.isReady || !this.recognizer) {
            console.warn('[MediaPipe] Cannot start: recognizer is not ready.');
            return;
        }

        this.videoElement = videoElement;
        this.canvasElement = canvasElement;
        if (this.canvasElement) {
            this.canvasCtx = this.canvasElement.getContext('2d');
            this.canvasElement.classList.add('active');
        }

        this.isRunning = true;
        this._resetSmoothingState();
        this.lastFrameTime = performance.now();
        this.lastFpsUpdate = performance.now();
        this.frameCount = 0;

        console.info('[MediaPipe] Continuous inference loop started.');
        this._scheduleNextFrame();
    }

    /**
     * Stops the continuous inference loop and clears the landmark canvas.
     */
    stop() {
        this.isRunning = false;
        if (this.animFrameId) {
            cancelAnimationFrame(this.animFrameId);
            this.animFrameId = null;
        }
        if (this.canvasCtx && this.canvasElement) {
            this.canvasCtx.clearRect(0, 0, this.canvasElement.width, this.canvasElement.height);
            this.canvasElement.classList.remove('active');
        }
        this._resetSmoothingState();
        console.info('[MediaPipe] Continuous inference loop stopped.');
    }

    _scheduleNextFrame() {
        if (!this.isRunning) return;
        this.animFrameId = requestAnimationFrame((timestamp) => this._processVideoFrame(timestamp));
    }

    /**
     * Executes inference on the current webcam frame.
     */
    _processVideoFrame(timestamp) {
        if (!this.isRunning) return;

        const video = this.videoElement;
        if (!video || video.readyState < 2 || video.paused || video.ended) {
            // Video not ready yet, re-schedule
            this._scheduleNextFrame();
            return;
        }

        // 1. Calculate FPS
        this.frameCount++;
        const elapsed = timestamp - this.lastFpsUpdate;
        if (elapsed >= 500) {
            this.currentFps = Math.round((this.frameCount * 1000) / elapsed);
            this.frameCount = 0;
            this.lastFpsUpdate = timestamp;
        }

        try {
            // 2. Perform MediaPipe gesture recognition
            const results = this.recognizer.recognizeForVideo(video, timestamp);

            // 3. Process detection output
            let handDetected = false;
            let rawGesture = 'None';
            let mappedGesture = 'UNKNOWN';
            let confidence = 0.0;
            let landmarks = null;

            if (results && results.gestures && results.gestures.length > 0 && results.gestures[0].length > 0) {
                handDetected = true;
                const topGesture = results.gestures[0][0];
                rawGesture = topGesture.categoryName;
                confidence = topGesture.score;
                mappedGesture = this.mapGestureCategory(rawGesture);

                if (results.landmarks && results.landmarks.length > 0) {
                    landmarks = results.landmarks[0];
                }
            }

            // 4. Draw landmarks overlay
            this._drawOverlay(landmarks);

            // 5. Apply prediction smoothing & stability logic
            this._evaluateStability(handDetected, mappedGesture, confidence);

            // 6. Emit live update callback for HUD/status indicators
            if (typeof this.onLiveUpdate === 'function') {
                this.onLiveUpdate({
                    handDetected,
                    rawGesture,
                    mappedGesture,
                    confidence,
                    fps: this.currentFps,
                    landmarks
                });
            }

        } catch (err) {
            console.error('[MediaPipe] Error during frame recognition:', err);
        }

        // Re-schedule next frame
        this._scheduleNextFrame();
    }

    /**
     * Prediction Smoothing & Stability Engine
     * Prevents single-frame noise, debounces speech, and prevents duplicate vocalization.
     */
    _evaluateStability(handDetected, mappedGesture, confidence) {
        const now = performance.now();

        // Case A: No hand detected in frame
        if (!handDetected) {
            this.noHandCount++;
            if (this.noHandCount >= this.options.noHandResetFrames) {
                if (this.candidateGesture !== null) {
                    this._resetSmoothingState();
                    if (typeof this.onNoHand === 'function') {
                        this.onNoHand();
                    }
                }
            }
            return;
        }

        // Case B: Hand detected
        this.noHandCount = 0;

        // Check per-frame confidence threshold
        const meetsThreshold = (confidence >= this.options.confidenceThreshold);

        // If below threshold or unmapped UNKNOWN, don't accumulate as valid candidate
        if (!meetsThreshold || mappedGesture === 'UNKNOWN') {
            this.candidateGesture = null;
            this.candidateCount = 0;
            this.candidateScores = [];
            return;
        }

        // Accumulate identical consecutive predictions
        if (mappedGesture === this.candidateGesture) {
            this.candidateCount++;
            this.candidateScores.push(confidence);
        } else {
            // New candidate gesture detected
            this.candidateGesture = mappedGesture;
            this.candidateCount = 1;
            this.candidateScores = [confidence];
        }

        // Check if candidate reached consecutive frame threshold
        if (this.candidateCount >= this.options.consecutiveFramesThreshold) {
            const candidate = this.candidateGesture;
            const avgConfidence = this.candidateScores.reduce((a, b) => a + b, 0) / this.candidateScores.length;

            // Check Debounce & Duplicate Speech Suppression Rules:
            // 1. If it's the SAME gesture as last spoken, require cooldownIntervalMs (e.g. 2.5s)
            // 2. If it's a NEW gesture, require transitionIntervalMs (e.g. 1.0s)
            const isSameAsLastSpoken = (candidate === this.lastSpokenGesture);
            const requiredCooldown = isSameAsLastSpoken ? this.options.cooldownIntervalMs : this.options.transitionIntervalMs;
            const timeSinceLastSpoken = now - this.lastSpokenTime;

            if (timeSinceLastSpoken >= requiredCooldown) {
                // Confirm gesture!
                this.lastSpokenGesture = candidate;
                this.lastSpokenTime = now;
                // Reset consecutive counter so user must hold gesture or transition
                this.candidateCount = 0;
                this.candidateScores = [];

                console.info(`[MediaPipe] Gesture confirmed: ${candidate} (confidence=${avgConfidence.toFixed(3)})`);

                if (typeof this.onGestureConfirmed === 'function') {
                    this.onGestureConfirmed({
                        gestureCode: candidate,
                        confidence: avgConfidence,
                        sourceType: 'WEBCAM'
                    });
                }
            }
        }
    }

    _resetSmoothingState() {
        this.candidateGesture = null;
        this.candidateCount = 0;
        this.candidateScores = [];
        this.noHandCount = 0;
    }

    /**
     * Draws hand skeletal connections and joint landmarks on the overlay canvas.
     */
    _drawOverlay(landmarks) {
        if (!this.canvasCtx || !this.canvasElement || !this.videoElement) return;

        const canvas = this.canvasElement;
        const ctx = this.canvasCtx;
        const video = this.videoElement;

        // Ensure canvas dimensions match video dimensions
        if (canvas.width !== video.videoWidth || canvas.height !== video.videoHeight) {
            if (video.videoWidth > 0 && video.videoHeight > 0) {
                canvas.width = video.videoWidth;
                canvas.height = video.videoHeight;
            } else {
                return;
            }
        }

        ctx.clearRect(0, 0, canvas.width, canvas.height);

        if (!landmarks || landmarks.length === 0) {
            return;
        }

        const width = canvas.width;
        const height = canvas.height;

        // 1. Draw skeletal connector lines
        ctx.save();
        ctx.lineWidth = 3;
        ctx.strokeStyle = '#06b6d4'; // Cyan neon line
        ctx.shadowColor = '#06b6d4';
        ctx.shadowBlur = 8;
        ctx.lineCap = 'round';
        ctx.lineJoin = 'round';

        for (const [startIdx, endIdx] of HAND_CONNECTIONS) {
            const start = landmarks[startIdx];
            const end = landmarks[endIdx];
            if (start && end) {
                ctx.beginPath();
                ctx.moveTo(start.x * width, start.y * height);
                ctx.lineTo(end.x * width, end.y * height);
                ctx.stroke();
            }
        }
        ctx.restore();

        // 2. Draw joint landmarks
        ctx.save();
        for (let i = 0; i < landmarks.length; i++) {
            const lm = landmarks[i];
            const x = lm.x * width;
            const y = lm.y * height;

            const isFingertip = (i === 4 || i === 8 || i === 12 || i === 16 || i === 20);

            ctx.beginPath();
            if (isFingertip) {
                // Fingertips: larger glowing purple/white points
                ctx.arc(x, y, 6, 0, 2 * Math.PI);
                ctx.fillStyle = '#a855f7';
                ctx.shadowColor = '#c084fc';
                ctx.shadowBlur = 10;
                ctx.fill();
                ctx.lineWidth = 2;
                ctx.strokeStyle = '#ffffff';
                ctx.stroke();
            } else {
                // Internal joints: smaller cyan points
                ctx.arc(x, y, 4, 0, 2 * Math.PI);
                ctx.fillStyle = '#06b6d4';
                ctx.shadowColor = '#06b6d4';
                ctx.shadowBlur = 6;
                ctx.fill();
            }
        }
        ctx.restore();
    }
}

// Attach to window
window.MediaPipeGestureRecognizer = MediaPipeGestureRecognizer;

