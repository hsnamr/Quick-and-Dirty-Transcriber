package com.example.transcriber.service;

import org.springframework.web.multipart.MultipartFile;

public interface TranscriptionServiceClient {

    void submitTranscriptionRequest(
            MultipartFile audioFile,
            Integer speakersCount,
            String language,
            String category,
            Long requestId,
            Long userId);
}
