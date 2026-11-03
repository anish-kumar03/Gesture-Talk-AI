package com.gesturetalk.service;

import com.gesturetalk.model.GestureType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.EnumMap;
import java.util.Map;

/**
 * Dedicated business logic service for mapping recognized hand gestures
 * to immutable, grammatically correct natural language sentences.
 *
 * Guarantees that the final user-facing text is never invented by Gemini or an external LLM.
 */
public class GestureMessageMapper {

    private static final Logger logger = LoggerFactory.getLogger(GestureMessageMapper.class);

    private static final Map<GestureType, String> MESSAGE_CATALOG;

    static {
        Map<GestureType, String> map = new EnumMap<>(GestureType.class);
        map.put(GestureType.THUMBS_UP, "Yes, that's correct.");
        map.put(GestureType.THUMBS_DOWN, "No, that's incorrect.");
        map.put(GestureType.OPEN_PALM, "Please stop or wait.");
        map.put(GestureType.V_SIGN, "Peace and greetings.");
        map.put(GestureType.INDEX_POINTING, "Please look there.");
        map.put(GestureType.UNKNOWN, "Gesture not recognized. Please try again.");
        MESSAGE_CATALOG = Collections.unmodifiableMap(map);
    }

    /**
     * Resolves the authoritative spoken sentence for a given gesture type.
     *
     * @param gestureType Target GestureType enum
     * @return Deterministic predefined text sentence
     */
    public String getMessage(GestureType gestureType) {
        if (gestureType == null) {
            return MESSAGE_CATALOG.get(GestureType.UNKNOWN);
        }
        return MESSAGE_CATALOG.getOrDefault(gestureType, MESSAGE_CATALOG.get(GestureType.UNKNOWN));
    }

    /**
     * Resolves the authoritative spoken sentence from a gesture code string.
     *
     * @param gestureCode String gesture representation (case-insensitive)
     * @return Deterministic predefined text sentence
     */
    public String getMessage(String gestureCode) {
        GestureType type = GestureType.fromString(gestureCode);
        return getMessage(type);
    }

    /**
     * Determines whether a gesture result should trigger automatic client vocalization.
     * UNKNOWN or unverified gestures must never be spoken automatically.
     *
     * @param gestureType Target GestureType
     * @param meetsThreshold Whether model confidence satisfied the safety threshold
     * @return true if automatic speech synthesis is permitted, false otherwise
     */
    public boolean isAutoSpeakAllowed(GestureType gestureType, boolean meetsThreshold) {
        if (gestureType == null || gestureType == GestureType.UNKNOWN) {
            return false;
        }
        return meetsThreshold;
    }

    /**
     * Returns a human-friendly display name for the gesture.
     */
    public String getDisplayName(GestureType gestureType) {
        if (gestureType == null) {
            return GestureType.UNKNOWN.getDisplayName();
        }
        return gestureType.getDisplayName();
    }
}

