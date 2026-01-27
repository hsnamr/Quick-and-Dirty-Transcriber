package com.example.audiototext.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.multipart.MultipartFile;

public class CreateTranscriptionRequestDTO {

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
