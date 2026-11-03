package com.gesturetalk.exception;

/**
 * Root unchecked exception for all GestureTalk AI domain errors.
 */
public class GestureRecognitionException extends RuntimeException {

    private final String errorCode;
    private final int httpStatus;

    public GestureRecognitionException(String errorCode, String message, int httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public GestureRecognitionException(String errorCode, String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
