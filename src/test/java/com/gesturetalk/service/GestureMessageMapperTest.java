package com.gesturetalk.service;

import com.gesturetalk.model.GestureType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GestureMessageMapper Unit Tests")
class GestureMessageMapperTest {

    private GestureMessageMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new GestureMessageMapper();
    }

    @Test
    @DisplayName("Should map all five gestures to exact deterministic sentences")
    void testPredefinedMessagesForAllGestures() {
        assertEquals("Yes, that's correct.", mapper.getMessage(GestureType.THUMBS_UP));
        assertEquals("No, that's incorrect.", mapper.getMessage(GestureType.THUMBS_DOWN));
        assertEquals("Please stop or wait.", mapper.getMessage(GestureType.OPEN_PALM));
        assertEquals("Peace and greetings.", mapper.getMessage(GestureType.V_SIGN));
        assertEquals("Please look there.", mapper.getMessage(GestureType.INDEX_POINTING));
        assertEquals("Gesture not recognized. Please try again.", mapper.getMessage(GestureType.UNKNOWN));
    }

    @Test
    @DisplayName("Should handle null GestureType and return UNKNOWN message")
    void testNullGestureType() {
        assertEquals("Gesture not recognized. Please try again.", mapper.getMessage((GestureType) null));
    }

    @ParameterizedTest(name = "String code \"{0}\" should map to expected phrase")
    @CsvSource(delimiter = '|', value = {
        "THUMBS_UP | Yes, that's correct.",
        "thumbs_down | No, that's incorrect.",
        "open-palm | Please stop or wait.",
        "V_SIGN | Peace and greetings.",
        "index_pointing | Please look there.",
        "UNKNOWN | Gesture not recognized. Please try again.",
        "UNSUPPORTED_GESTURE | Gesture not recognized. Please try again."
    })
    void testStringCodeMapping(String code, String expectedMessage) {
        assertEquals(expectedMessage, mapper.getMessage(code));
    }

    @Test
    @DisplayName("isAutoSpeakAllowed should permit speech ONLY for known gestures meeting threshold")
    void testAutoSpeakAuthorization() {
        // Known gestures with threshold satisfied -> TRUE
        assertTrue(mapper.isAutoSpeakAllowed(GestureType.THUMBS_UP, true));
        assertTrue(mapper.isAutoSpeakAllowed(GestureType.THUMBS_DOWN, true));
        assertTrue(mapper.isAutoSpeakAllowed(GestureType.OPEN_PALM, true));
        assertTrue(mapper.isAutoSpeakAllowed(GestureType.V_SIGN, true));
        assertTrue(mapper.isAutoSpeakAllowed(GestureType.INDEX_POINTING, true));

        // Known gestures failing threshold -> FALSE
        assertFalse(mapper.isAutoSpeakAllowed(GestureType.THUMBS_UP, false));
        assertFalse(mapper.isAutoSpeakAllowed(GestureType.V_SIGN, false));

        // UNKNOWN gesture MUST NEVER auto-speak -> FALSE
        assertFalse(mapper.isAutoSpeakAllowed(GestureType.UNKNOWN, true));
        assertFalse(mapper.isAutoSpeakAllowed(GestureType.UNKNOWN, false));

        // Null gesture MUST NEVER auto-speak -> FALSE
        assertFalse(mapper.isAutoSpeakAllowed(null, true));
    }

    @Test
    @DisplayName("Should return correct display names")
    void testDisplayNames() {
        assertEquals("Thumbs Up", mapper.getDisplayName(GestureType.THUMBS_UP));
        assertEquals("Open Palm", mapper.getDisplayName(GestureType.OPEN_PALM));
        assertEquals("Unknown Gesture", mapper.getDisplayName(GestureType.UNKNOWN));
        assertEquals("Unknown Gesture", mapper.getDisplayName(null));
    }
}
