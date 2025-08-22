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
@Schema(description = "Response data for post creation - returns full PostDto")
public class PostCreateResponse {
    
    @Schema(description = "The complete created post data", required = true)
    private PostDto post;
    
    @Schema(description = "Success message", example = "Post created successfully")
    private String message;
    
    // Convenience constructor for backward compatibility
    public PostCreateResponse(UUID postId, LocalDateTime createdAt) {
        this.post = new PostDto();
        this.post.setId(postId);
        this.post.setDate(createdAt);
        this.message = "Post created successfully";
    }
    
    // Constructor for full post data
    public PostCreateResponse(PostDto post, String message) {
        this.post = post;
        this.message = message;
    }
} 