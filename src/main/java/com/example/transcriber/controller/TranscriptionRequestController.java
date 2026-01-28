package com.example.transcriber.controller;

import com.example.transcriber.dto.request.CreateTranscriptionRequestDTO;
import com.example.transcriber.dto.request.UpdateTranscriptionRequestDTO;
import com.example.transcriber.dto.response.TranscriptionRequestDTO;
import com.example.transcriber.dto.response.TranscriptionRequestListDTO;
import com.example.transcriber.service.TranscriptionRequestService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/free-and-dirty-transcriber")
public class TranscriptionRequestController {

    private final TranscriptionRequestService transcriptionRequestService;

    public TranscriptionRequestController(TranscriptionRequestService transcriptionRequestService) {
        this.transcriptionRequestService = transcriptionRequestService;
    }

    /**
     * Create a new transcription request
     * 
     * Handles multipart/form-data file upload using MultipartFile.
     * The audio file is received as a MultipartFile and processed for transcription.
     * 
     * @param requestDTO DTO containing audio file (MultipartFile), speakers count, language, and category
     * @return TranscriptionRequestDTO with created request details
     */
    @PostMapping
    public ResponseEntity<?> createTranscriptionRequest(
            @Valid @ModelAttribute CreateTranscriptionRequestDTO requestDTO) {
        TranscriptionRequestDTO response = transcriptionRequestService.createTranscriptionRequest(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<TranscriptionRequestListDTO> listTranscriptionRequests(
            @RequestParam(required = false) Integer limit_per_page,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Long language_id,
            @RequestParam(required = false) Long start_date,
            @RequestParam(required = false) Long end_date,
            @RequestParam(required = false) String sort_by,
            @RequestParam(required = false) String order_by) {
        TranscriptionRequestListDTO response = transcriptionRequestService.listTranscriptionRequests(
                limit_per_page, page, search, status, category, language_id,
                start_date, end_date, sort_by, order_by);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TranscriptionRequestDTO> getTranscriptionRequest(@PathVariable Long id) {
        TranscriptionRequestDTO response = transcriptionRequestService.getTranscriptionRequest(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateTranscriptionRequest(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTranscriptionRequestDTO requestDTO) {
        TranscriptionRequestDTO response = transcriptionRequestService.updateTranscriptionRequest(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteTranscriptionRequest(@PathVariable Long id) {
        transcriptionRequestService.deleteTranscriptionRequest(id);
        return ResponseEntity.ok().build();
    }
}
