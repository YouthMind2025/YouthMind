package ai.youthmind.repository;

import ai.youthmind.entity.PsychologicalStateVector;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for managing psychological state vectors.
 * Provides custom queries for finding by user_id, date ranges, and state similarity search using PostgreSQL vector operations.
 */
@Repository
public interface PsychologicalStateVectorRepository extends JpaRepository<PsychologicalStateVector, UUID> {

    // Basic queries by user
    List<PsychologicalStateVector> findByUserId(UUID userId);
    
    Page<PsychologicalStateVector> findByUserId(UUID userId, Pageable pageable);
    
    List<PsychologicalStateVector> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    // Queries by vector type
    List<PsychologicalStateVector> findByUserIdAndVectorType(UUID userId, PsychologicalStateVector.VectorType vectorType);
    
    Page<PsychologicalStateVector> findByUserIdAndVectorType(UUID userId, PsychologicalStateVector.VectorType vectorType, Pageable pageable);
    
    List<PsychologicalStateVector> findByVectorType(PsychologicalStateVector.VectorType vectorType);
    
    // Queries by source
    List<PsychologicalStateVector> findByUserIdAndSourceType(UUID userId, PsychologicalStateVector.SourceType sourceType);
    
    List<PsychologicalStateVector> findByUserIdAndSourceId(UUID userId, UUID sourceId);
    
    List<PsychologicalStateVector> findByUserIdAndSourceTypeAndSourceId(UUID userId, PsychologicalStateVector.SourceType sourceType, UUID sourceId);
    
    // Date range queries
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.createdAt >= :startDate AND psv.createdAt <= :endDate ORDER BY psv.createdAt DESC")
    List<PsychologicalStateVector> findByUserIdAndDateRange(@Param("userId") UUID userId, 
                                                           @Param("startDate") LocalDateTime startDate, 
                                                           @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = :vectorType AND psv.createdAt >= :startDate AND psv.createdAt <= :endDate ORDER BY psv.createdAt DESC")
    List<PsychologicalStateVector> findByUserIdAndVectorTypeAndDateRange(@Param("userId") UUID userId, 
                                                                        @Param("vectorType") PsychologicalStateVector.VectorType vectorType,
                                                                        @Param("startDate") LocalDateTime startDate, 
                                                                        @Param("endDate") LocalDateTime endDate);
    
    // Recent vectors queries
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.createdAt >= :since ORDER BY psv.createdAt DESC")
    List<PsychologicalStateVector> findRecentByUserId(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = :vectorType AND psv.createdAt >= :since ORDER BY psv.createdAt DESC")
    List<PsychologicalStateVector> findRecentByUserIdAndVectorType(@Param("userId") UUID userId, 
                                                                  @Param("vectorType") PsychologicalStateVector.VectorType vectorType,
                                                                  @Param("since") LocalDateTime since);
    
    // Latest vector queries
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = :vectorType ORDER BY psv.createdAt DESC")
    Optional<PsychologicalStateVector> findLatestByUserIdAndVectorType(@Param("userId") UUID userId, 
                                                                      @Param("vectorType") PsychologicalStateVector.VectorType vectorType);
    
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId ORDER BY psv.createdAt DESC")
    Optional<PsychologicalStateVector> findLatestByUserId(@Param("userId") UUID userId);
    
    // Intensity factor queries
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = 'V_RAW' AND psv.intensityFactor >= :minIntensity ORDER BY psv.intensityFactor DESC")
    List<PsychologicalStateVector> findByUserIdAndMinIntensity(@Param("userId") UUID userId, @Param("minIntensity") Double minIntensity);
    
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = 'V_RAW' AND psv.intensityFactor BETWEEN :minIntensity AND :maxIntensity ORDER BY psv.intensityFactor DESC")
    List<PsychologicalStateVector> findByUserIdAndIntensityRange(@Param("userId") UUID userId, 
                                                                @Param("minIntensity") Double minIntensity, 
                                                                @Param("maxIntensity") Double maxIntensity);
    
    // Vector similarity search using PostgreSQL vector operations
    @Query(value = "SELECT * FROM psychological_state_vectors psv " +
                   "WHERE psv.user_id = :userId AND psv.vector_type = :vectorType " +
                   "ORDER BY psv.vector_data <-> cast(:queryVector as vector) " +
                   "LIMIT :limit", nativeQuery = true)
    List<PsychologicalStateVector> findSimilarVectors(@Param("userId") UUID userId, 
                                                     @Param("vectorType") String vectorType,
                                                     @Param("queryVector") String queryVector, 
                                                     @Param("limit") int limit);
    
    @Query(value = "SELECT * FROM psychological_state_vectors psv " +
                   "WHERE psv.user_id = :userId " +
                   "ORDER BY psv.vector_data <-> cast(:queryVector as vector) " +
                   "LIMIT :limit", nativeQuery = true)
    List<PsychologicalStateVector> findSimilarVectorsByUser(@Param("userId") UUID userId, 
                                                           @Param("queryVector") String queryVector, 
                                                           @Param("limit") int limit);
    
    // Cosine similarity search
    @Query(value = "SELECT *, (1 - (psv.vector_data <=> cast(:queryVector as vector))) as similarity " +
                   "FROM psychological_state_vectors psv " +
                   "WHERE psv.user_id = :userId AND psv.vector_type = :vectorType " +
                   "AND (1 - (psv.vector_data <=> cast(:queryVector as vector))) >= :minSimilarity " +
                   "ORDER BY similarity DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findVectorsByCosineSimilarity(@Param("userId") UUID userId, 
                                                 @Param("vectorType") String vectorType,
                                                 @Param("queryVector") String queryVector, 
                                                 @Param("minSimilarity") Double minSimilarity,
                                                 @Param("limit") int limit);
    
    // Dot product search
    @Query(value = "SELECT *, (psv.vector_data <#> cast(:queryVector as vector)) as dot_product " +
                   "FROM psychological_state_vectors psv " +
                   "WHERE psv.user_id = :userId AND psv.vector_type = :vectorType " +
                   "ORDER BY dot_product DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findVectorsByDotProduct(@Param("userId") UUID userId, 
                                          @Param("vectorType") String vectorType,
                                          @Param("queryVector") String queryVector, 
                                          @Param("limit") int limit);
    
    // Vector magnitude queries
    @Query(value = "SELECT *, sqrt((psv.vector_data <#> psv.vector_data)) as magnitude " +
                   "FROM psychological_state_vectors psv " +
                   "WHERE psv.user_id = :userId AND psv.vector_type = :vectorType " +
                   "ORDER BY magnitude DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findVectorsByMagnitude(@Param("userId") UUID userId, 
                                         @Param("vectorType") String vectorType,
                                         @Param("limit") int limit);
    
    // Statistical queries
    @Query("SELECT COUNT(psv) FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = :vectorType")
    Long countByUserIdAndVectorType(@Param("userId") UUID userId, @Param("vectorType") PsychologicalStateVector.VectorType vectorType);
    
    @Query("SELECT AVG(psv.intensityFactor) FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = 'V_RAW' AND psv.intensityFactor IS NOT NULL")
    Double getAverageIntensityByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT MAX(psv.intensityFactor) FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = 'V_RAW' AND psv.intensityFactor IS NOT NULL")
    Double getMaxIntensityByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT MIN(psv.intensityFactor) FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = 'V_RAW' AND psv.intensityFactor IS NOT NULL")
    Double getMinIntensityByUserId(@Param("userId") UUID userId);
    
    // Vector evolution queries
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = :vectorType ORDER BY psv.createdAt ASC")
    List<PsychologicalStateVector> findVectorEvolutionByUserIdAndType(@Param("userId") UUID userId, 
                                                                     @Param("vectorType") PsychologicalStateVector.VectorType vectorType);
    
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType = :vectorType AND psv.createdAt >= :since ORDER BY psv.createdAt ASC")
    List<PsychologicalStateVector> findVectorEvolutionSince(@Param("userId") UUID userId, 
                                                           @Param("vectorType") PsychologicalStateVector.VectorType vectorType,
                                                           @Param("since") LocalDateTime since);
    
    // Cleanup queries
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.createdAt < :cutoffDate ORDER BY psv.createdAt ASC")
    List<PsychologicalStateVector> findOldVectorsByUserId(@Param("userId") UUID userId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT COUNT(psv) FROM PsychologicalStateVector psv WHERE psv.userId = :userId")
    Long countByUserId(@Param("userId") UUID userId);
    
    // Batch operations
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.vectorType IN :vectorTypes ORDER BY psv.createdAt DESC")
    List<PsychologicalStateVector> findByUserIdAndVectorTypes(@Param("userId") UUID userId, 
                                                             @Param("vectorTypes") List<PsychologicalStateVector.VectorType> vectorTypes);
    
    @Query("SELECT psv FROM PsychologicalStateVector psv WHERE psv.userId = :userId AND psv.sourceType = :sourceType AND psv.sourceId IN :sourceIds ORDER BY psv.createdAt DESC")
    List<PsychologicalStateVector> findByUserIdAndSourceTypeAndSourceIds(@Param("userId") UUID userId, 
                                                                        @Param("sourceType") PsychologicalStateVector.SourceType sourceType,
                                                                        @Param("sourceIds") List<UUID> sourceIds);
    
    // Delete operations
    void deleteByUserId(UUID userId);
    
    void deleteByUserIdAndVectorType(UUID userId, PsychologicalStateVector.VectorType vectorType);
    
    void deleteByUserIdAndCreatedAtBefore(UUID userId, LocalDateTime cutoffDate);
}
