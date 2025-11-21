package ai.youthmind.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "moderation_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ModerationLog {
    
    public enum EntityType {
        POST, COMMENT, USER, MESSAGE
    }
    
    public enum ActionTaken {
        FLAGGED, MASKED, REMOVED, APPROVED, ESCALATED
    }
    
    public enum AppealStatus {
        NONE, PENDING, APPROVED, REJECTED
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    @NotNull
    private EntityType entityType;
    
    @Column(name = "entity_id", nullable = false)
    @NotNull
    private UUID entityId;
    
    @Column(name = "user_id")
    private UUID userId;
    
    @Column(name = "moderation_layer", nullable = false, length = 50)
    @NotNull
    private String moderationLayer;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "action_taken", nullable = false)
    @NotNull
    private ActionTaken actionTaken;
    
    @Column(name = "confidence_score", precision = 5, scale = 4)
    private BigDecimal confidenceScore;
    
    @Column(name = "reason_code", length = 50)
    private String reasonCode;
    
    @Column(name = "reason_details")
    private String reasonDetails;
    
    @Column(name = "moderator_id")
    private UUID moderatorId;
    
    @Column(name = "automated", nullable = false)
    private Boolean automated = true;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "appeal_status", nullable = false)
    private AppealStatus appealStatus = AppealStatus.NONE;
    
    @Column(name = "metadata", columnDefinition = "jsonb")
    private String metadata;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
    
    // Utility methods
    public boolean isAutomated() {
        return Boolean.TRUE.equals(automated);
    }
    
    public boolean hasAppeal() {
        return appealStatus != AppealStatus.NONE;
    }
    
    public boolean isResolved() {
        return resolvedAt != null;
    }
}
