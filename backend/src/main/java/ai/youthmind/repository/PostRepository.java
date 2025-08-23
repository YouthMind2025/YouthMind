package ai.youthmind.repository;

import ai.youthmind.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    
    List<Post> findByUserId(UUID userId);
    
    List<Post> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    Page<Post> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    List<Post> findByEmotionTag(String emotionTag);
    Page<Post> findByEmotionTag(String emotionTag, Pageable pageable);
    
    Page<Post> findByEmotionTagOrderByCreatedAtDesc(String emotionTag, Pageable pageable);
    
    List<Post> findByUserIdAndEmotionTag(UUID userId, String emotionTag);
    
    // Retrieve posts that have been flagged for moderation
    List<Post> findByFlaggedTrue();
    
    // Filtering with pagination
    Page<Post> findByEmotionTagIgnoreCase(String emotionTag, Pageable pageable);

    Page<Post> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Post> findByEmotionTagIgnoreCaseAndCreatedAtBetween(String emotionTag, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // New methods for enhanced functionality
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Page<Post> findAllByOrderByLikesDesc(Pageable pageable);
    
    Page<Post> findAllByOrderByViewsDesc(Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:query% OR p.content LIKE %:query% OR p.topics LIKE %:query% ORDER BY p.createdAt DESC")
    Page<Post> searchPosts(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE (p.title LIKE %:query% OR p.content LIKE %:query% OR p.topics LIKE %:query%) AND p.emotionTag = :emotion ORDER BY p.createdAt DESC")
    Page<Post> searchPostsByEmotion(@Param("query") String query, @Param("emotion") String emotion, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.likes DESC")
    Page<Post> findTrendingPosts(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.views DESC")
    Page<Post> findMostViewedPosts(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.bookmarks DESC")
    Page<Post> findMostBookmarkedPosts(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since AND (:emotion IS NULL OR p.emotionTag = :emotion) ORDER BY p.likes DESC")
    List<Post> findMostLikedPosts(@Param("since") LocalDateTime since, @Param("emotion") String emotion, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since AND (:emotion IS NULL OR p.emotionTag = :emotion) ORDER BY p.views DESC")
    List<Post> findMostViewedPosts(@Param("since") LocalDateTime since, @Param("emotion") String emotion, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since AND (:emotion IS NULL OR p.emotionTag = :emotion) ORDER BY p.bookmarks DESC")
    List<Post> findMostBookmarkedPosts(@Param("since") LocalDateTime since, @Param("emotion") String emotion, Pageable pageable);
    
    void deleteByUserId(UUID userId);
} 