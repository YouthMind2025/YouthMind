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
@Table(name = "content_safety_cache")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ContentSafetyCache {
    
    public enum ContentType {
        POST, COMMENT, MESSAGE, PROFILE_BIO, USERNAME
    }
    
    public enum ModerationResult {
        SAFE, FLAGGED, BLOCKED, NEEDS_REVIEW
    }
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "content_hash", nullable = false, unique = true, length = 64)
    @NotNull
    private String contentHash;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false)
    @NotNull
    private ContentType contentType;
    
    @Column(name = "safety_score", nullable = false, precision = 5, scale = 4)
    @NotNull
    private BigDecimal safetyScore;
    
    @Column(name = "categories", columnDefinition = "jsonb")
    private String categories;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "moderation_result", nullable = false)
    @NotNull
    private ModerationResult moderationResult;
    
    @Column(name = "expires_at", nullable = false)
    @NotNull
    private LocalDateTime expiresAt;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        
        // Set default expiration to 7 days from now
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusDays(7);
        }
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Utility methods
    public boolean isExpired() {
        return LocalDateTime.now().isAfter(expiresAt);
    }
    
    public boolean isSafe() {
        return moderationResult == ModerationResult.SAFE;
    }
    
    public boolean needsReview() {
        return moderationResult == ModerationResult.NEEDS_REVIEW;
    }
    
    public boolean isBlocked() {
        return moderationResult == ModerationResult.BLOCKED;
    }
}
