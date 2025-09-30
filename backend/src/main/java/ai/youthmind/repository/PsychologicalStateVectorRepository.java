package ai.youthmind.repository;

import ai.youthmind.entity.PsychologicalStateVector;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for psychological state vectors
 * Provides database access for 43-dimensional psychological state vectors
 */
@Repository
public interface PsychologicalStateVectorRepository extends JpaRepository<PsychologicalStateVector, UUID> {
    
    /**
     * Find vectors by user ID and vector type
     * 
     * @param userId User ID
     * @param vectorType Vector type
     * @return List of psychological state vectors
     */
    List<PsychologicalStateVector> findByUserIdAndVectorType(UUID userId, PsychologicalStateVector.VectorType vectorType);
    
    /**
     * Find the latest vector by user ID and vector type
     * 
     * @param userId User ID
     * @param vectorType Vector type
     * @return Latest psychological state vector
     */
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = :vectorType ORDER BY psv.createdAt DESC")
    Optional<PsychologicalStateVector> findLatestByUserIdAndVectorType(@Param("userId") UUID userId, 
                                                                      @Param("vectorType") PsychologicalStateVector.VectorType vectorType);
    
    /**
     * Find vectors by user ID and source type
     * 
     * @param userId User ID
     * @param sourceType Source type
     * @return List of psychological state vectors
     */
    List<PsychologicalStateVector> findByUserIdAndSourceType(UUID userId, PsychologicalStateVector.SourceType sourceType);
    
    /**
     * Find vectors by user ID within a time range
     * 
     * @param userId User ID
     * @param startTime Start time
     * @param endTime End time
     * @return List of psychological state vectors
     */
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.createdAt BETWEEN :startTime AND :endTime ORDER BY psv.createdAt DESC")
    List<PsychologicalStateVector> findByUserIdAndCreatedAtBetween(@Param("userId") UUID userId,
                                                                  @Param("startTime") LocalDateTime startTime,
                                                                  @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find vectors by source ID
     * 
     * @param sourceId Source ID (post ID, interaction ID, etc.)
     * @return List of psychological state vectors
     */
    List<PsychologicalStateVector> findBySourceId(UUID sourceId);
    
    /**
     * Find vectors by vector type and source type
     * 
     * @param vectorType Vector type
     * @param sourceType Source type
     * @return List of psychological state vectors
     */
    List<PsychologicalStateVector> findByVectorTypeAndSourceType(PsychologicalStateVector.VectorType vectorType,
                                                                PsychologicalStateVector.SourceType sourceType);
    
    /**
     * Count vectors by user ID and vector type
     * 
     * @param userId User ID
     * @param vectorType Vector type
     * @return Count of vectors
     */
    long countByUserIdAndVectorType(UUID userId, PsychologicalStateVector.VectorType vectorType);
    
    /**
     * Find vectors with intensity factor above threshold
     * 
     * @param threshold Intensity factor threshold
     * @return List of psychological state vectors
     */
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.intensityFactor >= :threshold ORDER BY psv.intensityFactor DESC")
    List<PsychologicalStateVector> findByIntensityFactorAboveThreshold(@Param("threshold") Double threshold);
    
    /**
     * Find vectors by user ID with intensity factor
     * 
     * @param userId User ID
     * @return List of psychological state vectors with intensity factor
     */
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.intensityFactor IS NOT NULL ORDER BY psv.createdAt DESC")
    List<PsychologicalStateVector> findByUserIdWithIntensityFactor(@Param("userId") UUID userId);
    
    /**
     * Delete old vectors by user ID and vector type before a specific date
     * 
     * @param userId User ID
     * @param vectorType Vector type
     * @param beforeDate Date before which to delete
     * @return Number of deleted vectors
     */
    @Query("DELETE FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = :vectorType AND psv.createdAt < :beforeDate")
    int deleteByUserIdAndVectorTypeAndCreatedAtBefore(@Param("userId") UUID userId,
                                                     @Param("vectorType") PsychologicalStateVector.VectorType vectorType,
                                                     @Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Find vectors by user ID ordered by creation time descending
     * 
     * @param userId User ID
     * @return List of psychological state vectors
     */
    List<PsychologicalStateVector> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Find vectors by user ID and vector type ordered by creation time descending
     * 
     * @param userId User ID
     * @param vectorType Vector type
     * @return List of psychological state vectors
     */
    List<PsychologicalStateVector> findByUserIdAndVectorTypeOrderByCreatedAtDesc(UUID userId, 
                                                                                PsychologicalStateVector.VectorType vectorType);
    
    /**
     * Check if vector exists by user ID and vector type
     * 
     * @param userId User ID
     * @param vectorType Vector type
     * @return true if vector exists
     */
    boolean existsByUserIdAndVectorType(UUID userId, PsychologicalStateVector.VectorType vectorType);
    
    /**
     * Find vectors by user ID and source ID
     * 
     * @param userId User ID
     * @param sourceId Source ID
     * @return List of psychological state vectors
     */
    List<PsychologicalStateVector> findByUserIdAndSourceId(UUID userId, UUID sourceId);
    
    /**
     * Find vectors by user ID and vector type and source type
     * 
     * @param userId User ID
     * @param vectorType Vector type
     * @param sourceType Source type
     * @return List of psychological state vectors
     */
    List<PsychologicalStateVector> findByUserIdAndVectorTypeAndSourceType(UUID userId,
                                                                          PsychologicalStateVector.VectorType vectorType,
                                                                          PsychologicalStateVector.SourceType sourceType);
}