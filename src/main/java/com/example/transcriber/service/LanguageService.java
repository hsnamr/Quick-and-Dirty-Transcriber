package com.example.transcriber.service;

import com.example.transcriber.model.TranscriptionLanguage;

import java.util.List;

public interface LanguageService {

    TranscriptionLanguage findByCode(String code);

    TranscriptionLanguage findByName(String name);

    TranscriptionLanguage getDefaultLanguage();

    TranscriptionLanguage resolveLanguage(String languageParam);
    
    List<TranscriptionLanguage> findAllActiveLanguages();
}
