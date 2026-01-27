package com.example.transcriber.config;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Interceptor for API versioning via headers
 * Checks for API-Version header and validates it
 */
@Component
public class ApiVersionInterceptor implements HandlerInterceptor {

    private static final Logger logger = LoggerFactory.getLogger(ApiVersionInterceptor.class);
    private static final String API_VERSION_HEADER = "API-Version";
    private static final String CURRENT_API_VERSION = "v1";
    private static final String DEFAULT_API_VERSION = "v1";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // Skip version check for actuator endpoints
        String path = request.getRequestURI();
        if (path != null && (path.startsWith("/actuator") || path.startsWith("/health"))) {
            return true;
        }

        // Get API version from header
        String apiVersion = request.getHeader(API_VERSION_HEADER);
        
        // If no version header, use default (v1)
        if (apiVersion == null || apiVersion.isEmpty()) {
            apiVersion = DEFAULT_API_VERSION;
            logger.debug("No API-Version header found, using default: {}", DEFAULT_API_VERSION);
        }

        // Validate version
        if (!isValidVersion(apiVersion)) {
            logger.warn("Invalid API version requested: {}", apiVersion);
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            try {
                response.getWriter().write("{\"error\":\"Invalid API version. Supported versions: v1\"}");
                response.setContentType("application/json");
            } catch (Exception e) {
                logger.error("Failed to write error response", e);
            }
            return false;
        }

        // Add version to request attribute for use in controllers if needed
        request.setAttribute("apiVersion", apiVersion);
        
        // Add version to response header
        response.setHeader(API_VERSION_HEADER, apiVersion);

        return true;
    }

    private boolean isValidVersion(String version) {
        // Currently only v1 is supported
        return "v1".equalsIgnoreCase(version);
    }
}
