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
    
    @Query("SELECT p FROM Post p WHERE p.title LIKE %:query% OR p.content LIKE %:query% OR p.topics LIKE %:query% ORDER BY p.createdAt DESC")
    Page<Post> searchPosts(@Param("query") String query, Pageable pageable);
    
    @Query("SELECT p FROM Post p WHERE (p.title LIKE %:query% OR p.content LIKE %:query% OR p.topics LIKE %:query%) AND p.emotion = :emotion ORDER BY p.createdAt DESC")
    Page<Post> searchPostsByEmotion(@Param("query") String query, @Param("emotion") String emotion, Pageable pageable);
    
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
    
    @Query("SELECT p FROM Post p WHERE p.createdAt >= :since AND (:emotion IS NULL OR p.emotion = :emotion) ORDER BY p.collect DESC")
    List<Post> findMostBookmarkedPosts(@Param("since") LocalDateTime since, @Param("emotion") String emotion, Pageable pageable);
    
    void deleteByUserId(UUID userId);
} 