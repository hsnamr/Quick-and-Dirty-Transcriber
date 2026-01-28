package com.example.transcriber.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Rate Limiting Configuration
 * 
 * Implements rate limiting using Bucket4j (same library used by Spring Cloud Gateway).
 * Limits the number of requests per time window to protect APIs from overuse.
 */
@Configuration
public class RateLimitingConfig {

    @Value("${rate.limit.requests-per-minute:10}")
    private int requestsPerMinute;

    @Value("${rate.limit.burst-capacity:20}")
    private int burstCapacity;

    /**
     * Creates a rate limiter bucket with:
     * - Refill rate: requestsPerMinute tokens per minute
     * - Burst capacity: burstCapacity tokens
     * 
     * This allows up to burstCapacity requests immediately, then refills at requestsPerMinute per minute.
     */
    @Bean
    public Bucket rateLimiterBucket() {
        Refill refill = Refill.intervally(requestsPerMinute, Duration.ofMinutes(1));
        Bandwidth limit = Bandwidth.classic(burstCapacity, refill);
        return Bucket.builder()
                .addLimit(limit)
                .build();
    }
}
