package com.example.audiototext.dto.response;

import java.time.LocalDateTime;

public class TranscriptionRequestDTO {
    private String id;
    private String type = "transcription_request";
    private TranscriptionRequestAttributes attributes;
    private TranscriptionRequestRelationships relationships;

    // Getters and setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public TranscriptionRequestAttributes getAttributes() {
        return attributes;
    }

    public void setAttributes(TranscriptionRequestAttributes attributes) {
        this.attributes = attributes;
    }

    public TranscriptionRequestRelationships getRelationships() {
        return relationships;
    }

    public void setRelationships(TranscriptionRequestRelationships relationships) {
        this.relationships = relationships;
    }

    public static class TranscriptionRequestAttributes {
        private Long id;
        private String fileName;
        private Integer speakersCount;
        private Double durationSecs;
        private String durationFormatted;
        private String status;
        private String statusDisplay;
        private String category;
        private String categoryName;
        private String categoryKey;
        private String languageName;
        private String languageCode;
        private String languageDisplayName;
        private Boolean autoDetectLanguage;
        private String userName;
        private String userEmail;
        private Long companyId; // Kept for API compatibility, but now uses userId
        private Boolean isProcessing;
        private Boolean isCompleted;
        private Boolean isFailed;
        private Boolean canBeDeleted;
        private Boolean canBeViewed;
        private Boolean canBeUpdated;
        private String fileExtension;
        private LocalDateTime createdAt;
        private String createdAtFormatted;
        private Long createdAtUnix;
        private LocalDateTime updatedAt;
        private String updatedAtFormatted;
        private Long updatedAtUnix;
        private Double processingTimeSeconds;

        // Getters and setters (abbreviated for brevity - implement all)
        public Long getId() {
            return id;
        }

        public void setId(Long id) {
            this.id = id;
        }

        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Long getCompanyId() {
            return companyId;
        }

        public void setCompanyId(Long companyId) {
            this.companyId = companyId;
        }

        // Add remaining getters and setters...
    }

    public static class TranscriptionRequestRelationships {
        private RelationshipData language;
        private RelationshipData user;

        // Getters and setters
        public RelationshipData getLanguage() {
            return language;
        }

        public void setLanguage(RelationshipData language) {
            this.language = language;
        }

        public RelationshipData getUser() {
            return user;
        }

        public void setUser(RelationshipData user) {
            this.user = user;
        }
    }

    public static class RelationshipData {
        private String id;
        private String type;

        // Getters and setters
        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getType() {
            return type;
        }

        public void setType(String type) {
            this.type = type;
        }
    }
}
