package com.example.audiototext.service.impl;

import com.example.audiototext.model.TranscriptionRequest;
import com.example.audiototext.model.enums.Status;
import com.example.audiototext.repository.TranscriptionRequestRepository;
import com.example.audiototext.service.EmailService;
import com.example.audiototext.service.QuotaService;
import com.example.audiototext.service.StatusManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class StatusManagementServiceImpl implements StatusManagementService {

    private static final Logger logger = LoggerFactory.getLogger(StatusManagementServiceImpl.class);

    private final TranscriptionRequestRepository requestRepository;
    private final QuotaService quotaService;
    private final EmailService emailService;

    public StatusManagementServiceImpl(TranscriptionRequestRepository requestRepository,
                                     QuotaService quotaService,
                                     EmailService emailService) {
        this.requestRepository = requestRepository;
        this.quotaService = quotaService;
        this.emailService = emailService;
    }

    @Override
    public void updateStatus(Long requestId, Status newStatus) {
        if (requestId == null || newStatus == null) {
            throw new IllegalArgumentException("Request ID and status are required");
        }

        // Find by numericId (for API compatibility)
        // Note: We need userId to find, but we don't have it here
        // For now, try to find by numericId - might need to update interface
        Optional<TranscriptionRequest> requestOpt = requestRepository.findByNumericId(requestId);
        if (requestOpt.isEmpty()) {
            throw new IllegalArgumentException("Transcription request not found: " + requestId);
        }

        TranscriptionRequest request = requestOpt.get();
        Status oldStatus = request.getStatus();

        // Validate status transition
        if (!isValidStatusTransition(oldStatus, newStatus)) {
            throw new IllegalStateException(
                    String.format("Invalid status transition from %s to %s", oldStatus, newStatus)
            );
        }

        // Update status
        request.setStatus(newStatus);
        requestRepository.save(request);

        logger.info("Updated request {} status from {} to {}", requestId, oldStatus, newStatus);

        // Handle status change side effects
        if (newStatus == Status.COMPLETED) {
            handleStatusChangeToCompleted(request);
        } else if (newStatus == Status.FAILED) {
            handleStatusChangeToFailed(request, "Status changed to failed");
        }
    }

    @Override
    public void handleStatusChangeToCompleted(TranscriptionRequest request) {
        logger.info("Handling status change to COMPLETED for request {}", request.getId());

        // Send completion email
        try {
            emailService.sendTranscriptionCompletedEmail(request);
        } catch (Exception e) {
            logger.error("Failed to send completion email for request {}", request.getId(), e);
            // Don't fail the request if email fails
        }
    }

    @Override
    public void handleStatusChangeToFailed(TranscriptionRequest request, String errorMessage) {
        logger.info("Handling status change to FAILED for request {}: {}", request.getId(), errorMessage);

        // Restore quota if it was consumed
        if (request.getQuotaConsumed() != null && request.getQuotaConsumed()) {
            try {
                // Use numericId for quota service
                quotaService.restoreQuota(
                        request.getUserId(),
                        request.getNumericId(),
                        request.getDurationSecs().doubleValue()
                );
                logger.info("Restored quota for failed request {}", request.getId());
            } catch (Exception e) {
                logger.error("Failed to restore quota for request {}", request.getId(), e);
                // Log but don't fail - quota restoration is best effort
            }
        }

        // Send failure email
        try {
            emailService.sendTranscriptionFailedEmail(request, errorMessage);
        } catch (Exception e) {
            logger.error("Failed to send failure email for request {}", request.getId(), e);
            // Don't fail the request if email fails
        }
    }

    /**
     * Validate status transition
     * Allowed transitions:
     * - PROCESSING -> COMPLETED
     * - PROCESSING -> FAILED
     */
    private boolean isValidStatusTransition(Status oldStatus, Status newStatus) {
        if (oldStatus == newStatus) {
            return true; // No change is valid
        }

        if (oldStatus == Status.PROCESSING) {
            return newStatus == Status.COMPLETED || newStatus == Status.FAILED;
        }

        // Once COMPLETED or FAILED, status should not change
        return false;
    }
}
