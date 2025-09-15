
package ai.youthmind.analytics.entity;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "user_behavior_analytics")
public class UserBehaviorAnalytics {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(nullable = false)
    private UUID userId;

    private UUID postId;

    private Long viewDurationSeconds;

    private String interactionType; // LIKE, COMMENT, SAVE, SHARE

    @Column(columnDefinition = "TEXT")
    private String metadataJson;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    public UserBehaviorAnalytics() {}

    public UserBehaviorAnalytics(UUID userId, UUID postId, Long viewDurationSeconds, String interactionType, String metadataJson) {
        this.userId = userId;
        this.postId = postId;
        this.viewDurationSeconds = viewDurationSeconds;
        this.interactionType = interactionType;
        this.metadataJson = metadataJson;
    }

    public UUID getId() { return id; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getPostId() { return postId; }
    public void setPostId(UUID postId) { this.postId = postId; }
    public Long getViewDurationSeconds() { return viewDurationSeconds; }
    public void setViewDurationSeconds(Long viewDurationSeconds) { this.viewDurationSeconds = viewDurationSeconds; }
    public String getInteractionType() { return interactionType; }
    public void setInteractionType(String interactionType) { this.interactionType = interactionType; }
    public String getMetadataJson() { return metadataJson; }
    public void setMetadataJson(String metadataJson) { this.metadataJson = metadataJson; }
    public Instant getCreatedAt() { return createdAt; }
}
