package ai.youthmind.entity;

import ai.youthmind.dto.ModerationResult;
import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking moderation audit trail and transparency
 */
@Entity
@Table(name = "moderation_audit")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ModerationAudit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "content_id", nullable = false)
    private UUID contentId;
    
    @Column(name = "content_type", nullable = false, length = 50)
    private String contentType; // "post" or "comment"
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "audit_type", nullable = false, length = 100)
    private String auditType; // "CONTEXT_WINDOW", "ESCALATION_LEVEL_1", etc.
    
    @Column(name = "decision", nullable = false, length = 20)
    private String decision; // "APPROVED" or "REJECTED"
    
    @Column(name = "confidence", nullable = false)
    private Double confidence;
    
    @Column(name = "reason", length = 1000)
    private String reason;
    
    @Column(name = "timestamp", nullable = false)
    private LocalDateTime timestamp;
    
    @Column(name = "moderation_result", columnDefinition = "TEXT")
    private String moderationResultJson; // Serialized ModerationResult
    
    @Column(name = "context_hash", length = 100)
    private String contextHash;
    
    @Column(name = "escalation_level")
    private Integer escalationLevel;
    
    @Column(name = "action_taken", length = 50)
    private String actionTaken; // "ALLOW", "HIDE", "SHADOW_BAN", etc.
    
    @Column(name = "processing_time_ms")
    private Long processingTimeMs;
    
    @Column(name = "ai_model_used", length = 100)
    private String aiModelUsed;
    
    @Column(name = "cache_hit")
    private Boolean cacheHit;
    
    @Column(name = "created_at", nullable = false, updatable = false)
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
