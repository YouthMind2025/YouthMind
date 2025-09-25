package ai.youthmind.repository;

import ai.youthmind.entity.UserInteractionModulation;
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
 * Repository for managing user interaction modulation parameters.
 * Provides queries for finding user interactions by type, date ranges, and interaction patterns.
 */
@Repository
public interface UserInteractionModulationRepository extends JpaRepository<UserInteractionModulation, UUID> {

    // Basic queries by user
    List<UserInteractionModulation> findByUserId(UUID userId);
    
    Page<UserInteractionModulation> findByUserId(UUID userId, Pageable pageable);
    
    List<UserInteractionModulation> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    // Queries by interaction type
    List<UserInteractionModulation> findByUserIdAndInteractionType(UUID userId, UserInteractionModulation.InteractionType interactionType);
    
    Page<UserInteractionModulation> findByUserIdAndInteractionType(UUID userId, UserInteractionModulation.InteractionType interactionType, Pageable pageable);
    
    List<UserInteractionModulation> findByInteractionType(UserInteractionModulation.InteractionType interactionType);
    
    
    // Date range queries
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.createdAt >= :startDate AND uim.createdAt <= :endDate ORDER BY uim.createdAt DESC")
    List<UserInteractionModulation> findByUserIdAndDateRange(@Param("userId") UUID userId, 
                                                            @Param("startDate") LocalDateTime startDate, 
                                                            @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.interactionType = :interactionType AND uim.createdAt >= :startDate AND uim.createdAt <= :endDate ORDER BY uim.createdAt DESC")
    List<UserInteractionModulation> findByUserIdAndInteractionTypeAndDateRange(@Param("userId") UUID userId, 
                                                                              @Param("interactionType") UserInteractionModulation.InteractionType interactionType,
                                                                              @Param("startDate") LocalDateTime startDate, 
                                                                              @Param("endDate") LocalDateTime endDate);
    
    
    // Recent interactions queries
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.createdAt >= :since ORDER BY uim.createdAt DESC")
    List<UserInteractionModulation> findRecentByUserId(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.interactionType = :interactionType AND uim.createdAt >= :since ORDER BY uim.createdAt DESC")
    List<UserInteractionModulation> findRecentByUserIdAndInteractionType(@Param("userId") UUID userId, 
                                                                        @Param("interactionType") UserInteractionModulation.InteractionType interactionType,
                                                                        @Param("since") LocalDateTime since);
    
    // Latest interaction queries
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.interactionType = :interactionType ORDER BY uim.createdAt DESC")
    Optional<UserInteractionModulation> findLatestByUserIdAndInteractionType(@Param("userId") UUID userId, 
                                                                            @Param("interactionType") UserInteractionModulation.InteractionType interactionType);
    
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId ORDER BY uim.createdAt DESC")
    Optional<UserInteractionModulation> findLatestByUserId(@Param("userId") UUID userId);
    
    // Parameter range queries
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.betaFactor >= :minBeta AND uim.betaFactor <= :maxBeta ORDER BY uim.betaFactor DESC")
    List<UserInteractionModulation> findByUserIdAndBetaRange(@Param("userId") UUID userId, 
                                                            @Param("minBeta") Double minBeta, 
                                                            @Param("maxBeta") Double maxBeta);
    
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.muFactor >= :minMu AND uim.muFactor <= :maxMu ORDER BY uim.muFactor DESC")
    List<UserInteractionModulation> findByUserIdAndMuRange(@Param("userId") UUID userId, 
                                                          @Param("minMu") Double minMu, 
                                                          @Param("maxMu") Double maxMu);
    
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.wsWeight >= :minWs AND uim.wsWeight <= :maxWs ORDER BY uim.wsWeight DESC")
    List<UserInteractionModulation> findByUserIdAndWsRange(@Param("userId") UUID userId, 
                                                          @Param("minWs") Double minWs, 
                                                          @Param("maxWs") Double maxWs);
    
    // Interaction pattern analysis
    @Query("SELECT uim.interactionType, COUNT(uim), AVG(uim.betaFactor), AVG(uim.muFactor), AVG(uim.wsWeight) " +
           "FROM UserInteractionModulation uim WHERE uim.userId = :userId GROUP BY uim.interactionType ORDER BY COUNT(uim) DESC")
    List<Object[]> getInteractionStatsByType(@Param("userId") UUID userId);
    
    @Query("SELECT DATE(uim.createdAt), COUNT(uim), AVG(uim.betaFactor), AVG(uim.muFactor), AVG(uim.wsWeight) " +
           "FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.createdAt >= :since " +
           "GROUP BY DATE(uim.createdAt) ORDER BY DATE(uim.createdAt)")
    List<Object[]> getInteractionTimeline(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    // Parameter evolution queries
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.interactionType = :interactionType ORDER BY uim.createdAt ASC")
    List<UserInteractionModulation> findParameterEvolutionByUserIdAndInteractionType(@Param("userId") UUID userId, 
                                                                                    @Param("interactionType") UserInteractionModulation.InteractionType interactionType);
    
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.interactionType = :interactionType AND uim.createdAt >= :since ORDER BY uim.createdAt ASC")
    List<UserInteractionModulation> findParameterEvolutionSince(@Param("userId") UUID userId, 
                                                               @Param("interactionType") UserInteractionModulation.InteractionType interactionType,
                                                               @Param("since") LocalDateTime since);
    
    // Statistical queries
    @Query("SELECT COUNT(uim) FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.interactionType = :interactionType")
    Long countByUserIdAndInteractionType(@Param("userId") UUID userId, @Param("interactionType") UserInteractionModulation.InteractionType interactionType);
    
    @Query("SELECT COUNT(uim) FROM UserInteractionModulation uim WHERE uim.userId = :userId")
    Long countByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(uim.betaFactor) FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.betaFactor IS NOT NULL")
    Double getAverageBetaByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(uim.muFactor) FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.muFactor IS NOT NULL")
    Double getAverageMuByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(uim.wsWeight) FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.wsWeight IS NOT NULL")
    Double getAverageWsByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT MAX(uim.betaFactor) FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.betaFactor IS NOT NULL")
    Double getMaxBetaByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT MIN(uim.betaFactor) FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.betaFactor IS NOT NULL")
    Double getMinBetaByUserId(@Param("userId") UUID userId);
    
    // Batch operations
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.interactionType IN :interactionTypes ORDER BY uim.createdAt DESC")
    List<UserInteractionModulation> findByUserIdAndInteractionTypes(@Param("userId") UUID userId, 
                                                                   @Param("interactionTypes") List<UserInteractionModulation.InteractionType> interactionTypes);
    
    // Existence checks
    boolean existsByUserIdAndInteractionType(UUID userId, UserInteractionModulation.InteractionType interactionType);
    
    // Cleanup queries
    @Query("SELECT uim FROM UserInteractionModulation uim WHERE uim.userId = :userId AND uim.createdAt < :cutoffDate ORDER BY uim.createdAt ASC")
    List<UserInteractionModulation> findOldByUserId(@Param("userId") UUID userId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Delete operations
    void deleteByUserId(UUID userId);
    
    void deleteByUserIdAndInteractionType(UUID userId, UserInteractionModulation.InteractionType interactionType);
    
    void deleteByUserIdAndCreatedAtBefore(UUID userId, LocalDateTime cutoffDate);
}
