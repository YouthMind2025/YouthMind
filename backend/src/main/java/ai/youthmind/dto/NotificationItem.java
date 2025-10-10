package ai.youthmind.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for individual notification items
 */
public class NotificationItem {
    private UUID id;
    private String notificationType; // "SYSTEM", "COMMENT", "RESONANCE", "MENTION", "FOLLOW"
    private String title;
    private String message;
    private String actionUrl; // URL to navigate to when notification is clicked
    private boolean isRead;
    private UUID fromUserId; // User who triggered the notification (null for system notifications)
    private String fromUserName;
    private String fromUserAvatar;
    private UUID relatedPostId; // Related post ID (if applicable)
    private String relatedPostTitle;
    private LocalDateTime createdAt;

    // Constructors
    public NotificationItem() {}

    public NotificationItem(UUID id, String notificationType, String title, String message, 
                           String actionUrl, boolean isRead, UUID fromUserId, String fromUserName, 
                           String fromUserAvatar, UUID relatedPostId, String relatedPostTitle, 
                           LocalDateTime createdAt) {
        this.id = id;
        this.notificationType = notificationType;
        this.title = title;
        this.message = message;
        this.actionUrl = actionUrl;
        this.isRead = isRead;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserAvatar = fromUserAvatar;
        this.relatedPostId = relatedPostId;
        this.relatedPostTitle = relatedPostTitle;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getActionUrl() {
        return actionUrl;
    }

    public void setActionUrl(String actionUrl) {
        this.actionUrl = actionUrl;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public UUID getFromUserId() {
        return fromUserId;
    }

    public void setFromUserId(UUID fromUserId) {
        this.fromUserId = fromUserId;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public String getFromUserAvatar() {
        return fromUserAvatar;
    }

    public void setFromUserAvatar(String fromUserAvatar) {
        this.fromUserAvatar = fromUserAvatar;
    }

    public UUID getRelatedPostId() {
        return relatedPostId;
    }

    public void setRelatedPostId(UUID relatedPostId) {
        this.relatedPostId = relatedPostId;
    }

    public String getRelatedPostTitle() {
        return relatedPostTitle;
    }

    public void setRelatedPostTitle(String relatedPostTitle) {
        this.relatedPostTitle = relatedPostTitle;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
