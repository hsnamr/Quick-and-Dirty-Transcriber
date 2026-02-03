package com.example.transcriber.controller;

import com.example.transcriber.dto.response.TranscriptionRequestDTO;
import com.example.transcriber.dto.response.TranscriptionRequestListDTO;
import com.example.transcriber.exception.ResourceNotFoundException;
import com.example.transcriber.exception.ValidationException;
import com.example.transcriber.security.JwtAuthenticationFilter;
import com.example.transcriber.service.TranscriptionRequestService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(TranscriptionRequestController.class)
@Import(com.example.transcriber.config.SecurityConfig.class)
class TranscriptionRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private TranscriptionRequestService transcriptionRequestService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    private static final Long REQUEST_ID = 100L;

    @Test
    @DisplayName("GET /api/free-and-dirty-transcriber returns 200 when user has VIEW_FD_TRANSCRIPTION")
    @WithMockUser(authorities = "VIEW_FD_TRANSCRIPTION")
    void listTranscriptionRequests_hasPermission_returns200() throws Exception {
        TranscriptionRequestListDTO listDTO = new TranscriptionRequestListDTO();
        when(transcriptionRequestService.listTranscriptionRequests(any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .thenReturn(listDTO);

        mockMvc.perform(get("/api/free-and-dirty-transcriber")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk());
        verify(transcriptionRequestService).listTranscriptionRequests(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/free-and-dirty-transcriber returns 403 when user lacks VIEW_FD_TRANSCRIPTION")
    @WithMockUser(authorities = "OTHER_PERMISSION")
    void listTranscriptionRequests_noPermission_returns403() throws Exception {
        mockMvc.perform(get("/api/free-and-dirty-transcriber")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        verify(transcriptionRequestService, never()).listTranscriptionRequests(any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("GET /api/free-and-dirty-transcriber/{id} returns 200 when found")
    @WithMockUser(authorities = "VIEW_FD_TRANSCRIPTION")
    void getTranscriptionRequest_found_returns200() throws Exception {
        TranscriptionRequestDTO dto = new TranscriptionRequestDTO();
        dto.setId(String.valueOf(REQUEST_ID));
        when(transcriptionRequestService.getTranscriptionRequest(REQUEST_ID)).thenReturn(dto);

        mockMvc.perform(get("/api/free-and-dirty-transcriber/{id}", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(String.valueOf(REQUEST_ID)));
        verify(transcriptionRequestService).getTranscriptionRequest(REQUEST_ID);
    }

    @Test
    @DisplayName("GET /api/free-and-dirty-transcriber/{id} returns 404 when not found")
    @WithMockUser(authorities = "VIEW_FD_TRANSCRIPTION")
    void getTranscriptionRequest_notFound_returns404() throws Exception {
        when(transcriptionRequestService.getTranscriptionRequest(REQUEST_ID))
                .thenThrow(new ResourceNotFoundException("Transcription request not found: " + REQUEST_ID));

        mockMvc.perform(get("/api/free-and-dirty-transcriber/{id}", REQUEST_ID)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error", containsString("not found")));
    }

    @Test
    @DisplayName("PUT /api/free-and-dirty-transcriber/{id} returns 200 when updated")
    @WithMockUser(authorities = "SUBMIT_FD_TRANSCRIPTION")
    void updateTranscriptionRequest_valid_returns200() throws Exception {
        TranscriptionRequestDTO dto = new TranscriptionRequestDTO();
        dto.setId(String.valueOf(REQUEST_ID));
        when(transcriptionRequestService.updateTranscriptionRequest(eq(REQUEST_ID), any())).thenReturn(dto);

        mockMvc.perform(put("/api/free-and-dirty-transcriber/{id}", REQUEST_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"interview\"}"))
                .andExpect(status().isOk());
        verify(transcriptionRequestService).updateTranscriptionRequest(eq(REQUEST_ID), any());
    }

    @Test
    @DisplayName("PUT /api/free-and-dirty-transcriber/{id} returns 403 when user lacks SUBMIT_FD_TRANSCRIPTION")
    @WithMockUser(authorities = "VIEW_FD_TRANSCRIPTION")
    void updateTranscriptionRequest_noPermission_returns403() throws Exception {
        mockMvc.perform(put("/api/free-and-dirty-transcriber/{id}", REQUEST_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"category\":\"interview\"}"))
                .andExpect(status().isForbidden());
        verify(transcriptionRequestService, org.mockito.Mockito.never()).updateTranscriptionRequest(any(), any());
    }

    @Test
    @DisplayName("DELETE /api/free-and-dirty-transcriber/{id} returns 200 with message when deleted")
    @WithMockUser(authorities = "DELETE_FD_TRANSCRIPTION")
    void deleteTranscriptionRequest_success_returns200WithMessage() throws Exception {
        mockMvc.perform(delete("/api/free-and-dirty-transcriber/{id}", REQUEST_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Audio to text transcription deleted successfully"));
        verify(transcriptionRequestService).deleteTranscriptionRequest(REQUEST_ID);
    }

    @Test
    @DisplayName("DELETE /api/free-and-dirty-transcriber/{id} returns 404 when not found")
    @WithMockUser(authorities = "DELETE_FD_TRANSCRIPTION")
    void deleteTranscriptionRequest_notFound_returns404() throws Exception {
        when(transcriptionRequestService.deleteTranscriptionRequest(REQUEST_ID))
                .thenThrow(new ResourceNotFoundException("Transcription request not found: " + REQUEST_ID));

        mockMvc.perform(delete("/api/free-and-dirty-transcriber/{id}", REQUEST_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }

    @Test
    @DisplayName("DELETE /api/free-and-dirty-transcriber/{id} returns 403 when user lacks DELETE_FD_TRANSCRIPTION")
    @WithMockUser(authorities = "VIEW_FD_TRANSCRIPTION")
    void deleteTranscriptionRequest_noPermission_returns403() throws Exception {
        mockMvc.perform(delete("/api/free-and-dirty-transcriber/{id}", REQUEST_ID)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
        verify(transcriptionRequestService, never()).deleteTranscriptionRequest(any());
    }
}
