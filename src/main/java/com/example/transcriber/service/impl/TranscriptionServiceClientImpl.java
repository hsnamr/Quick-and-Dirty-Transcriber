package com.example.audiototext.service.impl;

import com.example.audiototext.exception.ExternalServiceException;
import com.example.audiototext.service.TranscriptionServiceClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

/**
 * Client for external transcription service (Python service)
 * This is optional - can be used instead of Vosk for transcription
 */
@Service
public class TranscriptionServiceClientImpl implements TranscriptionServiceClient {

    private static final Logger logger = LoggerFactory.getLogger(TranscriptionServiceClientImpl.class);

    private final RestTemplate restTemplate;
    private final String serviceUrl;
    private final String uploadPath;
    private final int timeout;

    public TranscriptionServiceClientImpl(
            @Value("${audio.transcription.service.url:}") String serviceUrl,
            @Value("${audio.transcription.service.upload-path:/api/v1/upload-audio}") String uploadPath,
            @Value("${audio.transcription.service.timeout:30000}") int timeout) {
        this.serviceUrl = serviceUrl;
        this.uploadPath = uploadPath;
        this.timeout = timeout;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void submitTranscriptionRequest(
            MultipartFile audioFile,
            Integer speakersCount,
            String language,
            String category,
            Long requestId,
            Long userId) {
        
        if (serviceUrl == null || serviceUrl.isEmpty()) {
            throw new ExternalServiceException("External transcription service URL not configured");
        }

        String fullUrl = serviceUrl + uploadPath;
        logger.info("Submitting transcription request {} to external service: {}", requestId, fullUrl);

        try {
            // Prepare multipart request
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            
            // Add audio file
            body.add("audio_file", audioFile.getResource());
            
            // Add other parameters
            body.add("speakers_count", speakersCount);
            body.add("language", language != null ? language : "auto");
            body.add("category", category);
            body.add("request_id", requestId);
            body.add("transcription_id", requestId); // Alias
            body.add("user_id", userId);

            HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // Send request
            ResponseEntity<String> response = restTemplate.exchange(
                    fullUrl,
                    HttpMethod.POST,
                    requestEntity,
                    String.class
            );

            // Check response
            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Successfully submitted transcription request {} to external service", requestId);
            } else {
                logger.error("External service returned error status: {} for request {}", 
                        response.getStatusCode(), requestId);
                throw new ExternalServiceException(
                        "External transcription service returned error: " + response.getStatusCode()
                );
            }

        } catch (org.springframework.web.client.ResourceAccessException e) {
            logger.error("Connection timeout or service unavailable for request {}", requestId, e);
            throw new ExternalServiceException("Transcription service unavailable", true);
        } catch (org.springframework.web.client.HttpClientErrorException | 
                 org.springframework.web.client.HttpServerErrorException e) {
            logger.error("HTTP error from external service for request {}: {}", requestId, e.getMessage());
            throw new ExternalServiceException("External service error: " + e.getMessage());
        } catch (IOException e) {
            logger.error("IO error while submitting request {}", requestId, e);
            throw new ExternalServiceException("Failed to read audio file: " + e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error submitting request {} to external service", requestId, e);
            throw new ExternalServiceException("Failed to submit transcription request: " + e.getMessage());
        }
    }
}
