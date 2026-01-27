package com.example.transcriber.service.impl;

import com.example.transcriber.exception.ValidationException;
import com.example.transcriber.service.MetadataExtractionService;
import org.apache.tika.Tika;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.ParseContext;
import org.apache.tika.parser.Parser;
import org.apache.tika.parser.mp3.Mp3Parser;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.audio.exceptions.CannotReadException;
import org.jaudiotagger.audio.exceptions.InvalidAudioFrameException;
import org.jaudiotagger.audio.exceptions.ReadOnlyFileException;
import org.jaudiotagger.tag.TagException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.ContentHandler;
import org.xml.sax.helpers.DefaultHandler;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

@Service
public class MetadataExtractionServiceImpl implements MetadataExtractionService {

    private static final Logger logger = LoggerFactory.getLogger(MetadataExtractionServiceImpl.class);
    private static final List<String> SUPPORTED_FORMATS = Arrays.asList(
            "audio/mpeg", "audio/mp3", "audio/wav", "audio/x-wav",
            "audio/mp4", "audio/x-m4a", "audio/flac", "audio/x-flac",
            "audio/ogg", "audio/vorbis"
    );

    private final Tika tika = new Tika();

    @Override
    public AudioMetadata extractMetadata(MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new ValidationException("Audio file is required");
        }

        AudioMetadata metadata = new AudioMetadata();
        metadata.setFileName(audioFile.getOriginalFilename());
        metadata.setFileSize(audioFile.getSize());
        metadata.setContentType(audioFile.getContentType());

        // Validate file format
        String detectedType = detectContentType(audioFile);
        if (!isSupportedFormat(detectedType)) {
            throw new ValidationException(
                    String.format("Unsupported audio format: %s. Supported formats: MP3, WAV, M4A, FLAC, OGG", detectedType)
            );
        }
        metadata.setFormat(extractFormatFromContentType(detectedType));

        // Extract duration
        File tempFile = null;
        try {
            tempFile = createTempFile(audioFile);
            Double duration = extractDuration(tempFile, detectedType);
            
            if (duration == null || duration <= 0) {
                throw new ValidationException("Could not extract audio duration. The file may be corrupted or in an unsupported format.");
            }

            metadata.setDurationSeconds(duration);
            logger.info("Extracted metadata for file {}: duration={}s, size={} bytes, format={}",
                    audioFile.getOriginalFilename(), duration, audioFile.getSize(), metadata.getFormat());

        } catch (Exception e) {
            logger.error("Error extracting metadata from audio file: {}", audioFile.getOriginalFilename(), e);
            if (e instanceof ValidationException) {
                throw e;
            }
            throw new ValidationException("Failed to extract audio metadata: " + e.getMessage());
        } finally {
            if (tempFile != null && tempFile.exists()) {
                try {
                    Files.delete(tempFile.toPath());
                } catch (IOException e) {
                    logger.warn("Failed to delete temporary file: {}", tempFile.getAbsolutePath(), e);
                }
            }
        }

        return metadata;
    }

    private String detectContentType(MultipartFile file) {
        try {
            String contentType = tika.detect(file.getInputStream(), file.getOriginalFilename());
            if (contentType == null || contentType.isEmpty()) {
                contentType = file.getContentType();
            }
            return contentType != null ? contentType : "application/octet-stream";
        } catch (IOException e) {
            logger.warn("Failed to detect content type, using provided: {}", file.getContentType(), e);
            return file.getContentType() != null ? file.getContentType() : "application/octet-stream";
        }
    }

    private boolean isSupportedFormat(String contentType) {
        if (contentType == null) {
            return false;
        }
        return SUPPORTED_FORMATS.stream()
                .anyMatch(format -> contentType.toLowerCase().contains(format.toLowerCase()) ||
                        contentType.toLowerCase().contains(format.replace("audio/", "")));
    }

    private String extractFormatFromContentType(String contentType) {
        if (contentType == null) {
            return "unknown";
        }
        String lower = contentType.toLowerCase();
        if (lower.contains("mpeg") || lower.contains("mp3")) return "mp3";
        if (lower.contains("wav")) return "wav";
        if (lower.contains("m4a") || lower.contains("mp4")) return "m4a";
        if (lower.contains("flac")) return "flac";
        if (lower.contains("ogg") || lower.contains("vorbis")) return "ogg";
        return "unknown";
    }

    private File createTempFile(MultipartFile multipartFile) throws IOException {
        Path tempDir = Files.createTempDirectory("audio_metadata_");
        File tempFile = tempDir.resolve(multipartFile.getOriginalFilename()).toFile();
        
        try (InputStream inputStream = multipartFile.getInputStream();
             FileOutputStream outputStream = new FileOutputStream(tempFile)) {
            byte[] buffer = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
        }
        
        return tempFile;
    }

    private Double extractDuration(File audioFile, String contentType) {
        // Try JAudioTagger first (works well for MP3, WAV, FLAC, M4A)
        if (contentType.contains("mpeg") || contentType.contains("mp3") ||
            contentType.contains("wav") || contentType.contains("flac") ||
            contentType.contains("m4a") || contentType.contains("mp4")) {
            try {
                return extractDurationWithJAudioTagger(audioFile);
            } catch (Exception e) {
                logger.debug("JAudioTagger failed, trying Tika: {}", e.getMessage());
            }
        }

        // Fallback to Tika
        try {
            return extractDurationWithTika(audioFile);
        } catch (Exception e) {
            logger.error("Failed to extract duration with Tika", e);
            return null;
        }
    }

    private Double extractDurationWithJAudioTagger(File audioFile) {
        try {
            AudioFile audioFileObj = AudioFileIO.read(audioFile);
            int duration = audioFileObj.getAudioHeader().getTrackLength();
            return (double) duration;
        } catch (CannotReadException | IOException | TagException | ReadOnlyFileException |
                 InvalidAudioFrameException e) {
            logger.debug("JAudioTagger extraction failed", e);
            throw new RuntimeException("JAudioTagger extraction failed", e);
        }
    }

    private Double extractDurationWithTika(File audioFile) {
        try {
            Parser parser = new Mp3Parser();
            Metadata metadata = new Metadata();
            ContentHandler handler = new DefaultHandler();
            ParseContext parseContext = new ParseContext();

            try (InputStream inputStream = Files.newInputStream(audioFile.toPath())) {
                parser.parse(inputStream, handler, metadata, parseContext);
            }

            String durationStr = metadata.get("xmpDM:duration");
            if (durationStr != null && !durationStr.isEmpty()) {
                try {
                    // Duration is in milliseconds, convert to seconds
                    double durationMs = Double.parseDouble(durationStr);
                    return durationMs / 1000.0;
                } catch (NumberFormatException e) {
                    logger.warn("Could not parse duration: {}", durationStr);
                }
            }

            // Try alternative metadata keys
            String lengthStr = metadata.get("channels");
            // If no duration found, return null
            return null;
        } catch (Exception e) {
            logger.error("Tika extraction failed", e);
            throw new RuntimeException("Tika extraction failed", e);
        }
    }
}
