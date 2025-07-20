package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Post data transfer object for feed responses")
public class PostDto {
    
    @Schema(description = "Unique identifier for the post", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;
    
    @Schema(description = "Post content", example = "Feeling overwhelmed with school lately...")
    private String content;
    
    @Schema(description = "Emotion tag for the post", example = "stress")
    private String emotionTag;
    
    @Schema(description = "AI metadata as JSON string", example = "{\"sentiment\": \"negative\", \"confidence\": 0.85}")
    private String aiMetadata;
    
    @Schema(description = "Timestamp when the post was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
    
    @Schema(description = "Anonymous user identifier", example = "user_123")
    private String anonymousUserId;
    
    @Schema(description = "Number of understanding reactions", example = "5")
    private int understandingCount;
    
    @Schema(description = "Whether the current user has reacted with understanding", example = "false")
    private boolean hasUserUnderstood;
    
    // Explicit getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getEmotionTag() { return emotionTag; }
    public void setEmotionTag(String emotionTag) { this.emotionTag = emotionTag; }
    
    public String getAiMetadata() { return aiMetadata; }
    public void setAiMetadata(String aiMetadata) { this.aiMetadata = aiMetadata; }
    
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    public String getAnonymousUserId() { return anonymousUserId; }
    public void setAnonymousUserId(String anonymousUserId) { this.anonymousUserId = anonymousUserId; }
    
    public int getUnderstandingCount() { return understandingCount; }
    public void setUnderstandingCount(int understandingCount) { this.understandingCount = understandingCount; }
    
    public boolean isHasUserUnderstood() { return hasUserUnderstood; }
    public void setHasUserUnderstood(boolean hasUserUnderstood) { this.hasUserUnderstood = hasUserUnderstood; }
} 