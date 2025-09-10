package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "user_moderation_status")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserModerationStatus {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ModerationStatus status = ModerationStatus.CLEAN;
    
    @Column(name = "warning_level", nullable = false)
    private Integer warningLevel = 0;
    
    @Column(name = "last_warning_at")
    private LocalDateTime lastWarningAt;
    
    @Column(name = "last_moderation_at")
    private LocalDateTime lastModerationAt;
    
    @Column(name = "total_warnings", nullable = false)
    private Integer totalWarnings = 0;
    
    @Column(name = "total_bans", nullable = false)
    private Integer totalBans = 0;
    
    @Column(name = "auto_moderation_enabled", nullable = false)
    private Boolean autoModerationEnabled = true;
    
    @Column(name = "manual_review_required", nullable = false)
    private Boolean manualReviewRequired = false;
    
    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;
    
    public enum ModerationStatus {
        CLEAN, WATCHED, WARNED, RESTRICTED, BANNED
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
