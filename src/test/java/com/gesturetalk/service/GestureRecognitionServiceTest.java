package com.gesturetalk.service;

import com.gesturetalk.model.GestureAnalysis;
import com.gesturetalk.model.GestureType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
        assertTrue(result.isShouldAutoSpeak());
        assertEquals("WEBCAM", result.getSourceType());
    }

    @ParameterizedTest(name = "Gesture {0} should map to \"{1}\" and authorize auto-speech")
    @CsvSource(delimiter = '|', value = {
        "THUMBS_UP | Yes, that's correct.",
        "THUMBS_DOWN | No, that's incorrect.",
        "OPEN_PALM | Please stop or wait.",
        "V_SIGN | Peace and greetings.",
        "INDEX_POINTING | Please look there."
    })
    void testAllFiveSupportedGestures(String gestureCode, String expectedPhrase) {
        String mockJson = String.format("{\n" +
                "  \"gestureCode\": \"%s\",\n" +
                "  \"confidence\": 0.90,\n" +
                "  \"explanation\": \"Clear observed gesture posture.\"\n" +
                "}", gestureCode);

        VisionService stubVisionService = (imageBytes, mimeType) -> mockJson;
        GestureRecognitionService service = new GestureRecognitionService(stubVisionService, 0.75);

        GestureAnalysis result = service.parseAndEvaluate(mockJson, "WEBCAM");

        assertEquals(gestureCode, result.getGestureCode());
        assertEquals(expectedPhrase, result.getMappedMessage());
        assertTrue(result.isMeetsThreshold());
        assertTrue(result.isShouldAutoSpeak(), "Recognized gestures meeting threshold must authorize auto-speech");
    }

    @Test
    @DisplayName("Should coerce gesture below 0.75 threshold to UNKNOWN and inhibit auto-speech")
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
        assertFalse(result.isShouldAutoSpeak(), "Low confidence gestures must NEVER auto-speak");
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
        assertTrue(result.isShouldAutoSpeak());
    }

    @Test
    @DisplayName("Should safely handle malformed JSON and return UNKNOWN with auto-speak disabled")
    void testMalformedJsonFallback() {
        String malformed = "I think the gesture is THUMBS_UP with 90% confidence.";

        VisionService stubVisionService = (imageBytes, mimeType) -> malformed;
        GestureRecognitionService service = new GestureRecognitionService(stubVisionService, 0.75);

        GestureAnalysis result = service.parseAndEvaluate(malformed, "WEBCAM");

        assertEquals("UNKNOWN", result.getGestureCode());
        assertEquals(0.0, result.getConfidence());
        assertFalse(result.isMeetsThreshold());
        assertFalse(result.isShouldAutoSpeak());
    }

    @Test
    @DisplayName("Explicit UNKNOWN gesture from model must never auto-speak")
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
        assertFalse(result.isShouldAutoSpeak(), "UNKNOWN results must never be spoken automatically");
    }
}
