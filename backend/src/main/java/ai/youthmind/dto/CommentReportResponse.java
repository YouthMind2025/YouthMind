package ai.youthmind.dto;

import ai.youthmind.entity.CommentReport;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for comment report operations.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommentReportResponse {
    
    private UUID id;
    private UUID commentId;
    private UUID reporterId;
    private CommentReport.ReportReason reportReason;
    private String description;
    private CommentReport.ReportStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime resolvedAt;
    private UUID resolvedBy;
    private String resolutionNotes;
    
    public CommentReportResponse(CommentReport report) {
        this.id = report.getId();
        this.commentId = report.getCommentId();
        this.reporterId = report.getReporterId();
        this.reportReason = report.getReportReason();
        this.description = report.getDescription();
        this.status = report.getStatus();
        this.createdAt = report.getCreatedAt();
        this.resolvedAt = report.getResolvedAt();
        this.resolvedBy = report.getResolvedBy();
        this.resolutionNotes = report.getResolutionNotes();
    }
}
