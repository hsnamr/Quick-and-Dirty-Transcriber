package com.example.audiototext.config;

import com.example.audiototext.model.TranscriptionLanguage;
import com.example.audiototext.repository.TranscriptionLanguageRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * Initialize MongoDB with default languages
 * Replaces Flyway migrations for MongoDB
 */
@Configuration
public class MongoDataInitializer {

    private static final Logger logger = LoggerFactory.getLogger(MongoDataInitializer.class);

    @Bean
    CommandLineRunner initDatabase(TranscriptionLanguageRepository languageRepository) {
        return args -> {
            // Check if languages already exist
            if (languageRepository.count() > 0) {
                logger.info("Languages already exist in database, skipping initialization");
                return;
            }

            logger.info("Initializing default languages in MongoDB");

            List<TranscriptionLanguage> languages = Arrays.asList(
                    createLanguage("Auto Detect", "auto", true, true),
                    createLanguage("English", "en", true, true),
                    createLanguage("Arabic", "ar", true, true),
                    createLanguage("Spanish", "es", true, false),
                    createLanguage("French", "fr", true, false),
                    createLanguage("German", "de", true, false),
                    createLanguage("Italian", "it", true, false),
                    createLanguage("Portuguese", "pt", true, false),
                    createLanguage("Russian", "ru", true, false),
                    createLanguage("Chinese", "zh", true, false),
                    createLanguage("Japanese", "ja", true, false),
                    createLanguage("Korean", "ko", true, false)
            );

            languageRepository.saveAll(languages);
            logger.info("Initialized {} default languages", languages.size());
        };
    }

    private TranscriptionLanguage createLanguage(String name, String code, boolean active, boolean popular) {
        TranscriptionLanguage language = new TranscriptionLanguage();
        language.setName(name);
        language.setCode(code);
        language.setActive(active);
        language.setPopular(popular);
        language.setCreatedAt(LocalDateTime.now());
        language.setUpdatedAt(LocalDateTime.now());
        return language;
    }
}
