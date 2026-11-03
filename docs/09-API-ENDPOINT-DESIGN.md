# GestureTalk AI — API Endpoint Design & Contracts

| Document Metadata | Value |
| :--- | :--- |
| **Document ID** | DOC-09 |
| **Project Name** | GestureTalk AI |
| **Document Version** | 1.0.0 |
| **Classification** | Interface Specification & Planned API Contracts |
| **Status** | Planned Specification (Pre-Implementation Baseline) |
| **Parent Document** | [00-PROJECT-OVERVIEW.md](00-PROJECT-OVERVIEW.md) |

---

## 1. Specification Notice & Architectural Status

> [!NOTE]
> **Planned Architectural Contracts**
> 
> The endpoints specified in this document represent the planned HTTP interface contracts to be implemented by Jakarta Servlets in subsequent development phases. They serve as the authoritative protocol definition between the client browser (JavaScript fetch calls) and the Apache Tomcat backend container.

---

## 2. API Endpoint Directory

```
┌────────┬──────────────────────┬────────────────────────────┬─────────────────────────────────────────────────┐
│ Method │ Endpoint URL         │ Target Servlet Class       │ Functional Responsibility                       │
├────────┼──────────────────────┼────────────────────────────┼─────────────────────────────────────────────────┤
│ GET    │ /home                │ HomeServlet                │ Dispatches to index.jsp view template           │
├────────┼──────────────────────┼────────────────────────────┼─────────────────────────────────────────────────┤
│ GET    │ /api/health          │ HealthServlet              │ Emits operational readiness & DB/AI status      │
├────────┼──────────────────────┼────────────────────────────┼─────────────────────────────────────────────────┤
│ POST   │ /api/recognize       │ GestureRecognitionServlet  │ Receives image multipart, performs AI analysis  │
├────────┼──────────────────────┼────────────────────────────┼─────────────────────────────────────────────────┤
│ GET    │ /api/history         │ HistoryServlet             │ Retrieves chronological audit logs (with filter)│
├────────┼──────────────────────┼────────────────────────────┼─────────────────────────────────────────────────┤
│ DELETE │ /api/history         │ HistoryServlet             │ Purges all recorded session recognition logs    │
└────────┴──────────────────────┴────────────────────────────┴─────────────────────────────────────────────────┘
```

---

## 3. Comprehensive Endpoint Specifications

### 3.1 `GET /home`
* **Purpose**: Serves the main single-page application interface.
* **HTTP Method**: `GET`
* **URL**: `/home` (also mapped to root `/`)
* **Request Headers**: `Accept: text/html`
* **Request Body**: None.
* **Response**:
  * **Status Code**: `200 OK`
  * **Content-Type**: `text/html; charset=UTF-8`
  * **Payload**: Rendered `index.jsp` HTML markup.

---

### 3.2 `GET /api/health`
* **Purpose**: Health-check endpoint used by the client UI and monitoring agents to verify server uptime, MySQL database connectivity, and presence of the `GEMINI_API_KEY`.
* **HTTP Method**: `GET`
* **URL**: `/api/health`
* **Request Headers**: `Accept: application/json`
* **Request Parameters**: None.
* **Expected Response (`200 OK`)**:
```json
{
  "status": "UP",
  "timestamp": "2026-11-03T14:30:00Z",
  "environment": {
    "javaVersion": "21",
    "serverInfo": "Apache Tomcat/10.1.x",
    "geminiConfigured": true,
    "databaseConnected": true
  }
}
```
* **Degraded Response (`503 Service Unavailable`)**:
```json
{
  "status": "DEGRADED",
  "timestamp": "2026-11-03T14:30:00Z",
  "environment": {
    "javaVersion": "21",
    "serverInfo": "Apache Tomcat/10.1.x",
    "geminiConfigured": false,
    "databaseConnected": false
  },
  "error": "GEMINI_API_KEY is missing and MySQL database connection failed."
}
```

---

### 3.3 `POST /api/recognize`
* **Purpose**: Ingests an in-memory image buffer, validates binary headers, invokes Gemini Vision via the GenAI SDK, deterministically maps the gesture to a spoken sentence, logs the transaction to MySQL, and returns structured JSON to the client.
* **HTTP Method**: `POST`
* **URL**: `/api/recognize`
* **Content-Type**: `multipart/form-data`
* **Multipart Form Fields**:
  1. `image` *(Required)*: Binary file part containing the image (JPEG, PNG, or WebP). Maximum size: 5,242,880 bytes (5 MB).
  2. `sourceType` *(Optional)*: String indicating origin (`WEBCAM` or `UPLOAD`). Default: `WEBCAM`.

#### Success Response (`200 OK`) — Known Gesture:
```json
{
  "success": true,
  "gestureCode": "THUMBS_UP",
  "displayName": "Thumbs Up",
  "spokenMessage": "Yes, that's correct.",
  "confidence": 0.94,
  "explanation": "Closed fist with thumb extended vertically upward.",
  "sourceType": "WEBCAM",
  "loggedId": 42,
  "timestamp": "2026-11-03T14:30:15Z"
}
```

#### Success Response (`200 OK`) — Ambiguous / Unknown Gesture:
```json
{
  "success": true,
  "gestureCode": "UNKNOWN",
  "displayName": "Unknown Gesture",
  "spokenMessage": "Gesture not recognized. Please try again.",
  "confidence": 0.38,
  "explanation": "Hand position is ambiguous or does not conform to the 5 recognized signs.",
  "sourceType": "WEBCAM",
  "loggedId": 43,
  "timestamp": "2026-11-03T14:30:25Z"
}
```

#### Error Responses:
* **`400 Bad Request`** (Missing or empty file part):
```json
{
  "success": false,
  "errorCode": "EMPTY_PAYLOAD",
  "message": "No image payload found in the multipart request."
}
```
* **`413 Payload Too Large`** (Exceeds 5 MB):
```json
{
  "success": false,
  "errorCode": "FILE_TOO_LARGE",
  "message": "Uploaded image size (6291456 bytes) exceeds the 5MB maximum limit."
}
```
* **`415 Unsupported Media Type`** (Invalid magic bytes):
```json
{
  "success": false,
  "errorCode": "INVALID_IMAGE_FORMAT",
  "message": "Uploaded file binary header does not match valid JPEG, PNG, or WebP signatures."
}
```
* **`503 Service Unavailable`** (Gemini API quota or network failure):
```json
{
  "success": false,
  "errorCode": "AI_SERVICE_UNAVAILABLE",
  "message": "The AI vision service is currently experiencing high load. Please retry in 5 seconds."
}
```

---

### 3.4 `GET /api/history`
* **Purpose**: Retrieves recent recognition logs for display in the audit history table.
* **HTTP Method**: `GET`
* **URL**: `/api/history`
* **Query Parameters**:
  * `gesture` *(Optional)*: Filter records by gesture code (e.g., `?gesture=THUMBS_UP` or `?gesture=UNKNOWN`).
  * `limit` *(Optional)*: Number of records to return (default: `50`, max: `200`).
* **Expected Response (`200 OK`)**:
```json
{
  "success": true,
  "count": 2,
  "records": [
    {
      "id": 43,
      "gestureCode": "UNKNOWN",
      "mappedMessage": "Gesture not recognized. Please try again.",
      "explanation": "Hand position is ambiguous.",
      "confidence": 0.38,
      "sourceType": "WEBCAM",
      "createdAt": "2026-11-03 14:30:25"
    },
    {
      "id": 42,
      "gestureCode": "THUMBS_UP",
      "mappedMessage": "Yes, that's correct.",
      "explanation": "Closed fist with thumb extended vertically upward.",
      "confidence": 0.94,
      "sourceType": "WEBCAM",
      "createdAt": "2026-11-03 14:30:15"
    }
  ]
}
```

---

### 3.5 `DELETE /api/history`
* **Purpose**: Purges stored recognition history records to reset the session table.
* **HTTP Method**: `DELETE`
* **URL**: `/api/history`
* **Expected Response (`200 OK`)**:
```json
{
  "success": true,
  "deletedCount": 43,
  "message": "All gesture recognition history records have been successfully purged."
}
```
* **Error Response (`500 Internal Server Error`)**:
```json
{
  "success": false,
  "errorCode": "DATABASE_ERROR",
  "message": "Failed to clear history due to a database access failure."
}
```

---

## 4. Standardized JSON Error Contract

All error responses across all `/api/*` endpoints conform to a rigid, predictable JSON contract:

```json
{
  "success": false,
  "errorCode": "STRING_ERROR_IDENTIFIER",
  "message": "Human-readable descriptive explanation of the error.",
  "timestamp": "ISO-8601-UTC-TIMESTAMP"
}
```

---

## 5. Document Cross-References

* Workflow interactions: [05-APPLICATION-WORKFLOW.md](05-APPLICATION-WORKFLOW.md)
* Error definitions: [13-ERROR-HANDLING-AND-VALIDATION.md](13-ERROR-HANDLING-AND-VALIDATION.md)
* Database mapping: [10-DATABASE-DESIGN.md](10-DATABASE-DESIGN.md)

