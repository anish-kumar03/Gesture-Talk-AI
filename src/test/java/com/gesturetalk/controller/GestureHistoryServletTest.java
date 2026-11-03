package com.gesturetalk.controller;

import com.gesturetalk.dao.HistoryDAO;
import com.gesturetalk.model.GestureHistory;
import com.gesturetalk.util.JsonUtils;
import com.google.gson.JsonObject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.sql.SQLException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("GestureHistoryServlet Unit Tests")
class GestureHistoryServletTest {

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private HistoryDAO historyDAO;

    private GestureHistoryServlet servlet;
    private StringWriter responseWriter;

    @BeforeEach
    void setUp() throws Exception {
        servlet = new GestureHistoryServlet();
        servlet.setHistoryDAO(historyDAO);
        responseWriter = new StringWriter();
        when(response.getWriter()).thenReturn(new PrintWriter(responseWriter));
    }

    @Test
    @DisplayName("doGet should return 200 OK with records JSON")
    void testDoGetSuccess() throws Exception {
        List<GestureHistory> mockRecords = List.of(
                new GestureHistory(1L, "THUMBS_UP", "Yes, that's correct.", "Thumbs up visible", 0.95, "WEBCAM", null),
                new GestureHistory(2L, "V_SIGN", "Peace and greetings.", "V sign visible", 0.98, "UPLOAD", null)
        );

        when(request.getParameter("gesture")).thenReturn(null);
        when(request.getParameter("limit")).thenReturn("20");
        when(historyDAO.findByGesture(null, 20)).thenReturn(mockRecords);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        String body = responseWriter.toString();
        JsonObject json = JsonUtils.fromJson(body, JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals(2, json.get("count").getAsInt());
        assertTrue(body.contains("THUMBS_UP"));
        assertTrue(body.contains("V_SIGN"));
    }

    @Test
    @DisplayName("doGet should pass gesture filter to DAO")
    void testDoGetWithFilter() throws Exception {
        List<GestureHistory> mockRecords = List.of(
                new GestureHistory(1L, "INDEX_POINTING", "Please look there.", "Finger pointing", 0.89, "WEBCAM", null)
        );

        when(request.getParameter("gesture")).thenReturn("INDEX_POINTING");
        when(request.getParameter("limit")).thenReturn("20");
        when(historyDAO.findByGesture("INDEX_POINTING", 20)).thenReturn(mockRecords);

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(historyDAO).findByGesture("INDEX_POINTING", 20);
        String body = responseWriter.toString();
        assertTrue(body.contains("INDEX_POINTING"));
    }

    @Test
    @DisplayName("doGet should return 500 when DAO throws SQLException")
    void testDoGetDatabaseError() throws Exception {
        when(request.getParameter("gesture")).thenReturn(null);
        when(request.getParameter("limit")).thenReturn(null);
        when(historyDAO.findByGesture(null, 20)).thenThrow(new SQLException("Connection refused"));

        servlet.doGet(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String body = responseWriter.toString();
        assertTrue(body.contains("DATABASE_ERROR"));
        assertTrue(body.contains("Connection refused"));
    }

    @Test
    @DisplayName("doDelete should clear records and return 200 OK with deletedCount")
    void testDoDeleteSuccess() throws Exception {
        when(historyDAO.deleteAll()).thenReturn(14);

        servlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(historyDAO).deleteAll();
        JsonObject json = JsonUtils.fromJson(responseWriter.toString(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals(14, json.get("deletedCount").getAsInt());
    }

    @Test
    @DisplayName("doDelete should return 500 when database deletion fails")
    void testDoDeleteDatabaseError() throws Exception {
        when(historyDAO.deleteAll()).thenThrow(new SQLException("Table locked"));

        servlet.doDelete(request, response);

        verify(response).setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        String body = responseWriter.toString();
        assertTrue(body.contains("DATABASE_ERROR"));
        assertTrue(body.contains("Table locked"));
    }

    @Test
    @DisplayName("doPost with _method=DELETE should route to doDelete")
    void testDoPostMethodOverride() throws Exception {
        when(request.getParameter("_method")).thenReturn("DELETE");
        when(historyDAO.deleteAll()).thenReturn(5);

        servlet.doPost(request, response);

        verify(response).setStatus(HttpServletResponse.SC_OK);
        verify(historyDAO).deleteAll();
        JsonObject json = JsonUtils.fromJson(responseWriter.toString(), JsonObject.class);
        assertTrue(json.get("success").getAsBoolean());
        assertEquals(5, json.get("deletedCount").getAsInt());
    }
}
