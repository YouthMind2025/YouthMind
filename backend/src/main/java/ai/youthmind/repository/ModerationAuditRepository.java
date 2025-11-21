package ai.youthmind.repository;

import ai.youthmind.entity.ModerationAudit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Repository for ModerationAudit entity
 */
@Repository
public interface ModerationAuditRepository extends JpaRepository<ModerationAudit, UUID> {
    
    /**
     * Find moderation audits by content ID and type, ordered by timestamp descending
     */
    List<ModerationAudit> findByContentIdAndContentTypeOrderByTimestampDesc(UUID contentId, String contentType);
    
    /**
     * Find moderation audits by user ID and timestamp range, ordered by timestamp descending
     */
    List<ModerationAudit> findByUserIdAndTimestampAfterOrderByTimestampDesc(UUID userId, LocalDateTime timestamp);
    
    /**
     * Find moderation audits by audit type and timestamp range
     */
    List<ModerationAudit> findByAuditTypeAndTimestampAfterOrderByTimestampDesc(String auditType, LocalDateTime timestamp);
    
    /**
     * Find moderation audits by decision and timestamp range
     */
    List<ModerationAudit> findByDecisionAndTimestampAfterOrderByTimestampDesc(String decision, LocalDateTime timestamp);
    
    /**
     * Count moderation audits by user ID and timestamp range
     */
    long countByUserIdAndTimestampAfter(UUID userId, LocalDateTime timestamp);
    
    /**
     * Count moderation audits by decision and timestamp range
     */
    long countByDecisionAndTimestampAfter(String decision, LocalDateTime timestamp);
    
    /**
     * Find moderation audits by escalation level
     */
    List<ModerationAudit> findByEscalationLevelOrderByTimestampDesc(Integer escalationLevel);
    
    /**
     * Find moderation audits by action taken
     */
    List<ModerationAudit> findByActionTakenOrderByTimestampDesc(String actionTaken);
    
    /**
     * Find moderation audits by AI model used
     */
    List<ModerationAudit> findByAiModelUsedOrderByTimestampDesc(String aiModelUsed);
    
    /**
     * Find moderation audits with cache hits
     */
    List<ModerationAudit> findByCacheHitTrueOrderByTimestampDesc();
    
    /**
     * Get moderation statistics for a user within a time range
     */
    @Query("SELECT " +
           "COUNT(ma) as totalDecisions, " +
           "SUM(CASE WHEN ma.decision = 'APPROVED' THEN 1 ELSE 0 END) as approvedDecisions, " +
           "SUM(CASE WHEN ma.decision = 'REJECTED' THEN 1 ELSE 0 END) as rejectedDecisions, " +
           "AVG(ma.confidence) as averageConfidence " +
           "FROM ModerationAudit ma " +
           "WHERE ma.userId = :userId AND ma.timestamp >= :startDate")
    Object[] getModerationStatistics(@Param("userId") UUID userId, @Param("startDate") LocalDateTime startDate);
    
    /**
     * Get moderation statistics by audit type
     */
    @Query("SELECT " +
           "ma.auditType, " +
           "COUNT(ma) as totalDecisions, " +
           "SUM(CASE WHEN ma.decision = 'APPROVED' THEN 1 ELSE 0 END) as approvedDecisions, " +
           "SUM(CASE WHEN ma.decision = 'REJECTED' THEN 1 ELSE 0 END) as rejectedDecisions, " +
           "AVG(ma.confidence) as averageConfidence " +
           "FROM ModerationAudit ma " +
           "WHERE ma.timestamp >= :startDate " +
           "GROUP BY ma.auditType")
    List<Object[]> getModerationStatisticsByAuditType(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Get moderation statistics by action taken
     */
    @Query("SELECT " +
           "ma.actionTaken, " +
           "COUNT(ma) as totalActions, " +
           "AVG(ma.processingTimeMs) as averageProcessingTime " +
           "FROM ModerationAudit ma " +
           "WHERE ma.timestamp >= :startDate AND ma.actionTaken IS NOT NULL " +
           "GROUP BY ma.actionTaken")
    List<Object[]> getModerationStatisticsByAction(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Get escalation statistics
     */
    @Query("SELECT " +
           "ma.escalationLevel, " +
           "COUNT(ma) as totalEscalations, " +
           "AVG(ma.confidence) as averageConfidence " +
           "FROM ModerationAudit ma " +
           "WHERE ma.escalationLevel IS NOT NULL AND ma.timestamp >= :startDate " +
           "GROUP BY ma.escalationLevel " +
           "ORDER BY ma.escalationLevel")
    List<Object[]> getEscalationStatistics(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Get cache performance statistics
     */
    @Query("SELECT " +
           "COUNT(ma) as totalRequests, " +
           "SUM(CASE WHEN ma.cacheHit = true THEN 1 ELSE 0 END) as cacheHits, " +
           "AVG(ma.processingTimeMs) as averageProcessingTime " +
           "FROM ModerationAudit ma " +
           "WHERE ma.timestamp >= :startDate")
    Object[] getCachePerformanceStatistics(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Get AI model performance statistics
     */
    @Query("SELECT " +
           "ma.aiModelUsed, " +
           "COUNT(ma) as totalRequests, " +
           "AVG(ma.confidence) as averageConfidence, " +
           "AVG(ma.processingTimeMs) as averageProcessingTime " +
           "FROM ModerationAudit ma " +
           "WHERE ma.aiModelUsed IS NOT NULL AND ma.timestamp >= :startDate " +
           "GROUP BY ma.aiModelUsed")
    List<Object[]> getAiModelPerformanceStatistics(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Find recent moderation audits for a content item
     */
    @Query("SELECT ma FROM ModerationAudit ma " +
           "WHERE ma.contentId = :contentId AND ma.contentType = :contentType " +
           "ORDER BY ma.timestamp DESC")
    List<ModerationAudit> findRecentModerationAudits(@Param("contentId") UUID contentId, 
                                                     @Param("contentType") String contentType);
    
    /**
     * Find moderation audits requiring human review
     */
    @Query("SELECT ma FROM ModerationAudit ma " +
           "WHERE ma.actionTaken = 'ESCALATE' AND ma.timestamp >= :startDate " +
           "ORDER BY ma.timestamp DESC")
    List<ModerationAudit> findModerationAuditsRequiringHumanReview(@Param("startDate") LocalDateTime startDate);
    
    /**
     * Delete old moderation audits (for data retention)
     */
    void deleteByTimestampBefore(LocalDateTime timestamp);
}
