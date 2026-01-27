package com.example.audiototext.service.impl;

import com.example.audiototext.model.TranscriptionRequest;
import com.example.audiototext.service.EmailService;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
public class EmailServiceImpl implements EmailService {

    @Override
    @Async
    public void sendRequestSentEmail(TranscriptionRequest request) {
        // TODO: Implement email sending for request sent
    }

    @Override
    @Async
    public void sendTranscriptionCompletedEmail(TranscriptionRequest request) {
        // TODO: Implement email sending for transcription completed
    }

    @Override
    @Async
    public void sendTranscriptionFailedEmail(TranscriptionRequest request, String errorMessage) {
        // TODO: Implement email sending for transcription failed
    }
}
