package ai.youthmind.events.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

/**
 * Event published when a user likes a post.
 * Triggers R vector update for the user who liked the post.
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PostLikedEvent extends UserInteractionEvent {
    
    /**
     * ID of the liked post
     */
    private UUID postId;
    
    /**
     * Primary emotion detected in the post
     */
    private Emotion emotion;
    
    /**
     * Whether this is a like or unlike action
     */
    private Boolean isLike;
    
    /**
     * ID of the post creator (for analytics)
     */
    private UUID postCreatorId;

    public PostLikedEvent(UUID userId, UUID postId, Emotion emotion) {
        super(userId, "POST_LIKED");
        this.postId = postId;
        this.emotion = emotion;
        this.isLike = true;
        this.postCreatorId = null;
    }

    public PostLikedEvent(UUID userId, UUID postId, Emotion emotion, Boolean isLike) {
        super(userId, "POST_LIKED");
        this.postId = postId;
        this.emotion = emotion;
        this.isLike = isLike;
        this.postCreatorId = null;
    }

    public PostLikedEvent(UUID userId, UUID postId, Emotion emotion, Boolean isLike, UUID postCreatorId) {
        super(userId, "POST_LIKED");
        this.postId = postId;
        this.emotion = emotion;
        this.isLike = isLike;
        this.postCreatorId = postCreatorId;
    }

    public PostLikedEvent(UUID userId, UUID postId, Emotion emotion, String sessionId) {
        super(userId, "POST_LIKED", sessionId);
        this.postId = postId;
        this.emotion = emotion;
        this.isLike = true;
        this.postCreatorId = null;
    }

    @Override
    public String getEventType() {
        return "POST_LIKED";
    }

    @Override
    public boolean isValid() {
        return super.isValid() && postId != null && emotion != null && isLike != null;
    }
}
