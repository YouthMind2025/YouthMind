package ai.youthmind.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

/**
 * Request DTO for submitting content for moderation review.
 * Used by admin users to flag posts or other content for review.
 */
@Schema(description = "Request to submit content for moderation")
public class ModerationSubmissionRequest {
    
    @NotNull(message = "Content ID is required")
    @Schema(description = "ID of the content to be moderated (post, comment, etc.)", 
            example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID contentId;
    
    @NotBlank(message = "Content type is required")
    @Schema(description = "Type of content being moderated", 
            example = "post", allowableValues = {"post", "comment", "user"})
    private String contentType;
    
    @NotBlank(message = "Reason is required")
    @Schema(description = "Reason for flagging the content", 
            example = "Inappropriate language", 
            allowableValues = {"inappropriate_language", "harassment", "spam", "violence", "hate_speech", "other"})
    private String reason;
    
    @Schema(description = "Additional details or context for the moderation request", 
            example = "Contains explicit language that violates community guidelines")
    private String details;
    
    @Schema(description = "Priority level for moderation review", 
            example = "high", allowableValues = {"low", "medium", "high", "urgent"})
    private String priority = "medium";
    
    // Constructors
    public ModerationSubmissionRequest() {}
    
    public ModerationSubmissionRequest(UUID contentId, String contentType, String reason, String details, String priority) {
        this.contentId = contentId;
        this.contentType = contentType;
        this.reason = reason;
        this.details = details;
        this.priority = priority;
    }
    
    // Getters and setters
    public UUID getContentId() {
        return contentId;
    }
    
    public void setContentId(UUID contentId) {
        this.contentId = contentId;
    }
    
    public String getContentType() {
        return contentType;
    }
    
    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getDetails() {
        return details;
    }
    
    public void setDetails(String details) {
        this.details = details;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    // Validation methods
    public boolean isValidContentType() {
        return contentType != null && 
               (contentType.equals("post") || contentType.equals("comment") || contentType.equals("user"));
    }
    
    public boolean isValidPriority() {
        return priority != null && 
               (priority.equals("low") || priority.equals("medium") || 
                priority.equals("high") || priority.equals("urgent"));
    }
    
    public boolean isValidReason() {
        return reason != null && 
               (reason.equals("inappropriate_language") || reason.equals("harassment") || 
                reason.equals("spam") || reason.equals("violence") || 
                reason.equals("hate_speech") || reason.equals("other"));
    }
}
