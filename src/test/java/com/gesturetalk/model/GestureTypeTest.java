package com.gesturetalk.model;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("GestureType Enum & Mapping Tests")
class GestureTypeTest {

    @ParameterizedTest(name = "Input \"{0}\" should resolve to {1}")
    @CsvSource({
        "THUMBS_UP, THUMBS_UP",
        "thumbs_up, THUMBS_UP",
        "Thumbs Up, THUMBS_UP",
        "thumbs-up, THUMBS_UP",
        "THUMBS_DOWN, THUMBS_DOWN",
        "thumbs_down, THUMBS_DOWN",
        "OPEN_PALM, OPEN_PALM",
        "open_palm, OPEN_PALM",
        "V_SIGN, V_SIGN",
        "v_sign, V_SIGN",
        "v-sign, V_SIGN",
        "INDEX_POINTING, INDEX_POINTING",
        "index_pointing, INDEX_POINTING",
        "UNKNOWN, UNKNOWN",
        "INVALID_SIGN, UNKNOWN",
        "WAVING_HAND, UNKNOWN"
    })
    void testFromStringResolution(String input, GestureType expected) {
        assertEquals(expected, GestureType.fromString(input));
    }

    @Test
    @DisplayName("Null or empty input should resolve to UNKNOWN")
    void testNullOrEmptyResolvesToUnknown() {
        assertEquals(GestureType.UNKNOWN, GestureType.fromString(null));
        assertEquals(GestureType.UNKNOWN, GestureType.fromString(""));
        assertEquals(GestureType.UNKNOWN, GestureType.fromString("   "));
    }

    @Test
    @DisplayName("Deterministic output messages must match specifications exactly")
    void testDeterministicMessages() {
        assertEquals("Yes, that's correct.", GestureType.THUMBS_UP.getDefaultMessage());
        assertEquals("No, that's incorrect.", GestureType.THUMBS_DOWN.getDefaultMessage());
        assertEquals("Please stop or wait.", GestureType.OPEN_PALM.getDefaultMessage());
        assertEquals("Peace and greetings.", GestureType.V_SIGN.getDefaultMessage());
        assertEquals("Please look there.", GestureType.INDEX_POINTING.getDefaultMessage());
        assertEquals("Gesture not recognized. Please try again.", GestureType.UNKNOWN.getDefaultMessage());
    }
}

