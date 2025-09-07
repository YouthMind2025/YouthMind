package ai.youthmind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for checking moderation status of content.
 * Provides comprehensive information about the moderation process.
 */
@Schema(description = "Response containing moderation status information")
public class ModerationStatusResponse {
    
    @JsonProperty("content_id")
    @Schema(description = "ID of the content being moderated", 
            example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID contentId;
    
    @JsonProperty("content_type")
    @Schema(description = "Type of content being moderated", 
            example = "post")
    private String contentType;
    
    @JsonProperty("status")
    @Schema(description = "Current moderation status", 
            example = "pending_review", 
            allowableValues = {"pending_review", "approved", "rejected", "escalated", "resolved"})
    private String status;
    
    @JsonProperty("reason")
    @Schema(description = "Reason for moderation", 
            example = "inappropriate_language")
    private String reason;
    
    @JsonProperty("priority")
    @Schema(description = "Priority level assigned to this moderation case", 
            example = "high")
    private String priority;
    
    @JsonProperty("submitted_at")
    @Schema(description = "When the content was submitted for moderation")
    private LocalDateTime submittedAt;
    
    @JsonProperty("reviewed_at")
    @Schema(description = "When the content was last reviewed (if applicable)")
    private LocalDateTime reviewedAt;
    
    @JsonProperty("reviewed_by")
    @Schema(description = "ID of the admin who reviewed the content (if applicable)")
    private UUID reviewedBy;
    
    @JsonProperty("ai_confidence")
    @Schema(description = "AI model confidence score for automated moderation", 
            example = "0.85")
    private Double aiConfidence;
    
    @JsonProperty("moderator_notes")
    @Schema(description = "Notes from the moderator reviewing the content")
    private String moderatorNotes;
    
    @JsonProperty("action_taken")
    @Schema(description = "Action taken as a result of moderation", 
            example = "content_hidden", 
            allowableValues = {"none", "content_hidden", "user_warned", "user_suspended", "content_deleted"})
    private String actionTaken;
    
    @JsonProperty("escalation_level")
    @Schema(description = "Current escalation level", 
            example = "1")
    private Integer escalationLevel = 0;
    
    // Constructors
    public ModerationStatusResponse() {}
    
    public ModerationStatusResponse(UUID contentId, String contentType, String status) {
        this.contentId = contentId;
        this.contentType = contentType;
        this.status = status;
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
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public String getPriority() {
        return priority;
    }
    
    public void setPriority(String priority) {
        this.priority = priority;
    }
    
    public LocalDateTime getSubmittedAt() {
        return submittedAt;
    }
    
    public void setSubmittedAt(LocalDateTime submittedAt) {
        this.submittedAt = submittedAt;
    }
    
    public LocalDateTime getReviewedAt() {
        return reviewedAt;
    }
    
    public void setReviewedAt(LocalDateTime reviewedAt) {
        this.reviewedAt = reviewedAt;
    }
    
    public UUID getReviewedBy() {
        return reviewedBy;
    }
    
    public void setReviewedBy(UUID reviewedBy) {
        this.reviewedBy = reviewedBy;
    }
    
    public Double getAiConfidence() {
        return aiConfidence;
    }
    
    public void setAiConfidence(Double aiConfidence) {
        this.aiConfidence = aiConfidence;
    }
    
    public String getModeratorNotes() {
        return moderatorNotes;
    }
    
    public void setModeratorNotes(String moderatorNotes) {
        this.moderatorNotes = moderatorNotes;
    }
    
    public String getActionTaken() {
        return actionTaken;
    }
    
    public void setActionTaken(String actionTaken) {
        this.actionTaken = actionTaken;
    }
    
    public Integer getEscalationLevel() {
        return escalationLevel;
    }
    
    public void setEscalationLevel(Integer escalationLevel) {
        this.escalationLevel = escalationLevel;
    }
    
    // Helper methods
    public boolean isPending() {
        return "pending_review".equals(status);
    }
    
    public boolean isResolved() {
        return "resolved".equals(status) || "approved".equals(status) || "rejected".equals(status);
    }
    
    public boolean requiresAction() {
        return isPending() || "escalated".equals(status);
    }
}
