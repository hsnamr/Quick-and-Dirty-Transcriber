package com.example.audiototext.service.impl;

import com.example.audiototext.dto.request.CreateTranscriptionRequestDTO;
import com.example.audiototext.dto.request.UpdateTranscriptionRequestDTO;
import com.example.audiototext.dto.response.TranscriptionRequestDTO;
import com.example.audiototext.dto.response.TranscriptionRequestListDTO;
import com.example.audiototext.exception.ResourceNotFoundException;
import com.example.audiototext.exception.ValidationException;
import com.example.audiototext.model.TranscriptionRequest;
import com.example.audiototext.model.TranscriptionLanguage;
import com.example.audiototext.model.enums.Category;
import com.example.audiototext.model.enums.Status;
import com.example.audiototext.repository.TranscriptionRequestRepository;
import com.example.audiototext.security.UserContext;
import com.example.audiototext.service.*;
import com.example.audiototext.util.AudioValidator;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class TranscriptionRequestServiceImpl implements TranscriptionRequestService {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionRequestServiceImpl.class);

    private final TranscriptionRequestRepository requestRepository;
    private final MongoTemplate mongoTemplate;
    private final MetadataExtractionService metadataExtractionService;
    private final LanguageService languageService;
    private final QuotaService quotaService;
    private final TranscriptionEngineService transcriptionEngineService;
    private final StatusManagementService statusManagementService;
    private final EmailService emailService;

    @Value("${transcription.engine.type:vosk}")
    private String transcriptionEngineType;

    @Autowired
    public TranscriptionRequestServiceImpl(
            TranscriptionRequestRepository requestRepository,
            MongoTemplate mongoTemplate,
            MetadataExtractionService metadataExtractionService,
            LanguageService languageService,
            QuotaService quotaService,
            TranscriptionEngineService transcriptionEngineService,
            StatusManagementService statusManagementService,
            EmailService emailService) {
        this.requestRepository = requestRepository;
        this.mongoTemplate = mongoTemplate;
        this.metadataExtractionService = metadataExtractionService;
        this.languageService = languageService;
        this.quotaService = quotaService;
        this.transcriptionEngineService = transcriptionEngineService;
        this.statusManagementService = statusManagementService;
        this.emailService = emailService;
    }

    @Override
    public TranscriptionRequestDTO createTranscriptionRequest(CreateTranscriptionRequestDTO requestDTO) {
        logger.info("Creating transcription request for file: {}", requestDTO.getAudioFile().getOriginalFilename());

        // Get user context (set by JWT filter)
        Long userId = UserContext.getUserId();

        if (userId == null) {
            throw new ValidationException("User context not available. Authentication required.");
        }

        // 1. Validate audio file
        MultipartFile audioFile = requestDTO.getAudioFile();
        AudioValidator.validate(audioFile);

        // 2. Extract metadata
        MetadataExtractionService.AudioMetadata metadata;
        try {
            metadata = metadataExtractionService.extractMetadata(audioFile);
        } catch (Exception e) {
            logger.error("Failed to extract audio metadata", e);
            throw new ValidationException("Failed to extract audio metadata: " + e.getMessage());
        }

        // Validate duration
        AudioValidator.validateDuration(
                metadata.getDurationSeconds(),
                null, // min will be checked by quota service
                null  // max will be checked by quota service
        );

        // 3. Validate quota
        try {
            quotaService.validateQuota(userId, metadata.getDurationSeconds());
        } catch (Exception e) {
            logger.warn("Quota validation failed for user {}: {}", userId, e.getMessage());
            throw e; // Re-throw quota exceptions
        }

        // 4. Resolve language
        TranscriptionLanguage language = languageService.resolveLanguage(requestDTO.getLanguage());

        // 5. Parse category
        Category category;
        try {
            category = Category.fromCode(requestDTO.getCategory());
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid category: " + requestDTO.getCategory());
        }

        // 6. Create transcription request record
        TranscriptionRequest request = new TranscriptionRequest();
        
        // Generate numeric ID for API compatibility
        Long numericId = sequenceService.getNextSequence("transcription_request_sequence");
        request.setNumericId(numericId);
        
        request.setFileName(metadata.getFileName());
        request.setLanguage(language);
        request.setSpeakersCount(requestDTO.getSpeakersCount());
        request.setDurationSecs(BigDecimal.valueOf(metadata.getDurationSeconds()));
        request.setStatus(Status.PROCESSING);
        request.setCategory(category);
        request.setUserId(userId);
        request.setQuotaConsumed(false);

        // Save to get MongoDB ID
        request = requestRepository.save(request);
        logger.info("Created transcription request with ID: {} (numericId: {})", request.getId(), request.getNumericId());

        // 7. Consume quota (with transaction)
        try {
            quotaService.consumeQuota(userId, request.getNumericId(), metadata.getDurationSeconds());
        } catch (Exception e) {
            // If quota consumption fails, mark request as failed
            logger.error("Quota consumption failed for request {}", request.getId(), e);
            request.setStatus(Status.FAILED);
            requestRepository.save(request);
            throw new ValidationException("Failed to consume quota: " + e.getMessage());
        }

        // 8. Process transcription (async or sync based on configuration)
        try {
            // Process transcription using Vosk
            TranscriptionEngineService.TranscriptionResult result = transcriptionEngineService.transcribe(
                    audioFile,
                    language.getCode(),
                    requestDTO.getSpeakersCount()
            );

            // Update request with transcription results
            request.setTranscriptionText(result.getText());
            request.setTranscriptionJson(result.getJsonResult());
            request.setTranscriptionMetadata(result.getMetadata());
            request.setStatus(Status.COMPLETED);
            requestRepository.save(request);

            logger.info("Transcription completed for request {}", request.getId());

            // Send completion email
            try {
                emailService.sendTranscriptionCompletedEmail(request);
            } catch (Exception e) {
                logger.error("Failed to send completion email", e);
                // Don't fail the request if email fails
            }

        } catch (Exception e) {
            logger.error("Transcription processing failed for request {}", request.getId(), e);
            
            // Mark as failed and restore quota
            request.setStatus(Status.FAILED);
            requestRepository.save(request);
            
            statusManagementService.handleStatusChangeToFailed(request, e.getMessage());
            
            throw new RuntimeException("Transcription failed: " + e.getMessage(), e);
        }

        // 9. Send request sent email (if processing asynchronously, this would be sent immediately)
        try {
            emailService.sendRequestSentEmail(request);
        } catch (Exception e) {
            logger.error("Failed to send request sent email", e);
            // Don't fail the request if email fails
        }

        // 10. Convert to DTO and return
        return convertToDTO(request);
    }

    @Override
    @Transactional(readOnly = true)
    public TranscriptionRequestListDTO listTranscriptionRequests(
            Integer limitPerPage, Integer page, String search, String status,
            String category, Long languageId, Long startDate, Long endDate,
            String sortBy, String orderBy) {
        
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ValidationException("User context not available");
        }

        // Set defaults
        int limit = (limitPerPage != null && limitPerPage > 0 && limitPerPage <= 100) ? limitPerPage : 10;
        int pageNum = (page != null && page > 0) ? page - 1 : 0; // Page is 0-indexed

        // Build MongoDB query for filtering
        Query query = buildMongoQuery(userId, search, status, category, languageId, startDate, endDate);

        // Build sorting
        Sort sort = buildSort(sortBy, orderBy);
        query.with(sort);
        query.with(PageRequest.of(pageNum, limit));

        // Execute query
        long total = mongoTemplate.count(query, TranscriptionRequest.class);
        List<TranscriptionRequest> content = mongoTemplate.find(query, TranscriptionRequest.class);
        
        // Create Page manually
        Page<TranscriptionRequest> pageResult = new org.springframework.data.domain.PageImpl<>(
                content, PageRequest.of(pageNum, limit), total);

        // Build response
        TranscriptionRequestListDTO response = new TranscriptionRequestListDTO();
        
        // Convert to DTOs
        List<TranscriptionRequestDTO> dtos = pageResult.getContent().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
        response.setData(dtos);

        // Build pagination
        TranscriptionRequestListDTO.PaginationDTO pagination = new TranscriptionRequestListDTO.PaginationDTO();
        pagination.setPage(pageNum + 1);
        pagination.setPerPage(limit);
        pagination.setTotal(pageResult.getTotalElements());
        pagination.setTotalPages(pageResult.getTotalPages());
        response.setPagination(pagination);

        // Build overview (simplified - would need aggregation query in production)
        TranscriptionRequestListDTO.OverviewDTO overview = new TranscriptionRequestListDTO.OverviewDTO();
        overview.setTotal(pageResult.getTotalElements());
        // TODO: Calculate completed, processing, failed counts and used quota
        response.setOverview(overview);

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public TranscriptionRequestDTO getTranscriptionRequest(Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ValidationException("User context not available");
        }

        // Find by numericId for API compatibility
        TranscriptionRequest request = requestRepository.findByNumericIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcription request not found: " + id));

        // Only completed transcriptions can be viewed
        if (request.getStatus() != Status.COMPLETED) {
            throw new ValidationException(
                    "Transcription is not completed yet. Only completed transcriptions can be viewed."
            );
        }

        return convertToDTO(request);
    }

    @Override
    public TranscriptionRequestDTO updateTranscriptionRequest(Long id, UpdateTranscriptionRequestDTO requestDTO) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ValidationException("User context not available");
        }

        // Find by numericId for API compatibility
        TranscriptionRequest request = requestRepository.findByNumericIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcription request not found: " + id));

        // Parse and update category
        try {
            Category category = Category.fromCode(requestDTO.getCategory());
            request.setCategory(category);
            request = requestRepository.save(request);
        } catch (IllegalArgumentException e) {
            throw new ValidationException("Invalid category: " + requestDTO.getCategory());
        }

        return convertToDTO(request);
    }

    @Override
    public void deleteTranscriptionRequest(Long id) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new ValidationException("User context not available");
        }

        // Find by numericId for API compatibility
        TranscriptionRequest request = requestRepository.findByNumericIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Transcription request not found: " + id));

        // Only completed or failed requests can be deleted
        if (request.getStatus() == Status.PROCESSING) {
            throw new ValidationException(
                    "Transcription cannot be deleted while processing. Only completed or failed transcriptions can be deleted."
            );
        }

        requestRepository.delete(request);
        logger.info("Deleted transcription request: {}", id);
    }

    // Helper methods

    private Query buildMongoQuery(
            Long consumerId, String search, String status, String category,
            Long languageId, Long startDate, Long endDate) {
        
        Query query = new Query();
        
        // Consumer filter (required)
        query.addCriteria(Criteria.where("consumerId").is(consumerId));

        // Search filter
        if (search != null && !search.isEmpty()) {
            // Try to parse as numeric ID first
            try {
                Long searchId = Long.parseLong(search);
                Criteria searchCriteria = new Criteria().orOperator(
                        Criteria.where("fileName").regex(search, "i"),
                        Criteria.where("numericId").is(searchId),
                        Criteria.where("id").is(search)
                );
                query.addCriteria(searchCriteria);
            } catch (NumberFormatException e) {
                // Not a number, search by fileName or MongoDB id
                Criteria searchCriteria = new Criteria().orOperator(
                        Criteria.where("fileName").regex(search, "i"),
                        Criteria.where("id").is(search)
                );
                query.addCriteria(searchCriteria);
            }
        }

        // Status filter
        if (status != null && !status.isEmpty()) {
            try {
                Status statusEnum = Status.fromCode(status);
                query.addCriteria(Criteria.where("status").is(statusEnum));
            } catch (IllegalArgumentException e) {
                // Ignore invalid status
            }
        }

        // Category filter
        if (category != null && !category.isEmpty()) {
            try {
                Category categoryEnum = Category.fromCode(category);
                query.addCriteria(Criteria.where("category").is(categoryEnum));
            } catch (IllegalArgumentException e) {
                // Ignore invalid category
            }
        }

        // Language filter
        if (languageId != null) {
            // DBRef stores language reference, query by language id
            query.addCriteria(Criteria.where("language.$id").is(languageId.toString()));
            // Alternative: if language is embedded, use: Criteria.where("language.id").is(languageId.toString())
        }

        // Date range filter
        if (startDate != null) {
            LocalDateTime start = LocalDateTime.ofInstant(Instant.ofEpochSecond(startDate), ZoneId.systemDefault());
            query.addCriteria(Criteria.where("createdAt").gte(start));
        }
        if (endDate != null) {
            LocalDateTime end = LocalDateTime.ofInstant(Instant.ofEpochSecond(endDate), ZoneId.systemDefault());
            query.addCriteria(Criteria.where("createdAt").lte(end));
        }

        return query;
    }

    private Sort buildSort(String sortBy, String orderBy) {
        Sort.Direction direction = "desc".equalsIgnoreCase(orderBy) ? Sort.Direction.DESC : Sort.Direction.ASC;

        if (sortBy == null || sortBy.isEmpty()) {
            return Sort.by(direction, "createdAt");
        }

        // Map sort fields
        switch (sortBy.toLowerCase()) {
            case "id":
                return Sort.by(direction, "id");
            case "file_name":
                return Sort.by(direction, "fileName");
            case "created_at":
                return Sort.by(direction, "createdAt");
            case "category":
                return Sort.by(direction, "category");
            case "duration_secs":
            case "usedquota":
                return Sort.by(direction, "durationSecs");
            case "status":
                return Sort.by(direction, "status");
            default:
                return Sort.by(direction, "createdAt");
        }
    }

    private TranscriptionRequestDTO convertToDTO(TranscriptionRequest request) {
        TranscriptionRequestDTO dto = new TranscriptionRequestDTO();
        // Use numericId as the main ID for API compatibility
        dto.setId(request.getNumericId() != null ? request.getNumericId().toString() : request.getId());
        
        TranscriptionRequestDTO.TranscriptionRequestAttributes attributes = 
                new TranscriptionRequestDTO.TranscriptionRequestAttributes();
        // Convert MongoDB ObjectId (String) to Long for DTO
        // MongoDB ObjectIds are 24-char hex strings, so we'll use hash code or extract numeric part
        try {
            // Try to parse as Long first (in case we're using numeric IDs)
            attributes.setId(Long.parseLong(request.getId()));
        } catch (NumberFormatException e) {
            // If it's an ObjectId (24-char hex), use hash code to get a numeric value
            // This is a bit messy but works for compatibility
            long numericId = Math.abs(request.getId().hashCode());
            attributes.setId(numericId);
        }
        attributes.setFileName(request.getFileName());
        attributes.setSpeakersCount(request.getSpeakersCount());
        attributes.setDurationSecs(request.getDurationSecs().doubleValue());
        attributes.setDurationFormatted(formatDuration(request.getDurationSecs()));
        attributes.setStatus(request.getStatus().getCode());
        attributes.setStatusDisplay(request.getStatus().getDisplayName());
        attributes.setCategory(request.getCategory().getCode());
        attributes.setCategoryName(request.getCategory().getDisplayName());
        attributes.setCategoryKey(request.getCategory().getCode());
        attributes.setLanguageName(request.getLanguage().getName());
        attributes.setLanguageCode(request.getLanguage().getCode());
        attributes.setLanguageDisplayName(request.getLanguage().getName() + " (" + request.getLanguage().getCode() + ")");
        attributes.setAutoDetectLanguage("auto".equals(request.getLanguage().getCode()));
        attributes.setCompanyId(request.getUserId());
        attributes.setIsProcessing(request.getStatus() == Status.PROCESSING);
        attributes.setIsCompleted(request.getStatus() == Status.COMPLETED);
        attributes.setIsFailed(request.getStatus() == Status.FAILED);
        attributes.setCanBeDeleted(request.getStatus() != Status.PROCESSING);
        attributes.setCanBeViewed(request.getStatus() == Status.COMPLETED);
        attributes.setCanBeUpdated(true);
        
        // File extension
        String fileName = request.getFileName();
        if (fileName != null && fileName.contains(".")) {
            attributes.setFileExtension(fileName.substring(fileName.lastIndexOf('.')));
        }
        
        // Timestamps
        attributes.setCreatedAt(request.getCreatedAt());
        attributes.setCreatedAtFormatted(formatDateTime(request.getCreatedAt()));
        attributes.setCreatedAtUnix(request.getCreatedAt() != null ? 
                request.getCreatedAt().atZone(ZoneId.systemDefault()).toEpochSecond() : null);
        attributes.setUpdatedAt(request.getUpdatedAt());
        attributes.setUpdatedAtFormatted(formatDateTime(request.getUpdatedAt()));
        attributes.setUpdatedAtUnix(request.getUpdatedAt() != null ? 
                request.getUpdatedAt().atZone(ZoneId.systemDefault()).toEpochSecond() : null);
        
        // Processing time
        if (request.getCreatedAt() != null && request.getUpdatedAt() != null) {
            long seconds = java.time.Duration.between(request.getCreatedAt(), request.getUpdatedAt()).getSeconds();
            attributes.setProcessingTimeSeconds((double) seconds);
        }
        
        dto.setAttributes(attributes);
        
        // Relationships
        TranscriptionRequestDTO.TranscriptionRequestRelationships relationships = 
                new TranscriptionRequestDTO.TranscriptionRequestRelationships();
        
        TranscriptionRequestDTO.RelationshipData languageRel = new TranscriptionRequestDTO.RelationshipData();
        languageRel.setId(request.getLanguage().getId());
        languageRel.setType("transcription_language");
        relationships.setLanguage(languageRel);
        
        TranscriptionRequestDTO.RelationshipData userRel = new TranscriptionRequestDTO.RelationshipData();
        userRel.setId(String.valueOf(request.getUserId()));
        userRel.setType("user");
        relationships.setUser(userRel);
        
        dto.setRelationships(relationships);
        
        return dto;
    }

    private String formatDuration(BigDecimal durationSeconds) {
        if (durationSeconds == null) {
            return "00:00";
        }
        long totalSeconds = durationSeconds.longValue();
        long hours = totalSeconds / 3600;
        long minutes = (totalSeconds % 3600) / 60;
        long seconds = totalSeconds % 60;

        if (hours > 0) {
            return String.format("%02d:%02d:%02d", hours, minutes, seconds);
        } else {
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.toString().replace("T", " ").substring(0, 19);
    }
}
