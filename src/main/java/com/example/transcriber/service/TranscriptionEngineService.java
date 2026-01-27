package com.example.audiototext.service;

import org.springframework.web.multipart.MultipartFile;

public interface TranscriptionEngineService {

    TranscriptionResult transcribe(MultipartFile audioFile, String languageCode, Integer speakersCount);

    class TranscriptionResult {
        private String text;
        private Object jsonResult; // Structured result with timestamps, words, etc.
        private Object metadata; // Additional metadata
        private Double processingTimeSeconds;

        public String getText() {
            return text;
        }

        public void setText(String text) {
            this.text = text;
        }

        public Object getJsonResult() {
            return jsonResult;
        }

        public void setJsonResult(Object jsonResult) {
            this.jsonResult = jsonResult;
        }

        public Object getMetadata() {
            return metadata;
        }

        public void setMetadata(Object metadata) {
            this.metadata = metadata;
        }

        public Double getProcessingTimeSeconds() {
            return processingTimeSeconds;
        }

        public void setProcessingTimeSeconds(Double processingTimeSeconds) {
            this.processingTimeSeconds = processingTimeSeconds;
        }
    }
}
