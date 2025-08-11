package ai.youthmind.repository;

import ai.youthmind.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    
    List<Post> findByUserId(UUID userId);
    
    List<Post> findByUserIdOrderByCreatedAtDesc(UUID userId);
    
    List<Post> findByEmotionTag(String emotionTag);
    
    List<Post> findByUserIdAndEmotionTag(UUID userId, String emotionTag);
    
    // Retrieve posts that have been flagged for moderation
    List<Post> findByFlaggedTrue();
    
    // Filtering with pagination
    org.springframework.data.domain.Page<Post> findByEmotionTagIgnoreCase(String emotionTag, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Post> findByCreatedAtBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Post> findByEmotionTagIgnoreCaseAndCreatedAtBetween(String emotionTag, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, org.springframework.data.domain.Pageable pageable);
} 