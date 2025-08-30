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
} 