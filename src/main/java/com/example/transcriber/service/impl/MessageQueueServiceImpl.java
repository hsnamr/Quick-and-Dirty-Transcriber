package com.example.transcriber.service.impl;

import com.example.transcriber.model.TranscriptionRequest;
import com.example.transcriber.service.MessageQueueService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Service;
import org.springframework.util.concurrent.ListenableFuture;
import org.springframework.util.concurrent.ListenableFutureCallback;

import java.util.HashMap;
import java.util.Map;

@Service
public class MessageQueueServiceImpl implements MessageQueueService {

    private static final Logger logger = LoggerFactory.getLogger(MessageQueueServiceImpl.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Value("${kafka.topics.quota:companies_count}")
    private String quotaTopic;

    @Value("${kafka.topics.frontend-broadcast:frontend_audio_to_text_data}")
    private String frontendBroadcastTopic;

    public MessageQueueServiceImpl(KafkaTemplate<String, Object> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    @Override
    public void sendQuotaConsumptionMessage(Long userId, Long productId, Long count, Long date) {
        Map<String, Object> message = new HashMap<>();
        message.put("user_id", userId);
        message.put("product_id", productId);
        message.put("count", count);
        message.put("date", date);

        String key = String.format("user_%d_product_%d", userId, productId);
        sendMessage(quotaTopic, key, message);
    }

    @Override
    public void sendQuotaRestorationMessage(Long userId, Long productId, Long count, Long date) {
        Map<String, Object> message = new HashMap<>();
        message.put("user_id", userId);
        message.put("product_id", productId);
        message.put("count", -count); // Negative count for restoration
        message.put("date", date);

        String key = String.format("user_%d_product_%d", userId, productId);
        sendMessage(quotaTopic, key, message);
    }

    @Override
    public void sendFrontendBroadcast(TranscriptionRequest request) {
        Map<String, Object> message = new HashMap<>();
        message.put("request_id", request.getId());
        message.put("user_id", request.getUserId());
        message.put("account_id", request.getUserId());
        message.put("file_name", request.getFileName());
        message.put("duration_secs", request.getDurationSecs());
        message.put("speakers_count", request.getSpeakersCount());
        message.put("language", request.getLanguage().getName());
        message.put("category", request.getCategory().getCode());
        message.put("status", request.getStatus().getCode());
        message.put("created_at", request.getCreatedAt());
        message.put("updated_at", request.getUpdatedAt());
        message.put("product", "FDTranscriber");
        message.put("page_name", "audio_to_text");
        message.put("data_source_name", "fdtranscriber");
        message.put("filters", new HashMap<>());

        // Generate queue name and routing key
        String queueName = String.format("frontend_audio_to_text_data_user_%d_0_%d", 
            request.getUserId(), System.currentTimeMillis() / 1000);
        String routingKey = String.format("audio_to_text_data_user_%d_0_%d", 
            request.getUserId(), System.currentTimeMillis() / 1000);
        
        message.put("queue_name", queueName);
        message.put("routing_key", routingKey);

        String key = String.format("user_%d_request_%d", request.getUserId(), request.getId());
        sendMessage(frontendBroadcastTopic, key, message);
    }

    private void sendMessage(String topic, String key, Object message) {
        try {
            ListenableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, message);
            
            future.addCallback(new ListenableFutureCallback<SendResult<String, Object>>() {
                @Override
                public void onSuccess(SendResult<String, Object> result) {
                    logger.debug("Successfully sent message to topic: {}, key: {}, offset: {}", 
                            topic, key, result.getRecordMetadata().offset());
                }

                @Override
                public void onFailure(Throwable ex) {
                    logger.error("Failed to send message to topic: {}, key: {}", topic, key, ex);
                    // In production, consider:
                    // 1. Retry logic with exponential backoff
                    // 2. Dead letter topic for failed messages
                    // 3. Alerting/monitoring for persistent failures
                    // 4. Circuit breaker pattern if Kafka is down
                }
            });
        } catch (Exception e) {
            logger.error("Exception while sending message to topic: {}, key: {}", topic, key, e);
            // Re-throw to allow caller to handle if needed
            throw new RuntimeException("Failed to send Kafka message", e);
        }
    }
}
