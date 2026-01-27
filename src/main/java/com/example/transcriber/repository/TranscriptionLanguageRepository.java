package com.example.transcriber.repository;

import com.example.transcriber.model.TranscriptionLanguage;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranscriptionLanguageRepository extends MongoRepository<TranscriptionLanguage, String> {

    Optional<TranscriptionLanguage> findByCode(String code);

    Optional<TranscriptionLanguage> findByName(String name);

    Optional<TranscriptionLanguage> findByCodeAndActiveTrue(String code);
}
