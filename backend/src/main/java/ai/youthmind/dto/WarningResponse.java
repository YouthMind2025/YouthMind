package ai.youthmind.dto;

import ai.youthmind.entity.WarningHistory;
import ai.youthmind.entity.User;
import ai.youthmind.entity.UserModerationStatus;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO for warning system responses.
 */
public class WarningResponse {
    
    private UUID warningId;
    private UUID userId;
    private String username;
    private WarningHistory.WarningType warningType;
    private WarningHistory.Severity severity;
    private String reason;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private LocalDateTime acknowledgedAt;
    private UUID postId;
    private UUID commentId;
    private UUID moderatorId;
    private String moderatorUsername;
    
    // User status information
    private User.UserStatus userStatus;
    private Integer warningsCount;
    private LocalDateTime banExpiresAt;
    
    // Moderation status information
    private UserModerationStatus.ModerationStatus moderationStatus;
    private Integer warningLevel;
    private Boolean manualReviewRequired;
    
    // Constructors
    public WarningResponse() {}
    
    public WarningResponse(WarningHistory warning) {
        this.warningId = warning.getId();
        this.userId = warning.getUserId();
        this.warningType = warning.getWarningType();
        this.severity = warning.getSeverity();
        this.reason = warning.getReason();
        this.createdAt = warning.getCreatedAt();
        this.expiresAt = warning.getExpiresAt();
        this.acknowledgedAt = warning.getAcknowledgedAt();
        this.postId = warning.getPostId();
        this.commentId = warning.getCommentId();
        this.moderatorId = warning.getModeratorId();
    }
    
    // Getters and Setters
    public UUID getWarningId() {
        return warningId;
    }
    
    public void setWarningId(UUID warningId) {
        this.warningId = warningId;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getUsername() {
        return username;
    }
    
    public void setUsername(String username) {
        this.username = username;
    }
    
    public WarningHistory.WarningType getWarningType() {
        return warningType;
    }
    
    public void setWarningType(WarningHistory.WarningType warningType) {
        this.warningType = warningType;
    }
    
    public WarningHistory.Severity getSeverity() {
        return severity;
    }
    
    public void setSeverity(WarningHistory.Severity severity) {
        this.severity = severity;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }
    
    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }
    
    public LocalDateTime getAcknowledgedAt() {
        return acknowledgedAt;
    }
    
    public void setAcknowledgedAt(LocalDateTime acknowledgedAt) {
        this.acknowledgedAt = acknowledgedAt;
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
    
    public UUID getModeratorId() {
        return moderatorId;
    }
    
    public void setModeratorId(UUID moderatorId) {
        this.moderatorId = moderatorId;
    }
    
    public String getModeratorUsername() {
        return moderatorUsername;
    }
    
    public void setModeratorUsername(String moderatorUsername) {
        this.moderatorUsername = moderatorUsername;
    }
    
    public User.UserStatus getUserStatus() {
        return userStatus;
    }
    
    public void setUserStatus(User.UserStatus userStatus) {
        this.userStatus = userStatus;
    }
    
    public Integer getWarningsCount() {
        return warningsCount;
    }
    
    public void setWarningsCount(Integer warningsCount) {
        this.warningsCount = warningsCount;
    }
    
    public LocalDateTime getBanExpiresAt() {
        return banExpiresAt;
    }
    
    public void setBanExpiresAt(LocalDateTime banExpiresAt) {
        this.banExpiresAt = banExpiresAt;
    }
    
    public UserModerationStatus.ModerationStatus getModerationStatus() {
        return moderationStatus;
    }
    
    public void setModerationStatus(UserModerationStatus.ModerationStatus moderationStatus) {
        this.moderationStatus = moderationStatus;
    }
    
    public Integer getWarningLevel() {
        return warningLevel;
    }
    
    public void setWarningLevel(Integer warningLevel) {
        this.warningLevel = warningLevel;
    }
    
    public Boolean getManualReviewRequired() {
        return manualReviewRequired;
    }
    
    public void setManualReviewRequired(Boolean manualReviewRequired) {
        this.manualReviewRequired = manualReviewRequired;
    }
    
    @Override
    public String toString() {
        return "WarningResponse{" +
                "warningId=" + warningId +
                ", userId=" + userId +
                ", username='" + username + '\'' +
                ", warningType=" + warningType +
                ", severity=" + severity +
                ", reason='" + reason + '\'' +
                ", createdAt=" + createdAt +
                ", expiresAt=" + expiresAt +
                ", acknowledgedAt=" + acknowledgedAt +
                ", postId=" + postId +
                ", commentId=" + commentId +
                ", moderatorId=" + moderatorId +
                ", moderatorUsername='" + moderatorUsername + '\'' +
                ", userStatus=" + userStatus +
                ", warningsCount=" + warningsCount +
                ", banExpiresAt=" + banExpiresAt +
                ", moderationStatus=" + moderationStatus +
                ", warningLevel=" + warningLevel +
                ", manualReviewRequired=" + manualReviewRequired +
                '}';
    }
}
