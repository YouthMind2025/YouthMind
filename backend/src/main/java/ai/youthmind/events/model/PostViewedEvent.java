package ai.youthmind.events.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event published when a user views a post for a sufficient duration (≥15 seconds).
 * Triggers R vector update for the viewer.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostViewedEvent extends UserInteractionEvent {
    
    /**
     * ID of the viewed post
     */
    private UUID postId;
    
    /**
     * Duration of the view in seconds
     */
    private Integer duration;
    
    /**
     * Primary emotion detected in the post
     */
    private Emotion emotion;
    
    /**
     * Whether the user completed reading the post
     */
    private Boolean completed;

    public PostViewedEvent(UUID userId, UUID postId, Emotion emotion, Integer duration) {
        super(userId, "POST_VIEWED");
        this.postId = postId;
        this.duration = duration;
        this.emotion = emotion;
        this.completed = duration != null && duration >= 15;
    }

    public PostViewedEvent(UUID userId, UUID postId, Emotion emotion, Integer duration, String sessionId) {
        super(userId, "POST_VIEWED", sessionId);
        this.postId = postId;
        this.duration = duration;
        this.emotion = emotion;
        this.completed = duration != null && duration >= 15;
    }

    public PostViewedEvent(UUID userId, UUID postId, Emotion emotion, Integer duration, Boolean completed, String sessionId) {
        super(userId, "POST_VIEWED", sessionId);
        this.postId = postId;
        this.duration = duration;
        this.emotion = emotion;
        this.completed = completed;
    }

    @Override
    public String getEventType() {
        return "POST_VIEWED";
    }

    @Override
    public boolean isValid() {
        return super.isValid() && postId != null && duration != null && emotion != null;
    }

    /**
     * Check if this view event should trigger R vector update (duration ≥ 15s)
     */
    public boolean shouldUpdateRVector() {
        return duration != null && duration >= 15;
    }
}
