package com.gesturetalk.exception;

/**
 * Thrown when an uploaded image is empty, exceeds size thresholds, or fails magic byte verification.
 */
public class InvalidImageException extends GestureRecognitionException {

    public InvalidImageException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }
}

