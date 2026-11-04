package com.gesturetalk.controller;

import com.gesturetalk.dao.HistoryDAO;
import com.gesturetalk.model.GestureHistory;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.BufferedReader;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.sql.SQLException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GestureResultServlet Unit Tests")
class GestureResultServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HistoryDAO historyDAO;

    private GestureResultServlet servlet;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new GestureResultServlet();
        servlet.setHistoryDAO(historyDAO);
        servlet.setConfidenceThreshold(0.70);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    private void setRequestBody(String json) throws Exception {
        BufferedReader reader = new BufferedReader(new StringReader(json));
        when(request.getReader()).thenReturn(reader);
    }

    @Test
    @DisplayName("Should return 200 OK with mapped message and persist to DB for valid payload")
    void testSuccessfulGestureResult() throws Exception {
        String payload = "{\"gestureCode\":\"THUMBS_UP\",\"confidence\":0.94,\"sourceType\":\"WEBCAM\"}";
        setRequestBody(payload);
        when(historyDAO.insert(any(GestureHistory.class))).thenReturn(101L);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(historyDAO).insert(any(GestureHistory.class));
        String body = responseWriter.toString();
        assertTrue(body.contains("\"gestureCode\": \"THUMBS_UP\""));
        assertTrue(body.contains("\"mappedMessage\": \"Yes, that's correct.\""));
        assertTrue(body.contains("\"confidence\": 0.94"));
        assertTrue(body.contains("\"meetsThreshold\": true"));
        assertTrue(body.contains("\"shouldAutoSpeak\": true"));
    }

    @Test
    @DisplayName("Should return 200 OK with meetsThreshold false when confidence is below threshold")
    void testGestureBelowThreshold() throws Exception {
        String payload = "{\"gestureCode\":\"THUMBS_UP\",\"confidence\":0.55,\"sourceType\":\"WEBCAM\"}";
        setRequestBody(payload);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String body = responseWriter.toString();
        assertTrue(body.contains("\"gestureCode\": \"UNKNOWN\""));
        assertTrue(body.contains("\"meetsThreshold\": false"));
        assertTrue(body.contains("\"shouldAutoSpeak\": false"));
    }

    @Test
    @DisplayName("Should return 200 OK with shouldAutoSpeak false for UNKNOWN gesture type")
    void testUnknownGestureType() throws Exception {
        String payload = "{\"gestureCode\":\"UNKNOWN\",\"confidence\":0.92,\"sourceType\":\"WEBCAM\"}";
        setRequestBody(payload);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String body = responseWriter.toString();
        assertTrue(body.contains("\"gestureCode\": \"UNKNOWN\""));
        assertTrue(body.contains("\"shouldAutoSpeak\": false"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request for empty request body")
    void testEmptyRequestBody() throws Exception {
        setRequestBody("   ");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("EMPTY_REQUEST_BODY"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request for malformed JSON")
    void testMalformedJson() throws Exception {
        setRequestBody("{not_valid_json}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("MALFORMED_JSON"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when gestureCode is missing")
    void testMissingGestureCode() throws Exception {
        setRequestBody("{\"confidence\":0.95}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("MISSING_GESTURE_CODE"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request for unmapped gestureCode")
    void testUnmappedGestureCode() throws Exception {
        setRequestBody("{\"gestureCode\":\"DANCE_MOVE\",\"confidence\":0.90}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("UNMAPPED_GESTURE"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when confidence is missing")
    void testMissingConfidence() throws Exception {
        setRequestBody("{\"gestureCode\":\"THUMBS_UP\"}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("MISSING_CONFIDENCE"));
    }

    @Test
    @DisplayName("Should return 400 Bad Request when confidence is negative or out of bounds")
    void testInvalidConfidence() throws Exception {
        setRequestBody("{\"gestureCode\":\"THUMBS_UP\",\"confidence\":-0.5}");

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("INVALID_CONFIDENCE"));

        responseWriter.getBuffer().setLength(0);
        setRequestBody("{\"gestureCode\":\"THUMBS_UP\",\"confidence\":1.5}");

        servlet.doPost(request, response);
        verify(response, times(2)).setStatus(HttpServletResponse.SC_BAD_REQUEST);
        assertTrue(responseWriter.toString().contains("INVALID_CONFIDENCE"));
    }

    @Test
    @DisplayName("Should return 200 OK even if database insertion encounters SQL error")
    void testDatabaseFailureResilience() throws Exception {
        String payload = "{\"gestureCode\":\"OPEN_PALM\",\"confidence\":0.91,\"sourceType\":\"WEBCAM\"}";
        setRequestBody(payload);
        when(historyDAO.insert(any(GestureHistory.class))).thenThrow(new SQLException("DB connection refused"));

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String body = responseWriter.toString();
        assertTrue(body.contains("\"gestureCode\": \"OPEN_PALM\""));
        assertTrue(body.contains("\"mappedMessage\": \"Please stop or wait.\""));
    }
}

