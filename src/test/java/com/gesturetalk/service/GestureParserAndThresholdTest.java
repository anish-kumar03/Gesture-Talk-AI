package com.gesturetalk.service;

import com.gesturetalk.model.GestureAnalysis;
import com.gesturetalk.model.GestureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Gesture Recognition JSON Parsing & Threshold Verification")
class GestureParserAndThresholdTest {

    private MockVisionService mockVision;
    private GestureRecognitionService service;

    @BeforeEach
    void setUp() {
        mockVision = new MockVisionService();
        service = new GestureRecognitionService(mockVision, 0.75);
    }

    @Test
    @DisplayName("Should strip markdown code fences from JSON output")
    void testMarkdownCodeFenceStripping() {
        String wrappedJson = "```json\n{\n  \"gestureCode\": \"OPEN_PALM\",\n  \"confidence\": 0.94,\n  \"explanation\": \"Hand open\"\n}\n```";
        GestureAnalysis analysis = service.parseAndEvaluate(wrappedJson, "WEBCAM");

        assertEquals("OPEN_PALM", analysis.getGestureCode());
        assertEquals("Please stop or wait.", analysis.getMappedMessage());
        assertTrue(analysis.isMeetsThreshold());
        assertTrue(analysis.isShouldAutoSpeak());
    }

    @ParameterizedTest(name = "Unsupported gesture \"{0}\" must be coerced to UNKNOWN")
    @ValueSource(strings = {"FIST", "OK_SIGN", "WAVE", "PEACE_SIGN", "CROSS", "SHAKA", "ROCK_ON", "UNKNOWN_SIGN"})
    void testUnsupportedGesturesFallback(String unsupportedCode) {
        String json = String.format("{\n  \"gestureCode\": \"%s\",\n  \"confidence\": 0.99,\n  \"explanation\": \"Unsupported sign\"\n}", unsupportedCode);
        GestureAnalysis analysis = service.parseAndEvaluate(json, "UPLOAD");

        assertEquals("UNKNOWN", analysis.getGestureCode());
        assertEquals("Gesture not recognized. Please try again.", analysis.getMappedMessage());
        assertFalse(analysis.isShouldAutoSpeak());
    }

    @Test
    @DisplayName("Confidence exactly equal to threshold (0.750) must be accepted")
    void testExactThreshold() {
        String json = "{\n  \"gestureCode\": \"THUMBS_UP\",\n  \"confidence\": 0.750,\n  \"explanation\": \"Thumb upright\"\n}";
        GestureAnalysis analysis = service.parseAndEvaluate(json, "WEBCAM");

        assertEquals("THUMBS_UP", analysis.getGestureCode());
        assertTrue(analysis.isMeetsThreshold());
        assertTrue(analysis.isShouldAutoSpeak());
    }

    @Test
    @DisplayName("Confidence just below threshold (0.749) must be coerced to UNKNOWN")
    void testJustBelowThreshold() {
        String json = "{\n  \"gestureCode\": \"THUMBS_UP\",\n  \"confidence\": 0.749,\n  \"explanation\": \"Thumb upright\"\n}";
        GestureAnalysis analysis = service.parseAndEvaluate(json, "WEBCAM");

        assertEquals("UNKNOWN", analysis.getGestureCode());
        assertFalse(analysis.isMeetsThreshold());
        assertFalse(analysis.isShouldAutoSpeak());
        assertTrue(analysis.getExplanation().contains("threshold"));
    }

    @Test
    @DisplayName("Should handle missing optional explanation gracefully")
    void testMissingExplanation() {
        String json = "{\n  \"gestureCode\": \"V_SIGN\",\n  \"confidence\": 0.88\n}";
        GestureAnalysis analysis = service.parseAndEvaluate(json, "WEBCAM");

        assertEquals("V_SIGN", analysis.getGestureCode());
        assertEquals("Peace and greetings.", analysis.getMappedMessage());
        assertNotNull(analysis.getExplanation());
    }

    @Test
    @DisplayName("Should handle completely empty or invalid JSON string without crashing")
    void testCorruptedJson() {
        GestureAnalysis a1 = service.parseAndEvaluate("", "WEBCAM");
        assertEquals("UNKNOWN", a1.getGestureCode());
        assertFalse(a1.isShouldAutoSpeak());

        GestureAnalysis a2 = service.parseAndEvaluate("{ invalid json format }", "WEBCAM");
        assertEquals("UNKNOWN", a2.getGestureCode());
        assertFalse(a2.isShouldAutoSpeak());
    }
}

