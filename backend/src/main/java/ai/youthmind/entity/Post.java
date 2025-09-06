package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "posts")
@SQLDelete(sql = "UPDATE posts SET deleted_at = CURRENT_TIMESTAMP WHERE id = ?")
@Where(clause = "deleted_at IS NULL")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    public enum Emotion {
        HAPPY, SAD, ANGRY, ANXIOUS, EXCITED, CALM, 
        FRUSTRATED, HOPEFUL, LONELY, GRATEFUL, CONFUSED, 
        OVERWHELMED, CONTENT, WORRIED, PROUD, DISAPPOINTED
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull(message = "User ID is required")
    private UUID userId;
    
    @Column(name = "title", nullable = false)
    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    @NotBlank(message = "Content is required")
    @Size(max = 10000, message = "Content must not exceed 10000 characters")
    private String content;
    
    @Column(name = "emotion_tag", length = 100)
    @Deprecated // Keep for backward compatibility during migration
    private String emotionTag;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "emotion")
    private Emotion emotion;
    
    @Column(name = "topics", columnDefinition = "TEXT")
    @Size(max = 1000, message = "Topics must not exceed 1000 characters")
    private String topics;
    
    @Column(name = "pre_img")
    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String preImg;
    
    @Column(name = "likes", nullable = false)
    @Min(value = 0, message = "Likes count cannot be negative")
    private Integer likes = 0;
    
    @Column(name = "bookmarks", nullable = false)
    @Min(value = 0, message = "Bookmarks count cannot be negative")
    private Integer bookmarks = 0;
    
    @Column(name = "views", nullable = false)
    @Min(value = 0, message = "Views count cannot be negative")
    private Integer views = 0;
    
    @Column(name = "resonance_count", nullable = false)
    @Min(value = 0, message = "Resonance count cannot be negative")
    private Integer resonanceCount = 0;
    
    @Column(name = "collect_count", nullable = false)
    @Min(value = 0, message = "Collect count cannot be negative")
    private Integer collectCount = 0;
    
    @Column(name = "ai_metadata", columnDefinition = "jsonb") 
    private String aiMetadata;

    @Column(name = "flagged", nullable = false)
    private Boolean flagged = false;

    @Column(name = "flag_reason")
    @Size(max = 500, message = "Flag reason must not exceed 500 characters")
    private String flagReason;

    @Column(name = "flagged_at")
    private LocalDateTime flaggedAt;
    
    @Column(name = "is_anonymous", nullable = false)
    private Boolean isAnonymous = false;
    
    @Column(name = "version", nullable = false)
    @Version
    private Integer version = 1;
    
    @Column(name = "last_modified_by")
    private UUID lastModifiedBy;
    
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Explicit getters and setters to ensure they work
    public UUID getId() {
        return id;
    }
    
    public void setId(UUID id) {
        this.id = id;
    }
    
    public UUID getUserId() {
        return userId;
    }
    
    public void setUserId(UUID userId) {
        this.userId = userId;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getEmotionTag() {
        return emotionTag;
    }
    
    public void setEmotionTag(String emotionTag) {
        this.emotionTag = emotionTag;
    }
    
    public String getTopics() {
        return topics;
    }
    
    public void setTopics(String topics) {
        this.topics = topics;
    }
    
    public String getPreImg() {
        return preImg;
    }
    
    public void setPreImg(String preImg) {
        this.preImg = preImg;
    }
    
    public Integer getLikes() {
        return likes;
    }
    
    public void setLikes(Integer likes) {
        this.likes = likes;
    }
    
    public Integer getBookmarks() {
        return bookmarks;
    }
    
    public void setBookmarks(Integer bookmarks) {
        this.bookmarks = bookmarks;
    }
    
    public Integer getViews() {
        return views;
    }
    
    public void setViews(Integer views) {
        this.views = views;
    }
    
    public String getAiMetadata() {
        return aiMetadata;
    }
    
    public void setAiMetadata(String aiMetadata) {
        this.aiMetadata = aiMetadata;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isFlagged() {
        return flagged;
    }

    public void setFlagged(boolean flagged) {
        this.flagged = flagged;
    }

    public String getFlagReason() {
        return flagReason;
    }

    public void setFlagReason(String flagReason) {
        this.flagReason = flagReason;
    }

    public LocalDateTime getFlaggedAt() {
        return flaggedAt;
    }

    public void setFlaggedAt(LocalDateTime flaggedAt) {
        this.flaggedAt = flaggedAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
} 