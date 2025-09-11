package ai.youthmind.dto;

import ai.youthmind.entity.User;
import ai.youthmind.entity.UserModerationStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for user moderation status responses.
 */
public class UserModerationStatusResponse {
    
    private UUID userId;
    private String username;
    private UserModerationStatus.ModerationStatus status;
    private Integer warningLevel;
    private Integer totalWarnings;
    private Integer totalBans;
    private Boolean manualReviewRequired;
    private Boolean autoModerationEnabled;
    private LocalDateTime lastWarningAt;
    private LocalDateTime lastModerationAt;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // User status information
    private User.UserStatus userStatus;
    private Integer warningsCount;
    private LocalDateTime banExpiresAt;
    
    // Constructors
    public UserModerationStatusResponse() {}
    
    public UserModerationStatusResponse(UserModerationStatus status) {
        this.userId = status.getUserId();
        this.status = status.getStatus();
        this.warningLevel = status.getWarningLevel();
        this.totalWarnings = status.getTotalWarnings();
        this.totalBans = status.getTotalBans();
        this.manualReviewRequired = status.getManualReviewRequired();
        this.autoModerationEnabled = status.getAutoModerationEnabled();
        this.lastWarningAt = status.getLastWarningAt();
        this.lastModerationAt = status.getLastModerationAt();
        this.notes = status.getNotes();
        this.createdAt = status.getCreatedAt();
        this.updatedAt = status.getUpdatedAt();
    }
    
    // Getters and Setters
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
    
    public UserModerationStatus.ModerationStatus getStatus() {
        return status;
    }
    
    public void setStatus(UserModerationStatus.ModerationStatus status) {
        this.status = status;
    }
    
    public Integer getWarningLevel() {
        return warningLevel;
    }
    
    public void setWarningLevel(Integer warningLevel) {
        this.warningLevel = warningLevel;
    }
    
    public Integer getTotalWarnings() {
        return totalWarnings;
    }
    
    public void setTotalWarnings(Integer totalWarnings) {
        this.totalWarnings = totalWarnings;
    }
    
    public Integer getTotalBans() {
        return totalBans;
    }
    
    public void setTotalBans(Integer totalBans) {
        this.totalBans = totalBans;
    }
    
    public Boolean getManualReviewRequired() {
        return manualReviewRequired;
    }
    
    public void setManualReviewRequired(Boolean manualReviewRequired) {
        this.manualReviewRequired = manualReviewRequired;
    }
    
    public Boolean getAutoModerationEnabled() {
        return autoModerationEnabled;
    }
    
    public void setAutoModerationEnabled(Boolean autoModerationEnabled) {
        this.autoModerationEnabled = autoModerationEnabled;
    }
    
    public LocalDateTime getLastWarningAt() {
        return lastWarningAt;
    }
    
    public void setLastWarningAt(LocalDateTime lastWarningAt) {
        this.lastWarningAt = lastWarningAt;
    }
    
    public LocalDateTime getLastModerationAt() {
        return lastModerationAt;
    }
    
    public void setLastModerationAt(LocalDateTime lastModerationAt) {
        this.lastModerationAt = lastModerationAt;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
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
    
    @Override
    public String toString() {
        return "UserModerationStatusResponse{" +
                "userId=" + userId +
                ", username='" + username + '\'' +
                ", status=" + status +
                ", warningLevel=" + warningLevel +
                ", totalWarnings=" + totalWarnings +
                ", totalBans=" + totalBans +
                ", manualReviewRequired=" + manualReviewRequired +
                ", autoModerationEnabled=" + autoModerationEnabled +
                ", lastWarningAt=" + lastWarningAt +
                ", lastModerationAt=" + lastModerationAt +
                ", notes='" + notes + '\'' +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", userStatus=" + userStatus +
                ", warningsCount=" + warningsCount +
                ", banExpiresAt=" + banExpiresAt +
                '}';
    }
}
