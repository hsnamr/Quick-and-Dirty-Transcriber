# Audio To Text Service - Implementation Plan

## Document Information
- **Service Name**: Audio To Text Service (AI SUITE)
- **Technology Stack**: Java 17+ with Spring Boot 3.x
- **Document Version**: 1.0
- **Date**: January 27, 2026

---

## Table of Contents
1. [Project Setup](#1-project-setup)
2. [Database Setup](#2-database-setup)
3. [Core Domain Models](#3-core-domain-models)
4. [Security & Authentication](#4-security--authentication)
5. [Business Logic Layer](#5-business-logic-layer)
6. [REST API Layer](#6-rest-api-layer)
7. [External Integrations](#7-external-integrations)
8. [Error Handling](#8-error-handling)
9. [Testing Strategy](#9-testing-strategy)
10. [Deployment & Configuration](#10-deployment--configuration)
11. [Implementation Phases](#11-implementation-phases)

---

## 1. Project Setup

### 1.1 Initialize Spring Boot Project

**Tools & Dependencies**:
- **Build Tool**: Maven or Gradle (recommend Gradle for modern projects)
- **Java Version**: 17 or higher
- **Spring Boot Version**: 3.2.x or 3.3.x

**Required Dependencies**:
```xml
<!-- Spring Boot Starters -->
- spring-boot-starter-web (REST API)
- spring-boot-starter-data-jpa (Database)
- spring-boot-starter-validation (Input validation)
- spring-kafka (Kafka)
- spring-boot-starter-mail (Email)
- spring-boot-starter-security (Security & JWT)
- spring-boot-starter-actuator (Health checks & metrics)

<!-- Database -->
- postgresql (PostgreSQL driver) OR mysql-connector-java (MySQL driver)
- flyway-core OR liquibase-core (Database migrations)

<!-- JWT -->
- jjwt-api, jjwt-impl, jjwt-jackson (JWT handling)

<!-- Audio Metadata -->
- jmediainfo (Audio metadata extraction) OR
- tika-core (Apache Tika for file metadata)

<!-- Utilities -->
- lombok (Optional, reduces boilerplate)
- mapstruct (Optional, for DTO mapping)
- jackson-databind (JSON processing)

<!-- Testing -->
- spring-boot-starter-test
- testcontainers (Integration tests)
- mockito, junit-jupiter
```

**Project Structure**:
```
src/
├── main/
│   ├── java/
│   │   └── com/example/audiototext/
│   │       ├── AudioToTextApplication.java
│   │       ├── config/
│   │       ├── controller/
│   │       ├── service/
│   │       ├── repository/
│   │       ├── model/
│   │       ├── dto/
│   │       ├── exception/
│   │       ├── security/
│   │       └── util/
│   └── resources/
│       ├── application.properties
│       ├── application-dev.properties
│       ├── application-prod.properties
│       └── db/migration/
└── test/
    └── java/
```

### 1.2 Configuration Files

**application.properties**:
- Server port and context path
- Database connection
- RabbitMQ connection
- External service URLs
- File upload limits
- JWT configuration

**Environment-specific properties**:
- `application-dev.properties` (Development)
- `application-prod.properties` (Production)

---

## 2. Database Setup

### 2.1 Database Schema Implementation

**Technology**: Flyway or Liquibase for migrations

**Migration Files**:
1. `V1__Create_transcription_languages.sql`
   - Create `transcription_languages` table
   - Create indexes
   - Insert default languages (auto, en, ar, es, fr, de, etc.)
   - Create update timestamp trigger

2. `V2__Create_transcription_requests.sql`
   - Create `transcription_requests` table
   - Create foreign key to `transcription_languages`
   - Create indexes (user_id, consumer_id+status, consumer_id+category, etc.)
   - Create update timestamp trigger
   - Add CHECK constraints for status, category, speakers_count, duration

3. `V3__Add_additional_indexes.sql` (if needed)
   - Optimize query performance based on usage patterns

### 2.2 Entity Models

**TranscriptionLanguage Entity**:
- JPA entity mapping
- Enum for active/popular flags
- Timestamp auditing (@CreatedDate, @LastModifiedDate)

**TranscriptionRequest Entity**:
- JPA entity mapping
- Enums for Status and Category
- Relationships (ManyToOne to TranscriptionLanguage)
- Validation annotations
- Timestamp auditing

### 2.3 Repository Layer

**TranscriptionLanguageRepository**:
- Find by code
- Find active languages
- Find popular languages

**TranscriptionRequestRepository**:
- Custom query methods for filtering
- Pagination support
- Search by file name or ID
- Filter by status, category, language, date range
- Complex sorting queries

---

## 3. Core Domain Models

### 3.1 Enums

**Status Enum**:
```java
PROCESSING(0, "processing", "Processing"),
COMPLETED(1, "completed", "Completed"),
FAILED(2, "failed", "Failed")
```

**Category Enum**:
```java
MEETING(0, "meeting", "Meeting"),
INTERVIEW(1, "interview", "Interview"),
CUSTOMER_SUPPORT_CALL(2, "customer_support_call", "Customer Support Call"),
SALES_CALL(3, "sales_call", "Sales Call"),
TRAINING_SESSION(4, "training_session", "Training Session"),
PODCAST(5, "podcast", "Podcast"),
PRESENTATION(6, "presentation", "Presentation"),
VOICE_NOTE(7, "voice_note", "Voice Note"),
OTHER(8, "other", "Other")
```

### 3.2 DTOs (Data Transfer Objects)

**Request DTOs**:
- `CreateTranscriptionRequestDTO`: Multipart file upload with validation
- `UpdateTranscriptionRequestDTO`: Category update only

**Response DTOs**:
- `TranscriptionRequestDTO`: Single request response (JSON:API format)
- `TranscriptionRequestListDTO`: List response with pagination, filters, overview
- `LanguageDTO`: Language information
- `FilterOptionsDTO`: Available filter options
- `SortingOptionDTO`: Available sorting options
- `PaginationDTO`: Pagination metadata
- `OverviewDTO`: Statistics overview

**DTO Mapping**:
- Use MapStruct or manual mapping
- Convert entities to DTOs with computed fields
- Format dates, durations, status display names

---

## 4. Security & Authentication

### 4.1 JWT Authentication

**JWT Filter**:
- Extract token from `Authorization: Bearer <token>` or `auth-token` header
- Validate token signature using public key
- Validate token expiration
- Extract user context (user_id, consumer_id, permissions)
- Set authentication in SecurityContext

**JWT Configuration**:
- Public key from environment variable
- Issuer validation
- Token validation service

### 4.2 Authorization

**Permission Model**:
- Product: `AI_SUITE`
- Feature: `AUDIO_TRANSCRIPTION`
- Permissions: `VIEW_AUDIO_TRANSCRIPTION`, `SUBMIT_AUDIO_TRANSCRIPTION`, `DELETE_AUDIO_TRANSCRIPTION`

**Permission Evaluator**:
- Custom `PermissionEvaluator` implementation
- Check user permissions for each operation
- Verify user belongs to company
- Verify company has active subscription

**Security Configuration**:
- Configure Spring Security
- JWT authentication filter
- Method security for permission checks
- CORS configuration (if needed)

### 4.3 User Context

**UserContext Service**:
- Thread-local storage for current user
- Extract from JWT token
- Provide user_id, company_id, permissions

---

## 5. Business Logic Layer

### 5.1 TranscriptionRequestService

**Core Methods**:
- `createTranscriptionRequest()`: Main creation flow
- `getTranscriptionRequest()`: Get single request
- `listTranscriptionRequests()`: List with filters, pagination, sorting
- `updateTranscriptionRequest()`: Update category
- `deleteTranscriptionRequest()`: Delete request

**Business Logic**:
1. **Create Flow**:
   - Authenticate and authorize
   - Extract audio metadata (duration, file name, size, format)
   - Check rate limiting (enforced by filter)
   - Resolve language (default to "auto" if not provided)
   - Create transcription request record (status: PROCESSING)
   - Forward to external transcription service
   - Handle success/failure responses
   - Send email notifications
   - Return response

2. **List Flow**:
   - Build dynamic query based on filters
   - Apply pagination
   - Apply sorting
   - Calculate overview statistics
   - Build filter options
   - Return formatted response

3. **Get Flow**:
   - Verify request belongs to company
   - Check status (only COMPLETED can be viewed)
   - Prepare Kafka topic information
   - Send broadcast message
   - Return response

4. **Update Flow**:
   - Validate category
   - Update record
   - Return response

5. **Delete Flow**:
   - Verify status (only COMPLETED or FAILED can be deleted)
   - Delete record
   - Return response

### 5.2 MetadataExtractionService

**Purpose**: Extract audio file metadata

**Implementation**:
- Use JMediaInfo library or Apache Tika
- Extract duration (seconds, decimal precision)
- Extract file name, size, MIME type
- Validate audio format
- Handle errors gracefully

**Supported Formats**:
- MP3, WAV, M4A, FLAC, OGG, etc.

**Error Handling**:
- If extraction fails, throw validation exception
- Log errors for debugging

### 5.3 Rate Limiting

**Purpose**: Protect APIs from overuse by limiting requests per time window

**Implementation**:
- Uses Bucket4j library (same library used by Spring Cloud Gateway)
- Token bucket algorithm with configurable refill rate and burst capacity
- Applied via filter early in request processing chain

**Configuration**:
- `rate.limit.requests-per-minute`: Requests allowed per minute (default: 10)
- `rate.limit.burst-capacity`: Maximum burst capacity (default: 20)

**Behavior**:
- Allows up to `burst-capacity` requests immediately
- Refills at `requests-per-minute` rate
- Health check endpoints excluded from rate limiting
- Returns HTTP 429 (Too Many Requests) when limit exceeded

### 5.4 LanguageService

**Purpose**: Manage transcription languages

**Methods**:
- `findByCode()`: Find language by code
- `findByName()`: Find language by name
- `getDefaultLanguage()`: Get "auto" language
- `resolveLanguage()`: Resolve language from request parameter

**Language Resolution Logic**:
- If code provided: find by code, else find by name, else default to "auto"
- If not provided: default to "auto"
- Validate language exists and is active

### 5.5 StatusManagementService

**Purpose**: Manage transcription request status

**Status Transitions**:
- PROCESSING → COMPLETED
- PROCESSING → FAILED

**Status Change Actions**:
- On COMPLETED: Send completion email
- On FAILED: Restore quota, send failure email

**Status Update**:
- Handle callback from external service
- Update status atomically
- Trigger side effects (email, quota restoration)

---

## 6. REST API Layer

### 6.1 TranscriptionRequestController

**Endpoints**:

1. **POST** `/api/v1/audio-to-text`
   - Create transcription request
   - Multipart file upload
   - Request validation
   - Return 201 Created

2. **GET** `/api/v1/audio-to-text`
   - List transcription requests
   - Query parameters for filtering, pagination, sorting
   - Return 200 OK with list DTO

3. **GET** `/api/v1/audio-to-text/{id}`
   - Get transcription request details
   - Only COMPLETED can be viewed
   - Return 200 OK with request DTO and queue info

4. **PUT/PATCH** `/api/v1/audio-to-text/{id}`
   - Update transcription request category
   - Return 200 OK

5. **DELETE** `/api/v1/audio-to-text/{id}`
   - Delete transcription request
   - Only COMPLETED or FAILED can be deleted
   - Return 200 OK

**Controller Responsibilities**:
- Request validation (@Valid)
- Authentication/authorization checks
- Call service layer
- Convert to DTOs
- Handle exceptions
- Return appropriate HTTP status codes

### 6.2 Request Validation

**Bean Validation**:
- `@NotNull`, `@NotBlank` for required fields
- `@Min`, `@Max` for numeric ranges
- `@Pattern` for format validation
- Custom validators for complex rules

**Custom Validators**:
- Audio file validator (format, size, duration)
- Category validator
- Language code validator

### 6.3 Response Formatting

**JSON:API Format**:
- Use JSON:API structure for responses
- Include relationships
- Include metadata (pagination, filters, etc.)

**Error Responses**:
- Consistent error format
- Include error message, details, timestamp, path

---

## 7. External Integrations

### 7.1 Transcription Service Client

**Purpose**: Forward audio files to external Python transcription service

**Implementation**:
- Use Spring `WebClient` (reactive) or `RestTemplate` (blocking)
- Multipart file upload
- Configure timeout
- Handle streaming for large files
- Retry logic for transient failures

**Request Parameters**:
- audio_file (multipart)
- speakers_count
- language
- category
- request_id
- consumer_id
- user_id

**Response Handling**:
- Success (2xx): Request accepted
- Failure (4xx/5xx): Mark as failed

**Error Handling**:
- Connection timeout
- Service unavailable (503)
- Invalid response

### 7.2 Kafka Integration

**Configuration**:
- KafkaTemplate for producers
- ConsumerFactory and ListenerContainerFactory for consumers
- JSON serialization/deserialization
- Producer and consumer configurations

**Topics**:
1. **Status Updates Topic**: `audio_text_request_updater`
   - Consumer for status updates from transcription service
   - Handle gzipped JSON messages (if needed)
   - Update transcription request status
   - Manual acknowledgment


2. **Frontend Broadcast Topic**: `frontend_audio_to_text_data`
   - Producer for real-time updates to frontend
   - Key-based partitioning (consumer_id + request_id)

**Message Consumers**:
- `TranscriptionStatusUpdateConsumer`: Handle callbacks
  - Decompress gzipped messages (if needed)
  - Parse JSON (handled by JsonDeserializer)
  - Extract request_id and status
  - Update transcription request
  - Manual acknowledgment

**Message Producers**:
- `MessageQueueService`: Send frontend broadcast messages
  - Use KafkaTemplate.send()
  - Handle send callbacks for success/failure
  - Implement retry logic

**Error Handling**:
- Dead letter topic for failed messages
- Retry mechanism with exponential backoff
- Manual acknowledgment for at-least-once delivery
- Error callbacks for failed sends

### 7.3 Email Service

**Purpose**: Send email notifications

**Email Types**:
1. Request Sent (on submission)
2. Transcription Completed
3. Transcription Failed

**Implementation**:
- Use Spring Mail (SMTP)
- Async processing (@Async)
- Template engine (Thymeleaf or FreeMarker)
- Multi-language support (English, Arabic)

**Email Templates**:
- `fd_transcription_loading_en.html`
- `fd_transcription_loading_ar.html`
- `fd_transcription_complete_en.html`
- `fd_transcription_complete_ar.html`
- `fd_transcription_error_en.html`
- `fd_transcription_error_ar.html`

**Email Content Variables**:
- User information
- Company information
- Transcription request details
- Timestamps
- Links

**Error Handling**:
- Don't fail request if email fails
- Log email failures
- Retry mechanism (optional)

### 7.4 Quota/Subscription Service Integration

**Options**:
1. **REST API**: Call external service to check/consume quota
2. **Message Queue**: Send messages, service handles quota
3. **Internal**: Manage quota in database

**Recommended**: Message Queue approach (Option 2) for decoupling

**If Using REST API**:
- `QuotaServiceClient`: HTTP client for quota operations
- Check quota endpoint
- Consume quota endpoint
- Restore quota endpoint

---

## 8. Error Handling

### 8.1 Global Exception Handler

**@ControllerAdvice**:
- Handle all exceptions globally
- Convert to appropriate HTTP status codes
- Format error responses consistently

**Exception Types**:
- `AuthenticationException` → 401
- `AuthorizationException` → 403
- `ValidationException` → 422
- `ResourceNotFoundException` → 404
- `RateLimitExceededException` → 429
- `ExternalServiceException` → 503
- `IllegalStateException` → 422
- Generic `Exception` → 500

### 8.2 Custom Exceptions

**Custom Exception Classes**:
- `TranscriptionRequestNotFoundException`
- `InvalidAudioFileException`
- `TranscriptionServiceException`
- `InvalidStatusTransitionException`

### 8.3 Error Response Format

**Standard Format**:
```json
{
  "error": "Error message",
  "details": ["Detail 1", "Detail 2"],
  "timestamp": "2026-01-27T10:00:00Z",
  "path": "/api/v1/audio-to-text"
}
```

### 8.4 Logging

**Structured Logging**:
- Use SLF4J with Logback
- JSON format for production
- Include correlation ID
- Log levels: ERROR, WARN, INFO, DEBUG

**Log Information**:
- Request/response logging
- External service calls
- Status changes
- Rate limiting operations
- Errors with stack traces

---

## 9. Testing Strategy

### 9.1 Unit Tests

**Service Layer**:
- TranscriptionRequestService (mock dependencies)
- Rate limiting (test with different limits)
- MetadataExtractionService (test with sample files)
- LanguageService
- StatusManagementService

**Repository Layer**:
- Test custom query methods
- Test pagination and sorting
- Use @DataJpaTest

**Utility Classes**:
- DTO mappers
- Validators
- Formatters

### 9.2 Integration Tests

**API Tests**:
- Test all endpoints
- Test authentication/authorization
- Test validation
- Test error handling
- Use `@SpringBootTest` with `@AutoConfigureMockMvc`

**Database Tests**:
- Test entity relationships
- Test constraints
- Test transactions
- Use Testcontainers for real database

**External Service Tests**:
- Mock external transcription service (WireMock)
- Mock Kafka (Testcontainers)
- Mock email service

**Message Queue Tests**:
- Test message consumers
- Test message producers
- Use Testcontainers for Kafka

### 9.3 End-to-End Tests

**Complete Flows**:
- Create transcription request → receive callback → view result
- Create request → rate limit exceeded → error
- Create request → service failure → error handling
- Update category
- Delete request

### 9.4 Performance Tests

**Load Testing**:
- Use JMeter or Gatling
- Test concurrent requests
- Test file upload performance
- Test database query performance

**Targets**:
- List requests: < 500ms (p95)
- Get request: < 200ms (p95)
- Create request: < 2s (p95)

---

## 10. Deployment & Configuration

### 10.1 Configuration Management

**Environment Variables**:
- Database connection
- RabbitMQ connection
- External service URLs
- JWT public key
- Email SMTP settings
- File upload limits
- Rate limiting configuration

**Configuration Classes**:
- `DatabaseConfig`: DataSource, JPA settings
- `KafkaConfig`: Connection, topics, producers, consumers
- `SecurityConfig`: JWT, permissions
- `WebConfig`: CORS, file upload
- `EmailConfig`: SMTP settings

### 10.2 Health Checks

**Actuator Endpoints**:
- `/actuator/health`: Overall health
- `/actuator/health/readiness`: Readiness probe
- `/actuator/health/liveness`: Liveness probe
- `/actuator/metrics`: Application metrics
- `/actuator/info`: Application information

**Custom Health Indicators**:
- Database health
- Kafka health
- External transcription service health

### 10.3 Monitoring & Observability

**Metrics**:
- Request count, latency, error rate
- Rate limiting metrics
- External service health
- Database connection pool

**Tracing**:
- Correlation IDs
- Distributed tracing (if using microservices)

**Logging**:
- Structured JSON logs
- Centralized logging (ELK, Splunk, etc.)

### 10.4 Docker & Deployment

**Dockerfile**:
- Multi-stage build
- Optimize image size
- Use appropriate base image

**Docker Compose** (for local development):
- Application container
- PostgreSQL container
- Kafka container

**Deployment**:
- Kubernetes manifests (if using K8s)
- Environment-specific configurations
- Secrets management

---

## 11. Implementation Phases

### Phase 1: Foundation (Week 1-2)
- [ ] Project setup (Spring Boot, dependencies)
- [ ] Database schema (migrations, entities, repositories)
- [ ] Basic security (JWT authentication)
- [ ] Core domain models (entities, enums, DTOs)
- [ ] Basic service layer structure

### Phase 2: Core Features (Week 3-4)
- [ ] Metadata extraction service
- [ ] Language service
- [ ] Rate limiting (Bucket4j implementation)
- [ ] Transcription request service (CRUD operations)
- [ ] REST API endpoints (basic implementation)

### Phase 3: External Integrations (Week 5-6)
- [ ] Transcription service client
- [ ] RabbitMQ integration (consumer and producers)
- [ ] Email service

### Phase 4: Advanced Features (Week 7-8)
- [ ] Advanced filtering and sorting
- [ ] Pagination and overview statistics
- [ ] Status management and callbacks
- [ ] Frontend broadcast integration
- [ ] Error handling and validation

### Phase 5: Testing & Refinement (Week 9-10)
- [ ] Unit tests
- [ ] Integration tests
- [ ] End-to-end tests
- [ ] Performance testing
- [ ] Bug fixes and optimizations

### Phase 6: Documentation & Deployment (Week 11-12)
- [ ] API documentation (OpenAPI/Swagger)
- [ ] Deployment configuration
- [ ] Monitoring setup
- [ ] Health checks
- [ ] Final testing and deployment

---

## 12. Key Libraries & Frameworks

### Core Framework
- **Spring Boot 3.x**: Main framework
- **Spring Data JPA**: Database access
- **Spring Security**: Authentication & authorization
- **Spring Kafka**: Kafka integration
- **Spring Mail**: Email sending

### Database
- **PostgreSQL** or **MySQL**: Database
- **Flyway** or **Liquibase**: Migrations
- **HikariCP**: Connection pooling

### JWT
- **jjwt** (io.jsonwebtoken): JWT handling

### Audio Processing
- **JMediaInfo** (net.sourceforge.jmediainfo): Audio metadata extraction
- OR **Apache Tika**: File metadata extraction

### Utilities
- **Lombok**: Reduce boilerplate (optional)
- **MapStruct**: DTO mapping (optional)
- **Jackson**: JSON processing
- **Bean Validation**: Input validation

### Testing
- **JUnit 5**: Unit testing
- **Mockito**: Mocking
- **Testcontainers**: Integration testing
- **WireMock**: Mock external services
- **Spring Boot Test**: Integration tests

### Documentation
- **SpringDoc OpenAPI**: API documentation

### Build Tool
- **Maven** or **Gradle**: Build and dependency management

---

## 13. Open Questions & Decisions

### 13.1 Architecture Decisions

1. **User/Company Management**:
   - Decision: Assume external service (no users/companies tables in this service)
   - Implementation: Validate through JWT token claims
   - Action: Extract user_id and consumer_id from JWT

2. **Rate Limiting**:
   - Decision: Use Bucket4j library for rate limiting (same as Spring Cloud Gateway)
   - Implementation: Filter-based rate limiting with token bucket algorithm
   - Action: Implement RateLimitingFilter and RateLimitingConfig

3. **File Storage**:
   - Decision: Don't store files, forward directly to transcription service
   - Implementation: Stream file to external service
   - Action: Use streaming in TranscriptionServiceClient

4. **Transcription Results**:
   - Decision: Results stored in external service, this service only tracks requests
   - Implementation: Frontend retrieves results from external service via queue
   - Action: Implement frontend broadcast with queue information

5. **Authentication**:
   - Decision: Validate JWT locally using public key
   - Implementation: JWT filter with public key validation
   - Action: Configure JWT validation in SecurityConfig

6. **Email Templates**:
   - Decision: Use Thymeleaf templates in resources/templates
   - Implementation: Template engine with multi-language support
   - Action: Create email templates and EmailService

### 13.2 Implementation Notes

- Use transactions for quota operations to ensure atomicity
- Use async processing for email sending
- Cache language list (if frequently accessed)
- Implement retry logic for external service calls
- Use connection pooling for database and HTTP clients
- Implement circuit breaker pattern for external services (optional, using Resilience4j)

---

## 14. Success Criteria

### Functional Requirements
- ✅ All API endpoints implemented and working
- ✅ Authentication and authorization working
- ✅ Rate limiting working
- ✅ External service integration working
- ✅ Message queue integration working
- ✅ Email notifications working
- ✅ Status management working

### Non-Functional Requirements
- ✅ Response times meet targets
- ✅ Error handling comprehensive
- ✅ Logging and monitoring in place
- ✅ Health checks working
- ✅ Tests coverage > 80%
- ✅ Documentation complete

---

## 15. Risk Mitigation

### Technical Risks
1. **External Service Unavailability**: Implement retry logic and graceful degradation
2. **Message Queue Failures**: Use dead letter topics and monitoring
3. **Database Performance**: Optimize queries, use indexes, connection pooling
4. **File Upload Issues**: Implement streaming, size limits, timeout handling

### Business Risks
1. **Rate Limiting**: Configure appropriate limits based on expected load
2. **Data Consistency**: Use transactions appropriately
3. **Security Vulnerabilities**: Regular security audits, input validation

---

**End of Implementation Plan**
