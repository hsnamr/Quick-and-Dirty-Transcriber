package com.example.transcriber.service.impl;

import com.example.transcriber.service.UserContact;
import com.example.transcriber.service.UserEmailResolver;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.Optional;

/**
 * Default implementation that uses configuration for the recipient.
 * Use when no external user service is available (e.g. dev/test).
 * In production, provide a UserEmailResolver bean that calls your user service.
 */
@Component
@ConditionalOnMissingBean(UserEmailResolver.class)
public class ConfigurableUserEmailResolver implements UserEmailResolver {

    @Value("${transcriber.email.recipient-override:}")
    private String recipientOverride;

    @Value("${transcriber.email.recipient-name:User}")
    private String recipientName;

    @Value("${transcriber.email.default-language:en}")
    private String defaultLanguage;

    @Override
    public Optional<UserContact> resolve(Long userId) {
        if (userId == null) {
            return Optional.empty();
        }
        if (recipientOverride != null && !recipientOverride.isBlank()) {
            return Optional.of(new UserContact(
                recipientOverride.trim(),
                recipientName,
                defaultLanguage
            ));
        }
        return Optional.empty();
    }
}
