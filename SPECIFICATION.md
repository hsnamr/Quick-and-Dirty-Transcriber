# Free and Dirty Transcriber - REST API Specification

## Document Information
- **Service Name**: Free and Dirty Transcriber
- **Target Implementation**: Java with Spring Boot
- **API Version**: v1 (Header-based versioning)
- **Document Version**: 1.0
- **Date**: January 27, 2026

---

## Table of Contents
1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Data Models](#data-models)
4. [API Endpoints](#api-endpoints)
5. [Authentication & Authorization](#authentication--authorization)
6. [Business Logic](#business-logic)
7. [External Integrations](#external-integrations)
8. [Error Handling](#error-handling)
9. [Database Schema](#database-schema)
10. [Configuration](#configuration)
11. [Non-Functional Requirements](#non-functional-requirements)

---

## 1. Overview

### 1.1 Purpose
The Quick and Dirty Transcriber provides a REST API for converting audio files to text transcriptions using free and open-source libraries. It handles the complete lifecycle of transcription requests including file upload, processing status tracking, rate limiting for API protection, and result delivery.

### 1.2 Key Features
- Audio file upload and validation
- Audio metadata extraction (duration, file size, format)
- Multi-language support with auto-detection
- Speaker count specification (1-5 speakers)
- Transcription using Vosk (free, open-source speech recognition library)
- Optional integration with external transcription service
- Transcription request management (CRUD operations)
- Rate limiting for API protection
- Status tracking (processing, completed, failed)
- Email notifications for status changes
- Real-time status updates via Kafka message queue
- Comprehensive filtering, sorting, and pagination
- Transcription results storage (text, JSON, metadata)

### 1.3 Service Boundaries
This service is designed to be **fully independent** and can operate standalone. It should:
- Manage its own database schema (MongoDB)
- Handle its own authentication/authorization (or delegate to an auth service)
- Perform transcription using Vosk library (or integrate with external transcription service)
- Integrate with Kafka message queue for callbacks and broadcasts
- Integrate with email service for notifications

---

## 2. Architecture

### 2.1 High-Level Architecture
```
┌─────────────┐
│   Client    │
└──────┬──────┘
       │ HTTP/REST
       │
┌──────▼─────────────────────────────────────┐
│   Free and Dirty Transcriber (Spring Boot) │
│  ┌──────────────────────────────────────┐ │
│  │  REST API Controllers                │ │
│  └──────────────────────────────────────┘ │
│  ┌──────────────────────────────────────┐ │
│  │  Business Logic Layer                │ │
│  │  - Rate Limiting                    │ │
│  │  - Metadata Extraction              │ │
│  │  - Request Validation               │ │
│  │  - Vosk Transcription               │ │
│  └──────────────────────────────────────┘ │
│  ┌──────────────────────────────────────┐ │
│  │  Data Access Layer                   │ │
│  │  - MongoDB Repositories              │ │
│  └──────────────────────────────────────┘ │
└──────┬───────────────────┬────────────────┘
       │                   │
       │                   │
┌──────▼──────┐    ┌──────▼──────────────┐
│  Database   │    │  Message Queue      │
│  (MongoDB)  │    │  (Kafka)            │
└─────────────┘    └─────────────────────┘
       │
┌──────▼─────────────────────────────────────┐
│  External Services                         │
│  ┌──────────────────────────────────────┐ │
│  │  Transcription Service (Python)      │ │
│  │  (Optional - can use Vosk instead)   │ │
│  └──────────────────────────────────────┘ │
│  ┌──────────────────────────────────────┐ │
│  │  Email Service                       │ │
│  └──────────────────────────────────────┘ │
│  ┌──────────────────────────────────────┐ │
│  │  Quota/Subscription Service         │ │
│  └──────────────────────────────────────┘ │
└────────────────────────────────────────────┘
```

### 2.2 Component Responsibilities

#### REST API Layer
- Handle HTTP requests/responses
- Request validation
- Authentication/authorization checks
- Response serialization
- Header-based API versioning

#### Business Logic Layer
- Transcription request lifecycle management
- Rate limiting enforcement
- Audio metadata extraction
- Vosk transcription processing
- Status management
- Email notification triggers

#### Data Access Layer
- MongoDB operations
- Query optimization
- Document management

#### Integration Layer
- Vosk transcription library integration
- Optional external transcription service communication
- Kafka message queue integration
- Email service integration

---

## 3. Data Models

### 3.1 TranscriptionRequest

**Collection Name**: `transcription_requests`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | String (ObjectId) | PRIMARY KEY | MongoDB unique identifier |
| numericId | Long | UNIQUE, INDEXED | Numeric ID for API compatibility |
| fileName | String | NOT NULL, INDEXED | Original filename |
| language | DBRef | NOT NULL, INDEXED | Reference to TranscriptionLanguage |
| speakersCount | Integer | NOT NULL | Number of speakers (1-5) |
| durationSecs | BigDecimal | NOT NULL, > 0, INDEXED | Audio duration in seconds |
| status | Enum | NOT NULL, DEFAULT 'PROCESSING', INDEXED | Status: PROCESSING, COMPLETED, FAILED |
| category | Enum | NOT NULL, DEFAULT 'OTHER', INDEXED | Category (see below) |
| userId | Long | NOT NULL, INDEXED | User who created the request |
| consumerId | Long | NOT NULL, INDEXED | Consumer owning the request |
| transcriptionText | String | NULLABLE | Transcribed text result |
| transcriptionJson | Object | NULLABLE | Full transcription JSON result |
| transcriptionMetadata | Object | NULLABLE | Transcription metadata |
| createdAt | LocalDateTime | NOT NULL | Creation timestamp |
| updatedAt | LocalDateTime | NOT NULL | Last update timestamp |

**Status Enum Values**:
- `PROCESSING` (0): Transcription is being processed
- `COMPLETED` (1): Transcription completed successfully
- `FAILED` (2): Transcription failed

**Category Enum Values**:
- `MEETING` (0)
- `INTERVIEW` (1)
- `CUSTOMER_SUPPORT_CALL` (2)
- `SALES_CALL` (3)
- `TRAINING_SESSION` (4)
- `PODCAST` (5)
- `PRESENTATION` (6)
- `VOICE_NOTE` (7)
- `OTHER` (8) - Default

**Indexes**:
- Primary key on `id` (MongoDB ObjectId)
- Unique index on `numericId`
- Index on `fileName`
- Index on `language`
- Index on `durationSecs`
- Index on `status`
- Index on `category`
- Index on `userId`
- Index on `consumerId`

**Note**: `userId` and `consumerId` are stored as Long values. If `users` and `consumers` collections are in a separate service, maintain referential integrity through application logic.

### 3.2 TranscriptionLanguage

**Collection Name**: `transcription_languages`

| Field | Type | Constraints | Description |
|-------|------|-------------|-------------|
| id | String (ObjectId) | PRIMARY KEY | MongoDB unique identifier |
| name | String | NOT NULL, UNIQUE, INDEXED | Language name (e.g., "English", "Arabic") |
| code | String | NOT NULL, UNIQUE, INDEXED | Language code (e.g., "en", "ar", "auto") |
| description | String | NULLABLE | Optional description |
| active | Boolean | NOT NULL, DEFAULT true, INDEXED | Whether language is active |
| popular | Boolean | NOT NULL, DEFAULT false, INDEXED | Whether language is popular |
| createdAt | LocalDateTime | NOT NULL | Creation timestamp |
| updatedAt | LocalDateTime | NOT NULL | Last update timestamp |

**Indexes**:
- Primary key on `id` (MongoDB ObjectId)
- Unique index on `name`
- Unique index on `code`
- Index on `active`
- Index on `popular`

**Special Language**:
- Must have a language with `code = 'auto'` for auto-detection

### 3.3 DTOs (Data Transfer Objects)

#### TranscriptionRequestDTO (Response)
```json
{
  "id": "string",
  "type": "transcription_request",
  "attributes": {
    "id": 123,
    "file_name": "meeting_recording.mp3",
    "speakers_count": 2,
    "duration_secs": 3600.50,
    "duration_formatted": "60:00",
    "status": "processing",
    "status_display": "Processing",
    "category": "meeting",
    "category_name": "Meeting",
    "category_key": "meeting",
    "language_name": "English",
    "language_code": "en",
    "language_display_name": "English (en)",
    "auto_detect_language": false,
    "user_name": "John Doe",
    "user_email": "john@example.com",
    "consumer_id": 456,
    "is_processing": true,
    "is_completed": false,
    "is_failed": false,
    "can_be_deleted": false,
    "can_be_viewed": false,
    "can_be_updated": true,
    "file_extension": ".mp3",
    "created_at": "2026-01-27T10:00:00Z",
    "created_at_formatted": "2026-01-27 10:00:00",
    "created_at_unix": 1706352000,
    "updated_at": "2026-01-27T10:05:00Z",
    "updated_at_formatted": "2026-01-27 10:05:00",
    "updated_at_unix": 1706352300,
    "processing_time_seconds": 300.00,
    "transcription_text": "Full transcribed text...",
    "transcription_json": { /* Vosk JSON result */ },
    "transcription_metadata": { /* Metadata */ }
  },
  "relationships": {
    "language": {
      "data": {
        "id": "1",
        "type": "transcription_language"
      }
    },
    "user": {
      "data": {
        "id": "789",
        "type": "user"
      }
    }
  }
}
```

#### TranscriptionRequestListDTO (Response)
```json
{
  "data": [
    // Array of TranscriptionRequestDTO objects
  ],
  "overview": {
    "total": 100,
    "completed": 75,
    "processing": 20,
    "failed": 5,
  },
  "pagination": {
    "page": 1,
    "per_page": 10,
    "total": 100,
    "total_pages": 10
  },
  "filters": {
    "languages": [
      {
        "id": 1,
        "name": "English",
        "code": "en"
      }
    ],
    "categories": [
      {
        "key": "meeting",
        "name": "Meeting"
      }
    ],
    "statuses": ["processing", "completed", "failed"]
  },
  "sorting_options": [
    {
      "key": "id",
      "display_name": "ID",
      "sort_by": ["id"]
    },
    {
      "key": "file_name",
      "display_name": "File Name",
      "sort_by": ["LOWER(file_name)"]
    }
    // ... more options
  ]
}
```

#### CreateTranscriptionRequestDTO (Request)
```json
{
  "audio_file": "<multipart file>",
  "speakers_count": 2,
  "language": "en",  // Optional, defaults to "auto"
  "category": "meeting"
}
```

#### UpdateTranscriptionRequestDTO (Request)
```json
{
  "category": "interview"
}
```

---

## 4. API Endpoints

### 4.1 Base URL
```
/api/free-and-dirty-transcriber
```

### 4.2 API Versioning
API versioning is handled via the `API-Version` header:
- **Header**: `API-Version: v1`
- If no header is provided, `v1` is used as default
- Currently only `v1` is supported

### 4.3 Authentication
All endpoints require authentication via JWT token in the `Authorization` header:
```
Authorization: Bearer <jwt_token>
```

Alternatively, if using custom header (as in original):
```
auth-token: <jwt_token>
```

### 4.4 Endpoints

#### 4.4.1 Create Transcription Request

**POST** `/api/free-and-dirty-transcriber`

**Description**: Upload an audio file and create a transcription request. Transcription is performed using Vosk library (or optionally forwarded to external service).

**Content-Type**: `multipart/form-data`

**Request Parameters**:
| Parameter | Type | Required | Constraints | Description |
|-----------|------|----------|-------------|-------------|
| audio_file | File | Yes | Max size: 500MB | Audio file to transcribe |
| speakers_count | Integer | Yes | 1-5 | Number of speakers |
| language | String | No | Valid language code | Language code (defaults to "auto") |
| category | String | Yes | Valid category | Category enum value |

**Request Validation**:
1. `audio_file` must be present and valid audio file
2. `speakers_count` must be between 1 and 5
3. `category` must be a valid category enum value
4. Audio file must have valid duration (1 second to 3600 seconds / 1 hour)
5. API rate limits must not be exceeded
6. Audio metadata must be extractable

**Business Logic Flow**:
1. Authenticate and authorize user
2. Extract audio metadata (duration, file name, size, format)
3. Check rate limiting (enforced by filter)
4. Find or default to language
5. Create transcription request record with status `PROCESSING`
6. Process transcription:
   - **If using Vosk**: Process audio file directly using Vosk library
   - **If using external service**: Forward audio file to external transcription service
8. If transcription succeeds:
   - Update request with transcription results (text, JSON, metadata)
   - Update status to `COMPLETED`
   - Send completion email notification
   - Return success response
9. If transcription fails:
   - Update status to `FAILED`
   - Send failure email
   - Return error response

**Response Codes**:
- `201 Created`: Request created successfully
- `400 Bad Request`: Invalid parameters
- `401 Unauthorized`: Authentication failed
- `403 Forbidden`: Insufficient permissions
- `422 Unprocessable Entity`: Validation failed
- `429 Too Many Requests`: Rate limit exceeded
- `500 Internal Server Error`: Server error
- `503 Service Unavailable`: Transcription service unavailable (if using external service)

**Success Response** (201):
```json
{
  "id": "123",
  "type": "transcription_request",
  "attributes": {
    // TranscriptionRequestDTO attributes
  }
}
```

**Error Response** (422):
```json
{
  "error": "Invalid audio file or parameters",
  "details": [
    "File format not supported",
    "Duration too short"
  ]
}
```

**Error Response** (429):
```json
{
  "error": "Rate limit exceeded. Please try again later.",
  "message": "Too many requests. Please wait before making another request."
}
```

#### 4.4.2 List Transcription Requests

**GET** `/api/free-and-dirty-transcriber`

**Description**: Retrieve a paginated list of transcription requests with filtering and sorting.

**Query Parameters**:
| Parameter | Type | Required | Constraints | Description |
|-----------|------|----------|-------------|-------------|
| limit_per_page | Integer | No | 1-100, default: 10 | Items per page |
| page | Integer | No | >= 1, default: 1 | Page number |
| search | String | No | - | Search by file name or request ID |
| status | String | No | processing, completed, failed | Filter by status |
| category | String | No | Valid category | Filter by category |
| language_id | Integer | No | - | Filter by language ID |
| start_date | Integer | No | Unix timestamp | Filter by start date |
| end_date | Integer | No | Unix timestamp | Filter by end date |
| sort_by | String | No | Valid sort option | Sort field |
| order_by | String | No | asc, desc | Sort direction |

**Sort Options**:
- `id`: Sort by ID
- `file_name`: Sort by file name (case-insensitive)
- `created_at`: Sort by creation date
- `category`: Sort by category
- `duration_secs`: Sort by duration
- `status`: Sort by status

**Default Sorting**: Most recent first (`created_at DESC`)

**Response Codes**:
- `200 OK`: Success
- `401 Unauthorized`: Authentication failed
- `403 Forbidden`: Insufficient permissions

**Success Response** (200):
```json
{
  "data": [
    // Array of TranscriptionRequestDTO
  ],
  "overview": {
    "total": 100,
    "completed": 75,
    "processing": 20,
    "failed": 5,
  },
  "pagination": {
    "page": 1,
    "per_page": 10,
    "total": 100,
    "total_pages": 10
  },
  "filters": {
    "languages": [
      {
        "id": 1,
        "name": "English",
        "code": "en"
      }
    ],
    "categories": [
      {
        "key": "meeting",
        "name": "Meeting"
      }
    ],
    "statuses": ["processing", "completed", "failed"]
  },
  "sorting_options": [
    {
      "key": "id",
      "display_name": "ID",
      "sort_by": ["id"]
    }
    // ... more options
  ]
}
```

#### 4.4.3 Get Transcription Request Details

**GET** `/api/free-and-dirty-transcriber/{id}`

**Description**: Retrieve details of a specific transcription request. Only completed transcriptions can be viewed.

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | Transcription request numeric ID |

**Business Logic**:
1. Authenticate and authorize user
2. Find transcription request by numeric ID and consumer_id
3. Verify user has access to this company's requests
4. Check if status is `COMPLETED` (only completed can be viewed)
5. Send broadcast message to Kafka for real-time updates (if applicable)
6. Return transcription details

**Response Codes**:
- `200 OK`: Success
- `401 Unauthorized`: Authentication failed
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Transcription request not found
- `422 Unprocessable Entity`: Transcription not completed yet

**Success Response** (200):
```json
{
  "id": "123",
  "type": "transcription_request",
  "attributes": {
    // TranscriptionRequestDTO with transcription results
  }
}
```

**Error Response** (422):
```json
{
  "error": "Transcription is not completed yet. Only completed transcriptions can be viewed."
}
```

#### 4.4.4 Update Transcription Request Category

**PUT** `/api/free-and-dirty-transcriber/{id}`

**Description**: Update the category of a transcription request.

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | Transcription request numeric ID |

**Request Body**:
```json
{
  "category": "interview"
}
```

**Request Validation**:
- `category` must be a valid category enum value

**Response Codes**:
- `200 OK`: Success
- `400 Bad Request`: Invalid parameters
- `401 Unauthorized`: Authentication failed
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Transcription request not found
- `422 Unprocessable Entity`: Validation failed

**Success Response** (200):
```json
{
  "id": "123",
  "type": "transcription_request",
  "attributes": {
    // Updated TranscriptionRequestDTO
  }
}
```

#### 4.4.5 Delete Transcription Request

**DELETE** `/api/free-and-dirty-transcriber/{id}`

**Description**: Delete a transcription request. Only completed or failed requests can be deleted.

**Path Parameters**:
| Parameter | Type | Required | Description |
|-----------|------|----------|-------------|
| id | Long | Yes | Transcription request numeric ID |

**Business Logic**:
1. Authenticate and authorize user
2. Find transcription request by numeric ID and consumer_id
3. Verify status is `COMPLETED` or `FAILED` (cannot delete while processing)
4. Delete the transcription record
5. Return success response

**Response Codes**:
- `200 OK`: Success
- `401 Unauthorized`: Authentication failed
- `403 Forbidden`: Insufficient permissions
- `404 Not Found`: Transcription request not found
- `422 Unprocessable Entity`: Cannot delete while processing

**Success Response** (200):
```json
{
  "message": "Audio to text transcription deleted successfully"
}
```

**Error Response** (422):
```json
{
  "error": "Transcription cannot be deleted while processing. Only completed or failed transcriptions can be deleted."
}
```

---

## 5. Authentication & Authorization

### 5.1 Authentication
- **Method**: JWT (JSON Web Token)
- **Header**: `Authorization: Bearer <token>` or `auth-token: <token>`
- **Token Validation**:
  - Verify token signature
  - Check token expiration
  - Extract user information (user_id, consumer_id, permissions)
  - Validate user exists and is active

### 5.2 Authorization
The service must check user permissions for each operation:

**Permission Model**:
- Product: `FDTranscriber`
- Feature: `FD_TRANSCRIPTION`

**Required Permissions**:
- `VIEW_FD_TRANSCRIPTION`: Required for `index` and `show` operations
- `SUBMIT_FD_TRANSCRIPTION`: Required for `create` and `update` operations
- `DELETE_FD_TRANSCRIPTION`: Required for `destroy` operation

**Authorization Checks**:
1. User must belong to the company that owns the transcription request
2. User must have the required permission for the operation
3. Company must have active subscription for FDTranscriber product

### 5.3 User Context
After authentication, the service should have access to:
- `user_id`: Current user ID
- `consumer_id`: User's consumer ID
- `permissions`: User's permissions array
- `product_feature_permissions`: Product-specific permissions

---

## 6. Business Logic

### 6.1 Audio Metadata Extraction

**Purpose**: Extract metadata from uploaded audio files.

**Requirements**:
- Extract duration in seconds (decimal precision)
- Extract file name
- Extract file size
- Extract content type/MIME type
- Validate audio file format

**Implementation Notes**:
- Uses JAudioTagger and Apache Tika libraries
- Handle various audio formats (MP3, WAV, M4A, FLAC, OGG, etc.)
- Duration must be extracted accurately
- If duration cannot be extracted, the request should fail with appropriate error

**Error Handling**:
- If metadata extraction fails, return 422 with error message
- Log the failure for debugging

### 6.2 Transcription Processing

**Purpose**: Transcribe audio files to text.

**Implementation Options**:

#### Option 1: Vosk (Primary - Implemented)
- Uses Vosk free/open-source speech recognition library
- Supports multiple languages via language models
- Processes audio files directly in the service
- Converts audio to WAV 16kHz mono PCM format for Vosk
- Stores transcription results (text, JSON, metadata) in database

#### Option 2: External Service (Optional)
- Forward audio files to external Python transcription service
- Receive status updates via Kafka
- Store transcription results when received

**Transcription Results Storage**:
- `transcription_text`: Plain text transcription
- `transcription_json`: Full Vosk JSON result with word-level timestamps
- `transcription_metadata`: Additional metadata about the transcription

### 6.3 Rate Limiting

**Purpose**: Protect APIs from overuse by limiting the number of requests per time window.

**Implementation**:
- Uses Bucket4j library (same library used by Spring Cloud Gateway)
- Token bucket algorithm with configurable refill rate and burst capacity
- Applied via filter early in the request processing chain (before authentication)

**Configuration**:
- `rate.limit.requests-per-minute`: Number of requests allowed per minute (default: 10)
- `rate.limit.burst-capacity`: Maximum burst capacity (default: 20)

**Behavior**:
- Allows up to `burst-capacity` requests immediately
- Refills at `requests-per-minute` rate
- Health check endpoints (`/actuator/health/**`) are excluded from rate limiting
- Returns HTTP 429 (Too Many Requests) when limit is exceeded

**Error Response** (429):
```json
{
  "error": "Rate limit exceeded. Please try again later.",
  "message": "Too many requests. Please wait before making another request."
}
```

### 6.4 Status Management

#### 6.4.1 Status Lifecycle
```
PROCESSING → COMPLETED
PROCESSING → FAILED
```

**Status Transitions**:
- Initial status: `PROCESSING` (set on creation)
- `PROCESSING` → `COMPLETED`: When transcription completes successfully
- `PROCESSING` → `FAILED`: When transcription fails or request validation fails

**Status Update Triggers**:
1. **On Creation**: Set to `PROCESSING`
2. **On Transcription Completion**: Update to `COMPLETED` with results
3. **On Transcription Failure**: Update to `FAILED`
4. **On External Service Callback**: Update based on callback message (if using external service)

#### 6.4.2 Status Change Actions

**On Status Change to COMPLETED**:
- Send completion email notification
- Log activity (if applicable)

**On Status Change to FAILED**:
- Send failure email notification
- Log activity (if applicable)

### 6.5 Language Management

**Default Language**: If no language is specified, use "auto" (auto-detect)

**Language Resolution**:
1. If `language` parameter provided:
   - Try to find by code (e.g., "en", "ar")
   - If not found, try to find by name
   - If still not found, default to "auto"
2. If `language` parameter not provided:
   - Use "auto" language

**Language Validation**:
- Language must exist in `transcription_languages` collection
- Language must be active (if filtering by active)

**Vosk Model Support**:
- Language models must be downloaded and available for Vosk
- Models are stored in configured path (default: `./models`)
- Supported languages depend on available Vosk models

### 6.6 Search and Filtering

**Search Functionality**:
- Search by file name (case-insensitive, partial match)
- Search by request ID (numeric ID or MongoDB ObjectId)

**Filtering**:
- By status: `processing`, `completed`, `failed`
- By category: Any valid category enum value
- By language_id: Filter by language
- By date range: `start_date` and `end_date` (Unix timestamps)

**Sorting**:
- Support multiple sort fields
- Support ascending/descending order
- MongoDB query-based sorting

### 6.7 Pagination

**Implementation**:
- Use page-based pagination
- Default: 10 items per page
- Maximum: 100 items per page
- Return pagination metadata:
  - Current page
  - Items per page
  - Total items
  - Total pages

---

## 7. External Integrations

### 7.1 Transcription Service Integration

**Primary Method: Vosk (Implemented)**
- Uses Vosk Java API with JNI bindings
- Processes audio files directly in the service
- Supports multiple languages via language models
- Converts audio to required format (WAV 16kHz mono PCM)
- Stores transcription results in database

**Optional: External Transcription Service**
- Forward audio files to external Python transcription service for processing
- Endpoint: Configurable via environment variable
- Base URL: `AUDIO_TO_TEXT_SERVICE_URL`
- Upload Path: `audio_service_upload_path` (default: `/api/v1/upload-audio`)

**Request Format**: `multipart/form-data`

**Request Parameters**:
- `audio_file`: The audio file (multipart)
- `speakers_count`: Integer (1-5)
- `language`: Language code (string)
- `category`: Category string
- `request_id`: Transcription request ID
- `transcription_id`: Transcription request ID (alias)
- `consumer_id`: Consumer ID
- `user_id`: User ID

**Response Handling**:
- Success (2xx): Transcription accepted, request is processing
- Failure (4xx/5xx): Transcription rejected, mark request as failed

**Error Handling**:
- Connection timeout: Mark as failed
- Service unavailable (503): Return 503 to client
- Invalid response: Log error, mark as failed

**Implementation Notes**:
- Use HTTP client (e.g., RestTemplate, WebClient in Spring Boot)
- Set appropriate timeout values
- Handle file streaming for large files
- Log all requests and responses

### 7.2 Message Queue Integration (Kafka)

#### 7.2.1 Callback Consumer

**Purpose**: Receive status updates from transcription service (if using external service).

**Topic Configuration**:
- Topic name: `audio_text_request_updater`
- Consumer group: `free-and-dirty-transcriber`
- Auto offset reset: `earliest`

**Message Format** (JSON):
```json
{
  "status": "completed",  // or "failed", "success" (mapped to "completed")
  "engines": [
    {
      "sender_parameters": [
        {
          "type": "kafka",
          "identifers": {
            "request_id": 123
          }
        }
      ]
    }
  ]
}
```

**Processing Logic**:
1. Parse JSON message
2. Extract `request_id` from message structure
3. Extract `status` from message
4. Map "success" to "completed"
5. Validate status is "completed" or "failed"
6. Find transcription request by numeric ID
7. Update status and transcription results
8. Commit offset (or handle error)

**Error Handling**:
- Missing request_id: Log error, skip message
- Invalid status: Log error, skip message
- Request not found: Log error, skip message
- Update failure: Log error, retry or skip

#### 7.2.2 Frontend Broadcast

**Purpose**: Send transcription data to frontend via Kafka for real-time updates.

**When**: On `show` endpoint for completed transcriptions (if applicable)

**Topic**: `frontend_audio_to_text_data` (or configurable)

**Message Format**:
```json
{
  "request_id": 123,
    "consumer_id": 456,
  "account_id": 456,
  "company_schema": "company_456_schema",
  "queue_name": "frontend_audio_to_text_data_company_456_0_1706352000",
  "routing_key": "audio_to_text_data_company_456_0_1706352000",
  "user_id": 789,
  "file_name": "meeting.mp3",
  "duration_secs": 3600,
  "speakers_count": 2,
  "language": "English",
  "category": "meeting",
  "status": "completed",
  "created_at": "2026-01-27T10:00:00Z",
  "updated_at": "2026-01-27T10:05:00Z",
  "product_id": 456,
  "product": "FDTranscriber",
  "page_name": "audio_to_text",
  "data_source_name": "fd_transcriber",
  "request_timestamp": 1706352000,
  "filters": {}
}
```

### 7.3 Email Service Integration

**Purpose**: Send email notifications for transcription status changes.

**Email Types**:

1. **Request Sent** (on successful submission):
   - Subject: "Audio Transcription Request Sent"
   - Template: `audio_transcription_loading_{lang}` (en/ar)
   - Recipient: User who created the request

2. **Transcription Completed**:
   - Subject: "Audio Transcription Completed"
   - Template: `audio_transcription_complete_{lang}` (en/ar)
   - Recipient: User who created the request

3. **Transcription Failed**:
   - Subject: "Audio Transcription Failed"
   - Template: `audio_transcription_error_{lang}` (en/ar)
   - Recipient: User who created the request

**Email Content Variables**:
- User information (name, email)
- Company information
- Transcription request details (file name, duration, category, language)
- Timestamp and timezone
- Customer success manager contact (if available)
- Transcription link (if applicable)
- Error message (for failure emails)

**Implementation Notes**:
- Use email service (SMTP, SendGrid, AWS SES, etc.)
- Support multiple languages (English, Arabic)
- Handle email sending failures gracefully (log, don't fail request)
- Use async processing for email sending (if possible)

---

## 8. Error Handling

### 8.1 Error Response Format

**Standard Error Response**:
```json
{
  "error": "Error message",
  "details": "Additional details or array of error messages",
  "timestamp": "2026-01-27T10:00:00Z",
  "path": "/api/free-and-dirty-transcriber"
}
```

### 8.2 HTTP Status Codes

| Code | Usage | Description |
|------|-------|-------------|
| 200 | Success | Request successful |
| 201 | Created | Resource created successfully |
| 400 | Bad Request | Invalid request parameters |
| 401 | Unauthorized | Authentication required or failed |
| 403 | Forbidden | Insufficient permissions or quota exceeded |
| 404 | Not Found | Resource not found |
| 422 | Unprocessable Entity | Validation failed or business rule violation |
| 500 | Internal Server Error | Unexpected server error |
| 503 | Service Unavailable | External service unavailable |

### 8.3 Error Scenarios

#### 8.3.1 Authentication Errors
- **401 Unauthorized**: Missing or invalid token
- **401 Unauthorized**: Token expired
- **401 Unauthorized**: User not found or inactive

#### 8.3.2 Authorization Errors
- **403 Forbidden**: User lacks required permission
- **403 Forbidden**: User doesn't belong to company
- **429 Too Many Requests**: Rate limit exceeded

#### 8.3.3 Validation Errors (422)
- Missing required parameters
- Invalid file format
- Invalid speakers_count (not 1-5)
- Invalid category
- Invalid language code
- Duration too short (< 1 second)
- Duration too long (> 3600 seconds)
- Metadata extraction failed

#### 8.3.4 Business Logic Errors (422)
- Cannot view incomplete transcription
- Cannot delete processing transcription
- Transcription request not found (404)

#### 8.3.5 External Service Errors
- **503 Service Unavailable**: Transcription service down (if using external service)
- **500 Internal Server Error**: Transcription service error
- Connection timeout
- Invalid response from service

#### 8.3.6 System Errors (500)
- Database connection error
- Message queue connection error
- Email service error
- Vosk library initialization error
- Unexpected exceptions

### 8.4 Error Logging

**Log Levels**:
- **ERROR**: System errors, external service failures, critical business logic failures
- **WARN**: Validation failures, rate limit issues, non-critical errors
- **INFO**: Successful operations, status changes, important events
- **DEBUG**: Detailed request/response logging, intermediate steps

**Log Information**:
- Timestamp
- Request ID (correlation ID)
- User ID, Company ID
- Endpoint, HTTP method
- Error message and stack trace
- Request parameters (sanitized)
- Response status

---

## 9. Database Schema

### 9.1 Entity Relationship Diagram

```
┌─────────────────────────┐
│ transcription_requests  │
├─────────────────────────┤
│ _id (ObjectId)          │
│ numericId (Long)        │
│ fileName                │
│ language (DBRef)        │──┐
│ speakersCount           │  │
│ durationSecs            │  │
│ status                  │  │
│ category                │  │
│ userId                  │  │
│ consumerId               │  │
│ transcriptionText       │  │
│ transcriptionJson       │  │
│ transcriptionMetadata   │  │
│ createdAt               │  │
│ updatedAt               │  │
└─────────────────────────┘  │
                              │
┌─────────────────────────┐  │
│ transcription_languages │◄─┘
├─────────────────────────┤
│ _id (ObjectId)          │
│ name                    │
│ code                    │
│ description             │
│ active                  │
│ popular                 │
│ createdAt               │
│ updatedAt               │
└─────────────────────────┘
```

### 9.2 MongoDB Collections

#### transcription_requests Collection
```javascript
{
  "_id": ObjectId("..."),
  "numericId": NumberLong(123),
  "fileName": "meeting_recording.mp3",
  "language": DBRef("transcription_languages", ObjectId("...")),
  "speakersCount": 2,
  "durationSecs": NumberDecimal("3600.50"),
  "status": "PROCESSING",  // or "COMPLETED", "FAILED"
  "category": "MEETING",
  "userId": NumberLong(789),
  "consumerId": NumberLong(456),
  "transcriptionText": "Full transcribed text...",
  "transcriptionJson": { /* Vosk JSON result */ },
  "transcriptionMetadata": { /* Metadata */ },
  "createdAt": ISODate("2026-01-27T10:00:00Z"),
  "updatedAt": ISODate("2026-01-27T10:05:00Z")
}
```

**Indexes**:
- `_id`: Primary key (automatic)
- `numericId`: Unique index
- `fileName`: Index
- `language`: Index
- `durationSecs`: Index
- `status`: Index
- `category`: Index
- `userId`: Index
- `consumerId`: Index

#### transcription_languages Collection
```javascript
{
  "_id": ObjectId("..."),
  "name": "English",
  "code": "en",
  "description": "English language",
  "active": true,
  "popular": true,
  "createdAt": ISODate("2026-01-27T10:00:00Z"),
  "updatedAt": ISODate("2026-01-27T10:00:00Z")
}
```

**Indexes**:
- `_id`: Primary key (automatic)
- `name`: Unique index
- `code`: Unique index
- `active`: Index
- `popular`: Index

### 9.3 Initial Data

**Default Languages** (seeded automatically via MongoDataInitializer):
- Auto Detect (code: "auto")
- English (code: "en")
- Arabic (code: "ar")
- Spanish (code: "es")
- French (code: "fr")
- German (code: "de")
- Italian (code: "it")
- Portuguese (code: "pt")
- Russian (code: "ru")
- Chinese (code: "zh")
- Japanese (code: "ja")
- Korean (code: "ko")

Languages are automatically seeded on first application startup.

---

## 10. Configuration

### 10.1 Environment Variables

| Variable | Description | Example | Required |
|----------|-------------|---------|----------|
| `MONGODB_URI` | MongoDB connection URI | `mongodb://localhost:27017/free_and_dirty_transcriber` | Yes |
| `MONGODB_DATABASE` | MongoDB database name | `free_and_dirty_transcriber` | No (default: free_and_dirty_transcriber) |
| `KAFKA_BOOTSTRAP_SERVERS` | Kafka brokers | `localhost:9092` | Yes |
| `KAFKA_CONSUMER_GROUP_ID` | Kafka consumer group | `free-and-dirty-transcriber` | No (default: free-and-dirty-transcriber) |
| `KAFKA_TOPIC_STATUS_UPDATES` | Status updates topic | `audio_text_request_updater` | No (default: audio_text_request_updater) |
| `KAFKA_TOPIC_FRONTEND_BROADCAST` | Frontend broadcast topic | `frontend_audio_to_text_data` | No (default: frontend_audio_to_text_data) |
| `AUDIO_TO_TEXT_SERVICE_URL` | External transcription service base URL (optional) | `https://transcription-service.example.com` | No |
| `audio_service_upload_path` | Upload endpoint path (if using external service) | `/api/v1/upload-audio` | No |
| `TRANSCRIPTION_ENGINE_TYPE` | Transcription engine type | `vosk` or `external` | No (default: vosk) |
| `VOSK_MODELS_PATH` | Path to Vosk model files | `./models` | No (default: ./models) |
| `AUDIO_TEMP_DIR` | Temporary directory for audio processing | `/tmp/audio` | No (default: /tmp/audio) |
| `EMAIL_SMTP_HOST` | SMTP host | `smtp.example.com` | Yes |
| `EMAIL_SMTP_PORT` | SMTP port | `587` | Yes |
| `EMAIL_SMTP_USERNAME` | SMTP username | `noreply@example.com` | Yes |
| `EMAIL_SMTP_PASSWORD` | SMTP password | `password` | Yes |
| `JWT_PUBLIC_KEY` | JWT public key for token validation | `-----BEGIN PUBLIC KEY-----...` | Yes |
| `JWT_ISSUER` | JWT issuer | `amritech` | Yes |
| `MAX_AUDIO_DURATION_SECONDS` | Maximum audio duration | `3600` | No (default: 3600) |
| `MIN_AUDIO_DURATION_SECONDS` | Minimum audio duration | `1` | No (default: 1) |
| `MAX_FILE_SIZE_MB` | Maximum file size in MB | `500` | No (default: 500) |

### 10.2 Application Properties (Spring Boot)

```properties
# Server
server.port=8080
server.servlet.context-path=/api

# MongoDB
spring.data.mongodb.uri=${MONGODB_URI:mongodb://localhost:27017/free_and_dirty_transcriber}
spring.data.mongodb.database=${MONGODB_DATABASE:free_and_dirty_transcriber}
spring.data.mongodb.auto-index-creation=true

# Kafka
spring.kafka.bootstrap-servers=${KAFKA_BOOTSTRAP_SERVERS:localhost:9092}
spring.kafka.consumer.group-id=${KAFKA_CONSUMER_GROUP_ID:free-and-dirty-transcriber}
spring.kafka.consumer.auto-offset-reset=earliest
spring.kafka.consumer.enable-auto-commit=false
spring.kafka.producer.acks=all
spring.kafka.producer.retries=3
spring.kafka.producer.enable-idempotence=true

# Kafka Topics
kafka.topics.status-updates=${KAFKA_TOPIC_STATUS_UPDATES:audio_text_request_updater}
kafka.topics.frontend-broadcast=${KAFKA_TOPIC_FRONTEND_BROADCAST:frontend_audio_to_text_data}

# File Upload
spring.servlet.multipart.enabled=true
spring.servlet.multipart.max-file-size=500MB
spring.servlet.multipart.max-request-size=500MB

# Audio Processing
audio.processing.temp.dir=${AUDIO_TEMP_DIR:/tmp/audio}
audio.processing.max-file-size=500MB
audio.processing.supported-formats=mp3,wav,m4a,flac,ogg

# Transcription Engine
transcription.engine.type=${TRANSCRIPTION_ENGINE_TYPE:vosk}
transcription.engine.vosk.models.path=${VOSK_MODELS_PATH:./models}
transcription.engine.vosk.sample-rate=16000

# External Transcription Service (optional)
audio.transcription.service.url=${AUDIO_TO_TEXT_SERVICE_URL:}
audio.transcription.service.upload-path=${audio_service_upload_path:/api/v1/upload-audio}
audio.transcription.service.timeout=30000

# Audio Duration Limits
audio.transcription.max-duration-seconds=${MAX_AUDIO_DURATION_SECONDS:3600}
audio.transcription.min-duration-seconds=${MIN_AUDIO_DURATION_SECONDS:1}

# Rate Limiting
rate.limit.requests-per-minute=${RATE_LIMIT_REQUESTS_PER_MINUTE:10}
rate.limit.burst-capacity=${RATE_LIMIT_BURST_CAPACITY:20}

# JWT
jwt.public-key=${JWT_PUBLIC_KEY}
jwt.issuer=${JWT_ISSUER:amritech}

# Email
spring.mail.host=${EMAIL_SMTP_HOST}
spring.mail.port=${EMAIL_SMTP_PORT}
spring.mail.username=${EMAIL_SMTP_USERNAME}
spring.mail.password=${EMAIL_SMTP_PASSWORD}

# Logging
logging.level.com.example.audiototext=INFO
logging.level.org.springframework.web=DEBUG
```

---

## 11. Non-Functional Requirements

### 11.1 Performance
- **Response Time**: 
  - List requests: < 500ms (p95)
  - Get request: < 200ms (p95)
  - Create request: < 2s (p95) - includes file upload and metadata extraction
  - Transcription processing: Depends on audio duration and Vosk model size
- **Throughput**: 
  - Support at least 100 concurrent requests
  - Handle at least 1000 requests per minute
- **File Upload**: 
  - Support files up to 500MB
  - Stream large files for processing (don't load entirely in memory)

### 11.2 Scalability
- **Horizontal Scaling**: Service should be stateless (except database)
- **Database**: Use MongoDB connection pooling, optimize queries
- **Caching**: Consider caching language list
- **Load Balancing**: Support multiple instances behind load balancer
- **Vosk Models**: Models can be shared across instances or loaded per instance

### 11.3 Reliability
- **Availability**: 99.9% uptime target
- **Error Recovery**: 
  - Retry transient failures (external service, message queue)
  - Graceful degradation (email failures shouldn't fail requests)
  - Vosk library initialization failures should be handled gracefully
- **Data Consistency**: 
  - Implement idempotency for status updates

### 11.4 Security
- **Authentication**: JWT token validation on all endpoints
- **Authorization**: Permission-based access control
- **Input Validation**: Validate all inputs, sanitize file names
- **File Security**: 
  - Validate file types
  - Scan for malware (if applicable)
  - Store files securely (if storing)
  - Temporary files should be cleaned up after processing
- **Data Privacy**: 
  - Don't log sensitive information
  - Encrypt sensitive data at rest
  - Use HTTPS for all communications

### 11.5 Monitoring & Observability
- **Logging**: 
  - Structured logging (JSON format)
  - Log all API requests/responses
  - Log external service calls
  - Log Vosk transcription processing
  - Log errors with stack traces
- **Metrics**: 
  - Request count, latency, error rate
  - Rate limiting metrics
  - External service health (if using)
  - Vosk transcription performance metrics
  - Database connection pool metrics
- **Tracing**: 
  - Distributed tracing (if using microservices)
  - Correlation IDs for request tracking
- **Health Checks**: 
  - `/health` endpoint
  - `/health/readiness` endpoint
  - `/health/liveness` endpoint
  - Vosk library availability check

### 11.6 Testing Requirements
- **Unit Tests**: 
  - Business logic (rate limiting, validation, status management)
  - Service layer
  - Repository layer
  - Vosk transcription service
- **Integration Tests**: 
  - API endpoints
  - Database operations
  - External service integration (mocked)
  - Kafka message queue integration (test containers)
  - MongoDB integration (test containers)
- **End-to-End Tests**: 
  - Complete transcription flow
  - Error scenarios
- **Performance Tests**: 
  - Load testing
  - Stress testing
  - Vosk transcription performance

---

## 12. Implementation Notes

### 12.1 Technology Stack

**Core Framework**:
- Spring Boot 3.2.x
- Java 17 or higher

**Dependencies**:
- Spring Web (REST API)
- Spring Data MongoDB (Database)
- Spring Kafka (Message Queue)
- Spring Mail (Email)
- Jackson (JSON serialization)
- JWT library (`io.jsonwebtoken:jjwt`)
- Vosk (Speech Recognition - Apache 2.0 License)
- JAudioTagger (Audio Metadata)
- Apache Tika (File Metadata)
- Validation (Bean Validation)
- Lombok (optional, for reducing boilerplate)

**Database**:
- MongoDB 4.4+
- Automatic data initialization via MongoDataInitializer

**Message Queue**:
- Apache Kafka

**Build Tool**:
- Maven

### 12.2 Project Structure

```
src/
├── main/
│   ├── java/
│   │   └── com/example/audiototext/
│   │       ├── AudioToTextApplication.java
│   │       ├── config/
│   │       │   ├── SecurityConfig.java
│   │       │   ├── KafkaConfig.java
│   │       │   ├── DatabaseConfig.java
│   │       │   ├── VoskConfig.java
│   │       │   ├── MongoDataInitializer.java
│   │       │   ├── WebConfig.java
│   │       │   ├── ApiVersionInterceptor.java
│   │       │   └── ...
│   │       ├── controller/
│   │       │   └── TranscriptionRequestController.java
│   │       ├── service/
│   │       │   ├── TranscriptionRequestService.java
│   │       │   ├── RateLimitingConfig.java
│   │       │   ├── MetadataExtractionService.java
│   │       │   ├── TranscriptionEngineService.java
│   │       │   ├── VoskTranscriptionService.java
│   │       │   ├── TranscriptionServiceClient.java
│   │       │   ├── EmailService.java
│   │       │   ├── MessageQueueService.java
│   │       │   └── SequenceService.java
│   │       ├── repository/
│   │       │   ├── TranscriptionRequestRepository.java
│   │       │   └── TranscriptionLanguageRepository.java
│   │       ├── model/
│   │       │   ├── TranscriptionRequest.java
│   │       │   ├── TranscriptionLanguage.java
│   │       │   └── enums/
│   │       ├── dto/
│   │       │   ├── request/
│   │       │   └── response/
│   │       ├── exception/
│   │       │   ├── GlobalExceptionHandler.java
│   │       │   └── CustomExceptions.java
│   │       ├── util/
│   │       │   ├── AudioFormatConverter.java
│   │       │   ├── AudioValidator.java
│   │       │   └── VoskNativeLibraryLoader.java
│   │       └── security/
│   │           ├── JwtAuthenticationFilter.java
│   │           └── PermissionEvaluator.java
│   └── resources/
│       ├── application.properties
│       ├── application-dev.properties
│       ├── application-prod.properties
│       └── models/  (Vosk language models)
└── test/
    └── java/
        └── com/example/audiototext/
            ├── controller/
            ├── service/
            └── repository/
```

### 12.3 Key Implementation Considerations

1. **Transaction Management**: Use `@Transactional` for database operations (MongoDB transactions require replica set)
2. **Async Processing**: Use `@Async` for email sending and non-critical operations
3. **Caching**: Cache language list using Spring Cache
4. **File Handling**: Process audio files with Vosk, clean up temporary files
5. **Message Queue**: Use Spring Kafka with manual acknowledgment and error handling
6. **Error Handling**: Use `@ControllerAdvice` for global exception handling
7. **Validation**: Use Bean Validation (`@Valid`, `@NotNull`, etc.)
8. **Documentation**: Use OpenAPI/Swagger for API documentation
9. **Vosk Integration**: Handle JNI library loading, model caching, audio format conversion
10. **ID Management**: Use MongoDB ObjectId with numericId for API compatibility

---

## 13. Migration Considerations

### 13.1 Data Migration
If migrating from existing system:
1. Export existing data from source database
2. Transform data format if needed (SQL to MongoDB)
3. Import into MongoDB
4. Verify data integrity
5. Generate numericId for existing records

### 13.2 API Compatibility
- Maintain API contract (endpoints, request/response formats)
- Header-based versioning allows for future API changes
- Update clients to use new base URL and version header

### 13.3 Deployment Strategy
- Blue-green deployment
- Canary deployment
- Gradual rollout

---

## 14. Open Questions / Decisions Needed

1. **User/Company Management**: 
   - Are `users` and `companies` collections in this service or separate service?
   - If separate, how to validate user/company existence?

2. **Rate Limiting**:
   - Rate limiting is implemented using Bucket4j library
   - Configurable via application properties
   - Applied via filter early in request processing

3. **File Storage**:
   - Does this service store audio files or only process them?
   - If storing, where (local filesystem, S3, etc.)?
   - How long to retain audio files?

4. **Transcription Results**:
   - Current implementation stores transcription results in MongoDB
   - Consider storage optimization for large transcriptions
   - Consider archiving old transcriptions

5. **Authentication Service**:
   - Is authentication handled by this service or external auth service?
   - How to validate JWT tokens (local validation or call auth service)?

6. **Email Templates**:
   - Where are email templates stored?
   - How to handle multi-language templates?

7. **Activity Logging**:
   - Is user activity logging required?
   - If yes, how to integrate (separate service, database collection)?

8. **Vosk Model Management**:
   - How to handle model updates?
   - How to support new languages?
   - Model versioning strategy?

---

## 15. Appendix

### 15.1 API Examples

#### Create Request (cURL)
```bash
curl -X POST "https://api.example.com/api/free-and-dirty-transcriber" \
  -H "Authorization: Bearer <token>" \
  -H "API-Version: v1" \
  -F "audio_file=@meeting.mp3" \
  -F "speakers_count=2" \
  -F "language=en" \
  -F "category=meeting"
```

#### List Requests (cURL)
```bash
curl -X GET "https://api.example.com/api/free-and-dirty-transcriber?page=1&limit_per_page=10&status=completed" \
  -H "Authorization: Bearer <token>" \
  -H "API-Version: v1"
```

### 15.2 Status Codes Reference

| Code | Meaning | When to Use |
|------|---------|-------------|
| 200 | OK | Successful GET, PUT, DELETE |
| 201 | Created | Successful POST |
| 400 | Bad Request | Invalid request syntax |
| 401 | Unauthorized | Missing or invalid authentication |
| 403 | Forbidden | Valid auth but insufficient permissions |
| 404 | Not Found | Resource doesn't exist |
| 422 | Unprocessable Entity | Valid syntax but business rule violation |
| 500 | Internal Server Error | Unexpected server error |
| 503 | Service Unavailable | External service unavailable |

---

## Document Revision History

| Version | Date | Author | Changes |
|---------|------|--------|---------|
| 1.0 | 2026-01-27 | AI Assistant | Initial specification document reflecting current implementation |

---

**End of Specification Document**
