package ai.youthmind.repository;

import ai.youthmind.entity.WarningHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for managing warning history records.
 */
@Repository
public interface WarningHistoryRepository extends JpaRepository<WarningHistory, UUID> {
    
    /**
     * Find all warnings for a specific user, ordered by creation date (newest first).
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.user.id = :userId ORDER BY w.createdAt DESC")
    List<WarningHistory> findByUserIdOrderByCreatedAtDesc(@Param("userId") UUID userId);
    
    /**
     * Find active warnings for a user (not expired).
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.user.id = :userId AND w.expiresAt > :now ORDER BY w.createdAt DESC")
    List<WarningHistory> findByUserIdAndExpiresAtAfterOrderByCreatedAtDesc(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    /**
     * Find warnings by user and warning type.
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.user.id = :userId AND w.warningType = :warningType ORDER BY w.createdAt DESC")
    List<WarningHistory> findByUserIdAndWarningTypeOrderByCreatedAtDesc(@Param("userId") UUID userId, @Param("warningType") WarningHistory.WarningType warningType);
    
    /**
     * Find warnings by user and severity.
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.user.id = :userId AND w.severity = :severity ORDER BY w.createdAt DESC")
    List<WarningHistory> findByUserIdAndSeverityOrderByCreatedAtDesc(@Param("userId") UUID userId, @Param("severity") WarningHistory.Severity severity);
    
    /**
     * Find warnings issued by a specific moderator.
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.moderatorId = :moderatorId ORDER BY w.createdAt DESC")
    List<WarningHistory> findByModeratorIdOrderByCreatedAtDesc(@Param("moderatorId") UUID moderatorId);
    
    /**
     * Find warnings for a specific post.
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.postId = :postId ORDER BY w.createdAt DESC")
    List<WarningHistory> findByPostIdOrderByCreatedAtDesc(@Param("postId") UUID postId);
    
    /**
     * Find warnings for a specific comment.
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.commentId = :commentId ORDER BY w.createdAt DESC")
    List<WarningHistory> findByCommentIdOrderByCreatedAtDesc(@Param("commentId") UUID commentId);
    
    /**
     * Count total warnings for a user.
     */
    @Query("SELECT COUNT(w) FROM WarningHistory w WHERE w.user.id = :userId")
    long countByUserId(@Param("userId") UUID userId);
    
    /**
     * Count active warnings for a user (not expired).
     */
    @Query("SELECT COUNT(w) FROM WarningHistory w WHERE w.user.id = :userId AND w.expiresAt > :now")
    long countByUserIdAndExpiresAtAfter(@Param("userId") UUID userId, @Param("now") LocalDateTime now);
    
    /**
     * Count warnings by type for a user.
     */
    @Query("SELECT COUNT(w) FROM WarningHistory w WHERE w.user.id = :userId AND w.warningType = :warningType")
    long countByUserIdAndWarningType(@Param("userId") UUID userId, @Param("warningType") WarningHistory.WarningType warningType);
    
    /**
     * Count warnings by severity for a user.
     */
    @Query("SELECT COUNT(w) FROM WarningHistory w WHERE w.user.id = :userId AND w.severity = :severity")
    long countByUserIdAndSeverity(@Param("userId") UUID userId, @Param("severity") WarningHistory.Severity severity);
    
    /**
     * Find warnings created within a date range.
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.createdAt BETWEEN :startDate AND :endDate ORDER BY w.createdAt DESC")
    List<WarningHistory> findWarningsByDateRange(@Param("startDate") LocalDateTime startDate, 
                                                @Param("endDate") LocalDateTime endDate);
    
    /**
     * Find unacknowledged warnings for a user.
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.user.id = :userId AND w.acknowledgedAt IS NULL ORDER BY w.createdAt DESC")
    List<WarningHistory> findByUserIdAndAcknowledgedAtIsNullOrderByCreatedAtDesc(@Param("userId") UUID userId);
    
    /**
     * Find acknowledged warnings for a user.
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.user.id = :userId AND w.acknowledgedAt IS NOT NULL ORDER BY w.createdAt DESC")
    List<WarningHistory> findByUserIdAndAcknowledgedAtIsNotNullOrderByCreatedAtDesc(@Param("userId") UUID userId);
    
    /**
     * Find warnings that are about to expire (within next 7 days).
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.expiresAt BETWEEN :now AND :expiryThreshold AND w.acknowledgedAt IS NULL ORDER BY w.expiresAt ASC")
    List<WarningHistory> findWarningsExpiringSoon(@Param("now") LocalDateTime now, 
                                                 @Param("expiryThreshold") LocalDateTime expiryThreshold);
    
    /**
     * Find expired warnings that haven't been cleaned up.
     */
    @Query("SELECT w FROM WarningHistory w WHERE w.expiresAt < :now ORDER BY w.expiresAt ASC")
    List<WarningHistory> findExpiredWarnings(@Param("now") LocalDateTime now);
}
