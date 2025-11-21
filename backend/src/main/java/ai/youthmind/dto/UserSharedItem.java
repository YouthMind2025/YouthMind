package ai.youthmind.dto;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO for individual user shared items
 */
public class UserSharedItem {
    private UUID id;
    private String shareType; // "EXTERNAL", "INTERNAL", "COPY_LINK"
    private UUID postId;
    private String postTitle;
    private String postPreview;
    private String shareTarget; // "FACEBOOK", "TWITTER", "COPY", etc.
    private LocalDateTime sharedAt;

    // Constructors
    public UserSharedItem() {}

    public UserSharedItem(UUID id, String shareType, UUID postId, String postTitle, 
                         String postPreview, String shareTarget, LocalDateTime sharedAt) {
        this.id = id;
        this.shareType = shareType;
        this.postId = postId;
        this.postTitle = postTitle;
        this.postPreview = postPreview;
        this.shareTarget = shareTarget;
        this.sharedAt = sharedAt;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getShareType() {
        return shareType;
    }

    public void setShareType(String shareType) {
        this.shareType = shareType;
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

    public String getShareTarget() {
        return shareTarget;
    }

    public void setShareTarget(String shareTarget) {
        this.shareTarget = shareTarget;
    }

    public LocalDateTime getSharedAt() {
        return sharedAt;
    }

    public void setSharedAt(LocalDateTime sharedAt) {
        this.sharedAt = sharedAt;
    }
}
