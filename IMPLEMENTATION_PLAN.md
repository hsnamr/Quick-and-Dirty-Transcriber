# Audio Transcription Implementation Plan

## Document Information
- **Service Name**: Audio To Text Service
- **Goal**: Implement REST API that transcribes audio files to text using free/open source libraries
- **Date**: January 27, 2026
- **Aligned With**: SPECIFICATION.md and PLAN.md

---

## Executive Summary

This plan outlines the implementation of an audio-to-text transcription service that processes audio files and returns text transcriptions. The implementation will use free and open-source libraries, with a flexible architecture that supports both standalone transcription and integration with external services.

---

## Technology Selection for Transcription

### Option 1: Vosk (Recommended for Java)
- **License**: Apache 2.0 (Free & Open Source)
- **Language**: Java bindings available
- **Features**: 
  - Offline speech recognition
  - Multiple language models
  - Real-time and batch processing
  - Speaker diarization support
- **Pros**: Native Java support, offline capable, good accuracy
- **Cons**: Requires model files, may need JNI setup

### Option 2: Coqui STT
- **License**: MPL 2.0 (Free & Open Source)
- **Language**: Java API available
- **Features**: 
  - Deep learning-based STT
  - Streaming support
  - Language model support
- **Pros**: Modern architecture, good accuracy
- **Cons**: Less mature Java bindings

### Option 3: Local Whisper Service (Hybrid)
- **License**: MIT (Free & Open Source)
- **Language**: Python service, Java client
- **Features**:
  - State-of-the-art accuracy
  - Multiple model sizes
  - Multi-language support
- **Pros**: Best accuracy, well-maintained
- **Cons**: Requires Python service, more complex setup

### Decision: **Vosk** (Primary) with fallback to external service
- Use Vosk for standalone transcription
- Maintain ability to forward to external Python service (as per spec)
- Support both modes via configuration

---

## Implementation Phases

### Phase 1: Foundation & Database Setup (Week 1-2)
**Goal**: Set up project infrastructure and database

#### Tasks:
1. **Project Dependencies**
   - Add Vosk Java library dependency
   - Add audio processing libraries (JAudioTagger, Tika for metadata)
   - Set up Maven/Gradle build configuration
   - Configure Spring Boot dependencies

2. **Database Migrations**
   - Create Flyway migration for `transcription_languages` table
   - Create Flyway migration for `transcription_requests` table
   - Seed default languages (auto, en, ar, es, fr, de, etc.)
   - Add indexes and constraints

3. **Model Implementation**
   - Complete entity classes with proper JPA annotations
   - Implement enum classes (Status, Category)
   - Add validation annotations
   - Set up auditing (created_at, updated_at)

4. **Repository Layer**
   - Implement custom query methods
   - Add specification support for complex filtering
   - Implement pagination support

**Deliverables**:
- ✅ Database schema created and migrated
- ✅ Entities fully implemented
- ✅ Repositories with query methods
- ✅ Build configuration with all dependencies

---

### Phase 2: Core Transcription Engine (Week 3-4)
**Goal**: Implement the actual audio transcription functionality

#### Tasks:
1. **Audio Metadata Extraction**
   - Implement `MetadataExtractionServiceImpl`
   - Use JAudioTagger or Apache Tika for metadata
   - Extract: duration, file size, format, sample rate
   - Validate audio file formats (MP3, WAV, M4A, FLAC, OGG)
   - Handle errors gracefully

2. **Vosk Integration**
   - Set up Vosk model loading service
   - Create transcription engine service
   - Implement audio format conversion (if needed)
   - Support multiple languages via model selection
   - Implement speaker diarization (1-5 speakers)
   - Add model caching for performance

3. **Transcription Service**
   - Create `TranscriptionEngineService` interface
   - Implement Vosk-based transcription
   - Support batch processing
   - Handle long audio files (chunking if needed)
   - Implement progress tracking
   - Add result formatting (timestamps, speaker labels)

4. **Audio Processing Utilities**
   - Audio format conversion utilities
   - Audio validation utilities
   - File streaming utilities
   - Temporary file management

**Deliverables**:
- ✅ Audio metadata extraction working
- ✅ Vosk integration complete
- ✅ Transcription engine functional
- ✅ Support for multiple languages
- ✅ Speaker diarization support

---

### Phase 3: Business Logic & Services (Week 5-6)
**Goal**: Implement core business logic and service layer

#### Tasks:
1. **Language Service**
   - Implement `LanguageServiceImpl`
   - Language resolution logic
   - Default to "auto" language
   - Language validation
   - Cache active languages

2. **Rate Limiting**
   - Implement rate limiting using Bucket4j
   - Configure token bucket algorithm
   - Create rate limiting filter
   - Configure rate limits via properties

3. **Transcription Request Service**
   - Implement `TranscriptionRequestServiceImpl.createTranscriptionRequest()`
   - Complete request creation flow:
     - Authenticate/authorize
     - Extract metadata
     - Check rate limiting (enforced by filter)
     - Resolve language
     - Create request record
     - Process transcription (Vosk or external)
     - Update status
     - Send notifications
   - Implement status management
   - Handle errors and rollbacks

4. **Status Management Service**
   - Implement status transitions
   - Handle status change side effects
   - Email notifications

5. **External Service Client (Optional)**
   - Implement `TranscriptionServiceClientImpl`
   - Support forwarding to external Python service
   - Make it configurable (use Vosk or external)
   - Handle both modes

**Deliverables**:
- ✅ All service implementations complete
- ✅ Business logic working
- ✅ Rate limiting functional
- ✅ Status management working
- ✅ Transcription processing (Vosk) working

---

### Phase 4: REST API & DTOs (Week 7-8)
**Goal**: Complete REST API endpoints and DTO mapping

#### Tasks:
1. **DTO Implementation**
   - Complete all DTO classes
   - Implement DTO mappers (MapStruct or manual)
   - Format dates, durations, status display names
   - Build JSON:API format responses

2. **Controller Implementation**
   - Complete `TranscriptionRequestController`
   - Implement all 5 endpoints:
     - POST `/api/v1/audio-to-text` (Create)
     - GET `/api/v1/audio-to-text` (List)
     - GET `/api/v1/audio-to-text/{id}` (Get)
     - PUT `/api/v1/audio-to-text/{id}` (Update)
     - DELETE `/api/v1/audio-to-text/{id}` (Delete)
   - Request validation
   - Response formatting
   - Error handling

3. **Filtering & Pagination**
   - Implement complex filtering (status, category, language, date range)
   - Implement search (file name, ID)
   - Implement sorting (multiple fields)
   - Implement pagination
   - Build overview statistics
   - Build filter options

4. **Request Validation**
   - Custom validators for audio files
   - Category validation
   - Language validation
   - Duration validation
   - File size validation

**Deliverables**:
- ✅ All REST endpoints working
- ✅ DTOs complete and mapped
- ✅ Filtering, sorting, pagination working
- ✅ Request validation complete

---

### Phase 5: Security & Authentication (Week 9)
**Goal**: Implement JWT authentication and authorization

#### Tasks:
1. **JWT Token Validation**
   - Complete `JwtTokenValidator`
   - Parse JWT tokens
   - Validate signature with public key
   - Extract user context (user_id, consumer_id, permissions)
   - Handle token expiration

2. **JWT Authentication Filter**
   - Complete `JwtAuthenticationFilter`
   - Extract token from headers
   - Validate and set authentication
   - Set user context

3. **Permission Evaluation**
   - Complete `PermissionEvaluator`
   - Check product/feature permissions
   - Verify user belongs to company
   - Verify company subscription

4. **Security Configuration**
   - Complete `SecurityConfig`
   - Configure security filter chain
   - Set up method security
   - Configure CORS

**Deliverables**:
- ✅ JWT authentication working
- ✅ Authorization checks working
- ✅ Security configuration complete

---

### Phase 6: External Integrations (Week 10-11)
**Goal**: Integrate with Kafka, Email, and optional external services

#### Tasks:
1. **Kafka Integration**
   - Complete `MessageQueueServiceImpl`
   - Implement frontend broadcast producer
   - Complete `TranscriptionStatusUpdateConsumer`
   - Handle gzipped messages
   - Implement error handling and retries

2. **Email Service**
   - Complete `EmailServiceImpl`
   - Create email templates (Thymeleaf)
   - Multi-language templates (en/ar)
   - Async email sending
   - Template variables population

3. **External Transcription Service (Optional)**
   - Complete `TranscriptionServiceClientImpl`
   - HTTP client for external service
   - File streaming
   - Error handling
   - Retry logic

**Deliverables**:
- ✅ Kafka integration working
- ✅ Email notifications working
- ✅ External service integration (if used)

---

### Phase 7: Error Handling & Validation (Week 12)
**Goal**: Complete error handling and edge cases

#### Tasks:
1. **Global Exception Handler**
   - Complete `GlobalExceptionHandler`
   - Handle all exception types
   - Format error responses
   - Log errors appropriately

2. **Custom Exceptions**
   - Complete all custom exception classes
   - Add appropriate error messages
   - Handle edge cases

3. **Validation**
   - Complete all validators
   - Add comprehensive validation messages
   - Handle validation errors

4. **Error Logging**
   - Structured logging
   - Correlation IDs
   - Error tracking

**Deliverables**:
- ✅ Comprehensive error handling
- ✅ All validations working
- ✅ Proper error responses

---

### Phase 8: Testing & Documentation (Week 13-14)
**Goal**: Write tests and documentation

#### Tasks:
1. **Unit Tests**
   - Service layer tests
   - Repository tests
   - Utility tests
   - Mock dependencies

2. **Integration Tests**
   - API endpoint tests
   - Database tests
   - Kafka tests (Testcontainers)
   - External service tests (mocked)

3. **End-to-End Tests**
   - Complete transcription flow
   - Error scenarios
   - Rate limiting scenarios

4. **Documentation**
   - API documentation (OpenAPI/Swagger)
   - README with setup instructions
   - Vosk model setup guide
   - Configuration guide

**Deliverables**:
- ✅ Test coverage > 80%
- ✅ API documentation complete
- ✅ Setup documentation complete

---

## Technical Implementation Details

### Vosk Integration

#### Dependencies
```xml
<dependency>
    <groupId>com.alphacephei</groupId>
    <artifactId>vosk</artifactId>
    <version>0.3.45</version>
</dependency>
```

#### Model Files
- Download language models from: https://alphacephei.com/vosk/models
- Store models in: `src/main/resources/models/`
- Support models: English, Arabic, Spanish, French, German, etc.
- Auto-detect model based on language code

#### Implementation Pattern
```java
@Service
public class VoskTranscriptionService implements TranscriptionEngineService {
    private final Map<String, Model> modelCache = new ConcurrentHashMap<>();
    
    public TranscriptionResult transcribe(MultipartFile audioFile, String languageCode) {
        // Load model
        Model model = getModel(languageCode);
        
        // Create recognizer
        Recognizer recognizer = new Recognizer(model, 16000f);
        
        // Process audio
        // ... transcription logic
        
        return result;
    }
}
```

### Audio Processing

#### Supported Formats
- MP3, WAV, M4A, FLAC, OGG
- Convert to WAV (16kHz, mono) for Vosk if needed
- Use JAudioTagger for metadata
- Use Apache Tika as fallback

#### Metadata Extraction
```java
public AudioMetadata extractMetadata(MultipartFile file) {
    // Use JAudioTagger for MP3
    // Use Tika for other formats
    // Extract: duration, size, format, sample rate
}
```

### Transcription Result Storage

#### Options:
1. **Store in Database** (Recommended)
   - Add `transcription_text` column to `transcription_requests`
   - Add `transcription_json` for structured data (timestamps, speakers)
   - Store as TEXT/JSONB

2. **Store in File System**
   - Store transcription files
   - Reference in database

3. **Return Directly**
   - Don't store, return in response
   - Less persistent but simpler

**Decision**: Store in database for persistence and retrieval

---

## Configuration

### Application Properties Additions

```properties
# Transcription Engine Configuration
transcription.engine.type=${TRANSCRIPTION_ENGINE_TYPE:vosk}
transcription.engine.vosk.models.path=${VOSK_MODELS_PATH:./models}
transcription.engine.vosk.sample-rate=16000
transcription.engine.external.service.url=${EXTERNAL_TRANSCRIPTION_SERVICE_URL:}
transcription.engine.external.service.enabled=${EXTERNAL_TRANSCRIPTION_ENABLED:false}

# Audio Processing
audio.processing.temp.dir=${AUDIO_TEMP_DIR:/tmp/audio}
audio.processing.max-file-size=500MB
audio.processing.supported-formats=mp3,wav,m4a,flac,ogg

# Transcription Settings
transcription.max-duration-seconds=3600
transcription.min-duration-seconds=1
transcription.chunk-size-seconds=300
```

---

## Dependencies to Add

### Build Configuration (Maven)

```xml
<dependencies>
    <!-- Existing Spring Boot dependencies -->
    
    <!-- Vosk for transcription -->
    <dependency>
        <groupId>com.alphacephei</groupId>
        <artifactId>vosk</artifactId>
        <version>0.3.45</version>
    </dependency>
    
    <!-- Audio metadata extraction -->
    <dependency>
        <groupId>org.jaudiotagger</groupId>
        <artifactId>jaudiotagger</artifactId>
        <version>3.0.1</version>
    </dependency>
    
    <!-- Apache Tika for file metadata -->
    <dependency>
        <groupId>org.apache.tika</groupId>
        <artifactId>tika-core</artifactId>
        <version>2.9.1</version>
    </dependency>
    
    <!-- Audio format conversion (if needed) -->
    <dependency>
        <groupId>org.tritonus</groupId>
        <artifactId>tritonus-share</artifactId>
        <version>0.3.7-2</version>
    </dependency>
</dependencies>
```

---

## Database Schema Updates

### Add Transcription Result Columns

```sql
ALTER TABLE transcription_requests 
ADD COLUMN transcription_text TEXT,
ADD COLUMN transcription_json JSONB,
ADD COLUMN transcription_metadata JSONB;
```

---

## Success Criteria

### Functional
- ✅ Audio files can be uploaded and transcribed
- ✅ Transcription results are returned in API responses
- ✅ Multiple languages supported
- ✅ Speaker diarization works (1-5 speakers)
- ✅ All API endpoints functional
- ✅ Rate limiting working
- ✅ Status tracking working

### Non-Functional
- ✅ Transcription accuracy acceptable
- ✅ Response times within targets
- ✅ Error handling comprehensive
- ✅ Tests coverage > 80%

---

## Risk Mitigation

### Technical Risks

1. **Vosk Model Size**
   - Risk: Large model files
   - Mitigation: Use smaller models for common languages, lazy loading

2. **Transcription Performance**
   - Risk: Slow transcription for long files
   - Mitigation: Implement async processing, chunking, progress tracking

3. **Audio Format Compatibility**
   - Risk: Some formats not supported
   - Mitigation: Format conversion, clear error messages

4. **Memory Usage**
   - Risk: Large audio files consume memory
   - Mitigation: Streaming, temporary files, chunk processing

---

## Next Steps

1. **Begin Phase 1**: Set up dependencies and database
2. **Download Vosk Models**: Get language models for supported languages
3. **Implement Metadata Extraction**: Start with audio file analysis
4. **Integrate Vosk**: Set up basic transcription functionality
5. **Test with Sample Files**: Verify transcription works

---

**End of Implementation Plan**
