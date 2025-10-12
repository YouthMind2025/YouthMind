package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
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
    
    @Column(name = "title", nullable = false, length = 255)
    private String title;
    
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;
    
    @Column(name = "emotion", length = 100)
    private String emotion;
    
    @Column(name = "ai_emotion", length = 100)
    @Size(max = 100, message = "AI emotion must not exceed 100 characters")
    private String aiEmotion;
    
    @Column(name = "emotion_confidence")
    @DecimalMin(value = "0.0", message = "Emotion confidence must be at least 0.0")
    @DecimalMax(value = "1.0", message = "Emotion confidence must be at most 1.0")
    private Double emotionConfidence;
    
    @Column(name = "emotion_source", length = 50)
    @Size(max = 50, message = "Emotion source must not exceed 50 characters")
    private String emotionSource = "user";
    
    @Column(name = "pre_img", length = 500)
    private String preImg;
    
    @Column(name = "resonance", nullable = false)
    private int resonance = 0;
    
    @Column(name = "view_count", nullable = false)
    private int view = 0;
    
    @Column(name = "collect_count", nullable = false)
    private int collect = 0;
    
    @Column(name = "topics", columnDefinition = "TEXT")
    private String topics;
    
    @Column(name = "ai_metadata", columnDefinition = "TEXT") 
    private String aiMetadata;

    @Column(name = "flagged", nullable = false)
    private boolean flagged = false;

    @Column(name = "flag_reason")
    private String flagReason;

    @Column(name = "flagged_at")
    private LocalDateTime flaggedAt;
    
    // Enhanced moderation fields
    @Column(name = "masked_content", columnDefinition = "TEXT")
    private String maskedContent;
    
    @Column(name = "moderation_layer", length = 50)
    private String moderationLayer;
    
    @Column(name = "moderation_confidence", precision = 5, scale = 4)
    private BigDecimal moderationConfidence = BigDecimal.ZERO;
    
    @Column(name = "moderation_details", columnDefinition = "jsonb")
    private String moderationDetails;
    
    @Column(name = "content_hash", length = 64)
    private String contentHash;
    
    @Column(name = "original_content_backup", columnDefinition = "TEXT")
    private String originalContentBackup;
    
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
} 