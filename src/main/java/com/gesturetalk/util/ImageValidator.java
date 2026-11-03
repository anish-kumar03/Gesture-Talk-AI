package com.gesturetalk.util;

import com.gesturetalk.exception.InvalidImageException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Validates in-memory image byte arrays against size constraints and binary magic numbers.
 * Supported image formats: JPEG, PNG, WebP. Maximum size: 5 MB.
 */
public final class ImageValidator {

    private static final Logger logger = LoggerFactory.getLogger(ImageValidator.class);

    public static final int MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    // Magic Numbers
    private static final byte[] JPEG_MAGIC = new byte[]{(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = new byte[]{
        (byte) 0x89, (byte) 0x50, (byte) 0x4E, (byte) 0x47, 
        (byte) 0x0D, (byte) 0x0A, (byte) 0x1A, (byte) 0x0A
    };
    private static final byte[] RIFF_HEADER = new byte[]{(byte) 'R', (byte) 'I', (byte) 'F', (byte) 'F'};
    private static final byte[] WEBP_HEADER = new byte[]{(byte) 'W', (byte) 'E', (byte) 'B', (byte) 'P'};

    private ImageValidator() {
        // Utility class
    }

    /**
     * Validates image binary payload and determines its verified MIME type.
     *
     * @param bytes Raw image bytes in volatile memory
     * @return Canonical MIME type string (e.g. "image/jpeg", "image/png", "image/webp")
     * @throws InvalidImageException if payload is null, empty, exceeds 5MB, or lacks valid magic bytes
     */
    public static String validate(byte[] bytes) {
        if (bytes == null || bytes.length == 0) {
            logger.warn("Image validation rejected: Empty payload.");
            throw new InvalidImageException("EMPTY_IMAGE_PAYLOAD", 
                "No image payload provided in request.", 400);
        }

        if (bytes.length > MAX_FILE_SIZE_BYTES) {
            double sizeMb = bytes.length / (1024.0 * 1024.0);
            logger.warn("Image validation rejected: File size ({}) exceeds 5MB limit.", sizeMb);
            throw new InvalidImageException("FILE_TOO_LARGE", 
                String.format("Uploaded image (%.2f MB) exceeds the maximum allowed limit of 5.0 MB.", sizeMb), 413);
        }

        String mimeType = detectMimeType(bytes);
        if (mimeType == null) {
            logger.warn("Image validation rejected: Binary header does not match JPEG, PNG, or WebP.");
            throw new InvalidImageException("UNSUPPORTED_IMAGE_FORMAT", 
                "Uploaded file binary header does not match valid JPEG, PNG, or WebP signatures.", 415);
        }

        logger.debug("Image validated successfully: {} ({} bytes)", mimeType, bytes.length);
        return mimeType;
    }

    /**
     * Detects MIME type by inspecting the initial magic numbers.
     *
     * @param bytes Raw image bytes
     * @return Detected MIME type or null if unrecognized
     */
    public static String detectMimeType(byte[] bytes) {
        if (bytes == null || bytes.length < 12) {
            return null;
        }

        // Check JPEG (FF D8 FF)
        if (matchesPrefix(bytes, JPEG_MAGIC)) {
            return "image/jpeg";
        }

        // Check PNG (89 50 4E 47 0D 0A 1A 0A)
        if (matchesPrefix(bytes, PNG_MAGIC)) {
            return "image/png";
        }

        // Check WebP (RIFF....WEBP)
        if (matchesPrefix(bytes, RIFF_HEADER) && matchesOffset(bytes, WEBP_HEADER, 8)) {
            return "image/webp";
        }

        return null;
    }

    private static boolean matchesPrefix(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) {
            return false;
        }
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean matchesOffset(byte[] data, byte[] expected, int offset) {
        if (data.length < offset + expected.length) {
            return false;
        }
        for (int i = 0; i < expected.length; i++) {
            if (data[offset + i] != expected[i]) {
                return false;
            }
        }
        return true;
    }
}

