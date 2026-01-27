package com.example.audiototext.service.impl;

import com.example.audiototext.exception.ResourceNotFoundException;
import com.example.audiototext.model.TranscriptionLanguage;
import com.example.audiototext.repository.TranscriptionLanguageRepository;
import com.example.audiototext.service.LanguageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional(readOnly = true)
public class LanguageServiceImpl implements LanguageService {

    private static final Logger logger = LoggerFactory.getLogger(LanguageServiceImpl.class);
    private static final String DEFAULT_LANGUAGE_CODE = "auto";

    private final TranscriptionLanguageRepository languageRepository;

    public LanguageServiceImpl(TranscriptionLanguageRepository languageRepository) {
        this.languageRepository = languageRepository;
    }

    @Override
    @Cacheable(value = "languages", key = "#code")
    public TranscriptionLanguage findByCode(String code) {
        if (code == null || code.isEmpty()) {
            return getDefaultLanguage();
        }

        Optional<TranscriptionLanguage> language = languageRepository.findByCodeAndActiveTrue(code);
        if (language.isPresent()) {
            return language.get();
        }

        logger.warn("Language not found by code: {}, returning default", code);
        return getDefaultLanguage();
    }

    @Override
    public TranscriptionLanguage findByName(String name) {
        if (name == null || name.isEmpty()) {
            return getDefaultLanguage();
        }

        Optional<TranscriptionLanguage> language = languageRepository.findByName(name);
        if (language.isPresent() && language.get().getActive()) {
            return language.get();
        }

        logger.warn("Language not found by name: {}, returning default", name);
        return getDefaultLanguage();
    }

    @Override
    @Cacheable(value = "languages", key = "'default'")
    public TranscriptionLanguage getDefaultLanguage() {
        Optional<TranscriptionLanguage> autoLanguage = languageRepository.findByCode(DEFAULT_LANGUAGE_CODE);
        if (autoLanguage.isPresent()) {
            return autoLanguage.get();
        }

        // Fallback: try to find any active language or create a default one
        logger.error("Default 'auto' language not found in database. This should be seeded during migration.");
        throw new ResourceNotFoundException("Default language (auto) not found. Please check database migrations.");
    }

    @Override
    public TranscriptionLanguage resolveLanguage(String languageParam) {
        // If no language provided, default to "auto"
        if (languageParam == null || languageParam.isEmpty() || languageParam.equalsIgnoreCase("auto")) {
            return getDefaultLanguage();
        }

        // Try to find by code first
        TranscriptionLanguage language = findByCode(languageParam);
        if (language != null && !language.getCode().equals(DEFAULT_LANGUAGE_CODE)) {
            return language;
        }

        // Try to find by name
        language = findByName(languageParam);
        if (language != null && !language.getCode().equals(DEFAULT_LANGUAGE_CODE)) {
            return language;
        }

        // If still not found, default to "auto"
        logger.info("Could not resolve language: {}, using default 'auto'", languageParam);
        return getDefaultLanguage();
    }
}
