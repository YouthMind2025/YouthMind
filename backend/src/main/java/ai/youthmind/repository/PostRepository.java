package ai.youthmind.repository;

import ai.youthmind.entity.Post;
import ai.youthmind.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface PostRepository extends JpaRepository<Post, UUID> {
    
    List<Post> findByUser(User user);
    
    List<Post> findByUserOrderByCreatedAtDesc(User user);
    
    List<Post> findByEmotionTag(String emotionTag);
    
    List<Post> findByUserAndEmotionTag(User user, String emotionTag);
    
} 