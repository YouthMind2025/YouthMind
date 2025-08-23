package ai.youthmind.repository;

import ai.youthmind.entity.PostView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostViewRepository extends JpaRepository<PostView, UUID> {
    
    // Check if user has already viewed this post
    boolean existsByUserIdAndPostId(UUID userId, UUID postId);
    
    // Count total unique views for a post
    long countByPostId(UUID postId);
    
    // Get all unique viewers for a post
    List<PostView> findByPostId(UUID postId);
    
    // Get all posts viewed by a specific user
    List<PostView> findByUserId(UUID userId);
    
    // Get all views for a specific post with pagination support
    List<PostView> findByPostIdOrderByViewedAtDesc(UUID postId);
} 