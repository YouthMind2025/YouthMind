package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "posts")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Post {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "emotion", length = 100)
    private String emotion;
    
    @Column(name = "title", length = 255)
    private String title;
    
    @Column(name = "pre_img", length = 500)
    private String preImg;
    
    @Column(name = "resonance", nullable = false)
    private int resonance = 0;
    
    @Column(name = "view_count", nullable = false)
    private int viewCount = 0;
    
    @Column(name = "collect_count", nullable = false)
    private int collectCount = 0;
    
    @Column(name = "ai_metadata", columnDefinition = "TEXT") 
    private String aiMetadata;

    @Column(name = "flagged", nullable = false)
    private boolean flagged = false;

    @Column(name = "flag_reason")
    private String flagReason;

    @Column(name = "flagged_at")
    private LocalDateTime flaggedAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
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
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getEmotion() {
        return emotion;
    }
    
    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getPreImg() {
        return preImg;
    }
    
    public void setPreImg(String preImg) {
        this.preImg = preImg;
    }
    
    public int getResonance() {
        return resonance;
    }
    
    public void setResonance(int resonance) {
        this.resonance = resonance;
    }
    
    public int getViewCount() {
        return viewCount;
    }
    
    public void setViewCount(int viewCount) {
        this.viewCount = viewCount;
    }
    
    public int getCollectCount() {
        return collectCount;
    }
    
    public void setCollectCount(int collectCount) {
        this.collectCount = collectCount;
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
} 