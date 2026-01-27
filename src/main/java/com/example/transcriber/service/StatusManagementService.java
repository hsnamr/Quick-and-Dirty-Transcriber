package com.example.audiototext.service;

import com.example.audiototext.model.TranscriptionRequest;
import com.example.audiototext.model.enums.Status;

public interface StatusManagementService {

    void updateStatus(Long requestId, Status newStatus);

    void handleStatusChangeToCompleted(TranscriptionRequest request);

    void handleStatusChangeToFailed(TranscriptionRequest request, String errorMessage);
}
