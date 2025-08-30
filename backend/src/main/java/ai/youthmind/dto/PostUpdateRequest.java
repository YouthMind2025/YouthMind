package ai.youthmind.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.Size;

/**
 * Post Update Request Data Transfer Object
 * 
 * Provides a structured way to update post fields with validation.
 * Only allows updating specific fields that should be user-modifiable.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request object for updating post fields")
public class PostUpdateRequest {
    
    @Schema(description = "Post title", example = "Updated thoughts on today")
    @Size(min = 1, max = 255, message = "Title must be between 1 and 255 characters")
    private String title;
    
    @Schema(description = "Post content", example = "Updated content with new thoughts...")
    @Size(min = 1, max = 10000, message = "Content must be between 1 and 10000 characters")
    private String content;
    
    @Schema(description = "Emotion tag", example = "happy", allowableValues = {"angry", "sad", "happy", "conflicted", "surprised", "fear"})
    @Size(max = 100, message = "Emotion must not exceed 100 characters")
    private String emotion;
    
    @Schema(description = "Preview image URL", example = "https://example.com/new-image.jpg")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String preImg;
    
    @Schema(description = "Post topics/tags", example = "mental health, anxiety, coping")
    @Size(max = 1000, message = "Topics must not exceed 1000 characters")
    private String topics;
    
    /**
     * Check if this update request has any fields to update
     * @return true if at least one field is provided
     */
    public boolean hasUpdates() {
        return title != null || content != null || emotion != null || preImg != null || topics != null;
    }
    
    /**
     * Validate that the emotion is one of the allowed values
     * @return true if emotion is valid or null
     */
    public boolean isValidEmotion() {
        if (emotion == null) return true;
        
        String normalizedEmotion = emotion.toLowerCase().trim();
        return normalizedEmotion.matches("^(angry|sad|happy|conflicted|surprised|fear)$");
    }
    
    /**
     * Get a summary of what fields are being updated
     * @return String describing the update
     */
    public String getUpdateSummary() {
        StringBuilder summary = new StringBuilder("Updating post fields: ");
        boolean hasFields = false;
        
        if (title != null) {
            summary.append("title, ");
            hasFields = true;
        }
        if (content != null) {
            summary.append("content, ");
            hasFields = true;
        }
        if (emotion != null) {
            summary.append("emotion, ");
            hasFields = true;
        }
        if (preImg != null) {
            summary.append("preImg, ");
            hasFields = true;
        }
        if (topics != null) {
            summary.append("topics, ");
            hasFields = true;
        }
        
        if (hasFields) {
            summary.setLength(summary.length() - 2); // Remove last ", "
            summary.append(".");
        } else {
            summary.append("no fields provided.");
        }
        
        return summary.toString();
    }
} 