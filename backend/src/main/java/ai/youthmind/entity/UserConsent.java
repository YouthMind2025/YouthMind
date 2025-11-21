package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Entity representing user consent for content with masked words.
 * Tracks consent requests, decisions, and final content.
 */
@Entity
@Table(name = "user_consent")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserConsent {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "user_id", nullable = false)
    private UUID userId;
    
    @Column(name = "content_id", nullable = false)
    private UUID contentId;
    
    @Column(name = "content_type", nullable = false)
    private String contentType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false)
    private ConsentType consentType;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ConsentStatus status = ConsentStatus.PENDING;
    
    @Column(name = "original_content")
    private String originalContent;
    
    @Column(name = "masked_content")
    private String maskedContent;
    
    @Column(name = "final_content")
    private String finalContent;
    
    @Column(name = "masking_strategy")
    private String maskingStrategy;
    
    @Column(name = "masking_options")
    private String maskingOptions; // JSON string of masking options
    
    @Column(name = "requested_at", nullable = false)
    private LocalDateTime requestedAt;
    
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;
    
    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;
    
    @Column(name = "notes")
    private String notes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum ConsentType {
        MASKED_CONTENT,
        PROFANITY_CONTENT,
        SENSITIVE_CONTENT,
        AI_MODERATED_CONTENT
    }
    
    public enum ConsentStatus {
        PENDING,
        ACCEPTED,
        REJECTED,
        MODIFIED,
        EXPIRED
    }
    
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
