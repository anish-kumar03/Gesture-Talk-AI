package com.gesturetalk.controller;

import com.gesturetalk.dao.GestureHistoryDAO;
import com.gesturetalk.dao.HistoryDAO;
import com.gesturetalk.model.GestureHistory;
import com.gesturetalk.util.JsonUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller Servlet for querying and clearing gesture recognition audit records.
 * Mapped to GET /api/history and DELETE /api/history.
 */
@WebServlet(name = "GestureHistoryServlet", urlPatterns = {"/api/history"})
public class GestureHistoryServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(GestureHistoryServlet.class);

    private HistoryDAO historyDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            this.historyDAO = new GestureHistoryDAO();
            logger.info("GestureHistoryServlet initialized successfully.");
        } catch (Exception ex) {
            logger.error("GestureHistoryDAO deferred initialization: {}", ex.getMessage());
        }
    }

    /**
     * Package-private setter for unit/mock testing.
     */
    void setHistoryDAO(HistoryDAO historyDAO) {
        this.historyDAO = historyDAO;
    }

    private synchronized HistoryDAO getHistoryDAO() {
        if (this.historyDAO == null) {
            this.historyDAO = new GestureHistoryDAO();
        }
        return this.historyDAO;
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            String gestureFilter = request.getParameter("gesture");
            String limitParam = request.getParameter("limit");
            int limit = 20;

            if (limitParam != null && !limitParam.isBlank()) {
                try {
                    limit = Integer.parseInt(limitParam.trim());
                } catch (NumberFormatException ignored) {
                    limit = 20;
                }
            }

            List<GestureHistory> records = getHistoryDAO().findByGesture(gestureFilter, limit);

            Map<String, Object> payload = new HashMap<>();
            payload.put("success", true);
            payload.put("count", records.size());
            payload.put("records", records);

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(JsonUtils.toJson(payload));

        } catch (SQLException ex) {
            logger.error("Database error in GestureHistoryServlet.doGet: {}", ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(JsonUtils.errorJson("DATABASE_ERROR", 
                    "Failed to retrieve gesture recognition history from database: " + ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Unexpected error in GestureHistoryServlet.doGet: {}", ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(JsonUtils.errorJson("INTERNAL_SERVER_ERROR", 
                    "An unexpected error occurred: " + ex.getMessage()));
        }
    }

    @Override
    protected void doDelete(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            int deletedCount = getHistoryDAO().deleteAll();

            Map<String, Object> payload = new HashMap<>();
            payload.put("success", true);
            payload.put("deletedCount", deletedCount);
            payload.put("message", "All gesture recognition history records have been successfully cleared.");

            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(JsonUtils.toJson(payload));

        } catch (SQLException ex) {
            logger.error("Database error in GestureHistoryServlet.doDelete: {}", ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(JsonUtils.errorJson("DATABASE_ERROR", 
                    "Failed to clear gesture history: " + ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Unexpected error in GestureHistoryServlet.doDelete: {}", ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(JsonUtils.errorJson("INTERNAL_SERVER_ERROR", 
                    "An unexpected error occurred: " + ex.getMessage()));
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        String methodOverride = request.getParameter("_method");
        String action = request.getParameter("action");

        if ("DELETE".equalsIgnoreCase(methodOverride) || "clear".equalsIgnoreCase(action)) {
            doDelete(request, response);
        } else {
            // Default to doGet if POST without action
            doGet(request, response);
        }
    }
}
