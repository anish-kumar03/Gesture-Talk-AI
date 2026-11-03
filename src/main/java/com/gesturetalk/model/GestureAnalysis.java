package com.gesturetalk.model;

import java.time.Instant;

/**
 * Data Transfer Object representing the outcome of a gesture recognition analysis.
 */
public class GestureAnalysis {

    private final String gestureCode;
    private final String displayName;
    private final String mappedMessage;
    private final double confidence;
    private final String explanation;
    private final String sourceType;
    private final String timestamp;
    private final boolean meetsThreshold;

    public GestureAnalysis(GestureType gestureType, double confidence, String explanation, 
                           String sourceType, boolean meetsThreshold) {
        this.gestureCode = gestureType.name();
        this.displayName = gestureType.getDisplayName();
        this.mappedMessage = gestureType.getDefaultMessage();
        this.confidence = Math.round(confidence * 1000.0) / 1000.0;
        this.explanation = (explanation != null && !explanation.isBlank()) 
                           ? explanation.trim() 
                           : "No explanation provided.";
        this.sourceType = (sourceType != null && !sourceType.isBlank()) ? sourceType.trim().toUpperCase() : "WEBCAM";
        this.timestamp = Instant.now().toString();
        this.meetsThreshold = meetsThreshold;
    }

    public String getGestureCode() {
        return gestureCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getMappedMessage() {
        return mappedMessage;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getExplanation() {
        return explanation;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public boolean isMeetsThreshold() {
        return meetsThreshold;
    }
}

