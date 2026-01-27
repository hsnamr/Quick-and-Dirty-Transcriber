package com.example.audiototext.model.enums;

public enum Category {
    MEETING(0, "meeting", "Meeting"),
    INTERVIEW(1, "interview", "Interview"),
    CUSTOMER_SUPPORT_CALL(2, "customer_support_call", "Customer Support Call"),
    SALES_CALL(3, "sales_call", "Sales Call"),
    TRAINING_SESSION(4, "training_session", "Training Session"),
    PODCAST(5, "podcast", "Podcast"),
    PRESENTATION(6, "presentation", "Presentation"),
    VOICE_NOTE(7, "voice_note", "Voice Note"),
    OTHER(8, "other", "Other");

    private final Integer value;
    private final String code;
    private final String displayName;

    Category(Integer value, String code, String displayName) {
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

    public static Category fromCode(String code) {
        for (Category category : values()) {
            if (category.code.equals(code)) {
                return category;
            }
        }
        throw new IllegalArgumentException("Unknown category code: " + code);
    }
}
