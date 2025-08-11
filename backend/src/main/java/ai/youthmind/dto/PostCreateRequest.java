package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;
import java.util.Map;

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
    
    @Schema(description = "Optional AI metadata", example = "{\"sentiment\": \"negative\", \"confidence\": 0.85}")
    private Map<String, Object> aiMetadata;
} 