package com.example.transcriber.service;

import com.example.transcriber.model.TranscriptionRequest;
import com.example.transcriber.model.enums.Status;

public interface StatusManagementService {

    void updateStatus(Long requestId, Status newStatus);

    void handleStatusChangeToCompleted(TranscriptionRequest request);

    void handleStatusChangeToFailed(TranscriptionRequest request, String errorMessage);
}
