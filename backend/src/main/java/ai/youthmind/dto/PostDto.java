package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Post data transfer object matching iOS Post model exactly")
public class PostDto {
    private UUID id;
    
    // User ID for internal use
    private UUID userId;
    
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
    
    @Schema(description = "Emotion tag for the post", example = "happy")
    private String emotion;
    
    @Schema(description = "Timestamp when the post was created", example = "2024-01-15T10:30:00")
    private LocalDateTime date;
    
    // Additional fields for backend compatibility
    private String topics;
    private String aiMetadata;
    private boolean flagged;
    private String flagReason;
    private LocalDateTime flaggedAt;
    private UserDto author;
    private boolean liked;
    private boolean bookmarked;
    private int commentsCount;
    
    // Explicit getters and setters for iOS compatibility
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    
    public int getResonance() { return resonance; }
    public void setResonance(int resonance) { this.resonance = resonance; }
    
    public int getView() { return view; }
    public void setView(int view) { this.view = view; }
    
    public int getCollect() { return collect; }
    public void setCollect(int collect) { this.collect = collect; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getPreImg() { return preImg; }
    public void setPreImg(String preImg) { this.preImg = preImg; }
    
    public boolean isIsresonanced() { return isresonanced; }
    public void setIsresonanced(boolean isresonanced) { this.isresonanced = isresonanced; }
    
    public boolean isCollected() { return isCollected; }
    public void setIsCollected(boolean isCollected) { this.isCollected = isCollected; }
    
    public String getEmotion() { return emotion; }
    public void setEmotion(String emotion) { this.emotion = emotion; }
    
    public LocalDateTime getDate() { return date; }
    public void setDate(LocalDateTime date) { this.date = date; }
    
    // Additional getters and setters
    public String getTopics() { return topics; }
    public void setTopics(String topics) { this.topics = topics; }
    
    public String getAiMetadata() { return aiMetadata; }
    public void setAiMetadata(String aiMetadata) { this.aiMetadata = aiMetadata; }
    
    public boolean isFlagged() { return flagged; }
    public void setFlagged(boolean flagged) { this.flagged = flagged; }
    
    public String getFlagReason() { return flagReason; }
    public void setFlagReason(String flagReason) { this.flagReason = flagReason; }
    
    public LocalDateTime getFlaggedAt() { return flaggedAt; }
    public void setFlaggedAt(LocalDateTime flaggedAt) { this.flaggedAt = flaggedAt; }
    
    public LocalDateTime getCreatedAt() { return date; }
    public void setCreatedAt(LocalDateTime createdAt) { this.date = createdAt; }
    
    public UserDto getAuthor() { return author; }
    public void setAuthor(UserDto author) { this.author = author; }
    
    public boolean isLiked() { return liked; }
    public void setLiked(boolean liked) { this.liked = liked; }
    
    public boolean isBookmarked() { return bookmarked; }
    public void setBookmarked(boolean bookmarked) { this.bookmarked = bookmarked; }
    
    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
    
    // Aliases for backend compatibility
    public int getLikes() { return resonance; }
    public void setLikes(Integer likes) { this.resonance = likes != null ? likes : 0; }
    
    public int getViews() { return view; }
    public void setViews(Integer views) { this.view = views != null ? views : 0; }
    
    public int getBookmarks() { return collect; }
    public void setBookmarks(Integer bookmarks) { this.collect = bookmarks != null ? bookmarks : 0; }
} 