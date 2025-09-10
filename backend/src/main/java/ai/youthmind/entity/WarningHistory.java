package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "warning_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WarningHistory {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "warning_type", nullable = false)
    private WarningType warningType;
    
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false)
    private Severity severity = Severity.MEDIUM;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "moderator_id")
    private User moderator;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;
    
    @Column(name = "comment_id")
    private UUID commentId;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;
    
    @Column(name = "acknowledged_at")
    private LocalDateTime acknowledgedAt;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "acknowledged_by")
    private User acknowledgedBy;
    
    public enum WarningType {
        SPAM, HARASSMENT, INAPPROPRIATE_CONTENT, HATE_SPEECH, VIOLENCE, OTHER
    }
    
    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
