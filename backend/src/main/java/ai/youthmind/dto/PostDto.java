package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Post Data Transfer Object perfectly aligned with iOS Post model.
 * 
 * This DTO ensures seamless integration between backend and iOS frontend
 * while maintaining backward compatibility and extensibility for future features.
 * 
 * iOS Post Model Fields (exact match):
 * - id: UUID
 * - resonance: Int (likes count)
 * - view: Int (view count)
 * - collect: Int (save count)
 * - title: String
 * - content: String
 * - preImg: String (image URL)
 * - isresonanced: Bool (user liked)
 * - isCollected: Bool (user saved)
 * - emotion: String (emotion tag)
 * - date: Date (creation timestamp)
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Post data transfer object perfectly aligned with iOS Post model")
public class PostDto {
    
    // ===== CORE FIELDS (iOS Model Alignment) =====
    
    @Schema(description = "Unique post identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Number of likes/resonance", example = "15")
    private int resonance;
    
    @Schema(description = "Number of views", example = "120")
    private int view;
    
    @Schema(description = "Number of times this post has been collected/saved", example = "8")
    private int collect;
    
    @Schema(description = "Post title", example = "My thoughts on today")
    private String title;
    
    @Schema(description = "Post content", example = "Feeling overwhelmed with school lately...")
    private String content;
    
    @Schema(description = "Preview image URL", example = "https://example.com/image.jpg")
    private String preImg;
    
    @Schema(description = "Whether current user has resonated/liked this post", example = "false")
    private boolean isresonanced;
    
    @Schema(description = "Whether current user has collected/saved this post", example = "false")
    private boolean isCollected;
    
    @Schema(description = "Emotion tag for the post", example = "happy", allowableValues = {"angry", "sad", "happy", "conflicted", "surprised", "fear"})
    private String emotion;
    
    @Schema(description = "Timestamp when the post was created", example = "2024-01-15T10:30:00")
    private LocalDateTime date;
    
    // ===== BACKEND ENHANCEMENT FIELDS =====
    
    @Schema(description = "User ID for internal backend operations", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;
    
    @Schema(description = "Post topics/tags for search and categorization", example = "mental health, anxiety, coping")
    private String topics;
    
    @Schema(description = "AI-generated metadata and analysis", example = "sentiment: positive, empathy: high")
    private String aiMetadata;
    
    @Schema(description = "Whether post has been flagged for moderation", example = "false")
    private boolean flagged;
    
    @Schema(description = "Reason for flagging if applicable", example = "inappropriate content")
    private String flagReason;
    
    @Schema(description = "Timestamp when post was flagged", example = "2024-01-15T11:00:00")
    private LocalDateTime flaggedAt;
    
    @Schema(description = "Author information for enhanced user experience")
    private UserDto author;
    
    @Schema(description = "Number of comments on this post", example = "5")
    private int commentsCount;
    
    // ===== COMPATIBILITY ALIASES =====
    
    /**
     * Get likes count (alias for resonance)
     * @return Number of likes
     */
    public int getLikes() { return resonance; }
    
    /**
     * Set likes count (alias for resonance)
     * @param likes Number of likes
     */
    public void setLikes(Integer likes) { this.resonance = likes != null ? likes : 0; }
    
    /**
     * Get views count (alias for view)
     * @return Number of views
     */
    public int getViews() { return view; }
    
    /**
     * Set views count (alias for view)
     * @param views Number of views
     */
    public void setViews(Integer views) { this.view = views != null ? views : 0; }
    
    /**
     * Get bookmarks count (alias for collect)
     * @return Number of bookmarks
     */
    public int getBookmarks() { return collect; }
    
    /**
     * Set bookmarks count (alias for collect)
     * @param bookmarks Number of bookmarks
     */
    public void setBookmarks(Integer bookmarks) { this.collect = bookmarks != null ? bookmarks : 0; }
    
    /**
     * Get creation date (alias for date)
     * @return Creation timestamp
     */
    public LocalDateTime getCreatedAt() { return date; }
    
    /**
     * Set creation date (alias for date)
     * @param createdAt Creation timestamp
     */
    public void setCreatedAt(LocalDateTime createdAt) { this.date = createdAt; }
    
    /**
     * Check if user has liked the post (alias for isresonanced)
     * @return True if user has liked
     */
    public boolean isLiked() { return isresonanced; }
    
    /**
     * Set if user has liked the post (alias for isresonanced)
     * @param liked True if user has liked
     */
    public void setLiked(boolean liked) { this.isresonanced = liked; }
    
    /**
     * Check if user has bookmarked the post (alias for isCollected)
     * @return True if user has bookmarked
     */
    public boolean isBookmarked() { return isCollected; }
    
    /**
     * Set if user has bookmarked the post (alias for isCollected)
     * @param bookmarked True if user has bookmarked
     */
    public void setBookmarked(boolean bookmarked) { this.isCollected = bookmarked; }
    
    // ===== VALIDATION AND UTILITY METHODS =====
    
    /**
     * Check if this post has the required core fields for iOS compatibility
     * @return True if all required fields are present
     */
    public boolean hasRequiredFields() {
        return id != null && title != null && content != null && emotion != null && date != null;
    }
    
    /**
     * Get a summary of the post for display purposes
     * @return Post summary
     */
    public String getSummary() {
        if (content == null) return "";
        return content.length() > 100 ? content.substring(0, 100) + "..." : content;
    }
    
    /**
     * Check if the post has an image
     * @return True if preImg is not null or empty
     */
    public boolean hasImage() {
        return preImg != null && !preImg.trim().isEmpty();
    }
    
    /**
     * Get the emotion as a standardized string
     * @return Normalized emotion string
     */
    public String getNormalizedEmotion() {
        if (emotion == null) return "";
        return emotion.toLowerCase().trim();
    }

    public void setIsCollected(boolean isCollected) {
        this.isCollected = isCollected;
    }
} 