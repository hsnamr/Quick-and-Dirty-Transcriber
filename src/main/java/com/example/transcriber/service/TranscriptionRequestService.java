package com.example.audiototext.service;

import com.example.audiototext.dto.request.CreateTranscriptionRequestDTO;
import com.example.audiototext.dto.request.UpdateTranscriptionRequestDTO;
import com.example.audiototext.dto.response.TranscriptionRequestDTO;
import com.example.audiototext.dto.response.TranscriptionRequestListDTO;

public interface TranscriptionRequestService {

    TranscriptionRequestDTO createTranscriptionRequest(CreateTranscriptionRequestDTO requestDTO);

    TranscriptionRequestListDTO listTranscriptionRequests(
            Integer limitPerPage, Integer page, String search, String status,
            String category, Long languageId, Long startDate, Long endDate,
            String sortBy, String orderBy);

    TranscriptionRequestDTO getTranscriptionRequest(Long id);

    TranscriptionRequestDTO updateTranscriptionRequest(Long id, UpdateTranscriptionRequestDTO requestDTO);

    void deleteTranscriptionRequest(Long id);
}
