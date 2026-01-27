package com.example.audiototext.service;

import org.springframework.web.multipart.MultipartFile;

public interface MetadataExtractionService {

    AudioMetadata extractMetadata(MultipartFile audioFile);

    class AudioMetadata {
        private String fileName;
        private Double durationSeconds;
        private Long fileSize;
        private String contentType;
        private String format;

        // Getters and setters
        public String getFileName() {
            return fileName;
        }

        public void setFileName(String fileName) {
            this.fileName = fileName;
        }

        public Double getDurationSeconds() {
            return durationSeconds;
        }

        public void setDurationSeconds(Double durationSeconds) {
            this.durationSeconds = durationSeconds;
        }

        public Long getFileSize() {
            return fileSize;
        }

        public void setFileSize(Long fileSize) {
            this.fileSize = fileSize;
        }

        public String getContentType() {
            return contentType;
        }

        public void setContentType(String contentType) {
            this.contentType = contentType;
        }

        public String getFormat() {
            return format;
        }

        public void setFormat(String format) {
            this.format = format;
        }
    }
}
