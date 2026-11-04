package com.gesturetalk.controller;

import com.gesturetalk.dao.GestureHistoryDAO;
import com.gesturetalk.dao.HistoryDAO;
import com.gesturetalk.model.GestureAnalysis;
import com.gesturetalk.model.GestureHistory;
import com.gesturetalk.model.GestureType;
import com.gesturetalk.service.GestureMessageMapper;
import com.gesturetalk.util.JsonUtils;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller Servlet handling client-side MediaPipe gesture recognition payloads.
 * Mapped to POST /api/gesture-result.
 */
@WebServlet(name = "GestureResultServlet", urlPatterns = {"/api/gesture-result", "/api/gesture-result/*"})
public class GestureResultServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(GestureResultServlet.class);
    public static final double DEFAULT_CONFIDENCE_THRESHOLD = 0.70;

    private HistoryDAO historyDAO;
    private GestureMessageMapper messageMapper;
    private double confidenceThreshold;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            this.historyDAO = new GestureHistoryDAO();
            logger.info("GestureResultServlet initialized successfully.");
        } catch (Exception ex) {
            logger.warn("GestureHistoryDAO deferred initialization: {}", ex.getMessage());
        }
        this.messageMapper = new GestureMessageMapper();
        this.confidenceThreshold = resolveConfiguredThreshold();
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

    void setHistoryDAO(HistoryDAO historyDAO) {
        this.historyDAO = historyDAO;
    }

    void setMessageMapper(GestureMessageMapper messageMapper) {
        this.messageMapper = messageMapper;
    }

    void setConfidenceThreshold(double threshold) {
        this.confidenceThreshold = threshold;
    }

    private synchronized HistoryDAO getHistoryDAO() {
        if (this.historyDAO == null) {
            this.historyDAO = new GestureHistoryDAO();
        }
        return this.historyDAO;
    }

    private synchronized GestureMessageMapper getMessageMapper() {
        if (this.messageMapper == null) {
            this.messageMapper = new GestureMessageMapper();
        }
        return this.messageMapper;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }

        String rawBody = sb.toString().trim();
        if (rawBody.isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtils.errorJson("EMPTY_REQUEST_BODY", 
                "Request body cannot be empty. Expected JSON payload."));
            return;
        }

        JsonObject jsonObject;
        try {
            jsonObject = JsonParser.parseString(rawBody).getAsJsonObject();
        } catch (JsonSyntaxException | IllegalStateException ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtils.errorJson("MALFORMED_JSON", 
                "Request body contains invalid JSON: " + ex.getMessage()));
            return;
        }

        // 1. Validate gestureCode
        if (!jsonObject.has("gestureCode") || jsonObject.get("gestureCode").isJsonNull() || 
            jsonObject.get("gestureCode").getAsString().trim().isEmpty()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtils.errorJson("MISSING_GESTURE_CODE", 
                "Missing or empty mandatory field 'gestureCode'."));
            return;
        }

        String rawGestureCode = jsonObject.get("gestureCode").getAsString().trim();
        String normalizedCode = rawGestureCode.toUpperCase().replace(" ", "_").replace("-", "_");

        boolean isValidEnum = false;
        for (GestureType gt : GestureType.values()) {
            if (gt.name().equalsIgnoreCase(normalizedCode)) {
                isValidEnum = true;
                break;
            }
        }

        if (!isValidEnum) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtils.errorJson("UNMAPPED_GESTURE", 
                "Gesture code '" + rawGestureCode + "' is not a supported GestureType."));
            return;
        }

        GestureType gestureType = GestureType.valueOf(normalizedCode);

        // 2. Validate confidence
        if (!jsonObject.has("confidence") || jsonObject.get("confidence").isJsonNull()) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtils.errorJson("MISSING_CONFIDENCE", 
                "Missing mandatory field 'confidence'."));
            return;
        }

        double confidence;
        try {
            confidence = jsonObject.get("confidence").getAsDouble();
        } catch (Exception ex) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtils.errorJson("INVALID_CONFIDENCE", 
                "Confidence must be a valid numeric floating-point number."));
            return;
        }

        if (Double.isNaN(confidence) || confidence < 0.0 || confidence > 1.0) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write(JsonUtils.errorJson("INVALID_CONFIDENCE", 
                "Confidence must be a valid number between 0.0 and 1.0. Provided: " + confidence));
            return;
        }

        // 3. Extract sourceType (default to WEBCAM)
        String sourceType = "WEBCAM";
        if (jsonObject.has("sourceType") && !jsonObject.get("sourceType").isJsonNull() &&
            !jsonObject.get("sourceType").getAsString().trim().isEmpty()) {
            sourceType = jsonObject.get("sourceType").getAsString().trim().toUpperCase();
        }

        // 4. Threshold & Message Evaluation
        boolean meetsThreshold = (confidence >= this.confidenceThreshold);
        GestureMessageMapper mapper = getMessageMapper();

        GestureType effectiveType = gestureType;
        String explanation;
        if (effectiveType == GestureType.UNKNOWN) {
            explanation = "Gesture detected as UNKNOWN or unclassified.";
        } else if (!meetsThreshold) {
            logger.warn("MediaPipe gesture '{}' confidence ({}) is below safety threshold ({}). Coercing to UNKNOWN.",
                    effectiveType, confidence, this.confidenceThreshold);
            effectiveType = GestureType.UNKNOWN;
            explanation = String.format("Confidence (%.1f%%) fell below the required %.0f%% reliability threshold.",
                    confidence * 100.0, this.confidenceThreshold * 100.0);
        } else {
            explanation = "Real-time client-side gesture recognition via Google MediaPipe.";
            logger.info("Recognized MediaPipe gesture: {} (confidence={}, source={})", effectiveType, confidence, sourceType);
        }

        String mappedMessage = mapper.getMessage(effectiveType);
        boolean shouldAutoSpeak = mapper.isAutoSpeakAllowed(effectiveType, meetsThreshold);

        GestureAnalysis analysis = new GestureAnalysis(
            effectiveType,
            mappedMessage,
            confidence,
            explanation,
            sourceType,
            meetsThreshold,
            shouldAutoSpeak
        );

        // 5. Persist to MySQL database (gesture_history table)
        try {
            GestureHistory history = GestureHistory.fromAnalysis(analysis);
            long loggedId = getHistoryDAO().insert(history);
            logger.debug("Successfully logged MediaPipe gesture recognition to database with id: {}", loggedId);
        } catch (Exception dbEx) {
            logger.warn("Could not log gesture to database (database may be offline): {}", dbEx.getMessage());
        }

        // 6. Return response
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtils.toJson(analysis));
    }
}

