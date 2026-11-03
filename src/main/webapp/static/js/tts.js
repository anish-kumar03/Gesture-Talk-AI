/**
 * GestureTalk AI — Text-to-Speech Controller (Web Speech API)
 * Manages browser-native SpeechSynthesis with duplicate prevention,
 * speaking state tracking, replay, stop, rate, and voice selection.
 */

class SpeechManager {
    constructor(options = {}) {
        this.options = Object.assign({
            rate: 1.0,
            pitch: 1.0,
            volume: 1.0,
            voiceIndex: null
        }, options);

        this.isSupported = ('speechSynthesis' in window && 'SpeechSynthesisUtterance' in window);
        this.lastSpokenText = '';
        this.isSpeaking = false;
        this.currentUtterance = null;
        this.voices = [];

        // State callbacks
        this.onStateChange = null; // (isSpeaking) => {}
        this.onError = null;       // (errorMessage) => {}

        if (this.isSupported) {
            this._loadVoices();
            if (window.speechSynthesis.onvoiceschanged !== undefined) {
                window.speechSynthesis.onvoiceschanged = () => this._loadVoices();
            }
        }
    }

    _loadVoices() {
        if (!this.isSupported) return;
        this.voices = window.speechSynthesis.getVoices() || [];
    }

    getVoices() {
        if (this.voices.length === 0 && this.isSupported) {
            this.voices = window.speechSynthesis.getVoices() || [];
        }
        return this.voices;
    }

    /**
     * Speaks text using the browser's SpeechSynthesis engine.
     * Prevents duplicate playback and ensures only verified phrases are articulated.
     *
     * @param {string} text Text phrase to vocalize
     * @param {object} customOptions Override rate, pitch, or voice
     */
    speak(text, customOptions = {}) {
        if (!this.isSupported) {
            if (typeof this.onError === 'function') {
                this.onError('Browser SpeechSynthesis is not supported on this device.');
            }
            return false;
        }

        if (!text || text.trim() === '') {
            return false;
        }

        const cleanText = text.trim();

        // 1. Cancel any active speech to avoid audio overlaps and queue buildup
        this.stop();

        // 2. Cache phrase for replay capability
        this.lastSpokenText = cleanText;

        const mergedOptions = Object.assign({}, this.options, customOptions);
        const utterance = new SpeechSynthesisUtterance(cleanText);

        // Configure speech characteristics
        utterance.rate = Math.max(0.5, Math.min(2.0, mergedOptions.rate || 1.0));
        utterance.pitch = Math.max(0.5, Math.min(1.5, mergedOptions.pitch || 1.0));
        utterance.volume = Math.max(0.0, Math.min(1.0, mergedOptions.volume || 1.0));

        // Assign voice
        const availableVoices = this.getVoices();
        if (mergedOptions.voiceIndex !== null && availableVoices[mergedOptions.voiceIndex]) {
            utterance.voice = availableVoices[mergedOptions.voiceIndex];
        } else if (availableVoices.length > 0) {
            // Prefer English or system default voice
            const defaultVoice = availableVoices.find(v => v.default || v.lang.startsWith('en')) || availableVoices[0];
            utterance.voice = defaultVoice;
        }

        // Event hooks
        utterance.onstart = () => {
            this.isSpeaking = true;
            if (typeof this.onStateChange === 'function') {
                this.onStateChange(true);
            }
        };

        utterance.onend = () => {
            this.isSpeaking = false;
            this.currentUtterance = null;
            if (typeof this.onStateChange === 'function') {
                this.onStateChange(false);
            }
        };

        utterance.onerror = (event) => {
            // "canceled" or "interrupted" are normal when user clicks Stop or new recognition arrives
            this.isSpeaking = false;
            this.currentUtterance = null;
            if (typeof this.onStateChange === 'function') {
                this.onStateChange(false);
            }
            if (event.error !== 'canceled' && event.error !== 'interrupted') {
                console.warn('SpeechSynthesis error:', event.error);
                if (typeof this.onError === 'function') {
                    this.onError(`Speech error: ${event.error}`);
                }
            }
        };

        this.currentUtterance = utterance;

        try {
            window.speechSynthesis.speak(utterance);
            return true;
        } catch (ex) {
            console.error('Failed to trigger speech:', ex);
            this.isSpeaking = false;
            if (typeof this.onStateChange === 'function') {
                this.onStateChange(false);
            }
            return false;
        }
    }

    /**
     * Replays the last successfully spoken message.
     */
    replay(customOptions = {}) {
        if (!this.lastSpokenText) {
            return false;
        }
        return this.speak(this.lastSpokenText, customOptions);
    }

    /**
     * Immediately stops ongoing audio synthesis.
     */
    stop() {
        if (!this.isSupported) return;
        try {
            window.speechSynthesis.cancel();
        } catch (ignored) {}
        this.isSpeaking = false;
        if (typeof this.onStateChange === 'function') {
            this.onStateChange(false);
        }
    }

    hasLastMessage() {
        return !!this.lastSpokenText;
    }

    getLastMessage() {
        return this.lastSpokenText;
    }
}

// Expose globally
window.SpeechManager = SpeechManager;
