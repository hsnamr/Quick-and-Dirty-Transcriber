package com.example.audiototext.dto.request;

import jakarta.validation.constraints.NotNull;

public class UpdateTranscriptionRequestDTO {

    @NotNull(message = "Category is required")
    private String category;

    // Getters and setters
    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }
}
