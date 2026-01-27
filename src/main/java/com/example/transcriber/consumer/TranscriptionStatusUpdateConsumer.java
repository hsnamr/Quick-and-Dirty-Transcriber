package com.example.audiototext.consumer;

import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
public class TranscriptionStatusUpdateConsumer {

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
            // TODO: Implement message consumption
            // 1. Handle gzipped message if needed (decompress)
            // 2. Parse JSON (already parsed by JsonDeserializer)
            // 3. Extract request_id and status from message map
            // 4. Update transcription request status
            // 5. Handle status change side effects
            // 6. Acknowledge message after successful processing
            
            // Example: Extract data from message
            // String status = (String) message.get("status");
            // Map<String, Object> engines = (Map<String, Object>) message.get("engines");
            
            acknowledgment.acknowledge();
        } catch (Exception e) {
            // Log error and handle failure
            // Optionally: send to dead letter topic or retry
            // acknowledgment.nack(...) for retry
            throw e;
        }
    }
}
