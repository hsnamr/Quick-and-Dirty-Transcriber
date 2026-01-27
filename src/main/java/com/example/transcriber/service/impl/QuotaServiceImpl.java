package com.example.transcriber.service.impl;

import com.example.transcriber.exception.QuotaExceededException;
import com.example.transcriber.model.TranscriptionRequest;
import com.example.transcriber.repository.TranscriptionRequestRepository;
import com.example.transcriber.service.MessageQueueService;
import com.example.transcriber.service.QuotaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;

@Service
@Transactional
public class QuotaServiceImpl implements QuotaService {

    private static final Logger logger = LoggerFactory.getLogger(QuotaServiceImpl.class);

    private final TranscriptionRequestRepository requestRepository;
    private final MessageQueueService messageQueueService;

    @Value("${audio.transcription.max-duration-seconds:3600}")
    private Double maxDurationSeconds;

    @Value("${audio.transcription.min-duration-seconds:1}")
    private Double minDurationSeconds;

    // TODO: This should ideally come from an external quota service or database
    // For now, we'll use a simple approach - send messages to Kafka and let external service handle it
    // If quota service exists, we could call it via REST API here
    @Value("${quota.product.id:1}")
    private Long productId; // FDTranscriber product ID

    public QuotaServiceImpl(TranscriptionRequestRepository requestRepository,
                           MessageQueueService messageQueueService) {
        this.requestRepository = requestRepository;
        this.messageQueueService = messageQueueService;
    }

    @Override
    public void validateQuota(Long userId, Double durationSeconds) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID is required");
        }

        if (durationSeconds == null || durationSeconds <= 0) {
            throw new IllegalArgumentException("Duration must be greater than 0");
        }

        // Validate duration limits
        if (durationSeconds < minDurationSeconds) {
            throw new QuotaExceededException(
                    String.format("Audio duration too short. Minimum: %.1f seconds", minDurationSeconds)
            );
        }

        if (durationSeconds > maxDurationSeconds) {
            throw new QuotaExceededException(
                    String.format("Audio duration too long. Maximum: %.1f seconds", maxDurationSeconds)
            );
        }

        // TODO: Check actual quota availability
        // This would typically call an external quota service or check database
        // For now, we'll assume quota is managed externally via Kafka messages
        // The external service will validate quota when it receives the consumption message
        
        logger.info("Quota validation passed for consumer {} with duration {} seconds", consumerId, durationSeconds);
    }

    @Override
    public void consumeQuota(Long consumerId, Long requestId, Double durationSeconds) {
        if (consumerId == null || requestId == null || durationSeconds == null) {
            throw new IllegalArgumentException("Consumer ID, request ID, and duration are required");
        }

        // Find the request by numericId
        Optional<TranscriptionRequest> requestOpt = requestRepository.findByNumericIdAndConsumerId(requestId, consumerId);
        if (requestOpt.isEmpty()) {
            throw new IllegalArgumentException("Transcription request not found: " + requestId);
        }

        TranscriptionRequest request = requestOpt.get();
        
        // Double-check consumer matches
        if (!request.getConsumerId().equals(consumerId)) {
            throw new IllegalArgumentException("Request does not belong to consumer: " + consumerId);
        }

        // Mark quota as consumed
        if (!request.getQuotaConsumed()) {
            request.setQuotaConsumed(true);
            requestRepository.save(request);
            logger.info("Marked quota as consumed for request {} (consumer {}, duration {}s)", 
                    requestId, consumerId, durationSeconds);
        }

        // Send quota consumption message to Kafka
        // The external quota service will consume this message and update quota
        Long count = durationSeconds.longValue();
        Long date = Instant.now().getEpochSecond();
        
        try {
            messageQueueService.sendQuotaConsumptionMessage(consumerId, productId, count, date);
            logger.info("Sent quota consumption message: consumer={}, product={}, count={}", 
                    consumerId, productId, count);
        } catch (Exception e) {
            logger.error("Failed to send quota consumption message", e);
            // Don't fail the request if message sending fails - quota service might handle it differently
        }
    }

    @Override
    public void restoreQuota(Long userId, Long requestId, Double durationSeconds) {
        if (userId == null || requestId == null || durationSeconds == null) {
            throw new IllegalArgumentException("User ID, request ID, and duration are required");
        }

        // Find the request by numericId
        Optional<TranscriptionRequest> requestOpt = requestRepository.findByNumericIdAndUserId(requestId, userId);
        if (requestOpt.isEmpty()) {
            logger.warn("Transcription request not found for quota restoration: {}", requestId);
            return;
        }

        TranscriptionRequest request = requestOpt.get();
        
        // Only restore if quota was actually consumed
        if (!request.getQuotaConsumed()) {
            logger.info("Quota was not consumed for request {}, skipping restoration", requestId);
            return;
        }

        // Send quota restoration message (negative count)
        Long count = -durationSeconds.longValue(); // Negative to restore
        Long date = Instant.now().getEpochSecond();
        
        try {
            messageQueueService.sendQuotaRestorationMessage(userId, productId, count, date);
            logger.info("Sent quota restoration message: user={}, product={}, count={}", 
                    userId, productId, count);
        } catch (Exception e) {
            logger.error("Failed to send quota restoration message", e);
            // Log but don't fail - quota service might handle it
        }
    }
}
