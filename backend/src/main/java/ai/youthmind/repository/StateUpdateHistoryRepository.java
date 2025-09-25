package ai.youthmind.repository;

import ai.youthmind.entity.StateUpdateHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for state update history
 * Provides database access for psychological state update history
 */
@Repository
public interface StateUpdateHistoryRepository extends JpaRepository<StateUpdateHistory, UUID> {

    /**
     * Find state update history by user ID ordered by creation time descending
     * 
     * @param userId User ID
     * @return List of state update history
     */
    List<StateUpdateHistory> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    /**
     * Find state update history by user ID and update type
     * 
     * @param userId User ID
     * @param updateType Update type
     * @return List of state update history
     */
    List<StateUpdateHistory> findByUserIdAndUpdateType(UUID userId, StateUpdateHistory.UpdateType updateType);
    
    /**
     * Find state update history by user ID and update type ordered by creation time descending
     * 
     * @param userId User ID
     * @param updateType Update type
     * @return List of state update history
     */
    List<StateUpdateHistory> findByUserIdAndUpdateTypeOrderByCreatedAtDesc(UUID userId, StateUpdateHistory.UpdateType updateType);
    
    /**
     * Find state update history by user ID within a time range
     * 
     * @param userId User ID
     * @param startTime Start time
     * @param endTime End time
     * @return List of state update history
     */
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.createdAt BETWEEN :startTime AND :endTime ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdAndCreatedAtBetween(@Param("userId") UUID userId,
                                                            @Param("startTime") LocalDateTime startTime,
                                                            @Param("endTime") LocalDateTime endTime);
    
    /**
     * Find state update history by source ID
     * 
     * @param sourceId Source ID (post ID, interaction ID, etc.)
     * @return List of state update history
     */
    List<StateUpdateHistory> findBySourceId(UUID sourceId);
    
    /**
     * Find state update history by user ID and source ID
     * 
     * @param userId User ID
     * @param sourceId Source ID
     * @return List of state update history
     */
    List<StateUpdateHistory> findByUserIdAndSourceId(UUID userId, UUID sourceId);
    
    /**
     * Find state update history by update type
     * 
     * @param updateType Update type
     * @return List of state update history
     */
    List<StateUpdateHistory> findByUpdateType(StateUpdateHistory.UpdateType updateType);
    
    /**
     * Find state update history by update type ordered by creation time descending
     * 
     * @param updateType Update type
     * @return List of state update history
     */
    List<StateUpdateHistory> findByUpdateTypeOrderByCreatedAtDesc(StateUpdateHistory.UpdateType updateType);
    
    /**
     * Count state update history by user ID and update type
     * 
     * @param userId User ID
     * @param updateType Update type
     * @return Count of state update history
     */
    long countByUserIdAndUpdateType(UUID userId, StateUpdateHistory.UpdateType updateType);
    
    /**
     * Find state update history by user ID with significant changes
     * 
     * @param userId User ID
     * @param threshold Change magnitude threshold
     * @return List of state update history
     */
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.thetaFactor > :threshold " +
           "ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdWithSignificantChanges(@Param("userId") UUID userId, 
                                                               @Param("threshold") Double threshold);
    
    /**
     * Find state update history by user ID and update type with significant changes
     * 
     * @param userId User ID
     * @param updateType Update type
     * @param threshold Change magnitude threshold
     * @return List of state update history
     */
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.updateType = :updateType AND suh.thetaFactor > :threshold " +
           "ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdAndUpdateTypeWithSignificantChanges(@Param("userId") UUID userId,
                                                            @Param("updateType") StateUpdateHistory.UpdateType updateType,
                                                                             @Param("threshold") Double threshold);
    
    /**
     * Find latest state update history by user ID
     * 
     * @param userId User ID
     * @return Latest state update history
     */
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findLatestByUserId(@Param("userId") UUID userId);
    
    /**
     * Find latest state update history by user ID and update type
     * 
     * @param userId User ID
     * @param updateType Update type
     * @return Latest state update history
     */
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.updateType = :updateType ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findLatestByUserIdAndUpdateType(@Param("userId") UUID userId, 
                                                                @Param("updateType") StateUpdateHistory.UpdateType updateType);
    
    /**
     * Find state update history by user ID with alpha factor
     * 
     * @param userId User ID
     * @return List of state update history
     */
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.alphaUsed IS NOT NULL ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdWithAlphaFactor(@Param("userId") UUID userId);
    
    /**
     * Find state update history by user ID with theta factor
     * 
     * @param userId User ID
     * @return List of state update history
     */
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.thetaFactor IS NOT NULL ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdWithThetaFactor(@Param("userId") UUID userId);
    
    /**
     * Find state update history by user ID with beta factor
     * 
     * @param userId User ID
     * @return List of state update history
     */
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.betaFactor IS NOT NULL ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdWithBetaFactor(@Param("userId") UUID userId);
    
    /**
     * Find state update history by user ID with mu factor
     * 
     * @param userId User ID
     * @return List of state update history
     */
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.muFactor IS NOT NULL ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdWithMuFactor(@Param("userId") UUID userId);
    
    /**
     * Find state update history by user ID with Ws weight
     * 
     * @param userId User ID
     * @return List of state update history
     */
    @Query("SELECT suh FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.wsWeight IS NOT NULL ORDER BY suh.createdAt DESC")
    List<StateUpdateHistory> findByUserIdWithWsWeight(@Param("userId") UUID userId);
    
    /**
     * Delete old state update history by user ID before a specific date
     * 
     * @param userId User ID
     * @param beforeDate Date before which to delete
     * @return Number of deleted records
     */
    @Query("DELETE FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.createdAt < :beforeDate")
    int deleteByUserIdAndCreatedAtBefore(@Param("userId") UUID userId, @Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Delete old state update history by user ID and update type before a specific date
     * 
     * @param userId User ID
     * @param updateType Update type
     * @param beforeDate Date before which to delete
     * @return Number of deleted records
     */
    @Query("DELETE FROM StateUpdateHistory suh WHERE suh.userId = :userId AND suh.updateType = :updateType AND suh.createdAt < :beforeDate")
    int deleteByUserIdAndUpdateTypeAndCreatedAtBefore(@Param("userId") UUID userId,
                                                     @Param("updateType") StateUpdateHistory.UpdateType updateType,
                                                     @Param("beforeDate") LocalDateTime beforeDate);
    
    /**
     * Count state update history by user ID
     * 
     * @param userId User ID
     * @return Count of state update history
     */
    long countByUserId(UUID userId);
    
    /**
     * Check if state update history exists by user ID
     * 
     * @param userId User ID
     * @return true if state update history exists
     */
    boolean existsByUserId(UUID userId);
    
    /**
     * Check if state update history exists by user ID and update type
     * 
     * @param userId User ID
     * @param updateType Update type
     * @return true if state update history exists
     */
    boolean existsByUserIdAndUpdateType(UUID userId, StateUpdateHistory.UpdateType updateType);
}