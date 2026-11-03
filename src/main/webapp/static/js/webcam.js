/**
 * GestureTalk AI — Webcam Capture Controller
 * Uses HTML5 MediaDevices API and off-screen Canvas rasterization
 */

class WebcamManager {
    constructor(videoElementId, options = {}) {
        this.video = document.getElementById(videoElementId);
        this.stream = null;
        this.isStreaming = false;
        this.options = Object.assign({
            width: { ideal: 1280 },
            height: { ideal: 720 },
            facingMode: 'user',
            quality: 0.92
        }, options);

        // Off-screen canvas for high-performance frame capture
        this.canvas = document.createElement('canvas');
        this.ctx = this.canvas.getContext('2d');
    }

    /**
     * Checks if the browser environment supports getUserMedia
     */
    isSupported() {
        return !!(navigator.mediaDevices && navigator.mediaDevices.getUserMedia);
    }

    /**
     * Starts the webcam stream and attaches it to the video element
     */
    async start() {
        if (!this.isSupported()) {
            throw new Error('Webcam access is not supported by your browser. Please use the image upload fallback.');
        }

        if (this.isStreaming && this.stream) {
            return this.stream;
        }

        try {
            const constraints = {
                video: {
                    width: this.options.width,
                    height: this.options.height,
                    facingMode: this.options.facingMode
                },
                audio: false
            };

            this.stream = await navigator.mediaDevices.getUserMedia(constraints);
            this.video.srcObject = this.stream;

            await new Promise((resolve) => {
                this.video.onloadedmetadata = () => {
                    this.video.play();
                    this.isStreaming = true;
                    resolve();
                };
            });

            return this.stream;
        } catch (error) {
            this.stop();
            throw this._mapError(error);
        }
    }

    /**
     * Stops all active tracks on the webcam stream and clears video element
     */
    stop() {
        if (this.stream) {
            this.stream.getTracks().forEach(track => {
                track.stop();
            });
            this.stream = null;
        }
        if (this.video) {
            this.video.srcObject = null;
        }
        this.isStreaming = false;
    }

    /**
     * Captures the current video frame into a JPEG Blob via off-screen Canvas
     * @returns {Promise<{blob: Blob, dataUrl: string, width: number, height: number, sizeBytes: number}>}
     */
    async captureFrame() {
        if (!this.isStreaming || !this.video) {
            throw new Error('Cannot capture frame: Camera is not active.');
        }

        const videoWidth = this.video.videoWidth || 640;
        const videoHeight = this.video.videoHeight || 480;

        this.canvas.width = videoWidth;
        this.canvas.height = videoHeight;

        // Draw video frame to canvas (mirror image back to natural orientation if mirrored)
        this.ctx.save();
        this.ctx.drawImage(this.video, 0, 0, videoWidth, videoHeight);
        this.ctx.restore();

        const dataUrl = this.canvas.toDataURL('image/jpeg', this.options.quality);

        return new Promise((resolve, reject) => {
            this.canvas.toBlob((blob) => {
                if (!blob) {
                    reject(new Error('Failed to rasterize video frame into JPEG blob.'));
                    return;
                }
                resolve({
                    blob: blob,
                    dataUrl: dataUrl,
                    width: videoWidth,
                    height: videoHeight,
                    sizeBytes: blob.size,
                    mimeType: 'image/jpeg'
                });
            }, 'image/jpeg', this.options.quality);
        });
    }

    /**
     * Maps native DOMExceptions to clear, user-friendly diagnostic messages
     */
    _mapError(error) {
        let userMessage = 'Unable to access webcam.';

        if (error.name === 'NotAllowedError' || error.name === 'PermissionDeniedError') {
            userMessage = 'Camera permission was denied. Please click the lock/camera icon in your browser address bar and grant camera access.';
        } else if (error.name === 'NotFoundError' || error.name === 'DevicesNotFoundError') {
            userMessage = 'No camera device found on this system. Please connect a webcam or switch to image upload.';
        } else if (error.name === 'NotReadableError' || error.name === 'TrackStartError') {
            userMessage = 'Camera hardware is currently in use by another application (e.g. Zoom, Teams, or another tab).';
        } else if (error.name === 'OverconstrainedError') {
            userMessage = 'Camera does not satisfy requested resolution constraints.';
        } else if (error.name === 'SecurityError') {
            userMessage = 'Webcam access is blocked under this security origin. Please ensure you are running on http://localhost or HTTPS.';
        } else if (error.message) {
            userMessage = error.message;
        }

        const err = new Error(userMessage);
        err.originalError = error;
        return err;
    }
}

// Attach to window
window.WebcamManager = WebcamManager;

