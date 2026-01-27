package com.example.transcriber.consumer;

import com.example.transcriber.model.enums.Status;
import com.example.transcriber.service.StatusManagementService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class TranscriptionStatusUpdateConsumer {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionStatusUpdateConsumer.class);

    private final StatusManagementService statusManagementService;

    public TranscriptionStatusUpdateConsumer(StatusManagementService statusManagementService) {
        this.statusManagementService = statusManagementService;
    }

    @KafkaListener(
        topics = "audio_text_request_updater",
        groupId = "free-and-dirty-transcriber",
        containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleStatusUpdate(
            @Payload Map<String, Object> message,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            Acknowledgment acknowledgment) {
        try {
            logger.debug("Received status update message from topic: {}, partition: {}, offset: {}", 
                    topic, partition, offset);
            
            // Extract status
            String statusStr = (String) message.get("status");
            if (statusStr == null) {
                logger.warn("Status missing in message, skipping");
                acknowledgment.acknowledge();
                return;
            }
            
            // Map "success" to "completed"
            if ("success".equalsIgnoreCase(statusStr)) {
                statusStr = "completed";
            }
            
            Status newStatus;
            try {
                newStatus = Status.fromCode(statusStr);
            } catch (IllegalArgumentException e) {
                logger.warn("Invalid status in message: {}, skipping", statusStr);
                acknowledgment.acknowledge();
                return;
            }
            
            // Only process completed or failed statuses
            if (newStatus != Status.COMPLETED && newStatus != Status.FAILED) {
                logger.debug("Status {} is not completed or failed, skipping", newStatus);
                acknowledgment.acknowledge();
                return;
            }
            
            // Extract request_id from nested structure
            Long requestId = extractRequestId(message);
            if (requestId == null) {
                logger.warn("Request ID not found in message, skipping");
                acknowledgment.acknowledge();
                return;
            }
            
            logger.info("Updating transcription request {} to status {}", requestId, newStatus);
            
            // Update status (this will handle side effects)
            statusManagementService.updateStatus(requestId, newStatus);
            
            acknowledgment.acknowledge();
            logger.debug("Successfully processed status update for request {}", requestId);
            
        } catch (Exception e) {
            logger.error("Error processing status update message", e);
            // Acknowledge to avoid reprocessing the same message repeatedly
            // In production, consider implementing retry logic or dead letter topic
            acknowledgment.acknowledge();
        }
    }
    
    private Long extractRequestId(Map<String, Object> message) {
        try {
            // Try to extract from engines[0].sender_parameters[0].identifers.request_id
            Object enginesObj = message.get("engines");
            if (enginesObj instanceof List) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> engines = (List<Map<String, Object>>) enginesObj;
                if (!engines.isEmpty()) {
                    Map<String, Object> engine = engines.get(0);
                    Object senderParamsObj = engine.get("sender_parameters");
                    if (senderParamsObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, Object>> senderParams = (List<Map<String, Object>>) senderParamsObj;
                        if (!senderParams.isEmpty()) {
                            Map<String, Object> senderParam = senderParams.get(0);
                            Object identifiersObj = senderParam.get("identifers");
                            if (identifiersObj instanceof Map) {
                                @SuppressWarnings("unchecked")
                                Map<String, Object> identifiers = (Map<String, Object>) identifiersObj;
                                Object requestIdObj = identifiers.get("request_id");
                                if (requestIdObj instanceof Number) {
                                    return ((Number) requestIdObj).longValue();
                                } else if (requestIdObj != null) {
                                    return Long.parseLong(requestIdObj.toString());
                                }
                            }
                        }
                    }
                }
            }
            
            // Fallback: try direct request_id in message
            Object requestIdObj = message.get("request_id");
            if (requestIdObj instanceof Number) {
                return ((Number) requestIdObj).longValue();
            } else if (requestIdObj != null) {
                return Long.parseLong(requestIdObj.toString());
            }
            
        } catch (Exception e) {
            logger.warn("Error extracting request_id from message", e);
        }
        
        return null;
    }
}
