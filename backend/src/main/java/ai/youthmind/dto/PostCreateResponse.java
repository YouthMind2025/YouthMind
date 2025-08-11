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
@Schema(description = "Response data for post creation")
public class PostCreateResponse {
    
    @Schema(description = "Unique identifier for the created post", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID postId;
    
    @Schema(description = "Timestamp when the post was created", example = "2024-01-15T10:30:00")
    private LocalDateTime createdAt;
} 