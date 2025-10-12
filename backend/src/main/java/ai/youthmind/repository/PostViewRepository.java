package ai.youthmind.repository;

import ai.youthmind.entity.PostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, UUID> {
    
    // Check if user has already viewed this post
    boolean existsByUserIdAndPostId(UUID userId, UUID postId);
    
    // Count total unique views for a post
    long countByPostId(UUID postId);
    
    // Count views for a post after a specific date
    long countByPostIdAndViewedAtAfter(UUID postId, LocalDateTime viewedAt);
    
    // Get all unique viewers for a post
    List<PostView> findByPostId(UUID postId);
    
    // Get all posts viewed by a specific user
    List<PostView> findByUserId(UUID userId);
    
    // Get all views for a specific post with pagination support
    List<PostView> findByPostIdOrderByViewedAtDesc(UUID postId);
    
    // Get views for a post within a date range
    @Query("SELECT pv FROM PostView pv WHERE pv.postId = :postId AND pv.viewedAt BETWEEN :startDate AND :endDate")
    List<PostView> findByPostIdAndViewedAtBetween(
        @Param("postId") UUID postId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    // Get view count for a post within a date range
    @Query("SELECT COUNT(pv) FROM PostView pv WHERE pv.postId = :postId AND pv.viewedAt BETWEEN :startDate AND :endDate")
    long countByPostIdAndViewedAtBetween(
        @Param("postId") UUID postId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate
    );
    
    /**
     * Find views with duration greater than or equal to threshold
     * Used for identifying significant views (>= 15 seconds)
     */
    List<PostView> findByViewDurationSecondsGreaterThanEqual(Integer minDuration);

    /**
     * Count significant views for a post
     * @param postId Post ID
     * @param minDuration Minimum duration in seconds (typically 15)
     * @return Count of significant views
     */
    @Query("SELECT COUNT(pv) FROM PostView pv WHERE pv.postId = :postId AND pv.viewDurationSeconds >= :minDuration")
    Long countSignificantViewsByPostId(@Param("postId") UUID postId, @Param("minDuration") Integer minDuration);

    /**
     * Get average view duration for a post
     * @param postId Post ID
     * @return Average duration in seconds, or null if no views with duration
     */
    @Query("SELECT AVG(pv.viewDurationSeconds) FROM PostView pv WHERE pv.postId = :postId AND pv.viewDurationSeconds IS NOT NULL")
    Double getAverageViewDurationByPostId(@Param("postId") UUID postId);

    /**
     * Find all views by user with duration data
     * @param userId User ID
     * @return List of views that have duration recorded
     */
    @Query("SELECT pv FROM PostView pv WHERE pv.userId = :userId AND pv.viewDurationSeconds IS NOT NULL ORDER BY pv.viewStartedAt DESC")
    List<PostView> findByUserIdWithDuration(@Param("userId") UUID userId);

    /**
     * Get total significant view time for a user
     * @param userId User ID
     * @param minDuration Minimum duration to count as significant
     * @return Total seconds of significant views
     */
    @Query("SELECT COALESCE(SUM(pv.viewDurationSeconds), 0) FROM PostView pv WHERE pv.userId = :userId AND pv.viewDurationSeconds >= :minDuration")
    Long getTotalSignificantViewTimeByUserId(@Param("userId") UUID userId, @Param("minDuration") Integer minDuration);
} 