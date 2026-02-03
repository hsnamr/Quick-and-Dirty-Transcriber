package com.example.transcriber.controller;

import com.example.transcriber.dto.request.CreateTranscriptionRequestDTO;
import com.example.transcriber.dto.request.UpdateTranscriptionRequestDTO;
import com.example.transcriber.dto.response.TranscriptionRequestDTO;
import com.example.transcriber.dto.response.TranscriptionRequestListDTO;
import com.example.transcriber.service.TranscriptionRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/free-and-dirty-transcriber")
@Tag(name = "Transcription Requests", description = "Create, list, get, update, and delete transcription requests")
@SecurityRequirement(name = "bearerAuth")
public class TranscriptionRequestController {

    private final TranscriptionRequestService transcriptionRequestService;

    public TranscriptionRequestController(TranscriptionRequestService transcriptionRequestService) {
        this.transcriptionRequestService = transcriptionRequestService;
    }

    @Operation(summary = "Create transcription request", description = "Upload an audio file for transcription. Use multipart/form-data with audio file, speakers count (1-5), optional language, and category.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Request created"),
        @ApiResponse(responseCode = "400", description = "Invalid input or validation error"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PostMapping
    public ResponseEntity<?> createTranscriptionRequest(
            @Valid @ModelAttribute CreateTranscriptionRequestDTO requestDTO) {
        TranscriptionRequestDTO response = transcriptionRequestService.createTranscriptionRequest(requestDTO);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "List transcription requests", description = "List transcription requests with optional pagination, search, filters (status, category, language, date range), and sorting.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "List with items, overview stats, filter options, and sorting options"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - missing VIEW_FD_TRANSCRIPTION"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PreAuthorize("hasAuthority('VIEW_FD_TRANSCRIPTION')")
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

    @Operation(summary = "Get transcription request", description = "Get a single transcription request by numeric ID.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Transcription request details"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - missing VIEW_FD_TRANSCRIPTION"),
        @ApiResponse(responseCode = "404", description = "Request not found"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PreAuthorize("hasAuthority('VIEW_FD_TRANSCRIPTION')")
    @GetMapping("/{id}")
    public ResponseEntity<TranscriptionRequestDTO> getTranscriptionRequest(
            @Parameter(description = "Numeric ID of the transcription request") @PathVariable Long id) {
        TranscriptionRequestDTO response = transcriptionRequestService.getTranscriptionRequest(id);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Update transcription request", description = "Update a transcription request (e.g. category). Only allowed when status permits updates.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Updated transcription request"),
        @ApiResponse(responseCode = "400", description = "Invalid input"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "404", description = "Request not found"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PutMapping("/{id}")
    public ResponseEntity<?> updateTranscriptionRequest(
            @Parameter(description = "Numeric ID of the transcription request") @PathVariable Long id,
            @Valid @RequestBody UpdateTranscriptionRequestDTO requestDTO) {
        TranscriptionRequestDTO response = transcriptionRequestService.updateTranscriptionRequest(id, requestDTO);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "Delete transcription request", description = "Delete a transcription request. Only allowed when status permits deletion.")
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Deleted successfully"),
        @ApiResponse(responseCode = "401", description = "Unauthorized"),
        @ApiResponse(responseCode = "403", description = "Forbidden - missing DELETE_FD_TRANSCRIPTION"),
        @ApiResponse(responseCode = "404", description = "Request not found"),
        @ApiResponse(responseCode = "429", description = "Rate limit exceeded")
    })
    @PreAuthorize("hasAuthority('DELETE_FD_TRANSCRIPTION')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> deleteTranscriptionRequest(
            @Parameter(description = "Numeric ID of the transcription request") @PathVariable Long id) {
        transcriptionRequestService.deleteTranscriptionRequest(id);
        return ResponseEntity.ok(Map.of("message", "Audio to text transcription deleted successfully"));
    }
}
