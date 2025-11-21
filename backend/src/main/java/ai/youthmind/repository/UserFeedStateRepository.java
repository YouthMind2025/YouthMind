package ai.youthmind.repository;

import ai.youthmind.entity.UserFeedState;
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
 * Repository interface for UserFeedState entity
 * Provides data access methods for user feed state management
 */
@Repository
public interface UserFeedStateRepository extends JpaRepository<UserFeedState, UUID> {
    
    /**
     * Find user feed state by user ID
     * 
     * @param userId The user ID to search for
     * @return Optional containing the user feed state if found
     */
    Optional<UserFeedState> findByUserId(UUID userId);
    
    /**
     * Check if user feed state exists for a user
     * 
     * @param userId The user ID to check
     * @return true if user feed state exists
     */
    boolean existsByUserId(UUID userId);
    
    /**
     * Delete user feed state by user ID
     * 
     * @param userId The user ID
     * @return Number of deleted records
     */
    @Modifying
    @Transactional
    int deleteByUserId(UUID userId);
    
    /**
     * Find all user feed states with last access before cutoff time
     * Useful for cleanup of stale user states
     * 
     * @param cutoffTime The cutoff time for last access
     * @return List of user feed states with stale last access
     */
    List<UserFeedState> findByLastAccessedAtBefore(LocalDateTime cutoffTime);
    
    /**
     * Find all user feed states ordered by last access time descending
     * 
     * @return List of user feed states ordered by last access time
     */
    List<UserFeedState> findAllByOrderByLastAccessedAtDesc();
    
    /**
     * Count total number of user feed states
     * 
     * @return Total count of user feed states
     */
    @Query("SELECT COUNT(ufs) FROM UserFeedState ufs")
    Long countAllUserFeedStates();
    
    /**
     * Find user feed states with high block generation count
     * Useful for identifying active users
     * 
     * @param minBlocksGenerated Minimum number of blocks generated
     * @return List of user feed states with high block generation
     */
    @Query("SELECT ufs FROM UserFeedState ufs WHERE ufs.totalBlocksGenerated >= :minBlocksGenerated ORDER BY ufs.totalBlocksGenerated DESC")
    List<UserFeedState> findActiveUsers(@Param("minBlocksGenerated") Integer minBlocksGenerated);
    
    /**
     * Find user feed states by R vector version
     * Useful for tracking users on specific R vector versions
     * 
     * @param rVectorVersion The R vector version
     * @return List of user feed states with matching R vector version
     */
    List<UserFeedState> findByCurrentRVectorVersion(Integer rVectorVersion);
    
    /**
     * Update R vector version for all users
     * 
     * @param oldVersion The old R vector version
     * @param newVersion The new R vector version
     * @return Number of updated records
     */
    @Modifying
    @Transactional
    @Query("UPDATE UserFeedState ufs SET ufs.currentRVectorVersion = :newVersion WHERE ufs.currentRVectorVersion = :oldVersion")
    int updateRVectorVersionForAllUsers(@Param("oldVersion") Integer oldVersion, @Param("newVersion") Integer newVersion);
    
    /**
     * Delete all user feed states with last access before cutoff time
     * 
     * @param cutoffTime The cutoff time for last access
     * @return Number of deleted records
     */
    @Modifying
    @Transactional
    int deleteByLastAccessedAtBefore(LocalDateTime cutoffTime);
}


