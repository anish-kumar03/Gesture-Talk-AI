package com.gesturetalk.util;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;
import java.time.Instant;

/**
 * Utility wrapper for JSON serialization and standardized API response payloads.
 */
public final class JsonUtils {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();

    private JsonUtils() {
        // Utility class
    }

    public static Gson getGson() {
        return GSON;
    }

    public static String toJson(Object object) {
        return GSON.toJson(object);
    }

    public static <T> T fromJson(String json, Class<T> clazz) {
        return GSON.fromJson(json, clazz);
    }

    /**
     * Builds a standardized error response JSON string.
     */
    public static String errorJson(String errorCode, String message) {
        JsonObject obj = new JsonObject();
        obj.addProperty("success", false);
        obj.addProperty("errorCode", errorCode);
        obj.addProperty("message", message);
        obj.addProperty("timestamp", Instant.now().toString());
        return GSON.toJson(obj);
    }
}
