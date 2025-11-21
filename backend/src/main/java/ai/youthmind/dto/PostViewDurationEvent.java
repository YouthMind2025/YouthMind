package ai.youthmind.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for tracking post view duration events
 * Used to record when users start and end viewing posts
 * Enables significant view detection (15+ seconds) for R vector updates
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PostViewDurationEvent {
    
    // Note: Lombok @Builder, @Data generate all required methods
    
    /**
     * Unique identifier for the user viewing the post
     */
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    /**
     * Unique identifier for the post being viewed
     */
    @NotNull(message = "Post ID is required")
    private UUID postId;
    
    /**
     * Timestamp when the user started viewing the post
     */
    @NotNull(message = "View started timestamp is required")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime viewStartedAt;
    
    /**
     * Timestamp when the user stopped viewing the post
     * Null if view is still in progress
     */
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime viewEndedAt;
    
    /**
     * Duration in seconds that the user viewed the post
     * Null if view is still in progress
     * Calculated as (viewEndedAt - viewStartedAt)
     */
    @Min(value = 0, message = "View duration cannot be negative")
    private Integer viewDurationSeconds;
    
    /**
     * Session identifier to track continuous viewing sessions
     * Optional field for advanced analytics
     */
    private String sessionId;
    
    /**
     * Device type (mobile, tablet, desktop)
     * Optional field for device-specific analytics
     */
    private String deviceType;
    
    /**
     * IP address of the viewer
     * Optional field for security and analytics
     */
    private String ipAddress;
    
    /**
     * Check if this view event is complete (has end time and duration)
     * 
     * @return true if view has ended
     */
    public boolean isComplete() {
        return viewEndedAt != null && viewDurationSeconds != null;
    }
    
    /**
     * Check if this view qualifies as significant (>= 15 seconds)
     * Used to determine if this view should update the user's R vector
     * 
     * @return true if view duration is 15 seconds or more
     */
    public boolean isSignificantView() {
        return viewDurationSeconds != null && viewDurationSeconds >= 15;
    }
    
    /**
     * Calculate duration if not already set
     * Uses viewStartedAt and viewEndedAt to compute duration
     */
    public void calculateDuration() {
        if (viewStartedAt != null && viewEndedAt != null && viewDurationSeconds == null) {
            long seconds = java.time.Duration.between(viewStartedAt, viewEndedAt).getSeconds();
            this.viewDurationSeconds = (int) seconds;
        }
    }
    
    /**
     * Complete the view event with current timestamp
     * Sets viewEndedAt to now and calculates duration
     */
    public void completeView() {
        if (viewEndedAt == null) {
            this.viewEndedAt = LocalDateTime.now();
            calculateDuration();
        }
    }
    
    /**
     * Get view duration in minutes (for display purposes)
     * 
     * @return duration in minutes, or 0 if not complete
     */
    public double getDurationInMinutes() {
        return viewDurationSeconds != null ? viewDurationSeconds / 60.0 : 0.0;
    }
    
    /**
     * Create a new PostViewDurationEvent for an ongoing view
     * 
     * @param userId User viewing the post
     * @param postId Post being viewed
     * @return New event with viewStartedAt set to now
     */
    public static PostViewDurationEvent startView(UUID userId, UUID postId) {
        return PostViewDurationEvent.builder()
            .userId(userId)
            .postId(postId)
            .viewStartedAt(LocalDateTime.now())
            .build();
    }
    
    /**
     * Create a completed PostViewDurationEvent
     * 
     * @param userId User who viewed the post
     * @param postId Post that was viewed
     * @param durationSeconds Duration of the view in seconds
     * @return Completed event with all fields populated
     */
    public static PostViewDurationEvent createCompletedView(UUID userId, UUID postId, Integer durationSeconds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startedAt = now.minusSeconds(durationSeconds);
        
        return PostViewDurationEvent.builder()
            .userId(userId)
            .postId(postId)
            .viewStartedAt(startedAt)
            .viewEndedAt(now)
            .viewDurationSeconds(durationSeconds)
            .build();
    }
}

