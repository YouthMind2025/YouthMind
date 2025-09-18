package ai.youthmind.repository;

import ai.youthmind.entity.CommentReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for comment report operations.
 */
@Repository
public interface CommentReportRepository extends JpaRepository<CommentReport, UUID> {
    
    /**
     * Find reports by comment ID
     */
    List<CommentReport> findByCommentIdOrderByCreatedAtDesc(UUID commentId);
    
    /**
     * Find reports by reporter ID
     */
    Page<CommentReport> findByReporterIdOrderByCreatedAtDesc(UUID reporterId, Pageable pageable);
    
    /**
     * Find reports by status
     */
    Page<CommentReport> findByStatusOrderByCreatedAtDesc(CommentReport.ReportStatus status, Pageable pageable);
    
    /**
     * Find reports by reason
     */
    List<CommentReport> findByReportReasonOrderByCreatedAtDesc(CommentReport.ReportReason reason);
    
    /**
     * Check if user has already reported a comment
     */
    boolean existsByCommentIdAndReporterId(UUID commentId, UUID reporterId);
    
    /**
     * Count reports for a specific comment
     */
    @Query("SELECT COUNT(cr) FROM CommentReport cr WHERE cr.commentId = :commentId")
    long countByCommentId(@Param("commentId") UUID commentId);
    
    /**
     * Count reports by status
     */
    @Query("SELECT COUNT(cr) FROM CommentReport cr WHERE cr.status = :status")
    long countByStatus(@Param("status") CommentReport.ReportStatus status);
    
    /**
     * Find reports created within date range
     */
    List<CommentReport> findByCreatedAtBetweenOrderByCreatedAtDesc(LocalDateTime startDate, LocalDateTime endDate);
    
    /**
     * Find reports by comment ID and status
     */
    List<CommentReport> findByCommentIdAndStatusOrderByCreatedAtDesc(UUID commentId, CommentReport.ReportStatus status);
}
