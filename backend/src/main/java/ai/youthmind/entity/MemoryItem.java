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
@Table(name = "memory_items")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class MemoryItem {
    
    public enum MemoryType {
        CHAT_CONTEXT, USER_PREFERENCE, ENGAGEMENT_PATTERN, 
        PERSONALITY_TRAIT, CONVERSATION_TOPIC, EMOTIONAL_STATE,
        BEHAVIORAL_PATTERN, INTERACTION_HISTORY
    }
    
    public enum ImportanceLevel {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    @NotNull
    private UUID userId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "memory_type", nullable = false)
    @NotNull
    private MemoryType memoryType;
    
    @Column(name = "key_identifier", nullable = false, length = 255)
    @NotNull
    private String keyIdentifier;
    
    @Column(name = "content", columnDefinition = "TEXT", nullable = false)
    @NotNull
    private String content;
    
    @Column(name = "compressed_content", columnDefinition = "TEXT")
    private String compressedContent;
    
    @Column(name = "context_tags", columnDefinition = "jsonb")
    private String contextTags;
    
    @Column(name = "weight_score", precision = 10, scale = 6, nullable = false)
    private BigDecimal weightScore = BigDecimal.ZERO;
    
    @Column(name = "base_weight", precision = 5, scale = 4, nullable = false)
    private BigDecimal baseWeight = BigDecimal.ONE;
    
    @Column(name = "time_decay_factor", precision = 5, scale = 4, nullable = false)
    private BigDecimal timeDecayFactor = BigDecimal.ONE;
    
    @Column(name = "engagement_multiplier", precision = 5, scale = 4, nullable = false)
    private BigDecimal engagementMultiplier = BigDecimal.ONE;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "importance_level", nullable = false)
    private ImportanceLevel importanceLevel = ImportanceLevel.MEDIUM;
    
    @Column(name = "access_count", nullable = false)
    private Integer accessCount = 0;
    
    @Column(name = "last_accessed_at")
    private LocalDateTime lastAccessedAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "related_entity_id")
    private UUID relatedEntityId;
    
    @Column(name = "related_entity_type", length = 50)
    private String relatedEntityType;
    
    @Column(name = "source_context", columnDefinition = "TEXT")
    private String sourceContext;
    
    @Column(name = "embedding_vector", columnDefinition = "vector(384)")
    private String embeddingVector; // For semantic search
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @Column(name = "archived_at")
    private LocalDateTime archivedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        lastAccessedAt = LocalDateTime.now();
        
        // Set default expiration based on memory type
        if (expiresAt == null) {
            switch (memoryType) {
                case CHAT_CONTEXT:
                    expiresAt = LocalDateTime.now().plusDays(30);
                    break;
                case USER_PREFERENCE:
                    expiresAt = LocalDateTime.now().plusDays(365);
                    break;
                case ENGAGEMENT_PATTERN:
                    expiresAt = LocalDateTime.now().plusDays(90);
                    break;
                case PERSONALITY_TRAIT:
                    expiresAt = LocalDateTime.now().plusDays(180);
                    break;
                default:
                    expiresAt = LocalDateTime.now().plusDays(60);
            }
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Utility methods
    public void incrementAccess() {
        this.accessCount++;
        this.lastAccessedAt = LocalDateTime.now();
    }
    
    public boolean isExpired() {
        return expiresAt != null && LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isArchived() {
        return archivedAt != null;
    }
    
    public void archive() {
        this.archivedAt = LocalDateTime.now();
    }
    
    public BigDecimal calculateCurrentWeight() {
        // Apply time decay: weight = baseWeight * exp(-Δt/τ)
        long daysSinceCreation = java.time.temporal.ChronoUnit.DAYS.between(createdAt, LocalDateTime.now());
        double tau = 30.0; // 30 days decay constant
        double timeDecay = Math.exp(-daysSinceCreation / tau);
        
        return baseWeight
            .multiply(BigDecimal.valueOf(timeDecay))
            .multiply(engagementMultiplier)
            .multiply(timeDecayFactor);
    }
}
