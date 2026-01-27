package com.example.audiototext.model.enums;

public enum Status {
    PROCESSING(0, "processing", "Processing"),
    COMPLETED(1, "completed", "Completed"),
    FAILED(2, "failed", "Failed");

    private final Integer value;
    private final String code;
    private final String displayName;

    Status(Integer value, String code, String displayName) {
        this.value = value;
        this.code = code;
        this.displayName = displayName;
    }

    public Integer getValue() {
        return value;
    }

    public String getCode() {
        return code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public static Status fromCode(String code) {
        for (Status status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown status code: " + code);
    }
}
