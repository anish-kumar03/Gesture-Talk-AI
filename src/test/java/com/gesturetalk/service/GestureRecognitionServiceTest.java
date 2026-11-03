package com.gesturetalk.service;

import com.gesturetalk.model.GestureAnalysis;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GestureRecognitionService Unit & Validation Tests")
class GestureRecognitionServiceTest {

    @Test
    @DisplayName("Should accept high-confidence known gesture above threshold (0.92 >= 0.75)")
    void testHighConfidenceGesture() {
        String mockJson = "{\n" +
                "  \"gestureCode\": \"THUMBS_UP\",\n" +
                "  \"confidence\": 0.92,\n" +
                "  \"explanation\": \"Closed fist with thumb extended vertically upward.\"\n" +
                "}";

        VisionService stubVisionService = (imageBytes, mimeType) -> mockJson;
        GestureRecognitionService service = new GestureRecognitionService(stubVisionService, 0.75);

        byte[] fakeImage = new byte[]{1, 2, 3};
        GestureAnalysis result = service.recognize(fakeImage, "image/jpeg", "WEBCAM");

        assertNotNull(result);
        assertEquals("THUMBS_UP", result.getGestureCode());
        assertEquals("Thumbs Up", result.getDisplayName());
        assertEquals("Yes, that's correct.", result.getMappedMessage());
        assertEquals(0.92, result.getConfidence());
        assertTrue(result.isMeetsThreshold());
        assertEquals("WEBCAM", result.getSourceType());
    }

    @Test
    @DisplayName("Should coerce gesture below 0.75 threshold to UNKNOWN")
    void testLowConfidenceCoercedToUnknown() {
        String mockJson = "{\n" +
                "  \"gestureCode\": \"V_SIGN\",\n" +
                "  \"confidence\": 0.65,\n" +
                "  \"explanation\": \"Hand appears to form a V but finger spacing is irregular.\"\n" +
                "}";

        VisionService stubVisionService = (imageBytes, mimeType) -> mockJson;
        GestureRecognitionService service = new GestureRecognitionService(stubVisionService, 0.75);

        byte[] fakeImage = new byte[]{1, 2, 3};
        GestureAnalysis result = service.recognize(fakeImage, "image/png", "UPLOAD");

        assertNotNull(result);
        assertEquals("UNKNOWN", result.getGestureCode());
        assertEquals("Gesture not recognized. Please try again.", result.getMappedMessage());
        assertEquals(0.65, result.getConfidence());
        assertFalse(result.isMeetsThreshold());
        assertTrue(result.getExplanation().contains("reliability threshold"));
    }

    @Test
    @DisplayName("Should parse markdown-wrapped JSON cleanly")
    void testMarkdownWrappedJson() {
        String markdownJson = "```json\n" +
                "{\n" +
                "  \"gestureCode\": \"OPEN_PALM\",\n" +
                "  \"confidence\": 0.88,\n" +
                "  \"explanation\": \"Flat open hand with five digits extended.\"\n" +
                "}\n" +
                "```";

        VisionService stubVisionService = (imageBytes, mimeType) -> markdownJson;
        GestureRecognitionService service = new GestureRecognitionService(stubVisionService, 0.75);

        GestureAnalysis result = service.parseAndEvaluate(markdownJson, "WEBCAM");

        assertEquals("OPEN_PALM", result.getGestureCode());
        assertEquals("Please stop or wait.", result.getMappedMessage());
        assertEquals(0.88, result.getConfidence());
        assertTrue(result.isMeetsThreshold());
    }

    @Test
    @DisplayName("Should handle alternative field 'gesture' if returned by model")
    void testAlternativeFieldGesture() {
        String mockJson = "{\n" +
                "  \"gesture\": \"INDEX_POINTING\",\n" +
                "  \"confidence\": 0.95,\n" +
                "  \"explanation\": \"Single index finger pointing leftward.\"\n" +
                "}";

        VisionService stubVisionService = (imageBytes, mimeType) -> mockJson;
        GestureRecognitionService service = new GestureRecognitionService(stubVisionService, 0.75);

        GestureAnalysis result = service.parseAndEvaluate(mockJson, "WEBCAM");

        assertEquals("INDEX_POINTING", result.getGestureCode());
        assertEquals("Please look there.", result.getMappedMessage());
        assertTrue(result.isMeetsThreshold());
    }

    @Test
    @DisplayName("Should safely handle malformed JSON and return UNKNOWN")
    void testMalformedJsonFallback() {
        String malformed = "I think the gesture is THUMBS_UP with 90% confidence.";

        VisionService stubVisionService = (imageBytes, mimeType) -> malformed;
        GestureRecognitionService service = new GestureRecognitionService(stubVisionService, 0.75);

        GestureAnalysis result = service.parseAndEvaluate(malformed, "WEBCAM");

        assertEquals("UNKNOWN", result.getGestureCode());
        assertEquals(0.0, result.getConfidence());
        assertFalse(result.isMeetsThreshold());
    }

    @Test
    @DisplayName("Should handle explicit UNKNOWN gesture from model")
    void testExplicitUnknownFromModel() {
        String mockJson = "{\n" +
                "  \"gestureCode\": \"UNKNOWN\",\n" +
                "  \"confidence\": 0.30,\n" +
                "  \"explanation\": \"No hand is visible in the frame.\"\n" +
                "}";

        VisionService stubVisionService = (imageBytes, mimeType) -> mockJson;
        GestureRecognitionService service = new GestureRecognitionService(stubVisionService, 0.75);

        GestureAnalysis result = service.parseAndEvaluate(mockJson, "WEBCAM");

        assertEquals("UNKNOWN", result.getGestureCode());
        assertEquals(0.30, result.getConfidence());
        assertFalse(result.isMeetsThreshold());
    }
}
