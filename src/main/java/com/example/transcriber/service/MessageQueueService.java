package com.example.audiototext.service;

import com.example.audiototext.model.TranscriptionRequest;

public interface MessageQueueService {

    void sendQuotaConsumptionMessage(Long userId, Long productId, Long count, Long date);

    void sendQuotaRestorationMessage(Long userId, Long productId, Long count, Long date);

    void sendFrontendBroadcast(TranscriptionRequest request);
}
