package com.gesturetalk.model;

/**
 * Supported hand gesture catalog for GestureTalk AI.
 * Exactly five predefined static gestures are supported, plus an explicit UNKNOWN fallback.
 */
public enum GestureType {
    THUMBS_UP("Yes, that's correct.", "Thumbs Up"),
    THUMBS_DOWN("No, that's incorrect.", "Thumbs Down"),
    OPEN_PALM("Please stop or wait.", "Open Palm"),
    V_SIGN("Peace and greetings.", "V Sign"),
    INDEX_POINTING("Please look there.", "Index Pointing"),
    UNKNOWN("Gesture not recognized. Please try again.", "Unknown Gesture");

    private final String defaultMessage;
    private final String displayName;

    GestureType(String defaultMessage, String displayName) {
        this.defaultMessage = defaultMessage;
        this.displayName = displayName;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public String getDisplayName() {
        return displayName;
    }

    /**
     * Resolves a string code to a GestureType enum.
     * Safely returns UNKNOWN if code is null, unrecognized, or ambiguous.
     *
     * @param code String representation (case-insensitive)
     * @return Corresponding GestureType or UNKNOWN
     */
    public static GestureType fromString(String code) {
        if (code == null || code.trim().isEmpty()) {
            return UNKNOWN;
        }
        String normalized = code.trim().toUpperCase().replace(" ", "_").replace("-", "_");
        try {
            return GestureType.valueOf(normalized);
        } catch (IllegalArgumentException ex) {
            return UNKNOWN;
        }
    }
}
