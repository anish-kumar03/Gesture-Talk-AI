package com.gesturetalk.service;

import com.gesturetalk.exception.AIServiceException;
import com.gesturetalk.model.GestureType;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Mock implementation of {@link VisionService} for automated testing, offline development,
 * and deterministic accuracy benchmarking.
 * <p>
 * Eliminates dependencies on external networks or live Gemini API quotas during test execution.
 */
public class MockVisionService implements VisionService {

    private static final Logger logger = LoggerFactory.getLogger(MockVisionService.class);

    private String nextResponse = null;
    private RuntimeException simulatedException = null;
    private byte[] lastAnalyzedBytes = null;
    private String lastMimeType = null;
    private final AtomicInteger callCount = new AtomicInteger(0);

    public MockVisionService() {
        this.nextResponse = buildSuccessJson("THUMBS_UP", 0.95, "Mock detection: Upright thumb observed.");
    }

    public MockVisionService(String fixedResponseJson) {
        this.nextResponse = fixedResponseJson;
    }

    public MockVisionService(GestureType gesture, double confidence, String explanation) {
        setPredefinedGesture(gesture, confidence, explanation);
    }

    @Override
    public String analyzeGesture(byte[] imageBytes, String mimeType) {
        callCount.incrementAndGet();
        this.lastAnalyzedBytes = imageBytes;
        this.lastMimeType = mimeType;

        logger.debug("MockVisionService invoked (callCount={}, mimeType={})", callCount.get(), mimeType);

        if (simulatedException != null) {
            RuntimeException ex = this.simulatedException;
            this.simulatedException = null; // one-shot or reset
            throw ex;
        }

        if (nextResponse != null) {
            return nextResponse;
        }

        return buildSuccessJson("THUMBS_UP", 0.95, "Default mock detection: Upright thumb observed.");
    }

    public void setNextResponse(String rawJson) {
        this.nextResponse = rawJson;
        this.simulatedException = null;
    }

    public void setPredefinedGesture(GestureType gesture, double confidence, String explanation) {
        String code = gesture != null ? gesture.name() : "UNKNOWN";
        this.nextResponse = buildSuccessJson(code, confidence, explanation);
        this.simulatedException = null;
    }

    public void simulateRateLimit() {
        this.simulatedException = new AIServiceException("AI_RATE_LIMIT", 
                "Simulated Gemini API rate limit exceeded.", 503);
    }

    public void simulateTimeout() {
        this.simulatedException = new AIServiceException("AI_TIMEOUT", 
                "Simulated Gemini Vision API connection timeout.", 504);
    }

    public void simulateNetworkFailure() {
        this.simulatedException = new AIServiceException("AI_SERVICE_ERROR", 
                "Simulated network connection failure to Gemini API.", 502);
    }

    public void simulateEmptyResponse() {
        this.simulatedException = new AIServiceException("AI_EMPTY_RESPONSE", 
                "Simulated empty response text from model.", 502);
    }

    public void simulateMalformedJson() {
        this.nextResponse = "This is not valid JSON from the model.";
        this.simulatedException = null;
    }

    public int getCallCount() {
        return callCount.get();
    }

    public byte[] getLastAnalyzedBytes() {
        return lastAnalyzedBytes;
    }

    public String getLastMimeType() {
        return lastMimeType;
    }

    public void reset() {
        this.nextResponse = null;
        this.simulatedException = null;
        this.lastAnalyzedBytes = null;
        this.lastMimeType = null;
        this.callCount.set(0);
    }

    private static String buildSuccessJson(String gestureCode, double confidence, String explanation) {
        return String.format("{\n" +
                "  \"gestureCode\": \"%s\",\n" +
                "  \"confidence\": %.3f,\n" +
                "  \"explanation\": \"%s\"\n" +
                "}", gestureCode, confidence, explanation != null ? explanation.replace("\"", "\\\"") : "");
    }
}
