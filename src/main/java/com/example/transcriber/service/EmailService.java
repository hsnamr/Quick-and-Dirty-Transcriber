package com.example.transcriber.service;

import com.example.transcriber.model.TranscriptionRequest;

public interface EmailService {

    void sendRequestSentEmail(TranscriptionRequest request);

    void sendTranscriptionCompletedEmail(TranscriptionRequest request);

    void sendTranscriptionFailedEmail(TranscriptionRequest request, String errorMessage);
}
