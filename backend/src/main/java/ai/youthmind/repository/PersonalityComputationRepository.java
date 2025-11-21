package ai.youthmind.repository;

import ai.youthmind.entity.PersonalityComputation;
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
 * Repository for managing personality computations.
 * Provides queries for finding personality computations by user, computation period, and date ranges.
 */
@Repository
public interface PersonalityComputationRepository extends JpaRepository<PersonalityComputation, UUID> {

    // Basic queries by user
    List<PersonalityComputation> findByUserId(UUID userId);
    
    Page<PersonalityComputation> findByUserId(UUID userId, Pageable pageable);
    
    List<PersonalityComputation> findByUserIdOrderByComputationDateDesc(UUID userId);
    
    // Queries by computation period
    List<PersonalityComputation> findByUserIdAndComputationPeriod(UUID userId, PersonalityComputation.ComputationPeriod computationPeriod);
    
    Page<PersonalityComputation> findByUserIdAndComputationPeriod(UUID userId, PersonalityComputation.ComputationPeriod computationPeriod, Pageable pageable);
    
    List<PersonalityComputation> findByComputationPeriod(PersonalityComputation.ComputationPeriod computationPeriod);
    
    // Latest computation queries
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId ORDER BY pc.computationDate DESC")
    Optional<PersonalityComputation> findLatestByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationPeriod = :period ORDER BY pc.computationDate DESC")
    Optional<PersonalityComputation> findLatestByUserIdAndPeriod(@Param("userId") UUID userId, 
                                                                @Param("period") PersonalityComputation.ComputationPeriod period);
    
    // Date range queries
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationDate >= :startDate AND pc.computationDate <= :endDate ORDER BY pc.computationDate DESC")
    List<PersonalityComputation> findByUserIdAndDateRange(@Param("userId") UUID userId, 
                                                         @Param("startDate") LocalDateTime startDate, 
                                                         @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationPeriod = :period AND pc.computationDate >= :startDate AND pc.computationDate <= :endDate ORDER BY pc.computationDate DESC")
    List<PersonalityComputation> findByUserIdAndPeriodAndDateRange(@Param("userId") UUID userId, 
                                                                  @Param("period") PersonalityComputation.ComputationPeriod period,
                                                                  @Param("startDate") LocalDateTime startDate, 
                                                                  @Param("endDate") LocalDateTime endDate);
    
    // Recent computations queries
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationDate >= :since ORDER BY pc.computationDate DESC")
    List<PersonalityComputation> findRecentByUserId(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationPeriod = :period AND pc.computationDate >= :since ORDER BY pc.computationDate DESC")
    List<PersonalityComputation> findRecentByUserIdAndPeriod(@Param("userId") UUID userId, 
                                                               @Param("period") PersonalityComputation.ComputationPeriod period,
                                                               @Param("since") LocalDateTime since);
    
    // Confidence score queries
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.finalConfidence >= :minConfidence ORDER BY pc.finalConfidence DESC")
    List<PersonalityComputation> findByUserIdAndMinConfidence(@Param("userId") UUID userId, @Param("minConfidence") Double minConfidence);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.finalConfidence BETWEEN :minConfidence AND :maxConfidence ORDER BY pc.finalConfidence DESC")
    List<PersonalityComputation> findByUserIdAndConfidenceRange(@Param("userId") UUID userId, 
                                                               @Param("minConfidence") Double minConfidence, 
                                                               @Param("maxConfidence") Double maxConfidence);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationPeriod = :period AND pc.finalConfidence >= :minConfidence ORDER BY pc.finalConfidence DESC")
    List<PersonalityComputation> findByUserIdAndPeriodAndMinConfidence(@Param("userId") UUID userId, 
                                                                         @Param("period") PersonalityComputation.ComputationPeriod period,
                                                                         @Param("minConfidence") Double minConfidence);
    
    // High confidence computations
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.finalConfidence >= 0.8 ORDER BY pc.finalConfidence DESC")
    List<PersonalityComputation> findHighConfidenceByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationPeriod = :period AND pc.finalConfidence >= 0.8 ORDER BY pc.finalConfidence DESC")
    List<PersonalityComputation> findHighConfidenceByUserIdAndPeriod(@Param("userId") UUID userId, 
                                                                       @Param("period") PersonalityComputation.ComputationPeriod period);
    
    
    // Personality vector similarity search using PostgreSQL vector operations
    @Query(value = "SELECT *, (pc.active_personality <-> cast(:queryVector as vector)) as distance " +
                   "FROM personality_computations pc " +
                   "WHERE pc.user_id = :userId " +
                   "ORDER BY distance " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarActivePersonalities(@Param("userId") UUID userId, 
                                                 @Param("queryVector") String queryVector, 
                                                 @Param("limit") int limit);
    
    @Query(value = "SELECT *, (pc.subconscious_personality <-> cast(:queryVector as vector)) as distance " +
                   "FROM personality_computations pc " +
                   "WHERE pc.user_id = :userId " +
                   "ORDER BY distance " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarSubconsciousPersonalities(@Param("userId") UUID userId, 
                                                        @Param("queryVector") String queryVector, 
                                                        @Param("limit") int limit);
    
    @Query(value = "SELECT *, (pc.final_personality <-> cast(:queryVector as vector)) as distance " +
                   "FROM personality_computations pc " +
                   "WHERE pc.user_id = :userId " +
                   "ORDER BY distance " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarFinalPersonalities(@Param("userId") UUID userId, 
                                                 @Param("queryVector") String queryVector, 
                                                 @Param("limit") int limit);
    
    // Cosine similarity search
    @Query(value = "SELECT *, (1 - (pc.active_personality <=> cast(:queryVector as vector))) as similarity " +
                   "FROM personality_computations pc " +
                   "WHERE pc.user_id = :userId " +
                   "AND (1 - (pc.active_personality <=> cast(:queryVector as vector))) >= :minSimilarity " +
                   "ORDER BY similarity DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findActivePersonalitiesByCosineSimilarity(@Param("userId") UUID userId, 
                                                             @Param("queryVector") String queryVector, 
                                                             @Param("minSimilarity") Double minSimilarity,
                                                             @Param("limit") int limit);
    
    @Query(value = "SELECT *, (1 - (pc.final_personality <=> cast(:queryVector as vector))) as similarity " +
                   "FROM personality_computations pc " +
                   "WHERE pc.user_id = :userId " +
                   "AND (1 - (pc.final_personality <=> cast(:queryVector as vector))) >= :minSimilarity " +
                   "ORDER BY similarity DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findFinalPersonalitiesByCosineSimilarity(@Param("userId") UUID userId, 
                                                            @Param("queryVector") String queryVector, 
                                                            @Param("minSimilarity") Double minSimilarity,
                                                            @Param("limit") int limit);
    
    // Personality evolution queries
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId ORDER BY pc.computationDate ASC")
    List<PersonalityComputation> findPersonalityEvolutionByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationPeriod = :period ORDER BY pc.computationDate ASC")
    List<PersonalityComputation> findPersonalityEvolutionByUserIdAndPeriod(@Param("userId") UUID userId, 
                                                                             @Param("period") PersonalityComputation.ComputationPeriod period);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationDate >= :since ORDER BY pc.computationDate ASC")
    List<PersonalityComputation> findPersonalityEvolutionSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    // Resonance dimensions queries
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.resonanceDimensions IS NOT NULL ORDER BY pc.computationDate DESC")
    List<PersonalityComputation> findByUserIdWithResonanceDimensions(@Param("userId") UUID userId);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.resonanceDimensions IS NOT NULL AND pc.computationDate >= :since ORDER BY pc.computationDate DESC")
    List<PersonalityComputation> findByUserIdWithResonanceDimensionsSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    // Statistical queries
    @Query("SELECT COUNT(pc) FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationPeriod = :period")
    Long countByUserIdAndPeriod(@Param("userId") UUID userId, @Param("period") PersonalityComputation.ComputationPeriod period);
    
    @Query("SELECT COUNT(pc) FROM PersonalityComputation pc WHERE pc.userId = :userId")
    Long countByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(pc.finalConfidence) FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.finalConfidence IS NOT NULL")
    Double getAverageConfidenceByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT MAX(pc.finalConfidence) FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.finalConfidence IS NOT NULL")
    Double getMaxConfidenceByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT MIN(pc.finalConfidence) FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.finalConfidence IS NOT NULL")
    Double getMinConfidenceByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(pc.finalConfidence) FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationPeriod = :period AND pc.finalConfidence IS NOT NULL")
    Double getAverageConfidenceByUserIdAndPeriod(@Param("userId") UUID userId, @Param("period") PersonalityComputation.ComputationPeriod period);
    
    // Computation period analysis
    @Query("SELECT pc.computationPeriod, COUNT(pc), AVG(pc.finalConfidence) FROM PersonalityComputation pc WHERE pc.userId = :userId GROUP BY pc.computationPeriod ORDER BY COUNT(pc) DESC")
    List<Object[]> getComputationStatsByPeriod(@Param("userId") UUID userId);
    
    @Query("SELECT CAST(pc.computationDate AS date), COUNT(pc), AVG(pc.finalConfidence) FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationDate >= :since GROUP BY CAST(pc.computationDate AS date) ORDER BY CAST(pc.computationDate AS date)")
    List<Object[]> getComputationTimeline(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    // Batch operations
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationPeriod IN :periods ORDER BY pc.computationDate DESC")
    List<PersonalityComputation> findByUserIdAndPeriods(@Param("userId") UUID userId, 
                                                           @Param("periods") List<PersonalityComputation.ComputationPeriod> periods);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId IN :userIds ORDER BY pc.computationDate DESC")
    List<PersonalityComputation> findByUserIds(@Param("userIds") List<UUID> userIds);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId IN :userIds AND pc.computationPeriod = :period ORDER BY pc.computationDate DESC")
    List<PersonalityComputation> findByUserIdsAndPeriod(@Param("userIds") List<UUID> userIds, 
                                                          @Param("period") PersonalityComputation.ComputationPeriod period);
    
    // Existence checks
    boolean existsByUserId(UUID userId);
    
    boolean existsByUserIdAndComputationPeriod(UUID userId, PersonalityComputation.ComputationPeriod period);
    
    boolean existsByUserIdAndComputationDateAfter(UUID userId, LocalDateTime computationDate);
    
    // Cleanup queries
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.userId = :userId AND pc.computationDate < :cutoffDate ORDER BY pc.computationDate ASC")
    List<PersonalityComputation> findOldByUserId(@Param("userId") UUID userId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT pc FROM PersonalityComputation pc WHERE pc.computationDate < :cutoffDate ORDER BY pc.computationDate ASC")
    List<PersonalityComputation> findAllOld(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Delete operations
    void deleteByUserId(UUID userId);
    
    void deleteByUserIdAndComputationPeriod(UUID userId, PersonalityComputation.ComputationPeriod period);
    
    void deleteByUserIdAndComputationDateBefore(UUID userId, LocalDateTime cutoffDate);
    
    void deleteByComputationDateBefore(LocalDateTime cutoffDate);
}
