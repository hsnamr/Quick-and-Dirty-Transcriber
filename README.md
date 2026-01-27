# Free and Dirty Transcriber

REST API service for transcribing audio files to text using free and open-source libraries.

## Overview

This service provides a complete REST API for audio transcription with the following features:
- Audio file upload and validation
- Multi-language support with auto-detection
- Speaker diarization
- Transcription using Vosk (free, open-source speech recognition)
- Quota management
- Status tracking
- Email notifications
- Kafka integration for real-time updates

## Technology Stack

- **Java 17+**
- **Spring Boot 3.2.x**
- **MongoDB** (Database)
- **Kafka** (Message Queue)
- **Vosk** (Speech Recognition - Apache 2.0 License)
- **JAudioTagger** (Audio Metadata)
- **Apache Tika** (File Metadata)

## Quick Start

### Prerequisites

- Java 17 or higher
- Maven 3.6+
- MongoDB 4.4+ (or use MongoDB Atlas)
- Kafka (for message queue features)

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd Free-and-Dirty-Transcriber
   ```

2. **Download Vosk Models**
   
   Download language models from: https://alphacephei.com/vosk/models
   
   Recommended models:
   - English: `vosk-model-en-us-0.22` (1.8GB)
   - Arabic: `vosk-model-ar-0.22` (1.5GB)
   - Spanish: `vosk-model-es-0.42` (1.5GB)
   
   Extract models to: `src/main/resources/models/`
   
   Example structure:
   ```
   src/main/resources/models/
   ├── vosk-model-en-us-0.22/
   ├── vosk-model-ar-0.22/
   └── vosk-model-es-0.42/
   ```

3. **Configure MongoDB**
   
   Update `src/main/resources/application.properties`:
   ```properties
   spring.data.mongodb.uri=mongodb://localhost:27017/free_and_dirty_transcriber
   spring.data.mongodb.database=free_and_dirty_transcriber
   ```
   
   Or use MongoDB Atlas (cloud):
   ```properties
   spring.data.mongodb.uri=mongodb+srv://username:password@cluster.mongodb.net/free_and_dirty_transcriber
   ```

4. **Configure Kafka** (optional, for production)
   ```properties
   spring.kafka.bootstrap-servers=localhost:9092
   ```

5. **Build and Run**
   ```bash
   mvn clean install
   mvn spring-boot:run
   ```

## API Endpoints

### Base URL
```
/api/free-and-dirty-transcriber
```

### API Versioning
API versioning is handled via the `API-Version` header:
- **Header**: `API-Version: v1`
- If no header is provided, `v1` is used as default
- Currently only `v1` is supported

### Endpoints

1. **POST** `/api/free-and-dirty-transcriber` - Create transcription request
2. **GET** `/api/free-and-dirty-transcriber` - List transcription requests
3. **GET** `/api/free-and-dirty-transcriber/{id}` - Get transcription details
4. **PUT** `/api/free-and-dirty-transcriber/{id}` - Update transcription category
5. **DELETE** `/api/free-and-dirty-transcriber/{id}` - Delete transcription request

**Note**: All requests should include the `API-Version: v1` header.

## Configuration

### Environment Variables

- `MONGODB_URI` - MongoDB connection URI
- `KAFKA_BOOTSTRAP_SERVERS` - Kafka brokers
- `JWT_PUBLIC_KEY` - JWT public key for authentication
- `VOSK_MODELS_PATH` - Path to Vosk model files
- `TRANSCRIPTION_ENGINE_TYPE` - `vosk` or `external`

### Application Properties

Key configuration options:
- `transcription.engine.type` - Transcription engine (default: `vosk`)
- `transcription.engine.vosk.models.path` - Vosk models directory
- `audio.processing.max-file-size` - Maximum file size (default: 500MB)
- `transcription.max-duration-seconds` - Max audio duration (default: 3600s)

## Development

### Running Tests
```bash
mvn test
```

### Database Initialization
Default languages are automatically seeded on first startup via `MongoDataInitializer`.

## License

See LICENSE file for details.
