package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request data for creating a new post")
public class PostCreateRequest {
    
    @NotBlank(message = "Content is required")
    @Schema(description = "Post content", example = "Feeling overwhelmed with school lately...", required = true)
    private String content;
    
    @Schema(description = "Emotion tag for the post", example = "stress")
    private String emotionTag;
    
    @Schema(description = "Optional AI metadata as JSON string", example = "{\"sentiment\": \"negative\", \"confidence\": 0.85}")
    private String aiMetadata;
    
    // Explicit getters and setters
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getEmotionTag() {
        return emotionTag;
    }
    
    public void setEmotionTag(String emotionTag) {
        this.emotionTag = emotionTag;
    }
    
    public String getAiMetadata() {
        return aiMetadata;
    }
    
    public void setAiMetadata(String aiMetadata) {
        this.aiMetadata = aiMetadata;
    }
} 