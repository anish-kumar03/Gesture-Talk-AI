package com.gesturetalk.model;

import java.sql.Timestamp;
import java.time.Instant;
import java.time.format.DateTimeFormatter;

/**
 * Entity model representing a persistent gesture recognition audit record
 * in the gesture_history database table.
 */
public class GestureHistory {

    private final Long id;
    private final String gestureCode;
    private final String mappedMessage;
    private final String explanation;
    private final double confidence;
    private final String sourceType;
    private final Timestamp createdAt;

    /**
     * Constructor for creating a new record before persistence (ID assigned by DB).
     */
    public GestureHistory(String gestureCode, String mappedMessage, String explanation,
                          double confidence, String sourceType) {
        this(null, gestureCode, mappedMessage, explanation, confidence, sourceType, Timestamp.from(Instant.now()));
    }

    /**
     * Constructor for reconstructing an existing record from a JDBC ResultSet.
     */
    public GestureHistory(Long id, String gestureCode, String mappedMessage, String explanation,
                          double confidence, String sourceType, Timestamp createdAt) {
        this.id = id;
        this.gestureCode = (gestureCode != null && !gestureCode.isBlank()) ? gestureCode.trim() : "UNKNOWN";
        this.mappedMessage = (mappedMessage != null) ? mappedMessage.trim() : "";
        this.explanation = (explanation != null) ? explanation.trim() : "";
        this.confidence = Math.round(confidence * 1000.0) / 1000.0;
        this.sourceType = (sourceType != null && !sourceType.isBlank()) ? sourceType.trim().toUpperCase() : "WEBCAM";
        this.createdAt = (createdAt != null) ? createdAt : Timestamp.from(Instant.now());
    }

    /**
     * Factory method creating a GestureHistory instance from a GestureAnalysis DTO.
     *
     * @param analysis Analyzed gesture payload
     * @return populated GestureHistory entity ready for database insertion
     */
    public static GestureHistory fromAnalysis(GestureAnalysis analysis) {
        if (analysis == null) {
            return new GestureHistory("UNKNOWN", "Gesture not recognized.", "No analysis available.", 0.0, "WEBCAM");
        }
        return new GestureHistory(
                analysis.getGestureCode(),
                analysis.getMappedMessage(),
                analysis.getExplanation(),
                analysis.getConfidence(),
                analysis.getSourceType()
        );
    }

    public Long getId() {
        return id;
    }

    public String getGestureCode() {
        return gestureCode;
    }

    public String getMappedMessage() {
        return mappedMessage;
    }

    public String getExplanation() {
        return explanation;
    }

    public double getConfidence() {
        return confidence;
    }

    public String getSourceType() {
        return sourceType;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    /**
     * Formats the timestamp into a human-readable ISO-like string for JSON serialization.
     */
    public String getFormattedCreatedAt() {
        if (createdAt == null) {
            return "";
        }
        return createdAt.toInstant().toString();
    }

    @Override
    public String toString() {
        return "GestureHistory{" +
                "id=" + id +
                ", gestureCode='" + gestureCode + '\'' +
                ", mappedMessage='" + mappedMessage + '\'' +
                ", confidence=" + confidence +
                ", sourceType='" + sourceType + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }
}
