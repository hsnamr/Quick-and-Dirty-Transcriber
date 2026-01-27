package com.example.audiototext.service;

import com.example.audiototext.model.TranscriptionRequest;

public interface EmailService {

    void sendRequestSentEmail(TranscriptionRequest request);

    void sendTranscriptionCompletedEmail(TranscriptionRequest request);

    void sendTranscriptionFailedEmail(TranscriptionRequest request, String errorMessage);
}
