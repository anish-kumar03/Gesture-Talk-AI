/**
 * GestureTalk AI — Manual File Upload Controller
 * Handles drag-and-drop, client-side validation, and thumbnail extraction
 */

class UploadManager {
    constructor(dropzoneElementId, fileInputElementId, options = {}) {
        this.dropzone = document.getElementById(dropzoneElementId);
        this.fileInput = document.getElementById(fileInputElementId);
        this.options = Object.assign({
            maxSizeBytes: 5 * 1024 * 1024, // 5 MB
            allowedTypes: ['image/jpeg', 'image/png', 'image/webp']
        }, options);

        this.currentFile = null;
        this.onFileSelected = null; // Callback
        this.onError = null; // Callback

        this._bindEvents();
    }

    _bindEvents() {
        if (!this.dropzone || !this.fileInput) return;

        // Click on dropzone opens native file picker
        this.dropzone.addEventListener('click', () => {
            this.fileInput.click();
        });

        // File picker change
        this.fileInput.addEventListener('change', (e) => {
            const files = e.target.files;
            if (files && files.length > 0) {
                this.handleFile(files[0]);
            }
        });

        // Drag and drop events
        ['dragenter', 'dragover'].forEach(eventName => {
            this.dropzone.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.dropzone.classList.add('dragover');
            });
        });

        ['dragleave', 'drop'].forEach(eventName => {
            this.dropzone.addEventListener(eventName, (e) => {
                e.preventDefault();
                e.stopPropagation();
                this.dropzone.classList.remove('dragover');
            });
        });

        this.dropzone.addEventListener('drop', (e) => {
            const dt = e.dataTransfer;
            if (dt && dt.files && dt.files.length > 0) {
                this.handleFile(dt.files[0]);
            }
        });
    }

    /**
     * Validates and reads selected image file
     * @param {File} file
     */
    async handleFile(file) {
        try {
            this._validate(file);
            this.currentFile = file;

            const previewData = await this._readImageData(file);
            if (typeof this.onFileSelected === 'function') {
                this.onFileSelected({
                    file: file,
                    blob: file,
                    dataUrl: previewData.dataUrl,
                    width: previewData.width,
                    height: previewData.height,
                    sizeBytes: file.size,
                    mimeType: file.type,
                    name: file.name
                });
            }
        } catch (error) {
            if (typeof this.onError === 'function') {
                this.onError(error);
            } else {
                console.error('File upload error:', error);
            }
        }
    }

    _validate(file) {
        if (!file) {
            throw new Error('No file selected.');
        }

        if (file.size > this.options.maxSizeBytes) {
            const sizeMB = (file.size / (1024 * 1024)).toFixed(2);
            throw new Error(`File size (${sizeMB} MB) exceeds the maximum allowed limit of 5 MB.`);
        }

        if (!this.options.allowedTypes.includes(file.type)) {
            throw new Error(`Unsupported file type (${file.type || 'unknown'}). Please upload a JPEG, PNG, or WebP image.`);
        }
    }

    _readImageData(file) {
        return new Promise((resolve, reject) => {
            const reader = new FileReader();
            reader.onload = (e) => {
                const dataUrl = e.target.result;
                const img = new Image();
                img.onload = () => {
                    resolve({
                        dataUrl: dataUrl,
                        width: img.naturalWidth || img.width,
                        height: img.naturalHeight || img.height
                    });
                };
                img.onerror = () => {
                    reject(new Error('Selected file could not be decoded as a valid image.'));
                };
                img.src = dataUrl;
            };
            reader.onerror = () => {
                reject(new Error('Failed to read file from disk.'));
            };
            reader.readAsDataURL(file);
        });
    }

    reset() {
        this.currentFile = null;
        if (this.fileInput) {
            this.fileInput.value = '';
        }
    }
}

window.UploadManager = UploadManager;

