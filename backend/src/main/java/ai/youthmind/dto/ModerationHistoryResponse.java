package ai.youthmind.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for retrieving moderation history for a user.
 * Provides a comprehensive view of all moderation actions related to a user.
 */
@Schema(description = "Response containing moderation history for a user")
public class ModerationHistoryResponse {
    
    @JsonProperty("user_id")
    @Schema(description = "ID of the user whose moderation history is being retrieved", 
            example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID userId;
    
    @JsonProperty("total_moderation_cases")
    @Schema(description = "Total number of moderation cases for this user", 
            example = "5")
    private Integer totalModerationCases;
    
    @JsonProperty("pending_cases")
    @Schema(description = "Number of pending moderation cases", 
            example = "1")
    private Integer pendingCases;
    
    @JsonProperty("resolved_cases")
    @Schema(description = "Number of resolved moderation cases", 
            example = "4")
    private Integer resolvedCases;
    
    @JsonProperty("escalated_cases")
    @Schema(description = "Number of escalated moderation cases", 
            example = "0")
    private Integer escalatedCases;
    
    @JsonProperty("moderation_entries")
    @Schema(description = "List of moderation history entries")
    private List<ModerationHistoryEntry> moderationEntries;
    
    @JsonProperty("user_status")
    @Schema(description = "Current moderation status of the user", 
            example = "active", 
            allowableValues = {"active", "warned", "suspended", "banned"})
    private String userStatus;
    
    @JsonProperty("last_moderation_action")
    @Schema(description = "Most recent moderation action taken against this user")
    private LocalDateTime lastModerationAction;
    
    @JsonProperty("risk_score")
    @Schema(description = "Calculated risk score for this user (0-100)", 
            example = "25")
    private Integer riskScore;
    
    // Constructors
    public ModerationHistoryResponse() {}
    
    public ModerationHistoryResponse(UUID userId, List<ModerationHistoryEntry> moderationEntries) {
        this.userId = userId;
        this.moderationEntries = moderationEntries;
        this.totalModerationCases = moderationEntries != null ? moderationEntries.size() : 0;
    }
    
    // Getters and setters
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public Integer getTotalModerationCases() {
        return totalModerationCases;
    }
    
    public void setTotalModerationCases(Integer totalModerationCases) {
        this.totalModerationCases = totalModerationCases;
    }
    
    public Integer getPendingCases() {
        return pendingCases;
    }
    
    public void setPendingCases(Integer pendingCases) {
        this.pendingCases = pendingCases;
    }
    
    public Integer getResolvedCases() {
        return resolvedCases;
    }
    
    public void setResolvedCases(Integer resolvedCases) {
        this.resolvedCases = resolvedCases;
    }
    
    public Integer getEscalatedCases() {
        return escalatedCases;
    }
    
    public void setEscalatedCases(Integer escalatedCases) {
        this.escalatedCases = escalatedCases;
    }
    
    public List<ModerationHistoryEntry> getModerationEntries() {
        return moderationEntries;
    }
    
    public void setModerationEntries(List<ModerationHistoryEntry> moderationEntries) {
        this.moderationEntries = moderationEntries;
    }
    
    public String getUserStatus() {
        return userStatus;
    }
    
    public void setUserStatus(String userStatus) {
        this.userStatus = userStatus;
    }
    
    public LocalDateTime getLastModerationAction() {
        return lastModerationAction;
    }
    
    public void setLastModerationAction(LocalDateTime lastModerationAction) {
        this.lastModerationAction = lastModerationAction;
    }
    
    public Integer getRiskScore() {
        return riskScore;
    }
    
    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }
    
    /**
     * Inner class representing a single moderation history entry
     */
    @Schema(description = "Single moderation history entry")
    public static class ModerationHistoryEntry {
        
        @JsonProperty("content_id")
        @Schema(description = "ID of the content that was moderated")
        private UUID contentId;
        
        @JsonProperty("content_type")
        @Schema(description = "Type of content moderated")
        private String contentType;
        
        @JsonProperty("reason")
        @Schema(description = "Reason for moderation")
        private String reason;
        
        @JsonProperty("status")
        @Schema(description = "Final status of the moderation")
        private String status;
        
        @JsonProperty("action_taken")
        @Schema(description = "Action taken as a result")
        private String actionTaken;
        
        @JsonProperty("moderated_at")
        @Schema(description = "When the moderation occurred")
        private LocalDateTime moderatedAt;
        
        @JsonProperty("moderated_by")
        @Schema(description = "ID of the admin who performed the moderation")
        private UUID moderatedBy;
        
        @JsonProperty("moderator_notes")
        @Schema(description = "Notes from the moderator")
        private String moderatorNotes;
        
        // Constructors
        public ModerationHistoryEntry() {}
        
        public ModerationHistoryEntry(UUID contentId, String contentType, String reason, 
                                    String status, String actionTaken, LocalDateTime moderatedAt) {
            this.contentId = contentId;
            this.contentType = contentType;
            this.reason = reason;
            this.status = status;
            this.actionTaken = actionTaken;
            this.moderatedAt = moderatedAt;
        }
        
        // Getters and setters
        public UUID getContentId() { return contentId; }
        public void setContentId(UUID contentId) { this.contentId = contentId; }
        
        public String getContentType() { return contentType; }
        public void setContentType(String contentType) { this.contentType = contentType; }
        
        public String getReason() { return reason; }
        public void setReason(String reason) { this.reason = reason; }
        
        public String getStatus() { return status; }
        public void setStatus(String status) { this.status = status; }
        
        public String getActionTaken() { return actionTaken; }
        public void setActionTaken(String actionTaken) { this.actionTaken = actionTaken; }
        
        public LocalDateTime getModeratedAt() { return moderatedAt; }
        public void setModeratedAt(LocalDateTime moderatedAt) { this.moderatedAt = moderatedAt; }
        
        public UUID getModeratedBy() { return moderatedBy; }
        public void setModeratedBy(UUID moderatedBy) { this.moderatedBy = moderatedBy; }
        
        public String getModeratorNotes() { return moderatorNotes; }
        public void setModeratorNotes(String moderatorNotes) { this.moderatorNotes = moderatorNotes; }
    }
}
