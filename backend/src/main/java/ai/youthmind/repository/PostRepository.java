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
    
    List<Post> findByEmotion(String emotion);
    
    List<Post> findByUserIdAndEmotion(UUID userId, String emotion);
    
    // Retrieve posts that have been flagged for moderation
    List<Post> findByFlaggedTrue();
    
    // Find flagged posts by specific user
    List<Post> findByUserIdAndFlaggedTrue(UUID userId);
    
    // Count flagged posts
    long countByFlaggedTrue();
    
    // Filtering with pagination
    Page<Post> findByEmotionIgnoreCase(String emotion, Pageable pageable);

    Page<Post> findByCreatedAtBetween(LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    Page<Post> findByEmotionIgnoreCaseAndCreatedAtBetween(String emotion, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);
    
    // Enhanced methods from backend branch
    Page<Post> findByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
    
    Page<Post> findByEmotion(String emotion, Pageable pageable);
    
    Page<Post> findByEmotionOrderByCreatedAtDesc(String emotion, Pageable pageable);
    
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    Page<Post> findAllByOrderByResonanceDesc(Pageable pageable);
    
    Page<Post> findAllByOrderByViewDesc(Pageable pageable);
    
    // Enhanced search methods for POST-006
    @Query("SELECT p FROM Post p WHERE " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.topics) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> searchPosts(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.topics) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND LOWER(p.emotion) = LOWER(:emotion) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> searchPostsByEmotion(@Param("query") String query, @Param("emotion") String emotion, Pageable pageable);
    
    // Search with optional emotion filter
    @Query("SELECT p FROM Post p WHERE " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.topics) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:emotion IS NULL OR LOWER(p.emotion) = LOWER(:emotion)) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> searchPostsWithOptionalEmotion(@Param("query") String query, @Param("emotion") String emotion, Pageable pageable);
    
    // Search with relevance scoring (for future enhancement)
    @Query("SELECT p FROM Post p WHERE " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.topics) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND (:emotion IS NULL OR LOWER(p.emotion) = LOWER(:emotion)) " +
           "ORDER BY " +
           "CASE WHEN LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) THEN 3 " +
                "WHEN LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) THEN 2 " +
                "ELSE 1 END DESC, " +
           "p.createdAt DESC")
    Page<Post> searchPostsWithRelevance(@Param("query") String query, @Param("emotion") String emotion, Pageable pageable);
    
    // Non-paginated versions for advanced search
    @Query("SELECT p FROM Post p WHERE " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.topics) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<Post> searchPosts(@Param("query") String query);
    
    @Query("SELECT p FROM Post p WHERE " +
           "(LOWER(p.title) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.content) LIKE LOWER(CONCAT('%', :query, '%')) OR " +
           "LOWER(p.topics) LIKE LOWER(CONCAT('%', :query, '%'))) " +
           "AND LOWER(p.emotion) = LOWER(:emotion) " +
           "ORDER BY p.createdAt DESC")
    List<Post> searchPostsByEmotion(@Param("query") String query, @Param("emotion") String emotion);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.resonance DESC")
    Page<Post> findTrendingPosts(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.view DESC")
    Page<Post> findMostViewedPosts(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since ORDER BY p.collect DESC")
    Page<Post> findMostBookmarkedPosts(@Param("since") LocalDateTime since, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since AND (:emotion IS NULL OR p.emotion = :emotion) ORDER BY p.resonance DESC")
    List<Post> findMostLikedPosts(@Param("since") LocalDateTime since, @Param("emotion") String emotion, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since AND (:emotion IS NULL OR p.emotion = :emotion) ORDER BY p.view DESC")
    List<Post> findMostViewedPosts(@Param("since") LocalDateTime since, @Param("emotion") String emotion, Pageable pageable);
    
    /**
     * Atomically increment the view count for a post
     * This method uses a native SQL query for better performance
     */
    @Query(value = "UPDATE posts SET view_count = view_count + 1 WHERE id = :postId", nativeQuery = true)
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void incrementViewCount(@Param("postId") UUID postId);
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since AND (:emotion IS NULL OR p.emotion = :emotion) ORDER BY p.collect DESC")
    List<Post> findMostBookmarkedPosts(@Param("since") LocalDateTime since, @Param("emotion") String emotion, Pageable pageable);
    
    /**
     * Find posts sorted by popularity score with time decay
     * Popularity score = (resonance * 2 + view + collect * 3) * time_decay_factor
     * Time decay factor = 0.95^(hours_since_creation)
     */
    @Query("SELECT p FROM Post p WHERE (:emotion IS NULL OR LOWER(p.emotion) = LOWER(:emotion)) " +
           "ORDER BY " +
           "(p.resonance * 2.0 + p.view + p.collect * 3.0) * " +
           "POWER(0.95, TIMESTAMPDIFF(HOUR, p.createdAt, CURRENT_TIMESTAMP)) DESC")
    Page<Post> findPostsByPopularity(@Param("emotion") String emotion, Pageable pageable);

    Page<Post> findByFlaggedTrue(Pageable pageable);

    long countByFlaggedTrueAndFlaggedAtAfter(LocalDateTime date);

    long countByCreatedAtAfter(LocalDateTime date);
    
    void deleteByUserId(UUID userId);
    
    /**
     * Find posts by topics containing the given tag (case-insensitive)
     */
    Page<Post> findByTopicsContainingIgnoreCase(String tag, Pageable pageable);
    
    /**
     * Find posts by topics containing tag AND (content containing term OR title containing term)
     */
    @Query("SELECT p FROM Post p WHERE " +
           "LOWER(p.topics) LIKE LOWER(CONCAT('%', :tag, '%')) AND " +
           "(LOWER(p.content) LIKE LOWER(CONCAT('%', :term, '%')) OR " +
           "LOWER(p.title) LIKE LOWER(CONCAT('%', :term, '%'))) " +
           "ORDER BY p.createdAt DESC")
    Page<Post> findByTopicsContainingIgnoreCaseAndContentContainingIgnoreCaseOrTitleContainingIgnoreCase(
        @Param("tag") String tag, 
        @Param("term") String term, 
        Pageable pageable);
} 