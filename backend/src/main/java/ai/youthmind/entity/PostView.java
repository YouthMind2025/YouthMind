package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "post_views")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class PostView {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "post_id", nullable = false)
    private UUID postId;
    
    @Column(name = "viewed_at", nullable = false)
    private LocalDateTime viewedAt;
    
    // Constructor for creating new view records
    public PostView(UUID userId, UUID postId) {
        this.userId = userId;
        this.postId = postId;
        this.viewedAt = LocalDateTime.now();
    }
    
    // Explicit getters and setters to ensure they work
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public UUID getPostId() {
        return postId;
    }
    
    public void setPostId(UUID postId) {
        this.postId = postId;
    }
    
    public LocalDateTime getViewedAt() {
        return viewedAt;
    }
    
    public void setViewedAt(LocalDateTime viewedAt) {
        this.viewedAt = viewedAt;
    }
} 