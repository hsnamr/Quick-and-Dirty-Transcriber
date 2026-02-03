package com.example.transcriber.service;

import com.example.transcriber.dto.request.UpdateTranscriptionRequestDTO;
import com.example.transcriber.dto.response.TranscriptionRequestDTO;
import com.example.transcriber.exception.ResourceNotFoundException;
import com.example.transcriber.exception.ValidationException;
import com.example.transcriber.model.TranscriptionLanguage;
import com.example.transcriber.model.TranscriptionRequest;
import com.example.transcriber.model.enums.Category;
import com.example.transcriber.model.enums.Status;
import com.example.transcriber.repository.TranscriptionRequestRepository;
import com.example.transcriber.security.UserContext;
import com.example.transcriber.service.impl.TranscriptionRequestServiceImpl;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.MongoTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TranscriptionRequestServiceTest {

    @Mock
    private TranscriptionRequestRepository requestRepository;
    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private MetadataExtractionService metadataExtractionService;
    @Mock
    private LanguageService languageService;
    @Mock
    private QuotaService quotaService;
    @Mock
    private TranscriptionEngineService transcriptionEngineService;
    @Mock
    private StatusManagementService statusManagementService;
    @Mock
    private EmailService emailService;
    @Mock
    private SequenceService sequenceService;
    @Mock
    private MessageQueueService messageQueueService;

    @InjectMocks
    private TranscriptionRequestServiceImpl transcriptionRequestService;

    private static final Long USER_ID = 1L;
    private static final Long REQUEST_ID = 100L;

    @BeforeEach
    void setUp() {
        UserContext.setUserId(USER_ID);
    }

    @AfterEach
    void tearDown() {
        UserContext.clear();
    }

    private TranscriptionRequest createRequest(Status status) {
        TranscriptionLanguage lang = new TranscriptionLanguage();
        lang.setId("1");
        lang.setName("English");
        lang.setCode("en");

        TranscriptionRequest request = new TranscriptionRequest();
        request.setId("abc123");
        request.setNumericId(REQUEST_ID);
        request.setUserId(USER_ID);
        request.setFileName("test.mp3");
        request.setStatus(status);
        request.setCategory(Category.MEETING);
        request.setLanguage(lang);
        request.setDurationSecs(BigDecimal.valueOf(60));
        request.setSpeakersCount(2);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        return request;
    }

    @Test
    @DisplayName("getTranscriptionRequest throws ValidationException when user context is null")
    void getTranscriptionRequest_nullUserContext_throwsValidationException() {
        UserContext.clear();

        assertThatThrownBy(() -> transcriptionRequestService.getTranscriptionRequest(REQUEST_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User context not available");
        verify(requestRepository, never()).findByNumericIdAndUserId(any(), any());
    }

    @Test
    @DisplayName("getTranscriptionRequest throws ResourceNotFoundException when request not found")
    void getTranscriptionRequest_notFound_throwsResourceNotFoundException() {
        when(requestRepository.findByNumericIdAndUserId(REQUEST_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transcriptionRequestService.getTranscriptionRequest(REQUEST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("getTranscriptionRequest throws ValidationException when status is not COMPLETED")
    void getTranscriptionRequest_notCompleted_throwsValidationException() {
        TranscriptionRequest request = createRequest(Status.PROCESSING);
        when(requestRepository.findByNumericIdAndUserId(REQUEST_ID, USER_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> transcriptionRequestService.getTranscriptionRequest(REQUEST_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("not completed yet");
    }

    @Test
    @DisplayName("getTranscriptionRequest returns DTO when request is COMPLETED")
    void getTranscriptionRequest_completed_returnsDto() {
        TranscriptionRequest request = createRequest(Status.COMPLETED);
        when(requestRepository.findByNumericIdAndUserId(REQUEST_ID, USER_ID)).thenReturn(Optional.of(request));

        TranscriptionRequestDTO result = transcriptionRequestService.getTranscriptionRequest(REQUEST_ID);

        assertThat(result).isNotNull();
        assertThat(result.getAttributes()).isNotNull();
        assertThat(result.getAttributes().getFileName()).isEqualTo("test.mp3");
        assertThat(result.getAttributes().getIsCompleted()).isTrue();
        verify(messageQueueService).sendFrontendBroadcast(request);
    }

    @Test
    @DisplayName("updateTranscriptionRequest throws ResourceNotFoundException when request not found")
    void updateTranscriptionRequest_notFound_throwsResourceNotFoundException() {
        when(requestRepository.findByNumericIdAndUserId(REQUEST_ID, USER_ID)).thenReturn(Optional.empty());
        UpdateTranscriptionRequestDTO dto = new UpdateTranscriptionRequestDTO();
        dto.setCategory("meeting");

        assertThatThrownBy(() -> transcriptionRequestService.updateTranscriptionRequest(REQUEST_ID, dto))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
    }

    @Test
    @DisplayName("updateTranscriptionRequest throws ValidationException when category invalid")
    void updateTranscriptionRequest_invalidCategory_throwsValidationException() {
        TranscriptionRequest request = createRequest(Status.PROCESSING);
        when(requestRepository.findByNumericIdAndUserId(REQUEST_ID, USER_ID)).thenReturn(Optional.of(request));
        UpdateTranscriptionRequestDTO dto = new UpdateTranscriptionRequestDTO();
        dto.setCategory("invalid_category");

        assertThatThrownBy(() -> transcriptionRequestService.updateTranscriptionRequest(REQUEST_ID, dto))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("Invalid category");
        verify(requestRepository, never()).save(any());
    }

    @Test
    @DisplayName("updateTranscriptionRequest saves and returns DTO when valid")
    void updateTranscriptionRequest_valid_returnsUpdatedDto() {
        TranscriptionRequest request = createRequest(Status.PROCESSING);
        when(requestRepository.findByNumericIdAndUserId(REQUEST_ID, USER_ID)).thenReturn(Optional.of(request));
        when(requestRepository.save(any(TranscriptionRequest.class))).thenAnswer(i -> i.getArgument(0));
        UpdateTranscriptionRequestDTO dto = new UpdateTranscriptionRequestDTO();
        dto.setCategory("interview");

        TranscriptionRequestDTO result = transcriptionRequestService.updateTranscriptionRequest(REQUEST_ID, dto);

        assertThat(result).isNotNull();
        verify(requestRepository).save(any(TranscriptionRequest.class));
    }

    @Test
    @DisplayName("deleteTranscriptionRequest throws ResourceNotFoundException when request not found")
    void deleteTranscriptionRequest_notFound_throwsResourceNotFoundException() {
        when(requestRepository.findByNumericIdAndUserId(REQUEST_ID, USER_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> transcriptionRequestService.deleteTranscriptionRequest(REQUEST_ID))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("not found");
        verify(requestRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteTranscriptionRequest throws ValidationException when status is PROCESSING")
    void deleteTranscriptionRequest_processing_throwsValidationException() {
        TranscriptionRequest request = createRequest(Status.PROCESSING);
        when(requestRepository.findByNumericIdAndUserId(REQUEST_ID, USER_ID)).thenReturn(Optional.of(request));

        assertThatThrownBy(() -> transcriptionRequestService.deleteTranscriptionRequest(REQUEST_ID))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("cannot be deleted while processing");
        verify(requestRepository, never()).delete(any());
    }

    @Test
    @DisplayName("deleteTranscriptionRequest deletes when status is COMPLETED")
    void deleteTranscriptionRequest_completed_deletes() {
        TranscriptionRequest request = createRequest(Status.COMPLETED);
        when(requestRepository.findByNumericIdAndUserId(REQUEST_ID, USER_ID)).thenReturn(Optional.of(request));

        transcriptionRequestService.deleteTranscriptionRequest(REQUEST_ID);

        verify(requestRepository).delete(request);
    }

    @Test
    @DisplayName("listTranscriptionRequests throws ValidationException when user context is null")
    void listTranscriptionRequests_nullUserContext_throwsValidationException() {
        UserContext.clear();

        assertThatThrownBy(() -> transcriptionRequestService.listTranscriptionRequests(
                10, 1, null, null, null, null, null, null, null, null))
                .isInstanceOf(ValidationException.class)
                .hasMessageContaining("User context not available");
    }
}
