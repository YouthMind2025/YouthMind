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
    
    List<Post> findByEmotion(String emotion);
    
    List<Post> findByUserIdAndEmotion(UUID userId, String emotion);
    
    // Retrieve posts that have been flagged for moderation
    List<Post> findByFlaggedTrue();
    
    // Filtering with pagination
    org.springframework.data.domain.Page<Post> findByEmotionIgnoreCase(String emotion, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Post> findByCreatedAtBetween(java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, org.springframework.data.domain.Pageable pageable);

    org.springframework.data.domain.Page<Post> findByEmotionIgnoreCaseAndCreatedAtBetween(String emotion, java.time.LocalDateTime startDate, java.time.LocalDateTime endDate, org.springframework.data.domain.Pageable pageable);
} 