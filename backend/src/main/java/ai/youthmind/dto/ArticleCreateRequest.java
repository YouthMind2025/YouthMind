package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request DTO for creating new articles
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request to create a new article")
public class ArticleCreateRequest {
    
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Schema(description = "Article title", example = "The Future of AI in Education", required = true)
    private String title;
    
    @NotBlank(message = "Content is required")
    @Schema(description = "Article content in HTML format", example = "<h1>Introduction</h1><p>This article discusses...</p>", required = true)
    private String content;
    
    @Size(max = 500, message = "Introduction must not exceed 500 characters")
    @Schema(description = "Article introduction/summary", example = "Exploring how AI is transforming education...")
    private String intro;
    
    @Size(max = 500, message = "Cover image path must not exceed 500 characters")
    @Schema(description = "Cover image URL or filename", example = "cover1.jpg")
    private String coverImage;
    
    @Schema(description = "Whether to publish the article immediately", example = "true")
    private Boolean published = true;
}
