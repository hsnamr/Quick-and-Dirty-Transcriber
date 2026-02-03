package com.example.transcriber.service;

import java.util.Optional;

/**
 * Resolves recipient contact (email, name, preferred language) for the user who created a request.
 * Implementations may call an external user service or use configuration.
 * Recipient = user who created the request.
 */
public interface UserEmailResolver {

    /**
     * Resolve contact for the given user ID (the user who created the transcription request).
     *
     * @param userId user ID from the transcription request
     * @return contact if available, empty if user not found or email not available
     */
    Optional<UserContact> resolve(Long userId);
}
