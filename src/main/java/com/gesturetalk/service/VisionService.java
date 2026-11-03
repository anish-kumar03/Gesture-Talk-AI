package com.gesturetalk.service;

/**
 * Functional contract for computer vision gesture analysis engines.
 */
public interface VisionService {

    /**
     * Analyzes an in-memory image byte array and returns the raw structured JSON classification.
     *
     * @param imageBytes Validated image bytes in volatile memory
     * @param mimeType Canonical MIME type (e.g. "image/jpeg", "image/png", "image/webp")
     * @return Structured JSON response string
     */
    String analyzeGesture(byte[] imageBytes, String mimeType);
}

