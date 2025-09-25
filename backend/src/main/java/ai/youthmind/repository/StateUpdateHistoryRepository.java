package ai.youthmind.repository;

import ai.youthmind.entity.StateUpdateHistory;
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
 * Repository for managing state update history.
 * Provides queries for finding state change history, trend analysis, and state evolution patterns.
 */
@Repository
public interface StateUpdateHistoryRepository extends JpaRepository<StateUpdateHistory, UUID> {

    // Basic queries by user
    List<StateUpdateHistory> findByUserId(UUID userId);
    
    Page<StateUpdateHistory> findByUserId(UUID userId, Pageable pageable);
    
    List<StateUpdateHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    // Queries by update type
    List<StateUpdateHistory> findByUserIdAndUpdateType(UUID userId, StateUpdateHistory.UpdateType updateType);
    
    Page<StateUpdateHistory> findByUserIdAndUpdateType(UUID userId, StateUpdateHistory.UpdateType updateType, Pageable pageable);
    
    List<StateUpdateHistory> findByUpdateType(StateUpdateHistory.UpdateType updateType);
    
    
    // Queries by source
    List<StateUpdateHistory> findByUserIdAndSourceId(UUID userId, UUID sourceId);
    
    // Date range queries
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.createdAt >= :startDate AND suh.createdAt <= :endDate ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdAndDateRange(@Param("userId") UUID userId, 
                                                     @Param("startDate") LocalDateTime startDate, 
                                                     @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.updateType = :updateType AND suh.createdAt >= :startDate AND suh.createdAt <= :endDate ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdAndUpdateTypeAndDateRange(@Param("userId") UUID userId, 
                                                                  @Param("updateType") StateUpdateHistory.UpdateType updateType,
                                                                  @Param("startDate") LocalDateTime startDate, 
                                                                  @Param("endDate") LocalDateTime endDate);
    
    
    // Recent updates queries
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.createdAt >= :since ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findRecentByUserId(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.updateType = :updateType AND suh.createdAt >= :since ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findRecentByUserIdAndUpdateType(@Param("userId") UUID userId, 
                                                            @Param("updateType") StateUpdateHistory.UpdateType updateType,
                                                            @Param("since") LocalDateTime since);
    
    
    // Latest update queries
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId ORDER BY suh.createdAt DESC")
    Optional<StateUpdateHistory> findLatestByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.updateType = :updateType ORDER BY suh.createdAt DESC")
    Optional<StateUpdateHistory> findLatestByUserIdAndUpdateType(@Param("userId") UUID userId, 
                                                                @Param("updateType") StateUpdateHistory.UpdateType updateType);
    
    
    // Parameter range queries
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.thetaFactor >= :minTheta AND suh.thetaFactor <= :maxTheta ORDER BY suh.thetaFactor DESC")
    List<StateUpdateHistory> findByUserIdAndThetaRange(@Param("userId") UUID userId, 
                                                      @Param("minTheta") Double minTheta, 
                                                      @Param("maxTheta") Double maxTheta);
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.betaFactor >= :minBeta AND suh.betaFactor <= :maxBeta ORDER BY suh.betaFactor DESC")
    List<StateUpdateHistory> findByUserIdAndBetaRange(@Param("userId") UUID userId, 
                                                     @Param("minBeta") Double minBeta, 
                                                     @Param("maxBeta") Double maxBeta);
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.muFactor >= :minMu AND suh.muFactor <= :maxMu ORDER BY suh.muFactor DESC")
    List<StateUpdateHistory> findByUserIdAndMuRange(@Param("userId") UUID userId, 
                                                   @Param("minMu") Double minMu, 
                                                   @Param("maxMu") Double maxMu);
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.wsWeight >= :minWs AND suh.wsWeight <= :maxWs ORDER BY suh.wsWeight DESC")
    List<StateUpdateHistory> findByUserIdAndWsRange(@Param("userId") UUID userId, 
                                                   @Param("minWs") Double minWs, 
                                                   @Param("maxWs") Double maxWs);
    
    // State evolution queries
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId ORDER BY suh.createdAt ASC")
    List<StateUpdateHistory> findStateEvolutionByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.updateType = :updateType ORDER BY suh.createdAt ASC")
    List<StateUpdateHistory> findStateEvolutionByUserIdAndUpdateType(@Param("userId") UUID userId, 
                                                                    @Param("updateType") StateUpdateHistory.UpdateType updateType);
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.createdAt >= :since ORDER BY suh.createdAt ASC")
    List<StateUpdateHistory> findStateEvolutionSince(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    // Trend analysis queries
    @Query("SELECT suh.updateType, COUNT(suh) " +
           "FROM StateUpdateHistory suh WHERE suh.userId = :userId GROUP BY suh.updateType ORDER BY COUNT(suh) DESC")
    List<Object[]> getUpdateStatsByType(@Param("userId") UUID userId);
    
    @Query("SELECT DATE(suh.createdAt), COUNT(suh) " +
           "FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.createdAt >= :since " +
           "GROUP BY DATE(suh.createdAt) ORDER BY DATE(suh.createdAt)")
    List<Object[]> getUpdateTimeline(@Param("userId") UUID userId, @Param("since") LocalDateTime since);
    
    
    // Vector similarity search using PostgreSQL vector operations
    @Query(value = "SELECT *, (suh.previous_state <-> cast(:queryVector as vector)) as distance " +
                   "FROM state_update_history suh " +
                   "WHERE suh.user_id = :userId " +
                   "ORDER BY distance " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarPreviousStates(@Param("userId") UUID userId, 
                                            @Param("queryVector") String queryVector, 
                                            @Param("limit") int limit);
    
    @Query(value = "SELECT *, (suh.new_state <-> cast(:queryVector as vector)) as distance " +
                   "FROM state_update_history suh " +
                   "WHERE suh.user_id = :userId " +
                   "ORDER BY distance " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findSimilarNewStates(@Param("userId") UUID userId, 
                                       @Param("queryVector") String queryVector, 
                                       @Param("limit") int limit);
    
    // Cosine similarity search
    @Query(value = "SELECT *, (1 - (suh.previous_state <=> cast(:queryVector as vector))) as similarity " +
                   "FROM state_update_history suh " +
                   "WHERE suh.user_id = :userId " +
                   "AND (1 - (suh.previous_state <=> cast(:queryVector as vector))) >= :minSimilarity " +
                   "ORDER BY similarity DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findPreviousStatesByCosineSimilarity(@Param("userId") UUID userId, 
                                                        @Param("queryVector") String queryVector, 
                                                        @Param("minSimilarity") Double minSimilarity,
                                                        @Param("limit") int limit);
    
    @Query(value = "SELECT *, (1 - (suh.new_state <=> cast(:queryVector as vector))) as similarity " +
                   "FROM state_update_history suh " +
                   "WHERE suh.user_id = :userId " +
                   "AND (1 - (suh.new_state <=> cast(:queryVector as vector))) >= :minSimilarity " +
                   "ORDER BY similarity DESC " +
                   "LIMIT :limit", nativeQuery = true)
    List<Object[]> findNewStatesByCosineSimilarity(@Param("userId") UUID userId, 
                                                   @Param("queryVector") String queryVector, 
                                                   @Param("minSimilarity") Double minSimilarity,
                                                   @Param("limit") int limit);
    
    
    // Statistical queries
    @Query("SELECT COUNT(suh) FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.updateType = :updateType")
    Long countByUserIdAndUpdateType(@Param("userId") UUID userId, @Param("updateType") StateUpdateHistory.UpdateType updateType);
    
    @Query("SELECT COUNT(suh) FROM StateUpdateHistory suh WHERE suh.userId = :userId")
    Long countByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(suh.thetaFactor) FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.thetaFactor IS NOT NULL")
    Double getAverageThetaByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(suh.betaFactor) FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.betaFactor IS NOT NULL")
    Double getAverageBetaByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(suh.muFactor) FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.muFactor IS NOT NULL")
    Double getAverageMuByUserId(@Param("userId") UUID userId);
    
    @Query("SELECT AVG(suh.wsWeight) FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.wsWeight IS NOT NULL")
    Double getAverageWsByUserId(@Param("userId") UUID userId);
    
    // Batch operations
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.updateType IN :updateTypes ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdAndUpdateTypes(@Param("userId") UUID userId, 
                                                       @Param("updateTypes") List<StateUpdateHistory.UpdateType> updateTypes);
    
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId IN :userIds ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIds(@Param("userIds") List<UUID> userIds);
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId IN :userIds AND suh.updateType = :updateType ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdsAndUpdateType(@Param("userIds") List<UUID> userIds, 
                                                        @Param("updateType") StateUpdateHistory.UpdateType updateType);
    
    // Existence checks
    boolean existsByUserId(UUID userId);
    
    boolean existsByUserIdAndUpdateType(UUID userId, StateUpdateHistory.UpdateType updateType);
    
    boolean existsByUserIdAndCreatedAtAfter(UUID userId, LocalDateTime createdAt);
    
    // Cleanup queries
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.createdAt < :cutoffDate ORDER BY suh.createdAt ASC")
    List<StateUpdateHistory> findOldByUserId(@Param("userId") UUID userId, @Param("cutoffDate") LocalDateTime cutoffDate);
    
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.createdAt < :cutoffDate ORDER BY suh.createdAt ASC")
    List<StateUpdateHistory> findAllOld(@Param("cutoffDate") LocalDateTime cutoffDate);
    
    // Delete operations
    void deleteByUserId(UUID userId);
    
    void deleteByUserIdAndUpdateType(UUID userId, StateUpdateHistory.UpdateType updateType);
    
    void deleteByUserIdAndCreatedAtBefore(UUID userId, LocalDateTime cutoffDate);
    
    void deleteByCreatedAtBefore(LocalDateTime cutoffDate);
}
