package com.example.transcriber.filter;

import io.github.bucket4j.Bucket;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * Rate Limiting Filter
 * 
 * Protects APIs from overuse by limiting the number of requests per time window.
 * Uses Bucket4j (same library used by Spring Cloud Gateway's RateLimiter filter).
 * 
 * This filter runs early in the filter chain (before authentication) to protect
 * against abuse even before processing the request.
 */
@Component
@Order(1)
public class RateLimitingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RateLimitingFilter.class);

    private final Bucket rateLimiterBucket;

    @Autowired
    public RateLimitingFilter(Bucket rateLimiterBucket) {
        this.rateLimiterBucket = rateLimiterBucket;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, 
                                   FilterChain filterChain) throws ServletException, IOException {
        
        // Skip rate limiting for health check and API documentation endpoints
        String path = request.getRequestURI() != null ? request.getRequestURI() : "";
        if (path.startsWith("/actuator/health") || path.contains("api-docs") || path.contains("swagger-ui")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Try to consume a token from the bucket
        if (rateLimiterBucket.tryConsume(1)) {
            // Token consumed successfully, proceed with request
            filterChain.doFilter(request, response);
        } else {
            // Rate limit exceeded
            logger.warn("Rate limit exceeded for request: {} {}", request.getMethod(), path);
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType("application/json");
            response.getWriter().write(
                "{\"error\":\"Rate limit exceeded. Please try again later.\"," +
                "\"message\":\"Too many requests. Please wait before making another request.\"}"
            );
        }
    }
}
