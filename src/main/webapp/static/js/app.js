/**
 * GestureTalk AI — Central Client Application Coordinator
 * Phase 3: Gemini Vision Multimodal Recognition & Audio Synthesis
 * Strictly maintains architectural boundaries: zero business logic in JS, zero API keys in client.
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
    const videoElement = document.getElementById('webcamVideo');
    const videoPlaceholder = document.getElementById('viewfinderPlaceholder');
    const cameraScanline = document.getElementById('cameraScanline');
    const cameraStatusBadge = document.getElementById('cameraStatusBadge');
    const cameraStatusText = document.getElementById('cameraStatusText');

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

    // Speech Controls
    const voiceSelect = document.getElementById('voiceSelect');
    const voiceRate = document.getElementById('voiceRate');
    const voicePitch = document.getElementById('voicePitch');
    const rateValue = document.getElementById('rateValue');
    const pitchValue = document.getElementById('pitchValue');
    const btnTestSpeech = document.getElementById('btnTestSpeech');

    // 2. Initialize Managers
    const webcam = new WebcamManager('webcamVideo', {
        width: { ideal: 1280 },
        height: { ideal: 720 },
        quality: 0.92
    });

    const upload = new UploadManager('dropzoneArea', 'fileInput', {
        maxSizeBytes: 5 * 1024 * 1024
    });

    let currentCapturedPayload = null;
    let currentSourceType = 'WEBCAM';

    // 3. Tab Mode Switching
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

    // 4. Webcam Control Listeners
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

            setCameraStatus('Live Feed Active (30 FPS)', 'success');
            showToast('Camera Started', 'Webcam stream initialized successfully.', 'success');
        } catch (error) {
            btnStartCamera.disabled = false;
            btnStopCamera.disabled = true;
            btnCaptureGesture.disabled = true;
            setCameraStatus('Camera Offline', 'danger');
            showToast('Camera Error', error.message, 'danger');
        }
    });

    btnStopCamera.addEventListener('click', () => {
        webcam.stop();
        videoElement.classList.remove('active');
        cameraScanline.classList.remove('active');
        videoPlaceholder.style.display = 'flex';

        btnStartCamera.disabled = false;
        btnStopCamera.disabled = true;
        btnCaptureGesture.disabled = true;

        setCameraStatus('Camera Offline', 'secondary');
        showToast('Camera Stopped', 'Webcam hardware tracks released.', 'info');
    });

    btnCaptureGesture.addEventListener('click', async () => {
        btnCaptureGesture.disabled = true;
        try {
            // Flash animation on viewfinder
            videoElement.style.opacity = '0.3';
            setTimeout(() => { videoElement.style.opacity = '1'; }, 150);

            const captureData = await webcam.captureFrame();
            displayCapturedPayload(captureData, 'WEBCAM');
            showToast('Frame Captured', `Rasterized ${captureData.width}x${captureData.height} frame (${(captureData.sizeBytes / 1024).toFixed(1)} KB).`, 'success');
        } catch (error) {
            showToast('Capture Error', error.message, 'danger');
        } finally {
            btnCaptureGesture.disabled = false;
        }
    });

    // 5. Upload Manager Event Handlers
    upload.onFileSelected = (fileData) => {
        displayCapturedPayload(fileData, 'UPLOAD');
        showToast('Image Loaded', `Loaded "${fileData.name}" (${(fileData.sizeBytes / 1024).toFixed(1)} KB).`, 'success');
    };

    upload.onError = (error) => {
        showToast('Upload Rejected', error.message, 'warning');
    };

    // 6. Preview Display & Dispatch Handlers
    function displayCapturedPayload(payload, source) {
        currentCapturedPayload = payload;
        currentSourceType = source;
        previewImage.src = payload.dataUrl;
        previewDimensions.textContent = `${payload.width} × ${payload.height} px`;
        previewSize.textContent = `${(payload.sizeBytes / 1024).toFixed(1)} KB`;
        previewSource.textContent = source === 'WEBCAM' ? 'Live Webcam Snapshot' : `Uploaded File (${payload.name || 'Image'})`;
        previewContainer.classList.remove('d-none');

        // Initial preview state before analysis
        resultGestureBadge.className = 'gt-result-badge text-warning border-warning';
        resultGestureBadge.innerHTML = '<i class="bi bi-clock-history me-2"></i>READY FOR ANALYSIS';
        resultGestureName.textContent = 'Frame Captured in Memory';
        resultMessageText.textContent = 'Click "Analyze Gesture with Gemini" to trigger multimodal AI classification.';
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
        resetResultPanel();
    });

    // 7. Core AI Analysis Dispatcher (POST /api/recognize)
    if (btnAnalyzeGesture) {
        btnAnalyzeGesture.addEventListener('click', async () => {
            if (!currentCapturedPayload || !currentCapturedPayload.blob) {
                showToast('No Image', 'Please capture a frame or upload an image first.', 'warning');
                return;
            }

            setAnalyzingState(true);

            try {
                const formData = new FormData();
                const fileName = currentSourceType === 'WEBCAM' ? 'webcam-snapshot.jpg' : (currentCapturedPayload.name || 'upload.jpg');
                formData.append('image', currentCapturedPayload.blob, fileName);
                formData.append('sourceType', currentSourceType);

                // Compute context path dynamically
                const endpoint = (window.location.pathname.endsWith('/') 
                    ? window.location.pathname.slice(0, -1) 
                    : window.location.pathname).replace(/\/index\.jsp$/, '') + '/api/recognize';

                const response = await fetch(endpoint, {
                    method: 'POST',
                    body: formData
                });

                const data = await response.json();

                if (!response.ok) {
                    throw new Error(data.message || `Server responded with HTTP ${response.status}`);
                }

                // Render successful recognition outcome
                renderRecognitionSuccess(data);
                showToast('Recognition Complete', `Recognized: ${data.displayName} (${(data.confidence * 100).toFixed(1)}%)`, 'success');

                // Trigger browser SpeechSynthesis
                if (data.mappedMessage) {
                    speakMessage(data.mappedMessage);
                }

            } catch (error) {
                console.error('Recognition error:', error);
                renderRecognitionError(error.message);
                showToast('Recognition Failed', error.message, 'danger');
            } finally {
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

        // Visual Badge Styling per Gesture Type
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
        resultConfidenceVal.textContent = `${confidencePct}% (${data.meetsThreshold ? 'Verified' : 'Ambiguous'})`;

        resultExplanationText.textContent = data.explanation || 'Analyzed via Google Gemini Vision.';
        devModeBanner.classList.add('d-none');
    }

    function renderRecognitionError(errorMessage) {
        resultGestureBadge.className = 'gt-result-badge text-danger border-danger';
        resultGestureBadge.innerHTML = '<i class="bi bi-exclamation-triangle-fill me-2"></i>ERROR';
        resultGestureName.textContent = 'Recognition Encountered an Error';
        resultMessageText.textContent = errorMessage;
        resultConfidenceBar.style.width = '0%';
        resultConfidenceVal.textContent = '0%';
        resultExplanationText.textContent = 'Please ensure the server environment variable GEMINI_API_KEY is configured and active.';

        if (errorMessage.includes('GEMINI_API_KEY') || errorMessage.includes('AI_CONFIG_ERROR')) {
            devModeBanner.classList.remove('d-none');
            devModeBanner.innerHTML = `
                <i class="bi bi-key-fill text-warning me-1"></i>
                <strong>API Key Required:</strong> Set the <code>GEMINI_API_KEY</code> environment variable on your machine and restart Tomcat/IDE.
            `;
        }
    }

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

    // 8. Speech Synthesis
    function populateVoices() {
        if (!('speechSynthesis' in window)) {
            voiceSelect.innerHTML = '<option disabled selected>Speech Synthesis Unsupported</option>';
            return;
        }

        const voices = window.speechSynthesis.getVoices();
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
    }

    if ('speechSynthesis' in window) {
        populateVoices();
        if (speechSynthesis.onvoiceschanged !== undefined) {
            speechSynthesis.onvoiceschanged = populateVoices;
        }
    }

    voiceRate.addEventListener('input', () => {
        rateValue.textContent = `${parseFloat(voiceRate.value).toFixed(1)}x`;
    });

    voicePitch.addEventListener('input', () => {
        pitchValue.textContent = `${parseFloat(voicePitch.value).toFixed(1)}`;
    });

    function speakMessage(text) {
        if (!('speechSynthesis' in window) || !text) return;

        const utterance = new SpeechSynthesisUtterance(text);
        const voices = window.speechSynthesis.getVoices();
        if (voiceSelect.value !== '' && voices[voiceSelect.value]) {
            utterance.voice = voices[voiceSelect.value];
        }
        utterance.rate = parseFloat(voiceRate.value) || 1.0;
        utterance.pitch = parseFloat(voicePitch.value) || 1.0;

        window.speechSynthesis.cancel();
        window.speechSynthesis.speak(utterance);
    }

    btnTestSpeech.addEventListener('click', () => {
        const text = resultMessageText.textContent.includes('Start the webcam') 
            ? 'GestureTalk AI voice engine is active and ready.' 
            : resultMessageText.textContent;
        speakMessage(text);
        showToast('Speech Synthesized', 'Audio vocalization triggered through browser engine.', 'info');
    });

    // 9. Toast Notification Helper
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
        webcam.stop();
    });
});
