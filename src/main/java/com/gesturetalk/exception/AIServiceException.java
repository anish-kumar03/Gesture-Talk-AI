package com.gesturetalk.exception;

/**
 * Thrown when communication with Google Gemini fails due to quota, timeout, network or API errors.
 */
public class AIServiceException extends GestureRecognitionException {

    public AIServiceException(String errorCode, String message, int httpStatus) {
        super(errorCode, message, httpStatus);
    }

    public AIServiceException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(errorCode, message, httpStatus, cause);
    }
}
