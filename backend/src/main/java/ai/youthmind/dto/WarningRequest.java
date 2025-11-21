package ai.youthmind.dto;

import ai.youthmind.entity.WarningHistory.WarningType;
import ai.youthmind.entity.WarningHistory.Severity;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

/**
 * DTO for issuing warnings to users.
 */
public class WarningRequest {
    
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @NotNull(message = "Warning type is required")
    private WarningType warningType;
    
    @NotNull(message = "Severity is required")
    private Severity severity;
    
    @NotBlank(message = "Reason is required")
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;
    
    private UUID postId;
    
    private UUID commentId;
    
    // Constructors
    public WarningRequest() {}
    
    public WarningRequest(UUID userId, WarningType warningType, Severity severity, String reason) {
        this.userId = userId;
        this.warningType = warningType;
        this.severity = severity;
        this.reason = reason;
    }
    
    public WarningRequest(UUID userId, WarningType warningType, Severity severity, String reason, UUID postId, UUID commentId) {
        this.userId = userId;
        this.warningType = warningType;
        this.severity = severity;
        this.reason = reason;
        this.postId = postId;
        this.commentId = commentId;
    }
    
    // Getters and Setters
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public WarningType getWarningType() {
        return warningType;
    }
    
    public void setWarningType(WarningType warningType) {
        this.warningType = warningType;
    }
    
    public Severity getSeverity() {
        return severity;
    }
    
    public void setSeverity(Severity severity) {
        this.severity = severity;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public UUID getPostId() {
        return postId;
    }
    
    public void setPostId(UUID postId) {
        this.postId = postId;
    }
    
    public UUID getCommentId() {
        return commentId;
    }
    
    public void setCommentId(UUID commentId) {
        this.commentId = commentId;
    }
    
    @Override
    public String toString() {
        return "WarningRequest{" +
                "userId=" + userId +
                ", warningType=" + warningType +
                ", severity=" + severity +
                ", reason='" + reason + '\'' +
                ", postId=" + postId +
                ", commentId=" + commentId +
                '}';
    }
}
