package ai.youthmind.events.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * Event published when a user creates a new post.
 * Triggers R vector update for the post creator.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostCreatedEvent extends UserInteractionEvent {
    
    /**
     * ID of the created post
     */
    private UUID postId;
    
    /**
     * Primary emotion detected in the post
     */
    private Emotion emotion;
    
    /**
     * Content of the post (may be truncated for events)
     */
    private String content;
    
    /**
     * Category or topic of the post
     */
    private String category;

    public PostCreatedEvent(UUID userId, UUID postId, Emotion emotion) {
        super(userId, "POST_CREATED");
        this.postId = postId;
        this.emotion = emotion;
        this.content = null;
        this.category = null;
    }

    public PostCreatedEvent(UUID userId, UUID postId, Emotion emotion, String content, String category) {
        super(userId, "POST_CREATED");
        this.postId = postId;
        this.emotion = emotion;
        this.content = content;
        this.category = category;
    }

    public PostCreatedEvent(UUID userId, UUID postId, Emotion emotion, String sessionId) {
        super(userId, "POST_CREATED", sessionId);
        this.postId = postId;
        this.emotion = emotion;
        this.content = null;
        this.category = null;
    }

    @Override
    public String getEventType() {
        return "POST_CREATED";
    }

    @Override
    public boolean isValid() {
        return super.isValid() && postId != null && emotion != null;
    }
}
