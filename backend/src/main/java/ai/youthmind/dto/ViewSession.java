package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * View session data structure for tracking active post views
 * Used for Redis caching of view sessions with start/end tracking
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ViewSession implements Serializable {
    
    private UUID userId;
    private UUID postId;
    private LocalDateTime startTime;
    private String postEmotion;
    private LocalDateTime expiresAt;
    
    /**
     * Constructor for creating a new view session
     * Sets expiresAt to 2 hours from current time
     * 
     * @param userId The user ID
     * @param postId The post ID
     * @param startTime The start time of the view
     * @param postEmotion The emotion of the post being viewed
     */
    public ViewSession(UUID userId, UUID postId, LocalDateTime startTime, String postEmotion) {
        this.userId = userId;
        this.postId = postId;
        this.startTime = startTime;
        this.postEmotion = postEmotion;
        this.expiresAt = startTime.plusHours(2); // 2 hours TTL
    }
}
