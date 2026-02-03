package com.example.transcriber.service;

/**
 * Contact information for the user who created a transcription request.
 * Used to resolve recipient (email, name) and preferred email language (en/ar).
 */
public record UserContact(
    String email,
    String name,
    String preferredLanguage
) {
    /** Supported email template languages. */
    public static final String LANG_EN = "en";
    public static final String LANG_AR = "ar";

    public UserContact {
        if (email == null || email.isBlank()) {
            throw new IllegalArgumentException("email is required");
        }
        name = name != null ? name : "";
        preferredLanguage = (preferredLanguage != null && !preferredLanguage.isBlank())
            ? preferredLanguage : LANG_EN;
    }

    public UserContact(String email, String name) {
        this(email, name != null ? name : "", LANG_EN);
    }

    /** Returns template language code (en or ar). */
    public String getTemplateLang() {
        return LANG_AR.equalsIgnoreCase(preferredLanguage) ? LANG_AR : LANG_EN;
    }
}
