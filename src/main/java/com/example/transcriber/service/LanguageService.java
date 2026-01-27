package com.example.audiototext.service;

import com.example.audiototext.model.TranscriptionLanguage;

import java.util.List;

public interface LanguageService {

    TranscriptionLanguage findByCode(String code);

    TranscriptionLanguage findByName(String name);

    TranscriptionLanguage getDefaultLanguage();

    TranscriptionLanguage resolveLanguage(String languageParam);
    
    List<TranscriptionLanguage> findAllActiveLanguages();
}
