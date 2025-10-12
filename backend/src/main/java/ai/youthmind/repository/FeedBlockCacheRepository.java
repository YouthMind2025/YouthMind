package ai.youthmind.repository;

import ai.youthmind.entity.FeedBlockCache;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for FeedBlockCache entity
 * Provides data access methods for cached feed blocks
 */
@Repository
public interface FeedBlockCacheRepository extends JpaRepository<FeedBlockCache, UUID> {
    
    /**
     * Find all feed blocks for a specific user
     * 
     * @param userId The user ID to search for
     * @return List of feed blocks for the user
     */
    List<FeedBlockCache> findByUserId(UUID userId);
    
    /**
     * Find a specific feed block by user ID and block number
     * 
     * @param userId The user ID
     * @param blockNumber The block number
     * @return Optional containing the feed block if found
     */
    Optional<FeedBlockCache> findByUserIdAndBlockNumber(UUID userId, Integer blockNumber);
    
    /**
     * Find all feed blocks for a user ordered by block number descending
     * 
     * @param userId The user ID
     * @return List of feed blocks ordered by block number descending
     */
    List<FeedBlockCache> findByUserIdOrderByBlockNumberDesc(UUID userId);
    
    /**
     * Find the latest feed block for a user
     * 
     * @param userId The user ID
     * @return Optional containing the latest feed block if found
     */
    @Query("SELECT fbc FROM FeedBlockCache fbc WHERE fbc.userId = :userId ORDER BY fbc.blockNumber DESC LIMIT 1")
    Optional<FeedBlockCache> findLatestByUserId(@Param("userId") UUID userId);
    
    /**
     * Delete a specific feed block by user ID and block number
     * 
     * @param userId The user ID
     * @param blockNumber The block number
     * @return Number of deleted records
     */
    @Modifying
    @Transactional
    int deleteByUserIdAndBlockNumber(UUID userId, Integer blockNumber);
    
    /**
     * Delete all expired feed blocks before a cutoff time
     * 
     * @param cutoffTime The cutoff time for expiration
     * @return Number of deleted records
     */
    @Modifying
    @Transactional
    int deleteByExpiresAtBefore(LocalDateTime cutoffTime);
    
    /**
     * Count the number of feed blocks for a user
     * 
     * @param userId The user ID
     * @return Number of feed blocks for the user
     */
    Long countByUserId(UUID userId);
    
    /**
     * Find all valid (non-expired) feed blocks for a user
     * 
     * @param userId The user ID
     * @return List of valid feed blocks
     */
    @Query("SELECT fbc FROM FeedBlockCache fbc WHERE fbc.userId = :userId AND fbc.expiresAt > :currentTime ORDER BY fbc.blockNumber DESC")
    List<FeedBlockCache> findValidByUserId(@Param("userId") UUID userId, @Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find all expired feed blocks
     * 
     * @param currentTime Current time for comparison
     * @return List of expired feed blocks
     */
    @Query("SELECT fbc FROM FeedBlockCache fbc WHERE fbc.expiresAt <= :currentTime")
    List<FeedBlockCache> findExpiredBlocks(@Param("currentTime") LocalDateTime currentTime);
    
    /**
     * Find feed blocks by user ID and R vector version
     * 
     * @param userId The user ID
     * @param rVectorVersion The R vector version
     * @return List of feed blocks with matching R vector version
     */
    @Query("SELECT fbc FROM FeedBlockCache fbc WHERE fbc.userId = :userId AND fbc.rVectorVersion = :rVectorVersion")
    List<FeedBlockCache> findByUserIdAndRVectorVersion(@Param("userId") UUID userId, @Param("rVectorVersion") Integer rVectorVersion);
    
    /**
     * Delete all feed blocks for a user
     * 
     * @param userId The user ID
     * @return Number of deleted records
     */
    @Modifying
    @Transactional
    int deleteByUserId(UUID userId);
}


