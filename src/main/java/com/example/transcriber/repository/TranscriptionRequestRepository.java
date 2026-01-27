package com.example.transcriber.repository;

import com.example.transcriber.model.TranscriptionRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TranscriptionRequestRepository extends MongoRepository<TranscriptionRequest, String> {

    Optional<TranscriptionRequest> findByIdAndUserId(String id, Long userId);

    Optional<TranscriptionRequest> findByNumericIdAndUserId(Long numericId, Long userId);

    Optional<TranscriptionRequest> findByNumericId(Long numericId);

    Page<TranscriptionRequest> findByUserId(Long userId, Pageable pageable);

    // MongoDB query methods for filtering
    Page<TranscriptionRequest> findByUserIdAndStatus(Long userId, com.example.transcriber.model.enums.Status status, Pageable pageable);

    Page<TranscriptionRequest> findByUserIdAndCategory(Long userId, com.example.transcriber.model.enums.Category category, Pageable pageable);

    @Query("{ 'userId': ?0, 'fileName': { $regex: ?1, $options: 'i' } }")
    Page<TranscriptionRequest> findByUserIdAndFileNameContainingIgnoreCase(Long userId, String fileName, Pageable pageable);

    @Query("{ 'userId': ?0, 'createdAt': { $gte: ?1, $lte: ?2 } }")
    Page<TranscriptionRequest> findByUserIdAndCreatedAtBetween(Long userId, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, Pageable pageable);
}
