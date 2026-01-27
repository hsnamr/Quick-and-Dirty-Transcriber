package com.example.audiototext.model;

import com.example.audiototext.model.enums.Category;
import com.example.audiototext.model.enums.Status;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.DBRef;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Document(collection = "transcription_requests")
public class TranscriptionRequest {

    @Id
    private String id;

    @Indexed(unique = true)
    private Long numericId; // For API compatibility - auto-generated sequential ID

    @Indexed
    private String fileName;

    @DBRef
    @Indexed
    private TranscriptionLanguage language;

    private Integer speakersCount;

    @Indexed
    private BigDecimal durationSecs;

    @Indexed
    private Status status = Status.PROCESSING;

    @Indexed
    private Category category = Category.OTHER;

    @Indexed
    private Long userId;

    private Boolean quotaConsumed = false;

    private String transcriptionText;

    private Object transcriptionJson; // MongoDB stores JSON natively

    private Object transcriptionMetadata; // MongoDB stores JSON natively

    @CreatedDate
    private LocalDateTime createdAt;

    @LastModifiedDate
    private LocalDateTime updatedAt;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public TranscriptionLanguage getLanguage() {
        return language;
    }

    public void setLanguage(TranscriptionLanguage language) {
        this.language = language;
    }

    public Integer getSpeakersCount() {
        return speakersCount;
    }

    public void setSpeakersCount(Integer speakersCount) {
        this.speakersCount = speakersCount;
    }

    public BigDecimal getDurationSecs() {
        return durationSecs;
    }

    public void setDurationSecs(BigDecimal durationSecs) {
        this.durationSecs = durationSecs;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Category getCategory() {
        return category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public Boolean getQuotaConsumed() {
        return quotaConsumed;
    }

    public void setQuotaConsumed(Boolean quotaConsumed) {
        this.quotaConsumed = quotaConsumed;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getTranscriptionText() {
        return transcriptionText;
    }

    public void setTranscriptionText(String transcriptionText) {
        this.transcriptionText = transcriptionText;
    }

    public Object getTranscriptionJson() {
        return transcriptionJson;
    }

    public void setTranscriptionJson(Object transcriptionJson) {
        this.transcriptionJson = transcriptionJson;
    }

    public Object getTranscriptionMetadata() {
        return transcriptionMetadata;
    }

    public void setTranscriptionMetadata(Object transcriptionMetadata) {
        this.transcriptionMetadata = transcriptionMetadata;
    }

    public Long getNumericId() {
        return numericId;
    }

    public void setNumericId(Long numericId) {
        this.numericId = numericId;
    }
}
