package com.example.transcriber.util;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * Utility class for audio file validation
 */
public class AudioValidator {

    private static final Logger logger = LoggerFactory.getLogger(AudioValidator.class);
    private static final Tika tika = new Tika();
    
    private static final List<String> SUPPORTED_MIME_TYPES = Arrays.asList(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav",
            "audio/mp4", "audio/x-m4a", "audio/flac", "audio/x-flac",
            "audio/ogg", "audio/vorbis"
    );

    private static final List<String> SUPPORTED_EXTENSIONS = Arrays.asList(
            ".mp3", ".wav", ".m4a", ".flac", ".ogg"
    );

    private static final long MAX_FILE_SIZE = 500 * 1024 * 1024; // 500MB
    private static final long MIN_FILE_SIZE = 1024; // 1KB

    /**
     * Validate audio file
     * @param file Audio file to validate
     * @throws IllegalArgumentException if validation fails
     */
    public static void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Audio file is required");
        }

        // Check file size
        if (file.getSize() < MIN_FILE_SIZE) {
            throw new IllegalArgumentException("Audio file is too small (minimum 1KB)");
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IllegalArgumentException("Audio file is too large (maximum 500MB)");
        }

        // Check file extension
        String fileName = file.getOriginalFilename();
        if (fileName == null || fileName.isEmpty()) {
            throw new IllegalArgumentException("Audio file must have a filename");
        }

        String extension = getFileExtension(fileName);
        if (!SUPPORTED_EXTENSIONS.contains(extension.toLowerCase())) {
            throw new IllegalArgumentException(
                    String.format("Unsupported file extension: %s. Supported: %s", 
                            extension, String.join(", ", SUPPORTED_EXTENSIONS))
            );
        }

        // Check MIME type
        try {
            String detectedType = tika.detect(file.getInputStream(), fileName);
            if (detectedType != null && !isSupportedMimeType(detectedType)) {
                logger.warn("Detected MIME type {} may not be supported", detectedType);
            }
        } catch (IOException e) {
            logger.warn("Could not detect MIME type", e);
        }
    }

    /**
     * Check if MIME type is supported
     */
    public static boolean isSupportedMimeType(String mimeType) {
        if (mimeType == null) {
            return false;
        }
        return SUPPORTED_MIME_TYPES.stream()
                .anyMatch(supported -> mimeType.toLowerCase().contains(supported.toLowerCase()));
    }

    /**
     * Get file extension
     */
    private static String getFileExtension(String fileName) {
        int lastDot = fileName.lastIndexOf('.');
        if (lastDot == -1 || lastDot == fileName.length() - 1) {
            return "";
        }
        return fileName.substring(lastDot);
    }

    /**
     * Validate audio duration
     */
    public static void validateDuration(Double durationSeconds, Double minDuration, Double maxDuration) {
        if (durationSeconds == null || durationSeconds <= 0) {
            throw new IllegalArgumentException("Invalid audio duration");
        }

        if (minDuration != null && durationSeconds < minDuration) {
            throw new IllegalArgumentException(
                    String.format("Audio duration too short (minimum %.1f seconds)", minDuration)
            );
        }

        if (maxDuration != null && durationSeconds > maxDuration) {
            throw new IllegalArgumentException(
                    String.format("Audio duration too long (maximum %.1f seconds)", maxDuration)
            );
        }
    }
}
