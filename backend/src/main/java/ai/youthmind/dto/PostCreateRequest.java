package ai.youthmind.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request data for creating a new post - matches iOS CreatePostRequest")
public class PostCreateRequest {
    
    @NotBlank(message = "Title is required")
    @Schema(description = "Post title", example = "My thoughts on today", required = true)
    private String title;
    
    @NotBlank(message = "Content is required")
    private String content;
    
    @Schema(description = "Emotion for the post", example = "happy", required = false)
    private String emotion;
    
    @Schema(description = "Preview image URL", example = "https://example.com/image.jpg", required = false)
    private String preImg;
    
    @Schema(description = "Post topics/tags for search and categorization", example = "mental health,anxiety,coping", required = false)
    private String topics;
    
    // Explicit getters and setters
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getEmotion() {
        return emotion;
    }
    
    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
    
    public String getPreImg() {
        return preImg;
    }
    
    public void setPreImg(String preImg) {
        this.preImg = preImg;
    }
    
    public String getTopics() {
        return topics;
    }
    
    public void setTopics(String topics) {
        this.topics = topics;
    }
} 