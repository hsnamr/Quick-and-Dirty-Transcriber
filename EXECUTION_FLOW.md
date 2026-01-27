# Execution Flow Documentation

## Main Class

**Class**: `com.example.transcriber.FreeAndDirtyTranscriber`  
**Location**: `src/main/java/com/example/transcriber/FreeAndDirtyTranscriber.java`

```java
@SpringBootApplication
public class FreeAndDirtyTranscriber {
    public static void main(String[] args) {
        SpringApplication.run(FreeAndDirtyTranscriber.class, args);
    }
}
```

This is the entry point of the application. The `@SpringBootApplication` annotation enables:
- Auto-configuration
- Component scanning (within `com.example.transcriber` package)
- Configuration property support

---

## Application Startup Flow

### 1. Application Initialization

When `SpringApplication.run()` is called, Spring Boot performs the following steps:

#### 1.1 Context Creation
- Creates the Spring Application Context
- Scans for `@Configuration` classes
- Discovers and registers beans

#### 1.2 Configuration Loading
- Loads `application.properties` (or `application-{profile}.properties`)
- Processes environment variables
- Configures MongoDB, Kafka, JWT, Email, etc.

#### 1.3 Bean Initialization (Order of Execution)

**Phase 1: Infrastructure Beans**
1. **DatabaseConfig** (`@EnableMongoRepositories`)
   - Enables MongoDB repositories
   - Enables MongoDB auditing (createdAt/updatedAt)

2. **KafkaConfig**
   - Configures Kafka producers and consumers
   - Sets up serializers/deserializers
   - Configures listener container factories

3. **SecurityConfig**
   - Configures Spring Security
   - Sets up JWT authentication filter chain
   - Configures authorization rules

**Phase 2: Service Beans**
4. **VoskConfig** (`@PostConstruct`)
   - Initializes Vosk models directory
   - Verifies native library availability
   - Logs OS and architecture information

5. **EmailConfig**
   - Configures JavaMailSender
   - Sets up SMTP connection settings

6. **CacheConfig**
   - Configures caching (for language list)

7. **AsyncConfig**
   - Configures async execution (for email sending)

**Phase 3: Data Initialization**
8. **MongoDataInitializer** (`CommandLineRunner`)
   - Runs after all beans are initialized
   - Seeds default languages if database is empty
   - Creates: auto, en, ar, es, fr, de, it, pt, ru, zh, ja, ko

**Phase 4: Kafka Consumers**
9. **TranscriptionStatusUpdateConsumer**
   - Starts listening to `audio_text_request_updater` topic
   - Processes status update messages from external services

#### 1.4 Web Server Startup
- Starts embedded Tomcat server (default port: 8080)
- Context path: `/api`
- Registers REST controllers

---

## Request Execution Flow

### HTTP Request Flow

```
Client Request
    ↓
Tomcat Server (Port 8080)
    ↓
DispatcherServlet (Spring MVC)
    ↓
Security Filter Chain
    ↓
JwtAuthenticationFilter
    ↓
API Version Interceptor
    ↓
Controller
    ↓
Service Layer
    ↓
Repository Layer
    ↓
MongoDB / Kafka / External Services
```

### Detailed Request Flow: Create Transcription Request

**Endpoint**: `POST /api/free-and-dirty-transcriber`

#### Step 1: Request Reception
```
HTTP Request → Tomcat → DispatcherServlet
```

#### Step 2: Security Filter Chain
```
JwtAuthenticationFilter.doFilterInternal()
├── Extract JWT token from Authorization header
├── Validate token (JwtTokenValidator)
├── Extract claims (user_id, permissions)
├── Set UserContext (ThreadLocal)
└── Set SecurityContext authentication
```

#### Step 3: API Version Interceptor
```
ApiVersionInterceptor.preHandle()
├── Extract API-Version header
├── Validate version (default: v1)
└── Continue to controller
```

#### Step 4: Controller Layer
```
TranscriptionRequestController.createTranscriptionRequest()
├── Validate request DTO (@Valid)
└── Call TranscriptionRequestService
```

#### Step 5: Service Layer
```
TranscriptionRequestServiceImpl.createTranscriptionRequest()
├── 1. Get user context (UserContext.getUserId())
├── 2. Validate audio file (AudioValidator)
├── 3. Extract metadata (MetadataExtractionService)
│   └── Extract: duration, file name, size, format
├── 4. Validate quota (QuotaService) [COMMENTED OUT]
├── 5. Resolve language (LanguageService)
│   └── Default to "auto" if not provided
├── 6. Parse category (Category enum)
├── 7. Generate numeric ID (SequenceService)
├── 8. Create TranscriptionRequest entity
│   └── Status: PROCESSING
├── 9. Save to MongoDB (TranscriptionRequestRepository)
├── 10. Consume quota (QuotaService) [COMMENTED OUT]
├── 11. Process transcription (TranscriptionEngineService)
│   ├── If Vosk: Process directly
│   │   ├── Load Vosk model
│   │   ├── Convert audio to WAV 16kHz mono
│   │   ├── Run transcription
│   │   └── Extract text and JSON
│   └── If External: Forward to external service
├── 12. Update request with results
│   ├── Set transcriptionText
│   ├── Set transcriptionJson
│   ├── Set transcriptionMetadata
│   └── Set status: COMPLETED
├── 13. Save updated request
├── 14. Send completion email (EmailService) [Async]
└── 15. Send request sent email (EmailService) [Async]
```

#### Step 6: Response
```
TranscriptionRequestDTO
└── Convert entity to DTO
    └── Return 201 Created
```

### Request Flow: List Transcription Requests

**Endpoint**: `GET /api/free-and-dirty-transcriber`

```
1. JwtAuthenticationFilter → Authenticate
2. TranscriptionRequestController.listTranscriptionRequests()
3. TranscriptionRequestServiceImpl.listTranscriptionRequests()
   ├── Build MongoDB query (filters, search, date range)
   ├── Apply pagination
   ├── Apply sorting
   ├── Execute query
   ├── Calculate overview statistics
   │   ├── Total count
   │   ├── Completed count
   │   ├── Processing count
   │   ├── Failed count
   │   └── Used quota (sum of durations)
   ├── Build filter options
   │   ├── Languages (from database)
   │   ├── Categories (from enum)
   │   └── Statuses (from enum)
   ├── Build sorting options
   └── Return TranscriptionRequestListDTO
```

### Request Flow: Get Transcription Request

**Endpoint**: `GET /api/free-and-dirty-transcriber/{id}`

```
1. JwtAuthenticationFilter → Authenticate
2. TranscriptionRequestController.getTranscriptionRequest()
3. TranscriptionRequestServiceImpl.getTranscriptionRequest()
   ├── Find request by numericId and userId
   ├── Verify status is COMPLETED
   ├── Send frontend broadcast (MessageQueueService)
   │   └── Kafka topic: frontend_audio_to_text_data
   └── Return TranscriptionRequestDTO
```

### Request Flow: Kafka Status Update Consumer

**Topic**: `audio_text_request_updater`

```
1. Kafka message received
2. TranscriptionStatusUpdateConsumer.handleStatusUpdate()
   ├── Extract status from message
   ├── Map "success" → "completed"
   ├── Extract request_id from nested structure
   │   └── engines[0].sender_parameters[0].identifers.request_id
   ├── Update status (StatusManagementService)
   │   ├── Find request by numericId
   │   ├── Validate status transition
   │   ├── Update status
   │   └── Handle side effects:
   │       ├── If COMPLETED: Send completion email
   │       └── If FAILED: Restore quota + Send failure email
   └── Acknowledge message
```

---

## Component Interaction Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                    FreeAndDirtyTranscriber                 │
│                      (Main Class)                           │
└───────────────────────┬───────────────────────────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │   Spring Boot Application     │
        │        Context                │
        └───────────────┬───────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Security   │  │   Database   │  │    Kafka    │
│    Config    │  │    Config    │  │    Config   │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                  │
       ▼                 ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│ JWT Filter   │  │  MongoDB     │  │  Consumer   │
│              │  │  Repos       │  │  Producer   │
└──────┬───────┘  └──────┬───────┘  └──────┬───────┘
       │                 │                  │
       └─────────┬───────┴──────────┬───────┘
                 │                  │
                 ▼                  ▼
        ┌───────────────────────────────┐
        │    TranscriptionRequest       │
        │         Controller             │
        └───────────────┬───────────────┘
                        │
                        ▼
        ┌───────────────────────────────┐
        │  TranscriptionRequestService │
        └───────────────┬───────────────┘
                        │
        ┌───────────────┼───────────────┐
        │               │               │
        ▼               ▼               ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   Quota      │  │ Transcription│  │   Status     │
│   Service    │  │   Engine     │  │  Management  │
└──────────────┘  └──────────────┘  └──────────────┘
```

---

## Key Configuration Points

### Application Properties Loading Order
1. `application.properties` (base)
2. `application-{profile}.properties` (profile-specific)
3. Environment variables (override)

### Bean Initialization Order
- `@PostConstruct` methods run after dependency injection
- `CommandLineRunner` beans run after application context is fully loaded
- `@KafkaListener` methods start listening after context is ready

### Security Filter Chain Order
1. JwtAuthenticationFilter (custom)
2. UsernamePasswordAuthenticationFilter (Spring Security)
3. Authorization filters

---

## Thread Safety

- **UserContext**: Uses ThreadLocal for per-request user context
- **MongoDB**: Thread-safe connection pooling
- **Kafka**: Thread-safe producers and consumers
- **Vosk Models**: Cached per language (ConcurrentHashMap)

---

## Error Handling Flow

```
Exception thrown
    ↓
GlobalExceptionHandler (@ControllerAdvice)
    ├── ValidationException → 422 Unprocessable Entity
    ├── ResourceNotFoundException → 404 Not Found
    ├── QuotaExceededException → 403 Forbidden
    ├── ExternalServiceException → 503 Service Unavailable
    └── Generic Exception → 500 Internal Server Error
```

---

## Shutdown Flow

1. Spring Boot receives shutdown signal (SIGTERM)
2. Stops accepting new requests
3. Waits for active requests to complete
4. Closes Kafka consumers
5. Closes MongoDB connections
6. Cleans up resources
7. Application exits

---

## Notes

- All email operations are async (`@Async`)
- Quota operations are currently commented out
- Vosk models are loaded lazily on first use
- MongoDB indexes are created automatically on startup
- Kafka consumers use manual acknowledgment
