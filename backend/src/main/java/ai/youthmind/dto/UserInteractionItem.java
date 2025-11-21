package ai.youthmind.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for individual user interaction items
 */
public class UserInteractionItem {
    private UUID id;
    private String interactionType; // "RESONANCE", "COLLECT", "VIEW", "COMMENT"
    private UUID fromUserId;
    private String fromUserName;
    private String fromUserAvatar;
    private UUID postId;
    private String postTitle;
    private String postPreview;
    private LocalDateTime createdAt;

    // Constructors
    public UserInteractionItem() {}

    public UserInteractionItem(UUID id, String interactionType, UUID fromUserId, String fromUserName, 
                              String fromUserAvatar, UUID postId, String postTitle, String postPreview, 
                              LocalDateTime createdAt) {
        this.id = id;
        this.interactionType = interactionType;
        this.fromUserId = fromUserId;
        this.fromUserName = fromUserName;
        this.fromUserAvatar = fromUserAvatar;
        this.postId = postId;
        this.postTitle = postTitle;
        this.postPreview = postPreview;
        this.createdAt = createdAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getInteractionType() {
        return interactionType;
    }

    public void setInteractionType(String interactionType) {
        this.interactionType = interactionType;
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

    public UUID getPostId() {
        return postId;
    }

    public void setPostId(UUID postId) {
        this.postId = postId;
    }

    public String getPostTitle() {
        return postTitle;
    }

    public void setPostTitle(String postTitle) {
        this.postTitle = postTitle;
    }

    public String getPostPreview() {
        return postPreview;
    }

    public void setPostPreview(String postPreview) {
        this.postPreview = postPreview;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
