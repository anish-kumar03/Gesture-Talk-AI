package com.gesturetalk.controller;

import com.gesturetalk.exception.AIServiceException;
import com.gesturetalk.service.GestureRecognitionService;
import com.gesturetalk.service.VisionService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.ByteArrayInputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GestureRecognitionServlet Unit Tests")
class GestureRecognitionServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private Part imagePart;

    private GestureRecognitionServlet servlet;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new GestureRecognitionServlet();
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    @DisplayName("Should return 200 OK with analysis JSON for valid image")
    void testSuccessfulRecognition() throws Exception {
        byte[] validJpeg = new byte[32];
        validJpeg[0] = (byte) 0xFF;
        validJpeg[1] = (byte) 0xD8;
        validJpeg[2] = (byte) 0xFF;

        VisionService stubVisionService = (imageBytes, mime) ->
                "{\"gestureCode\":\"THUMBS_UP\",\"confidence\":0.95,\"explanation\":\"Clear thumbs up\"}";
        servlet.setRecognitionService(new GestureRecognitionService(stubVisionService, 0.75));

        when(request.getPart("image")).thenReturn(imagePart);
        when(request.getParameter("sourceType")).thenReturn("WEBCAM");
        when(imagePart.getInputStream()).thenReturn(new ByteArrayInputStream(validJpeg));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String body = responseWriter.toString();
        assertTrue(body.contains("THUMBS_UP"));
        assertTrue(body.contains("Yes, that's correct."));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when image part is missing")
    void testMissingImagePart() throws Exception {
        servlet.setRecognitionService(new GestureRecognitionService((imageBytes, mime) -> "{}", 0.75));
        when(request.getPart("image")).thenReturn(null);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("MISSING_IMAGE_PART"));
    }

    @Test
    @DisplayName("Should return 415 Unsupported Media Type for non-image files")
    void testCorruptedImage() throws Exception {
        servlet.setRecognitionService(new GestureRecognitionService((imageBytes, mime) -> "{}", 0.75));
        byte[] badData = new byte[]{'M', 'Z', 0, 0, 0, 0, 0, 0, 0, 0, 0, 0};

        when(request.getPart("image")).thenReturn(imagePart);
        when(imagePart.getInputStream()).thenReturn(new ByteArrayInputStream(badData));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_UNSUPPORTED_MEDIA_TYPE);
        assertTrue(responseWriter.toString().contains("UNSUPPORTED_IMAGE_FORMAT"));
    }

    @Test
    @DisplayName("Should return 503 Service Unavailable when AI service throws rate limit error")
    void testAIServiceRateLimit() throws Exception {
        byte[] validJpeg = new byte[32];
        validJpeg[0] = (byte) 0xFF;
        validJpeg[1] = (byte) 0xD8;
        validJpeg[2] = (byte) 0xFF;

        VisionService throwingService = (imageBytes, mime) -> {
            throw new AIServiceException("AI_RATE_LIMIT", "Rate limit exceeded", 503);
        };
        servlet.setRecognitionService(new GestureRecognitionService(throwingService, 0.75));

        when(request.getPart("image")).thenReturn(imagePart);
        when(imagePart.getInputStream()).thenReturn(new ByteArrayInputStream(validJpeg));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        assertTrue(responseWriter.toString().contains("AI_RATE_LIMIT"));
    }
}
