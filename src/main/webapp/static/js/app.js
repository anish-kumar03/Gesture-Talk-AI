/**
 * GestureTalk AI — Central Client Application Coordinator
 * Phase 2: Frontend, Webcam Capture & Manual Image Upload
 * Strictly maintains architectural boundaries: zero fake AI predictions, zero business logic in JS.
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

    // Preview Container
    const previewContainer = document.getElementById('previewContainer');
    const previewImage = document.getElementById('previewImage');
    const previewDimensions = document.getElementById('previewDimensions');
    const previewSize = document.getElementById('previewSize');
    const previewSource = document.getElementById('previewSource');
    const btnClearPreview = document.getElementById('btnClearPreview');

    // Result Panel
    const resultGestureBadge = document.getElementById('resultGestureBadge');
    const resultGestureName = document.getElementById('resultGestureName');
    const resultMessageText = document.getElementById('resultMessageText');
    const resultConfidenceBar = document.getElementById('resultConfidenceBar');
    const resultConfidenceVal = document.getElementById('resultConfidenceVal');
    const resultExplanationText = document.getElementById('resultExplanationText');
    const devModeBanner = document.getElementById('devModeBanner');

    // Speech Controls (Phase 2 Preview)
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

    // 3. Tab Mode Switching
    modeBtnWebcam.addEventListener('click', () => {
        modeBtnWebcam.classList.add('active');
        modeBtnUpload.classList.remove('active');
        webcamSection.classList.remove('d-none');
        uploadSection.classList.add('d-none');
    });

    modeBtnUpload.addEventListener('click', () => {
        modeBtnUpload.classList.add('active');
        modeBtnWebcam.classList.remove('active');
        uploadSection.classList.remove('d-none');
        webcamSection.classList.add('d-none');
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

    // 6. Preview & Display Handlers
    function displayCapturedPayload(payload, source) {
        currentCapturedPayload = payload;
        previewImage.src = payload.dataUrl;
        previewDimensions.textContent = `${payload.width} × ${payload.height} px`;
        previewSize.textContent = `${(payload.sizeBytes / 1024).toFixed(1)} KB`;
        previewSource.textContent = source === 'WEBCAM' ? 'Live Webcam Snapshot' : `Uploaded File (${payload.name || 'Image'})`;
        previewContainer.classList.remove('d-none');

        // Update Result Panel to show Phase 2 Verification State
        resultGestureBadge.className = 'gt-result-badge text-warning border-warning';
        resultGestureBadge.innerHTML = '<i class="bi bi-camera-fill me-2"></i>FRAME READY';
        resultGestureName.textContent = 'Frame Captured & Verified';
        resultMessageText.textContent = 'Frame payload rasterized in memory. In Phase 3, this JPEG buffer will be dispatched to the Gemini Vision Servlet.';
        resultConfidenceBar.style.width = '100%';
        resultConfidenceVal.textContent = '100% Client Ready';
        resultExplanationText.textContent = `Source: ${source} | Format: ${payload.mimeType} | Byte Size: ${payload.sizeBytes.toLocaleString()} bytes.`;
        devModeBanner.classList.remove('d-none');
    }

    btnClearPreview.addEventListener('click', () => {
        currentCapturedPayload = null;
        previewContainer.classList.add('d-none');
        previewImage.src = '';
        upload.reset();
        resetResultPanel();
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

    // 7. Speech Synthesis Initialization (Phase 2 Preview & Verification)
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

    btnTestSpeech.addEventListener('click', () => {
        if (!('speechSynthesis' in window)) {
            showToast('Audio Notice', 'SpeechSynthesis is not supported in this browser.', 'warning');
            return;
        }

        const phrase = resultMessageText.textContent.includes('Frame payload rasterized')
            ? 'GestureTalk AI frontend is ready. In Phase 3, this speech engine will vocalize recognized gestures.'
            : 'Hello! GestureTalk AI voice engine is operational.';

        const utterance = new SpeechSynthesisUtterance(phrase);
        const voices = window.speechSynthesis.getVoices();
        if (voiceSelect.value !== '' && voices[voiceSelect.value]) {
            utterance.voice = voices[voiceSelect.value];
        }
        utterance.rate = parseFloat(voiceRate.value) || 1.0;
        utterance.pitch = parseFloat(voicePitch.value) || 1.0;

        window.speechSynthesis.cancel(); // Cancel any ongoing speech
        window.speechSynthesis.speak(utterance);
        showToast('Speech Synthesized', 'Audio vocalization triggered through browser engine.', 'info');
    });

    // 8. Toast Helper
    function showToast(title, message, variant = 'info') {
        const toastEl = document.getElementById('appToast');
        const toastTitle = document.getElementById('toastTitle');
        const toastBody = document.getElementById('toastBody');
        const toastHeader = document.getElementById('toastHeader');

        toastTitle.textContent = title;
        toastBody.textContent = message;

        // Visual coloring
        let badgeColor = '#38bdf8';
        if (variant === 'success') badgeColor = '#10b981';
        if (variant === 'danger') badgeColor = '#ef4444';
        if (variant === 'warning') badgeColor = '#f59e0b';
        toastHeader.style.borderLeft = `4px solid ${badgeColor}`;

        const toast = new bootstrap.Toast(toastEl, { delay: 4000 });
        toast.show();
    }

    // Window cleanup on navigation / unload
    window.addEventListener('beforeunload', () => {
        webcam.stop();
    });
});

