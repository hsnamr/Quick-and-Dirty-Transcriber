package com.example.transcriber.repository;

import com.example.transcriber.model.TranscriptionLanguage;
import com.example.transcriber.model.TranscriptionRequest;
import com.example.transcriber.model.enums.Category;
import com.example.transcriber.model.enums.Status;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoDbTest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataMongoDbTest
@Testcontainers
class TranscriptionRequestRepositoryTest {

    @Container
    static MongoDBContainer mongo = new MongoDBContainer(DockerImageName.parse("mongo:6"));

    @DynamicPropertySource
    static void setMongoProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", mongo::getConnectionString);
    }

    @Autowired
    private TranscriptionRequestRepository repository;

    @Autowired
    private MongoTemplate mongoTemplate;

    private TranscriptionLanguage language;

    @BeforeEach
    void setUp() {
        language = new TranscriptionLanguage();
        language.setId("1");
        language.setName("English");
        language.setCode("en");
        language.setActive(true);
        mongoTemplate.save(language);
    }

    @Test
    @DisplayName("findByNumericIdAndUserId returns empty when no matching request")
    void findByNumericIdAndUserId_notFound_returnsEmpty() {
        Optional<TranscriptionRequest> result = repository.findByNumericIdAndUserId(999L, 1L);
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByNumericIdAndUserId returns request when found for user")
    void findByNumericIdAndUserId_found_returnsRequest() {
        TranscriptionRequest request = new TranscriptionRequest();
        request.setNumericId(100L);
        request.setUserId(1L);
        request.setFileName("test.mp3");
        request.setStatus(Status.PROCESSING);
        request.setCategory(Category.MEETING);
        request.setLanguage(language);
        request.setDurationSecs(BigDecimal.valueOf(60));
        request.setSpeakersCount(2);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        mongoTemplate.save(request);

        Optional<TranscriptionRequest> result = repository.findByNumericIdAndUserId(100L, 1L);

        assertThat(result).isPresent();
        assertThat(result.get().getNumericId()).isEqualTo(100L);
        assertThat(result.get().getUserId()).isEqualTo(1L);
        assertThat(result.get().getFileName()).isEqualTo("test.mp3");
        assertThat(result.get().getStatus()).isEqualTo(Status.PROCESSING);
    }

    @Test
    @DisplayName("findByNumericIdAndUserId returns empty when request belongs to different user")
    void findByNumericIdAndUserId_differentUser_returnsEmpty() {
        TranscriptionRequest request = new TranscriptionRequest();
        request.setNumericId(100L);
        request.setUserId(1L);
        request.setFileName("test.mp3");
        request.setStatus(Status.PROCESSING);
        request.setCategory(Category.MEETING);
        request.setLanguage(language);
        request.setDurationSecs(BigDecimal.valueOf(60));
        request.setSpeakersCount(2);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        mongoTemplate.save(request);

        Optional<TranscriptionRequest> result = repository.findByNumericIdAndUserId(100L, 2L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("findByUserId returns page of requests for user")
    void findByUserId_returnsPage() {
        TranscriptionRequest request = new TranscriptionRequest();
        request.setNumericId(101L);
        request.setUserId(1L);
        request.setFileName("test.mp3");
        request.setStatus(Status.PROCESSING);
        request.setCategory(Category.MEETING);
        request.setLanguage(language);
        request.setDurationSecs(BigDecimal.valueOf(60));
        request.setSpeakersCount(2);
        request.setCreatedAt(LocalDateTime.now());
        request.setUpdatedAt(LocalDateTime.now());
        mongoTemplate.save(request);

        var page = repository.findByUserId(1L, org.springframework.data.domain.PageRequest.of(0, 10));

        assertThat(page.getContent()).hasSize(1);
        assertThat(page.getContent().get(0).getUserId()).isEqualTo(1L);
    }
}
