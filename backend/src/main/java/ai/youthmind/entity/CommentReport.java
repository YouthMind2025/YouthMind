package ai.youthmind.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity for tracking comment reports submitted by users.
 * Used for moderation and content policy enforcement.
 */
@Entity
@Table(name = "comment_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentReport {
    
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    
    @Column(name = "comment_id", nullable = false)
    private UUID commentId;
    
    @Column(name = "reporter_id", nullable = false)
    private UUID reporterId;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "report_reason", nullable = false)
    private ReportReason reportReason;
    
    @Column(name = "description", columnDefinition = "TEXT")
    private String description;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ReportStatus status = ReportStatus.PENDING;
    
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
    
    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;
    
    @Column(name = "resolved_by")
    private UUID resolvedBy;
    
    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;
    
    public enum ReportReason {
        SPAM,
        HARASSMENT,
        HATE_SPEECH,
        INAPPROPRIATE_CONTENT,
        VIOLENCE,
        MISINFORMATION,
        COPYRIGHT_VIOLATION,
        OTHER
    }
    
    public enum ReportStatus {
        PENDING,
        UNDER_REVIEW,
        RESOLVED,
        DISMISSED
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
