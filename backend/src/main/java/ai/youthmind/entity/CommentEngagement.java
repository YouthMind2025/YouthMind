package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "comment_engagements")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentEngagement {
    
    public enum EngagementType {
        LIKE
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "comment_id", nullable = false)
    private UUID commentId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false)
    private EngagementType type;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Explicit getters and setters
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
    
    public UUID getCommentId() {
        return commentId;
    }
    
    public void setCommentId(UUID commentId) {
        this.commentId = commentId;
    }
    
    public EngagementType getType() {
        return type;
    }
    
    public void setType(EngagementType type) {
        this.type = type;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
