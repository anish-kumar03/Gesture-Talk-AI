package com.gesturetalk.controller;

import com.gesturetalk.dao.GestureHistoryDAO;
import com.gesturetalk.dao.HistoryDAO;
import com.gesturetalk.exception.GestureRecognitionException;
import com.gesturetalk.model.GestureAnalysis;
import com.gesturetalk.model.GestureHistory;
import com.gesturetalk.service.GestureRecognitionService;
import com.gesturetalk.util.ImageValidator;
import com.gesturetalk.util.JsonUtils;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.MultipartConfig;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Controller Servlet handling multipart image uploads and webcam frames for gesture recognition.
 * Mapped to POST /api/recognize.
 */
@WebServlet(name = "GestureRecognitionServlet", urlPatterns = {"/api/recognize"})
@MultipartConfig(
        fileSizeThreshold = 1024 * 1024,      // 1 MB buffer before temp disk spillover
        maxFileSize = 5 * 1024 * 1024,         // 5 MB max image file size
        maxRequestSize = 6 * 1024 * 1024       // 6 MB max overall multipart request size
)
public class GestureRecognitionServlet extends HttpServlet {

    private static final Logger logger = LoggerFactory.getLogger(GestureRecognitionServlet.class);

    private GestureRecognitionService recognitionService;
    private HistoryDAO historyDAO;

    @Override
    public void init() throws ServletException {
        super.init();
        try {
            this.recognitionService = new GestureRecognitionService();
            logger.info("GestureRecognitionServlet initialized successfully.");
        } catch (Exception ex) {
            logger.error("Warning: GestureRecognitionService initialization deferred: {}", ex.getMessage());
            // Allow lazy initialization on first request if environment variable was set after startup
        }
        try {
            this.historyDAO = new GestureHistoryDAO();
        } catch (Exception ex) {
            logger.warn("GestureHistoryDAO initialization deferred: {}", ex.getMessage());
        }
    }

    /**
     * Package-private setter for unit/mock testing.
     */
    void setRecognitionService(GestureRecognitionService service) {
        this.recognitionService = service;
    }

    /**
     * Package-private setter for DAO injection during testing.
     */
    void setHistoryDAO(HistoryDAO historyDAO) {
        this.historyDAO = historyDAO;
    }

    private synchronized GestureRecognitionService getRecognitionService() {
        if (this.recognitionService == null) {
            this.recognitionService = new GestureRecognitionService();
        }
        return this.recognitionService;
    }

    private synchronized HistoryDAO getHistoryDAO() {
        if (this.historyDAO == null) {
            this.historyDAO = new GestureHistoryDAO();
        }
        return this.historyDAO;
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) 
            throws ServletException, IOException {
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");

        try {
            // 1. Extract and read multipart image part
            Part imagePart = request.getPart("image");
            if (imagePart == null) {
                response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                response.getWriter().write(JsonUtils.errorJson("MISSING_IMAGE_PART", 
                    "Multipart request does not contain the mandatory 'image' part."));
                return;
            }

            byte[] imageBytes = readPartBytes(imagePart);

            // 2. Extract source type (WEBCAM or UPLOAD)
            String sourceType = request.getParameter("sourceType");
            if (sourceType == null || sourceType.isBlank()) {
                sourceType = "WEBCAM";
            }

            // 3. Validate image size and binary header signatures
            String mimeType = ImageValidator.validate(imageBytes);

            // 4. Execute AI Gesture Recognition
            GestureAnalysis analysis = getRecognitionService().recognize(imageBytes, mimeType, sourceType);

            // 5. Persist recognition audit record to MySQL via JDBC
            try {
                GestureHistory history = GestureHistory.fromAnalysis(analysis);
                long loggedId = getHistoryDAO().insert(history);
                logger.debug("Successfully logged gesture recognition to database with id: {}", loggedId);
            } catch (Exception dbEx) {
                logger.warn("Could not log gesture to database (database may be offline): {}", dbEx.getMessage());
            }

            // 6. Return structured JSON response
            response.setStatus(HttpServletResponse.SC_OK);
            response.getWriter().write(JsonUtils.toJson(analysis));

        } catch (GestureRecognitionException ex) {
            logger.warn("Gesture recognition domain exception: [{}] {}", ex.getErrorCode(), ex.getMessage());
            response.setStatus(ex.getHttpStatus());
            response.getWriter().write(JsonUtils.errorJson(ex.getErrorCode(), ex.getMessage()));
        } catch (Exception ex) {
            logger.error("Unexpected error in GestureRecognitionServlet: {}", ex.getMessage(), ex);
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            response.getWriter().write(JsonUtils.errorJson("INTERNAL_SERVER_ERROR", 
                "An unexpected server error occurred: " + ex.getMessage()));
        }
    }

    /**
     * Reads the multipart input stream entirely into volatile memory without disk writes.
     */
    private static byte[] readPartBytes(Part part) throws IOException {
        try (InputStream is = part.getInputStream();
             ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            return baos.toByteArray();
        }
    }
}

