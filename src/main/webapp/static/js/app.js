/**
 * GestureTalk AI — Central Client Application Coordinator
 * High-performance real-time gesture recognition powered by Google MediaPipe WebAssembly
 * with optional Gemini Vision multimodal analysis and deterministic speech articulation.
 */

document.addEventListener('DOMContentLoaded', () => {
    // 1. DOM Element Cache
    const modeBtnWebcam = document.getElementById('modeBtnWebcam');
    const modeBtnUpload = document.getElementById('modeBtnUpload');
    const webcamSection = document.getElementById('webcamSection');
    const uploadSection = document.getElementById('uploadSection');

    // Camera Controls
    const btnStartCamera = document.getElementById('btnStartCamera');
    const btnStopCamera = document.getElementById('btnStopCamera');
    const btnCaptureGesture = document.getElementById('btnCaptureGesture');
    const continuousRecognitionToggle = document.getElementById('continuousRecognitionToggle');
    const videoElement = document.getElementById('webcamVideo');
    const landmarkCanvas = document.getElementById('landmarkCanvas');
    const videoPlaceholder = document.getElementById('viewfinderPlaceholder');
    const cameraScanline = document.getElementById('cameraScanline');
    const cameraStatusBadge = document.getElementById('cameraStatusBadge');
    const cameraStatusText = document.getElementById('cameraStatusText');
    const fpsBadge = document.getElementById('fpsBadge');
    const handDetectionBadge = document.getElementById('handDetectionBadge');
    const engineStatusBadge = document.getElementById('engineStatusBadge');
    const geminiStatusBadge = document.getElementById('geminiStatusBadge');

    // Preview Container & Actions
    const previewContainer = document.getElementById('previewContainer');
    const previewImage = document.getElementById('previewImage');
    const previewDimensions = document.getElementById('previewDimensions');
    const previewSize = document.getElementById('previewSize');
    const previewSource = document.getElementById('previewSource');
    const btnClearPreview = document.getElementById('btnClearPreview');
    const btnAnalyzeGesture = document.getElementById('btnAnalyzeGesture');
    const analyzeBtnText = document.getElementById('analyzeBtnText');
    const analyzeSpinner = document.getElementById('analyzeSpinner');

    // Result Panel
    const resultGestureBadge = document.getElementById('resultGestureBadge');
    const resultGestureName = document.getElementById('resultGestureName');
    const resultMessageText = document.getElementById('resultMessageText');
    const resultConfidenceBar = document.getElementById('resultConfidenceBar');
    const resultConfidenceVal = document.getElementById('resultConfidenceVal');
    const resultExplanationText = document.getElementById('resultExplanationText');
    const devModeBanner = document.getElementById('devModeBanner');

    // Speech Controls & State Indicators
    const speechStatusBadge = document.getElementById('speechStatusBadge');
    const voiceSelect = document.getElementById('voiceSelect');
    const voiceRate = document.getElementById('voiceRate');
    const voicePitch = document.getElementById('voicePitch');
    const rateValue = document.getElementById('rateValue');
    const pitchValue = document.getElementById('pitchValue');
    const btnSpeakSpeech = document.getElementById('btnSpeakSpeech');
    const btnReplaySpeech = document.getElementById('btnReplaySpeech');
    const btnStopSpeech = document.getElementById('btnStopSpeech');

    // 2. Initialize Managers
    const webcam = new WebcamManager('webcamVideo', {
        width: { ideal: 1280 },
        height: { ideal: 720 },
        quality: 0.92
    });

    const upload = new UploadManager('dropzoneArea', 'fileInput', {
        maxSizeBytes: 5 * 1024 * 1024
    });

    const speech = new SpeechManager({
        rate: parseFloat(voiceRate.value) || 1.0,
        pitch: parseFloat(voicePitch.value) || 1.0
    });

    let currentCapturedPayload = null;
    let currentSourceType = 'WEBCAM';
    let isRecognitionInProgress = false;

    // Helper: Dynamic API Endpoint resolution
    function getApiEndpoint(subPath) {
        let basePath = window.location.pathname;
        if (basePath.endsWith('.jsp') || basePath.endsWith('.html')) {
            basePath = basePath.substring(0, basePath.lastIndexOf('/'));
        }
        if (basePath.endsWith('/')) {
            basePath = basePath.substring(0, basePath.length - 1);
        }
        return `${basePath}${subPath}`;
    }

    // 3. Initialize Google MediaPipe Gesture Recognizer
    let mpRecognizer = null;

    async function initMediaPipe() {
        if (!window.MediaPipeGestureRecognizer) {
            console.warn('[MediaPipe] MediaPipeGestureRecognizer class not loaded yet. Retrying...');
            setTimeout(initMediaPipe, 200);
            return;
        }

        try {
            if (engineStatusBadge) {
                engineStatusBadge.innerHTML = '<span class="spinner-border spinner-border-sm me-1"></span>Loading MediaPipe...';
            }

            mpRecognizer = new window.MediaPipeGestureRecognizer({
                consecutiveFramesThreshold: 4,
                confidenceThreshold: 0.70,
                cooldownIntervalMs: 2500,
                transitionIntervalMs: 1000,
                noHandResetFrames: 4,
                basePath: getApiEndpoint('')
            });

            // Set event handlers
            mpRecognizer.onGestureConfirmed = handleMediaPipeGestureConfirmed;
            mpRecognizer.onLiveUpdate = handleMediaPipeLiveUpdate;
            mpRecognizer.onNoHand = handleMediaPipeNoHand;
            mpRecognizer.onError = (err) => {
                console.error('[MediaPipe] Recognizer runtime error:', err);
                if (engineStatusBadge) {
                    engineStatusBadge.className = 'badge rounded-pill bg-dark border border-danger text-danger small px-2 py-1';
                    engineStatusBadge.innerHTML = '<i class="bi bi-exclamation-triangle me-1"></i>MediaPipe Fallback';
                }
            };

            await mpRecognizer.initialize();

            if (engineStatusBadge) {
                engineStatusBadge.className = 'badge rounded-pill bg-dark border border-success text-success small px-2 py-1';
                engineStatusBadge.innerHTML = '<i class="bi bi-cpu-fill text-success me-1"></i>MediaPipe Active (Local)';
            }
            console.info('[MediaPipe] Recognizer ready for live inference.');

        } catch (err) {
            console.warn('[MediaPipe] Failed to initialize MediaPipe:', err);
            if (engineStatusBadge) {
                engineStatusBadge.className = 'badge rounded-pill bg-dark border border-warning text-warning small px-2 py-1';
                engineStatusBadge.innerHTML = '<i class="bi bi-cloud-arrow-up text-warning me-1"></i>Gemini Fallback';
            }
            showToast('Local AI Engine Notice', 'MediaPipe failed to load on this browser. Camera snapshots will use Gemini Vision fallback.', 'warning');
        }
    }

    initMediaPipe();

    // 4. MediaPipe Live Callback Handlers
    async function handleMediaPipeGestureConfirmed({ gestureCode, confidence, sourceType }) {
        try {
            const endpoint = getApiEndpoint('/api/gesture-result');
            const response = await fetch(endpoint, {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({
                    gestureCode: gestureCode,
                    confidence: confidence,
                    sourceType: sourceType || 'WEBCAM'
                })
            });

            const data = await response.json();

            if (!response.ok) {
                console.warn('[MediaPipe] Backend rejected gesture payload:', data);
                return;
            }

            // Update UI Result Panel
            renderRecognitionSuccess(data);

            // Refresh History Table
            if (window.historyManager && typeof window.historyManager.loadHistory === 'function') {
                window.historyManager.loadHistory();
            }

            // Vocalize via Web Speech API
            if (data.shouldAutoSpeak && data.mappedMessage) {
                speech.speak(data.mappedMessage);
                showToast('Gesture Vocalized', `Speaking: "${data.mappedMessage}"`, 'success');
            }

        } catch (err) {
            console.error('[MediaPipe] Failed to dispatch gesture result to backend:', err);
        }
    }

    function handleMediaPipeLiveUpdate({ handDetected, rawGesture, mappedGesture, confidence, fps }) {
        if (fpsBadge) {
            fpsBadge.innerHTML = `<i class="bi bi-speedometer2 me-1"></i>${fps} FPS`;
        }
        if (handDetectionBadge) {
            if (handDetected) {
                const confPct = Math.round(confidence * 100);
                const isMapped = mappedGesture !== 'UNKNOWN';
                handDetectionBadge.className = isMapped 
                    ? 'badge bg-success bg-opacity-75 border border-success text-white small'
                    : 'badge bg-warning bg-opacity-75 border border-warning text-dark small';
                handDetectionBadge.innerHTML = `<i class="bi bi-hand-index-thumb me-1"></i>${isMapped ? mappedGesture : 'Hand in Frame'} (${confPct}%)`;
            } else {
                handDetectionBadge.className = 'badge bg-dark bg-opacity-75 border border-secondary text-light small';
                handDetectionBadge.innerHTML = '<i class="bi bi-hand-index me-1"></i>No Hand';
            }
        }
    }

    function handleMediaPipeNoHand() {
        if (handDetectionBadge) {
            handDetectionBadge.className = 'badge bg-dark bg-opacity-75 border border-secondary text-light small';
            handDetectionBadge.innerHTML = '<i class="bi bi-hand-index me-1"></i>No Hand';
        }
    }

    // 5. Speech State Binding & Indicator
    speech.onStateChange = (isSpeaking) => {
        if (isSpeaking) {
            speechStatusBadge.className = 'badge rounded-pill bg-success-subtle text-success border border-success small';
            speechStatusBadge.innerHTML = '<span class="spinner-grow spinner-grow-sm me-1" role="status" aria-hidden="true"></span>Speaking...';
            btnSpeakSpeech.disabled = true;
            btnStopSpeech.disabled = false;
        } else {
            speechStatusBadge.className = 'badge rounded-pill bg-dark border border-secondary text-muted small';
            speechStatusBadge.innerHTML = '<i class="bi bi-soundwave me-1"></i>Idle';
            btnSpeakSpeech.disabled = false;
            btnStopSpeech.disabled = true;
        }
        btnReplaySpeech.disabled = !speech.hasLastMessage();
    };

    speech.onError = (msg) => {
        showToast('Speech Warning', msg, 'warning');
    };

    function populateVoices() {
        if (!speech.isSupported) {
            voiceSelect.innerHTML = '<option disabled selected>Speech Synthesis Unsupported</option>';
            return;
        }

        const voices = speech.getVoices();
        voiceSelect.innerHTML = '';
        if (voices.length === 0) {
            voiceSelect.innerHTML = '<option value="">Default System Voice</option>';
            return;
        }

        voices.forEach((v, index) => {
            const opt = document.createElement('option');
            opt.value = index;
            opt.textContent = `${v.name} (${v.lang})`;
            if (v.default || v.lang.startsWith('en')) {
                opt.selected = true;
            }
            voiceSelect.appendChild(opt);
        });

        if (voiceSelect.value !== '') {
            speech.options.voiceIndex = parseInt(voiceSelect.value, 10);
        }
    }

    populateVoices();
    if (window.speechSynthesis && window.speechSynthesis.onvoiceschanged !== undefined) {
        window.speechSynthesis.onvoiceschanged = populateVoices;
    }

    voiceSelect.addEventListener('change', () => {
        speech.options.voiceIndex = voiceSelect.value !== '' ? parseInt(voiceSelect.value, 10) : null;
    });

    voiceRate.addEventListener('input', () => {
        const rate = parseFloat(voiceRate.value);
        rateValue.textContent = `${rate.toFixed(1)}x`;
        speech.options.rate = rate;
    });

    voicePitch.addEventListener('input', () => {
        const pitch = parseFloat(voicePitch.value);
        pitchValue.textContent = `${pitch.toFixed(1)}`;
        speech.options.pitch = pitch;
    });

    btnSpeakSpeech.addEventListener('click', () => {
        const text = resultMessageText.textContent.trim();
        if (text && !text.includes('Start the webcam') && !text.includes('Click "Analyze Gesture"')) {
            speech.speak(text);
        } else {
            showToast('No Message', 'Capture or recognize a hand gesture first before speaking.', 'info');
        }
    });

    btnReplaySpeech.addEventListener('click', () => {
        if (speech.hasLastMessage()) {
            speech.replay();
            showToast('Replaying Speech', `Replaying: "${speech.getLastMessage()}"`, 'info');
        } else {
            showToast('No Previous Speech', 'No previously vocalized phrase found in session.', 'warning');
        }
    });

    btnStopSpeech.addEventListener('click', () => {
        speech.stop();
        showToast('Speech Stopped', 'Vocalization interrupted.', 'info');
    });

    // 6. Tab Mode Switching
    modeBtnWebcam.addEventListener('click', () => {
        modeBtnWebcam.classList.add('active');
        modeBtnUpload.classList.remove('active');
        webcamSection.classList.remove('d-none');
        uploadSection.classList.add('d-none');
        currentSourceType = 'WEBCAM';
    });

    modeBtnUpload.addEventListener('click', () => {
        modeBtnUpload.classList.add('active');
        modeBtnWebcam.classList.remove('active');
        uploadSection.classList.remove('d-none');
        webcamSection.classList.add('d-none');
        currentSourceType = 'UPLOAD';
    });

    // 7. Webcam Control Listeners
    btnStartCamera.addEventListener('click', async () => {
        btnStartCamera.disabled = true;
        setCameraStatus('Connecting...', 'warning');

        try {
            await webcam.start();
            videoPlaceholder.style.display = 'none';
            videoElement.classList.add('active');
            cameraScanline.classList.add('active');

            btnStartCamera.disabled = true;
            btnStopCamera.disabled = false;
            btnCaptureGesture.disabled = false;

            // Start MediaPipe Real-Time Inference if enabled
            if (continuousRecognitionToggle && continuousRecognitionToggle.checked) {
                startMediaPipeTracking();
            } else {
                setCameraStatus('Live Feed Active (30 FPS)', 'success');
            }

            showToast('Camera Started', 'Webcam stream active. Position hand inside frame.', 'success');
        } catch (error) {
            btnStartCamera.disabled = false;
            btnStopCamera.disabled = true;
            btnCaptureGesture.disabled = true;
            setCameraStatus('Camera Offline', 'danger');
            showToast('Camera Error', error.message, 'danger');
        }
    });

    function startMediaPipeTracking() {
        if (mpRecognizer && mpRecognizer.isReady) {
            mpRecognizer.start(videoElement, landmarkCanvas);
            setCameraStatus('MediaPipe Live AI Active', 'success');
        } else {
            setCameraStatus('Live Feed Active (Model Loading...)', 'warning');
            const checkReadyInterval = setInterval(() => {
                if (mpRecognizer && mpRecognizer.isReady && webcam.isStreaming) {
                    clearInterval(checkReadyInterval);
                    mpRecognizer.start(videoElement, landmarkCanvas);
                    setCameraStatus('MediaPipe Live AI Active', 'success');
                }
            }, 300);
            setTimeout(() => clearInterval(checkReadyInterval), 10000);
        }
    }

    btnStopCamera.addEventListener('click', () => {
        if (mpRecognizer) {
            mpRecognizer.stop();
        }
        webcam.stop();
        videoElement.classList.remove('active');
        cameraScanline.classList.remove('active');
        videoPlaceholder.style.display = 'flex';

        btnStartCamera.disabled = false;
        btnStopCamera.disabled = true;
        btnCaptureGesture.disabled = true;

        if (fpsBadge) fpsBadge.innerHTML = '<i class="bi bi-speedometer2 me-1"></i>-- FPS';
        if (handDetectionBadge) {
            handDetectionBadge.className = 'badge bg-dark bg-opacity-75 border border-secondary text-light small';
            handDetectionBadge.innerHTML = '<i class="bi bi-hand-index me-1"></i>No Hand';
        }

        setCameraStatus('Camera Offline', 'secondary');
        showToast('Camera Stopped', 'Webcam hardware tracks released.', 'info');
    });

    if (continuousRecognitionToggle) {
        continuousRecognitionToggle.addEventListener('change', () => {
            if (continuousRecognitionToggle.checked) {
                if (webcam.isStreaming) {
                    startMediaPipeTracking();
                    showToast('Continuous AI Active', 'Real-time gesture recognition resumed.', 'info');
                }
            } else {
                if (mpRecognizer) {
                    mpRecognizer.stop();
                }
                setCameraStatus('Live Feed (Continuous AI Paused)', 'warning');
                showToast('Continuous AI Paused', 'Single snapshot mode is active.', 'info');
            }
        });
    }

    btnCaptureGesture.addEventListener('click', async () => {
        btnCaptureGesture.disabled = true;
        try {
            videoElement.style.opacity = '0.3';
            setTimeout(() => { videoElement.style.opacity = '1'; }, 150);

            const captureData = await webcam.captureFrame();
            displayCapturedPayload(captureData, 'WEBCAM');
            showToast('Snapshot Captured', `Captured ${captureData.width}x${captureData.height} frame (${(captureData.sizeBytes / 1024).toFixed(1)} KB).`, 'success');
        } catch (error) {
            showToast('Capture Error', error.message, 'danger');
        } finally {
            btnCaptureGesture.disabled = false;
        }
    });

    // 8. Upload Manager Event Handlers
    upload.onFileSelected = (fileData) => {
        displayCapturedPayload(fileData, 'UPLOAD');
        showToast('Image Loaded', `Loaded "${fileData.name}" (${(fileData.sizeBytes / 1024).toFixed(1)} KB).`, 'success');
    };

    upload.onError = (error) => {
        showToast('Upload Rejected', error.message, 'warning');
    };

    // 9. Preview Display & Dispatch Handlers
    function displayCapturedPayload(payload, source) {
        currentCapturedPayload = payload;
        currentSourceType = source;
        previewImage.src = payload.dataUrl;
        previewDimensions.textContent = `${payload.width} × ${payload.height} px`;
        previewSize.textContent = `${(payload.sizeBytes / 1024).toFixed(1)} KB`;
        previewSource.textContent = source === 'WEBCAM' ? 'Live Webcam Snapshot' : `Uploaded File (${payload.name || 'Image'})`;
        previewContainer.classList.remove('d-none');

        resultGestureBadge.className = 'gt-result-badge text-warning border-warning';
        resultGestureBadge.innerHTML = '<i class="bi bi-clock-history me-2"></i>READY FOR ANALYSIS';
        resultGestureName.textContent = 'Frame Captured in Memory';
        resultMessageText.textContent = 'Click "Analyze Gesture with Gemini" for deep diagnostic inspection or rely on instant MediaPipe live recognition.';
        resultConfidenceBar.style.width = '100%';
        resultConfidenceVal.textContent = '100% In-Memory';
        resultExplanationText.textContent = `Source: ${source} | Format: ${payload.mimeType} | Size: ${payload.sizeBytes.toLocaleString()} bytes.`;
        devModeBanner.classList.add('d-none');
    }

    btnClearPreview.addEventListener('click', () => {
        currentCapturedPayload = null;
        previewContainer.classList.add('d-none');
        previewImage.src = '';
        upload.reset();
        speech.stop();
        resetResultPanel();
    });

    // 10. Secondary Multimodal Analysis Dispatcher (POST /api/recognize with Gemini)
    if (btnAnalyzeGesture) {
        btnAnalyzeGesture.addEventListener('click', async () => {
            if (isRecognitionInProgress) {
                console.warn('Recognition already in progress; suppressing duplicate request.');
                return;
            }

            if (!currentCapturedPayload || !currentCapturedPayload.blob) {
                showToast('No Image', 'Please capture a frame or upload an image first.', 'warning');
                return;
            }

            isRecognitionInProgress = true;
            setAnalyzingState(true);

            try {
                const formData = new FormData();
                const fileName = currentSourceType === 'WEBCAM' ? 'webcam-snapshot.jpg' : (currentCapturedPayload.name || 'upload.jpg');
                formData.append('image', currentCapturedPayload.blob, fileName);
                formData.append('sourceType', currentSourceType);

                const endpoint = getApiEndpoint('/api/recognize');

                const response = await fetch(endpoint, {
                    method: 'POST',
                    body: formData
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.message || `Server responded with HTTP ${response.status}`);
                }

                // Render result
                renderRecognitionSuccess(data);

                // Auto-refresh recognition audit history table
                if (window.historyManager && typeof window.historyManager.loadHistory === 'function') {
                    window.historyManager.loadHistory();
                }

                // Auto-speak if authorized
                if (data.shouldAutoSpeak && data.mappedMessage) {
                    speech.speak(data.mappedMessage);
                    showToast('Vocalizing Gesture', `Speaking: "${data.mappedMessage}"`, 'success');
                } else if (data.gestureCode === 'UNKNOWN') {
                    speech.stop();
                    showToast('Unrecognized Sign', 'Gesture is ambiguous or below confidence threshold. Audio muted.', 'warning');
                }

            } catch (error) {
                console.error('Gemini Analysis error:', error);
                renderRecognitionError(error.message);
                showToast('Analysis Notice', error.message, 'warning');
            } finally {
                isRecognitionInProgress = false;
                setAnalyzingState(false);
            }
        });
    }

    function setAnalyzingState(isAnalyzing) {
        if (!btnAnalyzeGesture) return;
        btnAnalyzeGesture.disabled = isAnalyzing;
        btnCaptureGesture.disabled = isAnalyzing;

        if (isAnalyzing) {
            analyzeBtnText.textContent = 'Analyzing with Gemini...';
            analyzeSpinner.classList.remove('d-none');
            resultGestureBadge.className = 'gt-result-badge text-info border-info';
            resultGestureBadge.innerHTML = '<span class="spinner-border spinner-border-sm me-2"></span>CLASSIFYING';
            resultGestureName.textContent = 'Sending Multimodal Payload...';
            resultMessageText.textContent = 'Gemini Vision model is evaluating hand posture...';
        } else {
            analyzeBtnText.textContent = 'Analyze Gesture with Gemini';
            analyzeSpinner.classList.add('d-none');
            btnCaptureGesture.disabled = !webcam.isStreaming;
        }
    }

    function renderRecognitionSuccess(data) {
        const code = data.gestureCode || 'UNKNOWN';
        const confidencePct = Math.round(data.confidence * 100);

        let badgeClass = 'text-secondary border-secondary';
        let badgeIcon = 'bi-question-diamond-fill';

        if (code === 'THUMBS_UP') {
            badgeClass = 'text-success border-success';
            badgeIcon = 'bi-hand-thumbs-up-fill';
        } else if (code === 'THUMBS_DOWN') {
            badgeClass = 'text-danger border-danger';
            badgeIcon = 'bi-hand-thumbs-down-fill';
        } else if (code === 'OPEN_PALM') {
            badgeClass = 'text-info border-info';
            badgeIcon = 'bi-hand-index-thumb';
        } else if (code === 'V_SIGN') {
            badgeClass = 'text-purple border-purple';
            badgeIcon = 'bi-peace';
        } else if (code === 'INDEX_POINTING') {
            badgeClass = 'text-warning border-warning';
            badgeIcon = 'bi-hand-index-fill';
        }

        resultGestureBadge.className = `gt-result-badge ${badgeClass}`;
        resultGestureBadge.innerHTML = `<i class="bi ${badgeIcon} me-2"></i>${code}`;
        resultGestureName.textContent = data.displayName || code;
        resultMessageText.textContent = data.mappedMessage || 'No message mapped.';

        resultConfidenceBar.style.width = `${confidencePct}%`;
        resultConfidenceVal.textContent = `${confidencePct}% certainty (${data.meetsThreshold ? 'High Confidence' : 'Below Threshold'})`;

        resultExplanationText.textContent = data.explanation || 'Analyzed via GestureTalk AI recognition pipeline.';
        devModeBanner.classList.add('d-none');
        btnReplaySpeech.disabled = !speech.hasLastMessage();
    }

    function renderRecognitionError(errorMessage) {
        resultGestureBadge.className = 'gt-result-badge text-warning border-warning';
        resultGestureBadge.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-2"></i>GEMINI OVERLOAD';
        resultGestureName.textContent = 'Gemini Vision Service Busy';
        resultMessageText.textContent = 'Gemini returned HTTP 503 (Model Overloaded). Real-time MediaPipe recognition continues to operate locally.';
        resultConfidenceBar.style.width = '0%';
        resultConfidenceVal.textContent = '0%';
        resultExplanationText.textContent = 'Secondary Gemini AI service is currently busy or rate-limited. Real-time on-device recognition remains 100% functional.';

        devModeBanner.classList.remove('d-none');
        if (errorMessage.includes('503') || errorMessage.includes('UNAVAILABLE') || errorMessage.includes('overload')) {
            devModeBanner.innerHTML = `
                <i class="bi bi-lightning-charge-fill text-warning me-1"></i>
                <strong>Note:</strong> Gemini Vision is temporarily overloaded. <em>Real-time MediaPipe recognition is running in-browser with zero latency.</em>
            `;
        } else if (errorMessage.includes('GOOGLE_API_KEY') || errorMessage.includes('AI_CONFIG_ERROR')) {
            devModeBanner.innerHTML = `
                <i class="bi bi-key-fill text-warning me-1"></i>
                <strong>Optional Key Notice:</strong> Gemini Vision key is unset or unconfigured. <em>The primary MediaPipe engine does not require an API key.</em>
            `;
        } else {
            devModeBanner.innerHTML = `
                <i class="bi bi-info-circle-fill text-info me-1"></i>
                <strong>Notice:</strong> ${errorMessage}. Real-time recognition via MediaPipe remains active.
            `;
        }
    }

    // Auto-collapse mobile navigation menu on item click
    document.querySelectorAll('.navbar-nav .gt-nav-link').forEach(link => {
        link.addEventListener('click', () => {
            const navCollapse = document.getElementById('navContent');
            if (navCollapse && navCollapse.classList.contains('show')) {
                const bsCollapse = bootstrap.Collapse.getInstance(navCollapse);
                if (bsCollapse) {
                    bsCollapse.hide();
                }
            }
        });
    });

    function resetResultPanel() {
        resultGestureBadge.className = 'gt-result-badge text-muted border-secondary';
        resultGestureBadge.innerHTML = '<i class="bi bi-hourglass-split me-2"></i>AWAITING INPUT';
        resultGestureName.textContent = 'No Gesture Captured Yet';
        resultMessageText.textContent = 'Start the webcam or upload a photo to capture a hand gesture.';
        resultConfidenceBar.style.width = '0%';
        resultConfidenceVal.textContent = '0%';
        resultExplanationText.textContent = 'Awaiting image input from the camera or manual upload.';
        devModeBanner.classList.add('d-none');
    }

    function setCameraStatus(text, variant) {
        cameraStatusText.textContent = text;
        const dot = cameraStatusBadge.querySelector('.gt-pulse-dot');
        if (variant === 'success') {
            dot.style.backgroundColor = '#10b981';
            dot.style.boxShadow = '0 0 8px #10b981';
        } else if (variant === 'warning') {
            dot.style.backgroundColor = '#f59e0b';
            dot.style.boxShadow = '0 0 8px #f59e0b';
        } else {
            dot.style.backgroundColor = '#64748b';
            dot.style.boxShadow = 'none';
        }
    }

    // 11. Toast Notification Helper
    function showToast(title, message, variant = 'info') {
        const toastEl = document.getElementById('appToast');
        const toastTitle = document.getElementById('toastTitle');
        const toastBody = document.getElementById('toastBody');
        const toastHeader = document.getElementById('toastHeader');

        toastTitle.textContent = title;
        toastBody.textContent = message;

        let badgeColor = '#38bdf8';
        if (variant === 'success') badgeColor = '#10b981';
        if (variant === 'danger') badgeColor = '#ef4444';
        if (variant === 'warning') badgeColor = '#f59e0b';
        toastHeader.style.borderLeft = `4px solid ${badgeColor}`;

        const toast = new bootstrap.Toast(toastEl, { delay: 4500 });
        toast.show();
    }

    // Cleanup tracks on page unload
    window.addEventListener('beforeunload', () => {
        if (mpRecognizer) {
            mpRecognizer.stop();
        }
        webcam.stop();
        speech.stop();
    });
});
