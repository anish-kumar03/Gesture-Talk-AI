package com.gesturetalk.service;

import com.gesturetalk.model.GestureAnalysis;
import com.gesturetalk.model.GestureType;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Core business logic service that coordinates image analysis, parses Gemini output,
 * enforces confidence thresholds, and maps gestures to deterministic spoken phrases.
 */
public class GestureRecognitionService {

    private static final Logger logger = LoggerFactory.getLogger(GestureRecognitionService.class);

    public static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.75;

    private final VisionService visionService;
    private final double confidenceThreshold;

    public GestureRecognitionService() {
        this(new GeminiVisionService(), resolveConfiguredThreshold());
    }

    public GestureRecognitionService(VisionService visionService) {
        this(visionService, resolveConfiguredThreshold());
    }

    public GestureRecognitionService(VisionService visionService, double confidenceThreshold) {
        this.visionService = visionService;
        this.confidenceThreshold = confidenceThreshold;
        logger.info("GestureRecognitionService initialized with confidence threshold: {}", this.confidenceThreshold);
    }

    private static double resolveConfiguredThreshold() {
        String envThreshold = System.getenv("CONFIDENCE_THRESHOLD");
        if (envThreshold != null && !envThreshold.isBlank()) {
            try {
                double val = Double.parseDouble(envThreshold.trim());
                if (val >= 0.0 && val <= 1.0) {
                    return val;
                }
            } catch (NumberFormatException ignored) {
                logger.warn("Invalid CONFIDENCE_THRESHOLD env value '{}'. Using default: {}", 
                    envThreshold, DEFAULT_CONFIDENCE_THRESHOLD);
            }
        }
        return DEFAULT_CONFIDENCE_THRESHOLD;
    }

    /**
     * Executes gesture recognition on an in-memory image byte buffer.
     *
     * @param imageBytes Validated image bytes in volatile memory
     * @param mimeType Canonical MIME type ("image/jpeg", "image/png", "image/webp")
     * @param sourceType Origin of image ("WEBCAM" or "UPLOAD")
     * @return GestureAnalysis containing validated classification outcome
     */
    public GestureAnalysis recognize(byte[] imageBytes, String mimeType, String sourceType) {
        // 1. Invoke Gemini Vision service
        String rawJson = visionService.analyzeGesture(imageBytes, mimeType);

        // 2. Parse and validate JSON structure
        return parseAndEvaluate(rawJson, sourceType);
    }

    /**
     * Parses raw JSON from Gemini and applies threshold enforcement.
     */
    public GestureAnalysis parseAndEvaluate(String rawJson, String sourceType) {
        String cleanJson = sanitizeJson(rawJson);

        String gestureCodeStr = "UNKNOWN";
        double confidence = 0.0;
        String explanation = "Failed to parse model diagnostic reasoning.";

        try {
            JsonObject jsonObject = JsonParser.parseString(cleanJson).getAsJsonObject();

            if (jsonObject.has("gestureCode") && !jsonObject.get("gestureCode").isJsonNull()) {
                gestureCodeStr = jsonObject.get("gestureCode").getAsString();
            } else if (jsonObject.has("gesture") && !jsonObject.get("gesture").isJsonNull()) {
                gestureCodeStr = jsonObject.get("gesture").getAsString();
            }

            if (jsonObject.has("confidence") && !jsonObject.get("confidence").isJsonNull()) {
                confidence = jsonObject.get("confidence").getAsDouble();
            }

            if (jsonObject.has("explanation") && !jsonObject.get("explanation").isJsonNull()) {
                explanation = jsonObject.get("explanation").getAsString();
            }
        } catch (JsonSyntaxException | IllegalStateException ex) {
            logger.warn("Failed to parse Gemini output as JSON: {}. Falling back to UNKNOWN.", cleanJson, ex);
            return new GestureAnalysis(GestureType.UNKNOWN, 0.0, 
                "Malformed model JSON output. Image ambiguity detected.", sourceType, false);
        }

        // 3. Map string token to supported GestureType enum
        GestureType parsedType = GestureType.fromString(gestureCodeStr);

        // 4. Threshold & Ambiguity Validation
        // Confidence is a model-estimated score; force UNKNOWN if below threshold
        boolean meetsThreshold = (confidence >= this.confidenceThreshold);

        GestureType finalGestureType;
        if (parsedType == GestureType.UNKNOWN) {
            finalGestureType = GestureType.UNKNOWN;
            logger.debug("Gesture classified as UNKNOWN by model (confidence={})", confidence);
        } else if (!meetsThreshold) {
            logger.warn("Gesture '{}' confidence ({}) is below safety threshold ({}). Coercing to UNKNOWN.",
                    parsedType, confidence, this.confidenceThreshold);
            finalGestureType = GestureType.UNKNOWN;
            explanation = String.format("Confidence (%.1f%%) fell below the required %.0f%% reliability threshold.",
                    confidence * 100.0, this.confidenceThreshold * 100.0);
        } else {
            finalGestureType = parsedType;
            logger.info("Recognized gesture: {} (confidence={}, source={})", finalGestureType, confidence, sourceType);
        }

        return new GestureAnalysis(finalGestureType, confidence, explanation, sourceType, meetsThreshold);
    }

    /**
     * Strips any markdown code fence wrappers (e.g. ```json ... ```) if present.
     */
    private static String sanitizeJson(String raw) {
        if (raw == null) {
            return "{}";
        }
        String trimmed = raw.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }

    public double getConfidenceThreshold() {
        return confidenceThreshold;
    }
}
