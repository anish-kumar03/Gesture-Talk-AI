package com.gesturetalk.service;

import com.gesturetalk.exception.AIServiceException;
import com.google.genai.Client;
import com.google.genai.types.Blob;
import com.google.genai.types.Content;
import com.google.genai.types.GenerateContentConfig;
import com.google.genai.types.GenerateContentResponse;
import com.google.genai.types.Part;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import java.util.Arrays;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Service encapsulating all communication with Google Gemini Multimodal Vision API
 * using the official Google GenAI Java SDK (com.google.genai).
 */
public class GeminiVisionService implements VisionService {

    private static final Logger logger = LoggerFactory.getLogger(GeminiVisionService.class);

    public static final String DEFAULT_MODEL = "gemini-2.5-flash";

    private static final String CLASSIFICATION_PROMPT = 
        "You are an expert computer vision system for hand gesture recognition.\n" +
        "Analyze the provided image and classify the primary hand gesture shown.\n" +
        "You MUST classify the gesture into EXACTLY ONE of the following codes:\n" +
        "1. \"THUMBS_UP\"      - Closed fist with thumb pointing straight upward.\n" +
        "2. \"THUMBS_DOWN\"    - Closed fist with thumb pointing straight downward.\n" +
        "3. \"OPEN_PALM\"      - Flat open hand with all 5 digits extended facing camera.\n" +
        "4. \"V_SIGN\"         - Index and middle fingers extended in a V-shape; other fingers folded.\n" +
        "5. \"INDEX_POINTING\" - Single index finger extended pointing; thumb and other fingers folded.\n" +
        "6. \"UNKNOWN\"        - If no hand is visible, multiple hands show conflicting signs, " +
        "the hand is blurred/occluded, or the gesture does not unambiguously match the 5 above.\n\n" +
        "You must output STRICT JSON with this exact schema:\n" +
        "{\n" +
        "  \"gestureCode\": \"THUMBS_UP | THUMBS_DOWN | OPEN_PALM | V_SIGN | INDEX_POINTING | UNKNOWN\",\n" +
        "  \"confidence\": 0.0 to 1.0,\n" +
        "  \"explanation\": \"Brief 1-sentence description of the observed hand posture.\"\n" +
        "}";

    private final Client client;
    private final String modelName;

    /**
     * Production constructor initializing the official Google GenAI Client from environment variables.
     */
    public GeminiVisionService() {
        String apiKey = resolveApiKey();
        if (apiKey == null || apiKey.trim().isEmpty()) {
            logger.error("CRITICAL: Neither GEMINI_API_KEY nor GOOGLE_API_KEY is configured in the environment.");
            throw new AIServiceException("AI_CONFIG_ERROR", 
                "GEMINI_API_KEY environment variable is not configured on the server.", 500);
        }

        String configuredModel = System.getenv("GEMINI_MODEL");
        this.modelName = (configuredModel != null && !configuredModel.isBlank()) ? configuredModel.trim() : DEFAULT_MODEL;

        try {
            this.client = Client.builder().apiKey(apiKey).build();
            logger.info("GeminiVisionService initialized with model: {}", this.modelName);
        } catch (Exception ex) {
            logger.error("Failed to initialize Google GenAI SDK client: {}", ex.getMessage(), ex);
            throw new AIServiceException("AI_CLIENT_INIT_ERROR", 
                "Failed to initialize official Google GenAI SDK client: " + ex.getMessage(), 500, ex);
        }
    }

    /**
     * Dependency-injection constructor for testing with mock clients.
     */
    public GeminiVisionService(Client client, String modelName) {
        this.client = client;
        this.modelName = (modelName != null && !modelName.isBlank()) ? modelName : DEFAULT_MODEL;
    }

    /**
     * Resolves the API key from system environment variables.
     */
    private static String resolveApiKey() {
        String key = System.getenv("GEMINI_API_KEY");
        if (key != null && !key.trim().isEmpty()) {
            return key.trim();
        }
        key = System.getenv("GOOGLE_API_KEY");
        if (key != null && !key.trim().isEmpty()) {
            return key.trim();
        }
        return null;
    }

    /**
     * Analyzes image bytes using Gemini Vision and returns the raw structured JSON string.
     *
     * @param imageBytes Validated in-memory image byte array
     * @param mimeType Canonical image MIME type (e.g. "image/jpeg", "image/png")
     * @return Raw JSON response from Gemini
     * @throws AIServiceException if API call fails, times out, or quota is exhausted
     */
    public String analyzeGesture(byte[] imageBytes, String mimeType) {
        if (imageBytes == null || imageBytes.length == 0) {
            throw new IllegalArgumentException("Image bytes cannot be null or empty.");
        }

        try {
            // Encode in-memory bytes into base64 for Blob payload
            String base64Data = Base64.getEncoder().encodeToString(imageBytes);

            Blob imageBlob = Blob.builder()
                    .mimeType(mimeType)
                    .data(base64Data)
                    .build();

            Part imagePart = Part.builder()
                    .inlineData(imageBlob)
                    .build();

            Part textPart = Part.builder()
                    .text(CLASSIFICATION_PROMPT)
                    .build();

            Content content = Content.builder()
                    .parts(Arrays.asList(textPart, imagePart))
                    .build();

            GenerateContentConfig config = GenerateContentConfig.builder()
                    .responseMimeType("application/json")
                    .temperature(0.2f)
                    .build();

            logger.debug("Dispatching multimodal request to Gemini ({})", modelName);
            GenerateContentResponse response = client.models.generateContent(modelName, content, config);

            String rawText = response.text();
            if (rawText == null || rawText.isBlank()) {
                logger.warn("Gemini returned null or empty response text.");
                throw new AIServiceException("AI_EMPTY_RESPONSE", 
                    "Gemini Vision API returned an empty classification response.", 502);
            }

            logger.debug("Received Gemini raw response: {}", rawText);
            return rawText;

        } catch (AIServiceException ex) {
            throw ex;
        } catch (Exception ex) {
            String errorMsg = ex.getMessage() != null ? ex.getMessage() : ex.toString();
            String sanitizedMsg = sanitizeErrorMessage(errorMsg);
            logger.error("Gemini Vision API call failed: {}", sanitizedMsg);

            if (errorMsg.contains("429") || errorMsg.contains("RESOURCE_EXHAUSTED")) {
                throw new AIServiceException("AI_RATE_LIMIT", 
                    "Gemini API rate limit or quota exceeded. Please retry in a few seconds.", 503, ex);
            }
            if (errorMsg.contains("API_KEY_INVALID") || errorMsg.contains("401") || errorMsg.contains("403")) {
                throw new AIServiceException("AI_AUTH_ERROR", 
                    "Invalid or unauthorized GEMINI_API_KEY provided to Gemini API.", 500, ex);
            }
            if (errorMsg.contains("timeout") || errorMsg.contains("timed out")) {
                throw new AIServiceException("AI_TIMEOUT", 
                    "Gemini Vision API connection timed out. Please retry.", 504, ex);
            }

            throw new AIServiceException("AI_SERVICE_ERROR", 
                "Failed to process image with Gemini Vision: " + sanitizedMsg, 502, ex);
        }
    }

    /**
     * Sanitizes error messages to ensure API keys and credentials are never exposed in logs or responses.
     */
    private static String sanitizeErrorMessage(String msg) {
        if (msg == null) return "Unknown AI service error.";
        return msg.replaceAll("(?i)(key|api[_-]?key)=([a-zA-Z0-9_\\-]+)", "$1=REDACTED");
    }

    public String getModelName() {
        return modelName;
    }
}
