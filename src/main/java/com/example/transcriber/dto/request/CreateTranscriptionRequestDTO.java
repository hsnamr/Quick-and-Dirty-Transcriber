package com.example.transcriber.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for creating a transcription request
 * 
 * Uses MultipartFile to handle audio file uploads via multipart/form-data.
 * The audio file is received as a MultipartFile which provides access to:
 * - File content (via getInputStream())
 * - Original filename (via getOriginalFilename())
 * - File size (via getSize())
 * - Content type (via getContentType())
 */
public class CreateTranscriptionRequestDTO {

    /**
     * Audio file to transcribe
     * Received as MultipartFile from multipart/form-data request
     */
    @NotNull(message = "Audio file is required")
    private MultipartFile audioFile;

    @NotNull(message = "Speakers count is required")
    @Min(value = 1, message = "Speakers count must be at least 1")
    @Max(value = 5, message = "Speakers count must be at most 5")
    private Integer speakersCount;

    private String language; // Optional, defaults to "auto"

    @NotNull(message = "Category is required")
    private String category;

    // Getters and setters
    public MultipartFile getAudioFile() {
        return audioFile;
    }

    public void setAudioFile(MultipartFile audioFile) {
        this.audioFile = audioFile;
    }

    public Integer getSpeakersCount() {
        return speakersCount;
    }

    public void setSpeakersCount(Integer speakersCount) {
        this.speakersCount = speakersCount;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
