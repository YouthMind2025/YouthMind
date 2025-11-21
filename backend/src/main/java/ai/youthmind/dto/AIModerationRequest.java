package ai.youthmind.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Request DTO for AI-powered content moderation
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AIModerationRequest {
    
    @NotBlank(message = "Content cannot be blank")
    @Size(max = 10000, message = "Content cannot exceed 10,000 characters")
    private String content;
    
    @NotNull(message = "Content type is required")
    private ContentType contentType;
    
    @Size(max = 500, message = "Context cannot exceed 500 characters")
    private String context; // Additional context for moderation
    
    public enum ContentType {
        POST, COMMENT, MESSAGE, BIO, USERNAME
    }
    
    /**
     * Validate the request
     */
    public boolean isValid() {
        return content != null && !content.trim().isEmpty() && 
               contentType != null && 
               content.length() <= 10000 &&
               (context == null || context.length() <= 500);
    }
}
