package com.example.transcriber.service;

public interface QuotaService {

    void validateQuota(Long userId, Double durationSeconds);

    void consumeQuota(Long userId, Long requestId, Double durationSeconds);

    void restoreQuota(Long userId, Long requestId, Double durationSeconds);
}
