package com.example.transcriber.service;

import com.example.transcriber.dto.request.CreateTranscriptionRequestDTO;
import com.example.transcriber.dto.request.UpdateTranscriptionRequestDTO;
import com.example.transcriber.dto.response.TranscriptionRequestDTO;
import com.example.transcriber.dto.response.TranscriptionRequestListDTO;

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
