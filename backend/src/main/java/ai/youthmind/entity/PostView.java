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
    
    @Column(name = "view_started_at")
    private LocalDateTime viewStartedAt;

    @Column(name = "view_ended_at")
    private LocalDateTime viewEndedAt;

    @Column(name = "view_duration_seconds")
    private Integer viewDurationSeconds;
    
    // Constructor for creating new view records
    public PostView(UUID userId, UUID postId) {
        this.userId = userId;
        this.postId = postId;
        this.viewedAt = LocalDateTime.now();
        this.viewStartedAt = LocalDateTime.now();
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
    
    public LocalDateTime getViewStartedAt() {
        return viewStartedAt;
    }
    
    public void setViewStartedAt(LocalDateTime viewStartedAt) {
        this.viewStartedAt = viewStartedAt;
    }
    
    public LocalDateTime getViewEndedAt() {
        return viewEndedAt;
    }
    
    public void setViewEndedAt(LocalDateTime viewEndedAt) {
        this.viewEndedAt = viewEndedAt;
    }
    
    public Integer getViewDurationSeconds() {
        return viewDurationSeconds;
    }
    
    public void setViewDurationSeconds(Integer viewDurationSeconds) {
        this.viewDurationSeconds = viewDurationSeconds;
    }
    
    /**
     * Mark the view as ended and calculate duration
     */
    public void endView() {
        if (this.viewEndedAt == null) {
            this.viewEndedAt = LocalDateTime.now();
            if (this.viewStartedAt != null) {
                this.viewDurationSeconds = (int) java.time.Duration.between(
                    this.viewStartedAt, this.viewEndedAt
                ).getSeconds();
            }
        }
    }

    /**
     * Check if this view is significant (>= 15 seconds)
     * @return true if view duration is 15 seconds or more
     */
    public boolean isSignificantView() {
        return viewDurationSeconds != null && viewDurationSeconds >= 15;
    }

    /**
     * Update duration with explicit value
     * @param durationSeconds Duration in seconds
     */
    public void setDuration(Integer durationSeconds) {
        this.viewDurationSeconds = durationSeconds;
        if (this.viewStartedAt != null && durationSeconds != null) {
            this.viewEndedAt = this.viewStartedAt.plusSeconds(durationSeconds);
        }
    }
} 